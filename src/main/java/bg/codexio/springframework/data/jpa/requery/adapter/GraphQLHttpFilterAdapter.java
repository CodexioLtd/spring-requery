package bg.codexio.springframework.data.jpa.requery.adapter;

import bg.codexio.springframework.data.jpa.requery.payload.FilterOperation;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequest;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequestWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.language.*;
import graphql.parser.Parser;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.regex.Pattern;

/**
 * A filter adapter that handles GraphQL requests and translates filter
 * arguments
 * into a {@link FilterRequestWrapper}. This adapter supports both simple and
 * complex filters,
 * leveraging {@link GraphQLComplexFilterAdapter} for complex filter handling.
 */
public class GraphQLHttpFilterAdapter
        implements HttpFilterAdapter {
    private final ObjectMapper objectMapper;
    private final GraphQLComplexFilterAdapter graphQLComplexFilterAdapter;
    private static final Map<String, FilterOperation> OPERATION_MAP =
            // Mapping of GraphQL suffixes to corresponding filter operations
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


    /**
     * Constructs a {@code GraphQLHttpFilterAdapter} with the specified
     * dependencies.
     *
     * @param objectMapper                the {@link ObjectMapper} for JSON
     *                                    processing
     * @param graphQLComplexFilterAdapter the adapter for handling complex
     *                                    GraphQL filters
     */
    public GraphQLHttpFilterAdapter(
            ObjectMapper objectMapper,
            GraphQLComplexFilterAdapter graphQLComplexFilterAdapter
    ) {
        this.objectMapper = objectMapper;
        this.graphQLComplexFilterAdapter = graphQLComplexFilterAdapter;
    }

    /**
     * Determines whether the given {@link HttpServletRequest} supports
     * GraphQL requests.
     * <p>
     * This method checks if the request URL contains the "/graphql" path,
     * which indicates
     * that the request is related to GraphQL operations.
     * </p>
     *
     * @param req the {@link HttpServletRequest} to evaluate
     * @return {@code true} if the request URL contains "/graphql", {@code
     * false} otherwise
     */
    @Override
    public boolean supports(HttpServletRequest req) {
        return req.getRequestURL()
                  .toString()
                  .contains("/graphql");
    }

    /**
     * Adapts the filter parameters from the given {@link HttpServletRequest}
     * into a {@link FilterRequestWrapper}.
     * It checks what the HTTP method of the request is and calls the
     * appropriate method based on it.
     *
     * @param request the HTTP servlet request containing filter parameters
     * @param <T>     the type of the result in the {@link HttpServletRequest}
     * @return a {@link FilterRequestWrapper} containing the adapted filter
     * requests
     */
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

    /**
     * Processes a GET request, extracting filter data from the query parameter.
     *
     * @param request the HTTP servlet request
     * @param <T>     the type of the result in the {@link HttpServletRequest}
     * @return a {@link FilterRequestWrapper} representing the filter
     */
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

    /**
     * Processes a POST request, extracting filter data from the request body.
     *
     * @param request the HTTP servlet request
     * @param <T>     the type of the result in the {@link HttpServletRequest}
     * @return a {@link FilterRequestWrapper} representing the filter
     */
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

    /**
     * Parses a GraphQL query and converts it into a list of
     * {@link FilterRequest}.
     *
     * @param query the GraphQL query string
     * @return a list of {@link FilterRequest} extracted from the query
     */
    private List<FilterRequest> parseGraphQLQuery(String query) {
        var parser = new Parser();
        var document = parser.parseDocument(query);

        return processDocument(document);
    }

    /**
     * Processes the GraphQL document, extracting filter arguments into
     * {@link FilterRequest}s.
     *
     * @param document the GraphQL document
     * @return a list of {@link FilterRequest} extracted from the document
     */
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

    /**
     * Recursively handles a map value, extracting nested values and creating
     * {@link FilterRequest} objects for each key-value pair.
     *
     * @param containingObjectName the name of the object containing this map
     * @param extractedValue       the value extracted from the map, expected
     *                             to be another map
     * @param filterRequests       the list of filter requests to be populated
     */
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

    /**
     * Extracts the base name of a field by removing the operation suffix
     * (e.g., "_gt", "_lte") using the {@code OPERATION_MAP}.
     *
     * @param name the field name with the possible operation suffix
     * @return the base field name without the operation suffix
     */
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

    /**
     * Extracts the value from a GraphQL {@link Value} object depending on
     * its type.
     * Supported types include String, Integer, Boolean, Float, Enum, Object,
     * and Array.
     *
     * @param value the {@link Value} object to extract from
     * @return the corresponding Java object representation of the value
     */
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

    /**
     * Processes a complex {@link ObjectValue}, extracting each field's name
     * and value
     * into a {@link Map}. This method is used to handle object-structured
     * values.
     *
     * @param value the {@link ObjectValue} to extract fields from
     * @return a map of field names to their corresponding values
     */
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

    /**
     * Determines the filter operation (e.g., EQ, GT, LTE) based on the
     * argument's suffix.
     * If no matching suffix is found in the {@code OPERATION_MAP}, it
     * defaults to {@link FilterOperation#EQ}.
     *
     * @param argumentName the argument name to analyze for an operation suffix
     * @return the corresponding {@link FilterOperation} or
     * {@link FilterOperation#EQ} if not found
     */
    private FilterOperation getOperationFromArgument(String argumentName) {
        return OPERATION_MAP.entrySet()
                            .stream()
                            .filter(entry -> argumentName.endsWith(entry.getKey()))
                            .findFirst()
                            .map(Map.Entry::getValue)
                            .orElse(FilterOperation.EQ);
    }

    /**
     * Extracts the body of a filter from a GraphQL query string using
     * regular expressions.
     * The method finds the part of the query that represents the filter and
     * returns it.
     *
     * @param query the full GraphQL query string
     * @return an {@link Optional} containing the extracted filter body, or
     * empty if not found
     */
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
