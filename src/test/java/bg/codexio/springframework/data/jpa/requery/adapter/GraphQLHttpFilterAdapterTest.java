package bg.codexio.springframework.data.jpa.requery.adapter;

import bg.codexio.springframework.data.jpa.requery.payload.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class GraphQLHttpFilterAdapterTest {

    private ObjectMapper objectMapper;
    private GraphQLComplexFilterAdapter graphQLComplexFilterAdapter;
    private GraphQLHttpFilterAdapter adapter;
    private HttpServletRequest request;

    private static FilterRequestWrapper<?> getMockSimpleFilterRequestWrapper() {
        FilterRequest nameFilterRequest = new FilterRequest(
                "name",
                "John",
                FilterOperation.EQ
        );

        FilterRequest zipFilterRequest = new FilterRequest(
                "address.zip",
                List.of("10001"),
                FilterOperation.IN
        );

        // Create the expected FilterRequestWrapper containing both filter requests
        FilterRequestWrapper<?> expectedWrapper = new FilterRequestWrapper<>(List.of(nameFilterRequest, zipFilterRequest));
        return expectedWrapper;
    }

    private static FilterRequestWrapper<Object> getComplexFilterRequestWrapper() {
        var groupOperations = new FilterRequest[]{
                new FilterRequest("email", "example.com", FilterOperation.CONTAINS)
        };
        var nonPriorityGroupOperators = new FilterLogicalOperator[]{
                FilterLogicalOperator.AND
        };

        // Create right side operands
        UnaryGroupRequest rightSideOperands = new UnaryGroupRequest(
                new FilterGroupRequest(
                        new FilterRequest[]{
                                new FilterRequest("firstName", List.of("John", "Vasko"), FilterOperation.IN),
                                new FilterRequest("lastName", "Doe", FilterOperation.BEGINS_WITH_CASEINS)
                        },
                        new FilterLogicalOperator[]{FilterLogicalOperator.OR},
                        new UnaryGroupRequest(
                                new FilterGroupRequest(
                                        new FilterRequest[]{
                                                new FilterRequest("age", 25, FilterOperation.GT)
                                        },
                                        new FilterLogicalOperator[0],
                                        null
                                ),
                                FilterLogicalOperator.AND
                        )
                ),
                FilterLogicalOperator.OR
        );

        FilterGroupRequest expectedFilterGroupRequest = new FilterGroupRequest(
                groupOperations,
                nonPriorityGroupOperators,
                rightSideOperands
        );

        // Create an expected FilterRequestWrapper with the complex filter group
        var expectedWrapper = new FilterRequestWrapper<>(expectedFilterGroupRequest);
        return expectedWrapper;
    }

    @BeforeEach
    void setUp() {
        objectMapper = mock(ObjectMapper.class);
        graphQLComplexFilterAdapter = mock(GraphQLComplexFilterAdapter.class);
        adapter = new GraphQLHttpFilterAdapter(objectMapper, graphQLComplexFilterAdapter);
        request = mock(HttpServletRequest.class);
    }

    @Test
    void testSupportsReturnsTrue() {
        boolean result = adapter.supports(mock(HttpServletRequest.class));

        assertTrue(result);
    }

    @Test
    void testAdaptOtherHttpMethod() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("PUT"); // Or any method other than GET or POST

        // Act
        FilterRequestWrapper<?> result = adapter.adapt(request);

        // Assert
        assertEquals(new FilterRequestWrapper<>(), result);
    }

    @Test
    void testAdaptGetRequestWithComplexQuery() throws JsonProcessingException {
        // Arrange
        String complexQuery = "query { users(filter: {\"groupOperations\": [{\"field\": \"email\", \"operation\": \"CONTAINS\", \"value\": \"example.com\"}], \"nonPriorityGroupOperators\": [\"AND\"], \"rightSideOperands\": {\"unaryGroupOperator\": \"OR\", \"unaryGroup\": {\"groupOperations\": [{\"field\": \"firstName\", \"operation\": \"IN\", \"value\": [\"John\", \"Vasko\"]}, {\"field\": \"lastName\", \"operation\": \"BEGINS_WITH_CASEINS\", \"value\": \"Doe\"}], \"nonPriorityGroupOperators\": [\"OR\"], \"rightSideOperands\": {\"unaryGroupOperator\": \"AND\", \"unaryGroup\": {\"groupOperations\": [{\"field\": \"age\", \"operation\": \"GT\", \"value\": 25}], \"nonPriorityGroupOperators\": []}}}}}) { id firstName lastName address { id city } } }";
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameter("query")).thenReturn(complexQuery);

        // Create a FilterRequest for what the complex filter is expected to generate
        FilterRequest expectedFilterRequest = new FilterRequest(
                "email",
                "example.com",
                FilterOperation.CONTAINS
        );

        var expectedWrapper = new FilterRequestWrapper<>(List.of(expectedFilterRequest));
        when(graphQLComplexFilterAdapter.adapt(anyString())).thenReturn(expectedWrapper);

        // Act
        FilterRequestWrapper<?> result = adapter.adapt(request);

        // Assert
        assertEquals(expectedWrapper, result);
    }

    @Test
    void testAdaptGetRequestWithSimpleQuery() {
        // Arrange
        String simpleQuery = "{ user(name: \"John\", address: { zip_in: [\"10001\"] }) { id name friends { id city } } }";
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameter("query")).thenReturn(simpleQuery);

        var expectedWrapper = getMockSimpleFilterRequestWrapper();

        var result = adapter.adapt(request);

        assertEquals(expectedWrapper, result);
    }

    @Test
    void testAdaptPostRequestWithComplexQuery() throws Exception {
        String requestBody = "{\"query\":\"users(filter: {\\\"groupOperations\\\": [{\\\"field\\\": \\\"email\\\", \\\"operation\\\": \\\"CONTAINS\\\", \\\"value\\\": \\\"example.com\\\"}], \\\"nonPriorityGroupOperators\\\": [\\\"AND\\\"], \\\"rightSideOperands\\\": {\\\"unaryGroupOperator\\\": \\\"OR\\\", \\\"unaryGroup\\\": {\\\"groupOperations\\\": [{\\\"field\\\": \\\"firstName\\\", \\\"operation\\\": \\\"IN\\\", \\\"value\\\": [\\\"John\\\", \\\"Vasko\\\"]}, {\\\"field\\\": \\\"lastName\\\", \\\"operation\\\": \\\"BEGINS_WITH_CASEINS\\\", \\\"value\\\": \\\"Doe\\\"]}], \\\"nonPriorityGroupOperators\\\": [\\\"OR\\\"], \\\"rightSideOperands\\\": {\\\"unaryGroupOperator\\\": \\\"AND\\\", \\\"unaryGroup\\\": {\\\"groupOperations\\\": [{\\\"field\\\": \\\"age\\\", \\\"operation\\\": \\\"GT\\\", \\\"value\\\": 25}], \\\"nonPriorityGroupOperators\\\": []}}}}}) { id firstName lastName address { id city } } \"}";

        when(request.getMethod()).thenReturn("POST");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("query", "users(filter: {\"groupOperations\": [{\"field\": \"email\", \"operation\": \"CONTAINS\", \"value\": \"example.com\"}], \"nonPriorityGroupOperators\": [\"AND\"], \"rightSideOperands\": {\"unaryGroupOperator\": \"OR\", \"unaryGroup\": {\"groupOperations\": [{\"field\": \"firstName\", \"operation\": \"IN\", \"value\": [\"John\", \"Vasko\"]}, {\"field\": \"lastName\", \"operation\": \"BEGINS_WITH_CASEINS\", \"value\": \"Doe\"}], \"nonPriorityGroupOperators\": [\"OR\"], \"rightSideOperands\": {\"unaryGroupOperator\": \"AND\", \"unaryGroup\": {\"groupOperations\": [{\"field\": \"age\", \"operation\": \"GT\", \"value\": 25}], \"nonPriorityGroupOperators\": []}}}}}) { id firstName lastName address { id city } }");

        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(jsonMap);

        var expectedWrapper = getComplexFilterRequestWrapper();

        when(graphQLComplexFilterAdapter.adapt(anyString())).thenReturn(expectedWrapper);

        FilterRequestWrapper<?> result = adapter.adapt(request);

        assertEquals(expectedWrapper, result);
    }

    @Test
    void testAdaptPostRequestWithSimpleQuery() throws Exception {
        // Arrange
        String requestBody = "{\"query\": \"{ user(name: \\\"John\\\", address: { zip_in: [\\\"10001\\\"] }) { id name friends { id city } } }\"}";

        // Mocking the request method and reader to return the POST method and the requestBody respectively
        when(request.getMethod()).thenReturn("POST");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        // Create the expected jsonMap that should be returned by objectMapper.readValue
        Map<String, Object> jsonMap = Map.of("query", "{ user(name: \"John\", address: { zip_in: [\"10001\"] }) { id name friends { id city } } }");

        // Mocking objectMapper.readValue to return the expected jsonMap when it processes the requestBody
        when(objectMapper.readValue(eq(requestBody), any(TypeReference.class))).thenReturn(jsonMap);

        var expectedWrapper = getMockSimpleFilterRequestWrapper();

        // Act
        FilterRequestWrapper<?> result = adapter.adapt(request);

        // Assert
        assertEquals(expectedWrapper, result);
    }

    @Test
    void testAdaptGetRequestWithInvalidQuery() {
        var query = "invalid query";
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameter("query")).thenReturn(query);

        var result = adapter.adapt(request);

        assertEquals(new FilterRequestWrapper<>(), result);
    }

    @Test
    void testAdaptPostRequestWithInvalidQuery() throws Exception {
        var requestBody = "invalid json";
        when(request.getMethod()).thenReturn("POST");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));
        when(objectMapper.readValue(requestBody, new TypeReference<Map<String, Object>>() {
        }))
                .thenThrow(new JsonProcessingException("Invalid JSON") {
                });

        var result = adapter.adapt(request);

        assertEquals(new FilterRequestWrapper<>(), result);
    }
}
