package bg.codexio.springframework.data.jpa.requery.adapter;

import bg.codexio.springframework.data.jpa.requery.payload.FilterOperation;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequest;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequestWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.language.*;
import graphql.parser.Parser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            String query = request.getParameter("query");

            // Check if the query has a complex filter argument
            boolean isComplex = queryHasFilterArgument(query);

            if (isComplex) {
                // Extract the filter body from the query
                String filterBody = extractFilterBody(query);

                // Pass the filter body to the complex filter adapter
                return graphQLComplexFilterAdapter.adapt(filterBody);
            } else {
                // Simple filter handling
                List<FilterRequest> filterRequests = parseGraphQLQuery(query);
                return new FilterRequestWrapper<>(filterRequests);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> FilterRequestWrapper<T> processPostRequest(HttpServletRequest request) {
        try {
            StringBuilder requestBody = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }

            Map<String, Object> jsonMap = objectMapper.readValue(requestBody.toString(), Map.class);

            String query = (String) jsonMap.get("query");

            boolean isComplex = queryHasFilterArgument(query);

            if (isComplex) {
                String extractedFilter = extractFilterBody(query);
                return graphQLComplexFilterAdapter.adapt(extractedFilter);

            } else {
                List<FilterRequest> filterRequests = parseGraphQLQuery(query);
                return new FilterRequestWrapper<>(filterRequests);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new FilterRequestWrapper<>();
        }
    }

    private List<FilterRequest> parseGraphQLQuery(String query) {
        Parser parser = new Parser();
        Document document = parser.parseDocument(query);

        return processDocument(document);
    }

    private List<FilterRequest> processDocument(Document document) {
        List<FilterRequest> filterRequests = new ArrayList<>();
        List<OperationDefinition> operationDefinitionList = document.getDefinitionsOfType(OperationDefinition.class);
        for (OperationDefinition operation : operationDefinitionList) {
            if (!operation.getOperation().equals(OperationDefinition.Operation.QUERY)) {
                continue;
            }
            for (Field field : operation.getSelectionSet().getSelectionsOfType(Field.class)) {
                for (Argument argument : field.getArguments()) {
                    Value<?> value = argument.getValue();
                    Object extractedValue = extractValue(value);
                    if (extractedValue instanceof Map) {
                        handleMapValue(argument.getName(), extractedValue, filterRequests);
                        continue;
                    }
                    FilterRequest filterRequest = new FilterRequest(
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
                Value<?> nestedValue = (Value<?>) entry.getValue();
                Object nestedExtractedValue = extractValue(nestedValue);
                if (nestedExtractedValue instanceof Map) {
                    handleMapValue(entry.getKey(), nestedExtractedValue, filterRequests);
                }

                FilterRequest filterRequest = new FilterRequest(
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

        for (String suffix : suffixes) {
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
        Map<String, Object> result = new HashMap<>();
        for (ObjectField field : value.getObjectFields()) {
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


    //TODO: Has to be fixed!
    private boolean queryHasFilterArgument(String query) {
        return query.matches(".*\\b\\w+\\s*\\(.*filter\\s*:.*\\).*");
    }

    private String extractFilterBody(String query) {
        Parser parser = new Parser();
        Document document = parser.parseDocument(query);

        List<OperationDefinition> operationDefinitionList = document.getDefinitionsOfType(OperationDefinition.class);
        for (OperationDefinition operation : operationDefinitionList) {
            if (!operation.getOperation().equals(OperationDefinition.Operation.QUERY)) {
                continue;
            }
            for (Field field : operation.getSelectionSet().getSelectionsOfType(Field.class)) {
                for (Argument argument : field.getArguments()) {
                    if (argument.getName().equalsIgnoreCase("filter")) {
                        return String.valueOf(argument.getValue());
                    }
                }
            }
        }
        return "";
    }
}
