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
        var nameFilterRequest = new FilterRequest(
                "name",
                "John",
                FilterOperation.EQ
        );

        var zipFilterRequest = new FilterRequest(
                "address.zip",
                List.of("10001"),
                FilterOperation.IN
        );

        // Create the expected FilterRequestWrapper containing both filter
        // requests
        return new FilterRequestWrapper<>(List.of(
                nameFilterRequest,
                zipFilterRequest
        ));
    }

    private static FilterRequestWrapper<Object> getComplexFilterRequestWrapper() {
        var groupOperations = new FilterRequest[]{
                new FilterRequest(
                        "email",
                        "example.com",
                        FilterOperation.CONTAINS
                )
        };
        var nonPriorityGroupOperators = new FilterLogicalOperator[]{
                FilterLogicalOperator.AND
        };

        var rightSideOperands = new UnaryGroupRequest(
                new FilterGroupRequest(
                        new FilterRequest[]{
                                new FilterRequest(
                                        "firstName",
                                        List.of(
                                                "John",
                                                "Vasko"
                                        ),
                                        FilterOperation.IN
                                ), new FilterRequest(
                                "lastName",
                                "Doe",
                                FilterOperation.BEGINS_WITH_CASEINS
                        )
                        },
                        new FilterLogicalOperator[]{FilterLogicalOperator.OR},
                        new UnaryGroupRequest(
                                new FilterGroupRequest(
                                        new FilterRequest[]{
                                                new FilterRequest(
                                                        "age",
                                                        25,
                                                        FilterOperation.GT
                                                )
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

        return new FilterRequestWrapper<>(expectedFilterGroupRequest);
    }

    @BeforeEach
    void setUp() {
        this.objectMapper = mock(ObjectMapper.class);
        this.graphQLComplexFilterAdapter =
                mock(GraphQLComplexFilterAdapter.class);
        this.adapter = new GraphQLHttpFilterAdapter(
                this.objectMapper,
                this.graphQLComplexFilterAdapter
        );
        this.request = mock(HttpServletRequest.class);
    }

    @Test
    void testSupportsReturnsTrue() {
        var result = this.adapter.supports(mock(HttpServletRequest.class));

        assertTrue(result);
    }

    @Test
    void testAdaptOtherHttpMethod() {
        var request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("PUT"); // Or any method other
        // than GET or POST

        var result = this.adapter.adapt(request);

        assertEquals(
                new FilterRequestWrapper<>(),
                result
        );
    }

    @Test
    void testAdaptGetRequestWithComplexQuery() throws JsonProcessingException {
        var complexQuery = "query { users(filter: {" + "\"groupOperations\": [{"
                + "\"field\": \"email\", \"operation\": \"CONTAINS\", "
                + "\"value\": \"example.com\""
                + "}], \"nonPriorityGroupOperators\": [\"AND\"],"
                + "\"rightSideOperands\": {" + "\"unaryGroupOperator\": \"OR\","
                + "\"unaryGroup\": {" + "\"groupOperations\": [{"
                + "\"field\": \"firstName\", \"operation\": \"IN\", "
                + "\"value\": [\"John\", \"Vasko\"]" + "}, {"
                + "\"field\": \"lastName\", \"operation\": "
                + "\"BEGINS_WITH_CASEINS\", \"value\": \"Doe\""
                + "}], \"nonPriorityGroupOperators\": [\"OR\"],"
                + "\"rightSideOperands\": {"
                + "\"unaryGroupOperator\": \"AND\"," + "\"unaryGroup\": {"
                + "\"groupOperations\": [{"
                + "\"field\": \"age\", \"operation\": \"GT\", \"value\": 25"
                + "}], \"nonPriorityGroupOperators\": []"
                + "}}}}}) { id firstName lastName address { id city } } }";
        when(this.request.getMethod()).thenReturn("GET");
        when(this.request.getParameter("query")).thenReturn(complexQuery);

        var expectedFilterRequest = new FilterRequest(
                "email",
                "example.com",
                FilterOperation.CONTAINS
        );

        var expectedWrapper =
                new FilterRequestWrapper<>(List.of(expectedFilterRequest));
        when(this.graphQLComplexFilterAdapter.adapt(anyString())).thenReturn(expectedWrapper);

        var result = this.adapter.adapt(this.request);

        assertEquals(
                expectedWrapper,
                result
        );
    }

    @Test
    void testAdaptGetRequestWithSimpleQuery() {
        var simpleQuery = "{ user(name: \"John\", address: { zip_in: "
                + "[\"10001\"] }) { id name friends { id city } } }";
        when(this.request.getMethod()).thenReturn("GET");
        when(this.request.getParameter("query")).thenReturn(simpleQuery);

        var expectedWrapper = getMockSimpleFilterRequestWrapper();

        var result = this.adapter.adapt(this.request);

        assertEquals(
                expectedWrapper,
                result
        );
    }

    @Test
    void testAdaptPostRequestWithComplexQuery() throws Exception {
        var requestBody =
                "{" + "\"query\":\"users(filter: {" + "\"groupOperations\": [{"
                        + "\"field\": \"email\", \"operation\": \"CONTAINS\","
                        + " \"value\": \"example.com\"" + "}],"
                        + "\"nonPriorityGroupOperators\": [\"AND\"],"
                        + "\"rightSideOperands\": {"
                        + "\"unaryGroupOperator\": \"OR\","
                        + "\"unaryGroup\": {" + "\"groupOperations\": [{"
                        + "\"field\": \"firstName\", \"operation\": \"IN\", "
                        + "\"value\": [\"John\", \"Vasko\"]" + "}, {"
                        + "\"field\": \"lastName\", \"operation\": "
                        + "\"BEGINS_WITH_CASEINS\", \"value\": \"Doe\"" + "}],"
                        + "\"nonPriorityGroupOperators\": [\"OR\"],"
                        + "\"rightSideOperands\": {"
                        + "\"unaryGroupOperator\": \"AND\","
                        + "\"unaryGroup\": {" + "\"groupOperations\": [{"
                        + "\"field\": \"age\", \"operation\": \"GT\", "
                        + "\"value\": 25"
                        + "}], \"nonPriorityGroupOperators\": []"
                        + "}}}}}) { id firstName lastName address { id city }"
                        + " } \"" + "}";

        when(this.request.getMethod()).thenReturn("POST");
        when(this.request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        var jsonMap = new HashMap<String, Object>();
        jsonMap.put(
                "query",
                "users(filter: {\"groupOperations\": [{\"field\": \"email\", "
                        + "\"operation\": \"CONTAINS\", \"value\": \"example"
                        + ".com\"}], \"nonPriorityGroupOperators\": "
                        + "[\"AND\"], \"rightSideOperands\": "
                        + "{\"unaryGroupOperator\": \"OR\", \"unaryGroup\": "
                        + "{\"groupOperations\": [{\"field\": \"firstName\", "
                        + "\"operation\": \"IN\", \"value\": [\"John\", "
                        + "\"Vasko\"]}, {\"field\": \"lastName\", "
                        + "\"operation\": \"BEGINS_WITH_CASEINS\", \"value\":"
                        + " \"Doe\"}], \"nonPriorityGroupOperators\": "
                        + "[\"OR\"], \"rightSideOperands\": "
                        + "{\"unaryGroupOperator\": \"AND\", \"unaryGroup\": "
                        + "{\"groupOperations\": [{\"field\": \"age\", "
                        + "\"operation\": \"GT\", \"value\": 25}], "
                        + "\"nonPriorityGroupOperators\": []}}}}}) { id "
                        + "firstName lastName address { id city } }"
        );

        when(this.objectMapper.readValue(
                anyString(),
                any(TypeReference.class)
        )).thenReturn(jsonMap);

        var expectedWrapper = getComplexFilterRequestWrapper();

        when(this.graphQLComplexFilterAdapter.adapt(anyString())).thenReturn(expectedWrapper);

        var result = this.adapter.adapt(this.request);

        assertEquals(
                expectedWrapper,
                result
        );
    }

    @Test
    void testAdaptPostRequestWithSimpleQuery() throws Exception {
        var requestBody = "{\"query\": \"{ user(name: \\\"John\\\", address: "
                + "{ zip_in: [\\\"10001\\\"] }) { id name friends { id city }"
                + " } }\"}";

        when(this.request.getMethod()).thenReturn("POST");
        when(this.request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));

        var jsonMap = Map.of(
                "query",
                "{ user(name: \"John\", address: { zip_in: [\"10001\"] }) { "
                        + "id name friends { id city } } }"
        );

        when(this.objectMapper.readValue(
                eq(requestBody),
                any(TypeReference.class)
        )).thenReturn(jsonMap);

        var expectedWrapper = getMockSimpleFilterRequestWrapper();

        var result = this.adapter.adapt(this.request);

        assertEquals(
                expectedWrapper,
                result
        );
    }

    @Test
    void testAdaptGetRequestWithInvalidQuery() {
        var query = "invalid query";
        when(this.request.getMethod()).thenReturn("GET");
        when(this.request.getParameter("query")).thenReturn(query);

        var result = this.adapter.adapt(this.request);

        assertEquals(
                new FilterRequestWrapper<>(),
                result
        );
    }

    @Test
    void testAdaptPostRequestWithInvalidQuery() throws Exception {
        var requestBody = "invalid json";
        when(this.request.getMethod()).thenReturn("POST");
        when(this.request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));
        when(this.objectMapper.readValue(
                requestBody,
                new TypeReference<Map<String, Object>>() {}
        )).thenThrow(new JsonProcessingException("Invalid JSON") {});

        var result = this.adapter.adapt(this.request);

        assertEquals(
                new FilterRequestWrapper<>(),
                result
        );
    }
}
