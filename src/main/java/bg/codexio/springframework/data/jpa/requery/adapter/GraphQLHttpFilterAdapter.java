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
public class GraphQLHttpFilterAdapter
        implements HttpFilterAdapter {
    private final ObjectMapper objectMapper;
    private final GraphQLComplexFilterAdapter graphQLComplexFilterAdapter;
    private static final Map<String, FilterOperation> OPERATION_MAP =
            Map.ofEntries(
            Map.entry(
                    "_gt",
                    FilterOperation.GT
            ),
            Map.entry(
                    "_gte",
                    FilterOperation.GTE
            ),
            Map.entry(
                    "_lt",
                    FilterOperation.LT
            ),
            Map.entry(
                    "_lte",
                    FilterOperation.LTE
            ),
            Map.entry(
                    "_in",
                    FilterOperation.IN
            ),
            Map.entry(
                    "_not_in",
                    FilterOperation.NOT_IN
            ),
            Map.entry(
                    "_contains",
                    FilterOperation.CONTAINS
            ),
            Map.entry(
                    "_starts_with",
                    FilterOperation.BEGINS_WITH
            ),
            Map.entry(
                    "_ends_with",
                    FilterOperation.ENDS_WITH
            ),
            Map.entry(
                    "_empty",
                    FilterOperation.EMPTY
            ),
            Map.entry(
                    "_not_empty",
                    FilterOperation.NOT_EMPTY
            ),
            Map.entry(
                    "_begins_with_caseins",
                    FilterOperation.BEGINS_WITH_CASEINS
            ),
            Map.entry(
                    "_ends_with_caseins",
                    FilterOperation.ENDS_WITH_CASEINS
            ),
            Map.entry(
                    "_contains_caseins",
                    FilterOperation.CONTAINS_CASEINS
            )
    );


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
        if (request.getMethod()
                   .equals("GET")) {
            return processGetRequest(request);
        } else if (request.getMethod()
                          .equals("POST")) {
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
                return this.graphQLComplexFilterAdapter.adapt(extractedFilter.get());
            }

            return new FilterRequestWrapper<>(parseGraphQLQuery(query));
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

            var jsonMap = this.objectMapper.readValue(
                    requestBody.toString(),
                    new TypeReference<Map<String, Object>>() {}
            );

            var query = (String) jsonMap.get("query");
            var extractedFilter = extractFilterBody(query);
            if (extractedFilter.isPresent()) {
                return this.graphQLComplexFilterAdapter.adapt(extractedFilter.get());
            }

            return new FilterRequestWrapper<>(parseGraphQLQuery(query));
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
        var operationDefinitionList =
                document.getDefinitionsOfType(OperationDefinition.class);
        for (var operation : operationDefinitionList) {
            if (!operation.getOperation()
                          .equals(OperationDefinition.Operation.QUERY)) {
                continue;
            }
            for (var field : operation.getSelectionSet()
                                      .getSelectionsOfType(Field.class)) {
                for (var argument : field.getArguments()) {
                    var value = argument.getValue();
                    var extractedValue = extractValue(value);
                    if (extractedValue instanceof Map) {
                        handleMapValue(
                                argument.getName(),
                                extractedValue,
                                filterRequests
                        );
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
            for (var entry :
                    ((Map<String, Object>) extractedValue).entrySet()) {
                var nestedValue = (Value<?>) entry.getValue();
                var nestedExtractedValue = extractValue(nestedValue);
                if (nestedExtractedValue instanceof Map) {
                    handleMapValue(
                            entry.getKey(),
                            nestedExtractedValue,
                            filterRequests
                    );
                }

                var filterRequest = new FilterRequest(
                        containingObjectName.concat(".")
                                            .concat(extractName(entry.getKey())),
                        nestedExtractedValue,
                        getOperationFromArgument(entry.getKey())
                );
                filterRequests.add(filterRequest);
            }
        }
    }

    private String extractName(String name) {
        return OPERATION_MAP.keySet()
                            .stream()
                            .filter(name::endsWith)
                            .findFirst()
                            .map(suffix -> name.substring(
                                    0,
                                    name.length() - suffix.length()
                            ))
                            .orElse(name);
    }


    private Object extractValue(Value<?> value) {
        return switch (value) {
            case StringValue sv -> sv.getValue();
            case IntValue iv -> iv.getValue();
            case BooleanValue bv -> bv.isValue();
            case FloatValue fv -> fv.getValue();
            case EnumValue ev -> ev.getName();
            case ObjectValue ov -> handleComplexObject(ov);
            case ArrayValue av -> av.getValues()
                                    .stream()
                                    .map(this::extractValue)
                                    .toList();
            default -> null;
        };
    }

    private Map<String, Object> handleComplexObject(ObjectValue value) {
        var result = new HashMap<String, Object>();
        for (var field : value.getObjectFields()) {
            result.put(
                    field.getName(),
                    field.getValue()
            );
        }
        return result;
    }

    private FilterOperation getOperationFromArgument(String argumentName) {
        return OPERATION_MAP.entrySet()
                            .stream()
                            .filter(entry -> argumentName.endsWith(entry.getKey()))
                            .findFirst()
                            .map(Map.Entry::getValue)
                            .orElse(FilterOperation.EQ);
    }


    private Optional<String> extractFilterBody(String query) {
        // Use regex to extract the contents of the filter argument
        var pattern = Pattern.compile(
                "filter\\s*:\\s*(\\{.*\\})",
                Pattern.DOTALL
        );
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

            if (query.charAt(i) == '"' && query.charAt(i - 1) != '\\'
                    && quoteCounter > 0) {
                started = true;
                quoteCounter--;
                continue;
            }

            if (query.charAt(i) == '"' && query.charAt(i - 1) != '\\'
                    && quoteCounter == 0) {
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
            }
        }
        return Optional.of(filter.toString());
    }
}
