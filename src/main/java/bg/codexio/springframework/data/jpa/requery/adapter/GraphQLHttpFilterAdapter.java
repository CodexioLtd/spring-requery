package bg.codexio.springframework.data.jpa.requery.adapter;

import bg.codexio.springframework.data.jpa.requery.payload.FilterOperation;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequest;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequestWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.language.*;
import graphql.parser.Parser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class GraphQLHttpFilterAdapter implements HttpFilterAdapter {
    private final ObjectMapper objectMapper;
    private final GraphQLComplexFilterAdapter graphQLComplexFilterAdapter;

    public GraphQLHttpFilterAdapter(
            ObjectMapper objectMapper,
            GraphQLComplexFilterAdapter graphQLComplexFilterAdapter
    ) {
        this.objectMapper = objectMapper;
        this.graphQLComplexFilterAdapter = graphQLComplexFilterAdapter;
    }

    @Override
    public boolean supports(HttpServletRequest req) {
        //fix later
        return true;
    }

    @Override
    public <T> FilterRequestWrapper<T> adapt(HttpServletRequest request) {
        if (request.getMethod().equals("GET")) {
            return processGetRequest(request);
        } else if (request.getMethod().equals("POST")) {
            return processPostRequest(request);
        } else {
            return new FilterRequestWrapper<>();
        }
    }

    private <T> FilterRequestWrapper<T> processGetRequest(HttpServletRequest request) {
        try {
            var query = request.getParameter("query");

            var extractedFilter = extractFilterBody(query);
            if (extractedFilter.isPresent()) {
                return graphQLComplexFilterAdapter.adapt(extractedFilter.get());
            } else {
                return new FilterRequestWrapper<>(parseGraphQLQuery(query));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new FilterRequestWrapper<>();
        }
    }

    private <T> FilterRequestWrapper<T> processPostRequest(HttpServletRequest request) {
        try {
            var requestBody = new StringBuilder();
            var reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }

            var jsonMap = objectMapper.readValue(
                    requestBody.toString(),
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            var query = (String) jsonMap.get("query");
            var extractedFilter = extractFilterBody(query);
            if (extractedFilter.isPresent()) {
                return graphQLComplexFilterAdapter.adapt(extractedFilter.get());
            } else {
                return new FilterRequestWrapper<>(parseGraphQLQuery(query));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new FilterRequestWrapper<>();
        }
    }

    private List<FilterRequest> parseGraphQLQuery(String query) {
        var parser = new Parser();
        var document = parser.parseDocument(query);

        return processDocument(document);
    }

    private List<FilterRequest> processDocument(Document document) {
        var filterRequests = new ArrayList<FilterRequest>();
        var operationDefinitionList = document.getDefinitionsOfType(OperationDefinition.class);
        for (var operation : operationDefinitionList) {
            if (!operation.getOperation().equals(OperationDefinition.Operation.QUERY)) {
                continue;
            }
            for (var field : operation.getSelectionSet().getSelectionsOfType(Field.class)) {
                for (var argument : field.getArguments()) {
                    var value = argument.getValue();
                    var extractedValue = extractValue(value);
                    if (extractedValue instanceof Map) {
                        handleMapValue(argument.getName(), extractedValue, filterRequests);
                        continue;
                    }
                    var filterRequest = new FilterRequest(
                            extractName(argument.getName()),
                            extractedValue,
                            getOperationFromArgument(argument.getName())
                    );
                    filterRequests.add(filterRequest);
                }
            }
        }
        return filterRequests;
    }

    private void handleMapValue(
            String containingObjectName,
            Object extractedValue,
            List<FilterRequest> filterRequests
    ) {
        if (extractedValue instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) extractedValue).entrySet()) {
                var nestedValue = (Value<?>) entry.getValue();
                var nestedExtractedValue = extractValue(nestedValue);
                if (nestedExtractedValue instanceof Map) {
                    handleMapValue(entry.getKey(), nestedExtractedValue, filterRequests);
                }

                var filterRequest = new FilterRequest(
                        containingObjectName.concat(".").concat(extractName(entry.getKey())),
                        nestedExtractedValue,
                        getOperationFromArgument(entry.getKey())
                );
                filterRequests.add(filterRequest);
            }
        }
    }

    private String extractName(String name) {
        String[] suffixes = {
                "_gt",
                "_gte",
                "_lt",
                "_lte",
                "_in",
                "_not_in",
                "_contains",
                "_starts_with",
                "_ends_with",
                "_empty",
                "_not_empty",
                "_begins_with_caseins",
                "_ends_with_caseins",
                "_contains_caseins"
        };

        for (var suffix : suffixes) {
            if (name.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length());
            }
        }

        return name;
    }

    private Object extractValue(Value<?> value) {
        if (value instanceof StringValue) {
            return ((StringValue) value).getValue();
        } else if (value instanceof IntValue) {
            return ((IntValue) value).getValue();
        } else if (value instanceof BooleanValue) {
            return ((BooleanValue) value).isValue();
        } else if (value instanceof FloatValue) {
            return ((FloatValue) value).getValue();
        } else if (value instanceof EnumValue) {
            return ((EnumValue) value).getName();
        } else if (value instanceof ArrayValue) {
            return ((ArrayValue) value).getValues().stream().map(this::extractValue).toList();
        } else if (value instanceof ObjectValue) {
            return handleComplexObject((ObjectValue) value);
        } else {
            return null;
        }
    }

    private Map<String, Object> handleComplexObject(ObjectValue value) {
        var result = new HashMap<String, Object>();
        for (var field : value.getObjectFields()) {
            result.put(field.getName(), field.getValue());
        }
        return result;
    }

    private FilterOperation getOperationFromArgument(String argumentName) {
        if (argumentName.endsWith("_gt")) {
            return FilterOperation.GT;
        } else if (argumentName.endsWith("_gte")) {
            return FilterOperation.GTE;
        } else if (argumentName.endsWith("_lt")) {
            return FilterOperation.LT;
        } else if (argumentName.endsWith("_lte")) {
            return FilterOperation.LTE;
        } else if (argumentName.endsWith("_in")) {
            return FilterOperation.IN;
        } else if (argumentName.endsWith("_not_in")) {
            return FilterOperation.NOT_IN;
        } else if (argumentName.endsWith("_contains")) {
            return FilterOperation.CONTAINS;
        } else if (argumentName.endsWith("_starts_with")) {
            return FilterOperation.BEGINS_WITH;
        } else if (argumentName.endsWith("_ends_with")) {
            return FilterOperation.ENDS_WITH;
        } else if (argumentName.endsWith("_empty")) {
            return FilterOperation.EMPTY;
        } else if (argumentName.endsWith("_not_empty")) {
            return FilterOperation.NOT_EMPTY;
        } else if (argumentName.endsWith("_begins_with_caseins")) {
            return FilterOperation.BEGINS_WITH_CASEINS;
        } else if (argumentName.endsWith("_ends_with_caseins")) {
            return FilterOperation.ENDS_WITH_CASEINS;
        } else if (argumentName.endsWith("_contains_caseins")) {
            return FilterOperation.CONTAINS_CASEINS;
        }
        return FilterOperation.EQ;
    }

    private Optional<String> extractFilterBody(String query) {
        // Use regex to extract the contents of the filter argument
        var pattern = Pattern.compile("filter\\s*:\\s*(\\{.*\\})", Pattern.DOTALL);
        var matcher = pattern.matcher(query);

        if (!matcher.find()) {
            return Optional.empty();
        }
        query = matcher.group(1);
        var filter = new StringBuilder();
        var quoteCounter = 0;
        var braceCounter = 0;
        var started = false;
        for (var i = 0; i < query.length(); i++) {
            if (started && quoteCounter == 0 && braceCounter == 0) {
                break;
            }

            filter.append(query.charAt(i));

            if (query.charAt(i) == '"' && query.charAt(i - 1) != '\\' && quoteCounter > 0) {
                started = true;
                quoteCounter--;
                continue;
            }

            if (query.charAt(i) == '"' && query.charAt(i - 1) != '\\' && quoteCounter == 0) {
                started = true;
                quoteCounter++;
                continue;
            }

            if (query.charAt(i) == '{' && quoteCounter == 0) {
                started = true;
                braceCounter++;
                continue;
            }

            if (query.charAt(i) == '}' && quoteCounter == 0) {
                started = true;
                braceCounter--;
                continue;
            }
        }
        return Optional.of(filter.toString());
    }
}
