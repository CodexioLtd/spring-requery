package bg.codexio.springframework.data.jpa.requery.adapter;

import bg.codexio.springframework.data.jpa.requery.payload.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonHttpFilterAdapterTest {

    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final JsonHttpFilterAdapter adapter = new JsonHttpFilterAdapter(objectMapper);

    @Test
    void testSupportsReturnsTrue() {
        boolean result = adapter.supports(mock(HttpServletRequest.class));

        assertTrue(result);
    }

    @Test
    void testAdaptSingleConditionSimpleFilter() throws JsonProcessingException {
        // Arrange
        String filterJson = "{\"field\": \"firstName\", \"operation\": \"EQ\", \"value\": \"John\"}";
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("filter")).thenReturn(filterJson);

        FilterRequest expectedFilterRequest = new FilterRequest("firstName", "John", FilterOperation.EQ);
        FilterRequestWrapper<FilterRequest> expectedWrapper = new FilterRequestWrapper<>(List.of(expectedFilterRequest));

        when(objectMapper.readValue(filterJson, FilterRequest.class)).thenReturn(expectedFilterRequest);

        // Act
        FilterRequestWrapper<?> result = adapter.adapt(request);

        // Assert
        assertEquals(expectedWrapper, result);
    }

    @Test
    void testAdaptMultipleConditionsSimpleFilter() throws JsonProcessingException {
        // Arrange
        String filterJson = "[{\"field\": \"lastName\", \"operation\": \"CONTAINS\", \"value\": \"Doe\"}, {\"field\": \"age\", \"operation\": \"GTE\", \"value\": 25}]";
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("filter")).thenReturn(filterJson);

        FilterRequest[] filterRequests = new FilterRequest[]{
                new FilterRequest("lastName", "Doe", FilterOperation.CONTAINS),
                new FilterRequest("age", 25, FilterOperation.GTE)
        };
        FilterRequestWrapper<FilterRequest> expectedWrapper = new FilterRequestWrapper<>(List.of(filterRequests));

        when(objectMapper.readValue(filterJson, FilterRequest[].class)).thenReturn(filterRequests);

        // Act
        FilterRequestWrapper<?> result = adapter.adapt(request);

        // Assert
        assertEquals(expectedWrapper, result);
    }

    @Test
    void testAdaptComplexFilter() throws JsonProcessingException {
        // Arrange
        String complexFilterJson = "{ \"groupOperations\": [{\"field\": \"email\", \"operation\": \"CONTAINS\", \"value\": \"example.com\"}], \"nonPriorityGroupOperators\": [\"AND\"], \"rightSideOperands\": { \"unaryGroupOperator\": \"OR\", \"unaryGroup\": { \"groupOperations\": [{\"field\": \"firstName\", \"operation\": \"IN\", \"value\": [\"John\", \"Jane\"]}, {\"field\": \"lastName\", \"operation\": \"BEGINS_WITH_CASEINS\", \"value\": \"Doe\"}], \"nonPriorityGroupOperators\": [\"OR\"], \"rightSideOperands\": { \"unaryGroupOperator\": \"AND\", \"unaryGroup\": { \"groupOperations\": [{\"field\": \"age\", \"operation\": \"GT\", \"value\": 25}], \"nonPriorityGroupOperators\": [] } } } }";
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("complexFilter")).thenReturn(complexFilterJson);

        // Define the nested FilterGroupRequest structure
        FilterGroupRequest innerMostGroupRequest = new FilterGroupRequest(
                new FilterRequest[]{new FilterRequest("age", 25, FilterOperation.GT)},
                new FilterLogicalOperator[]{},
                null  // No rightSideOperands for the innermost group
        );

        FilterGroupRequest middleGroupRequest = new FilterGroupRequest(
                new FilterRequest[]{
                        new FilterRequest("firstName", List.of("John", "Jane"), FilterOperation.IN),
                        new FilterRequest("lastName", "Doe", FilterOperation.BEGINS_WITH_CASEINS)
                },
                new FilterLogicalOperator[]{FilterLogicalOperator.OR},
                new UnaryGroupRequest(innerMostGroupRequest, FilterLogicalOperator.AND)
        );

        FilterGroupRequest outerGroupRequest = new FilterGroupRequest(
                new FilterRequest[]{
                        new FilterRequest("email", "example.com", FilterOperation.CONTAINS)
                },
                new FilterLogicalOperator[]{FilterLogicalOperator.AND},
                new UnaryGroupRequest(middleGroupRequest, FilterLogicalOperator.OR)
        );

        FilterRequestWrapper<FilterGroupRequest> expectedWrapper = new FilterRequestWrapper<>(outerGroupRequest);
        when(objectMapper.readValue(complexFilterJson, FilterGroupRequest.class)).thenReturn(outerGroupRequest);

        // Act
        FilterRequestWrapper<?> result = adapter.adapt(request);

        // Assert
        assertEquals(expectedWrapper, result);
    }

    @Test
    void testAdaptNoFilterParameters() throws JsonProcessingException {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("filter")).thenReturn(null);
        when(request.getParameter("complexFilter")).thenReturn(null);

        // Act
        FilterRequestWrapper<?> result = adapter.adapt(request);

        // Assert
        assertEquals(new FilterRequestWrapper<>(), result);
    }

    @Test
    void testAdaptInvalidFilterParameter() throws JsonProcessingException {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("filter")).thenReturn("invalid filter");
        when(request.getParameter("complexFilter")).thenReturn(null);

        // Act
        FilterRequestWrapper<?> result = adapter.adapt(request);

        // Assert
        assertEquals(new FilterRequestWrapper<>(), result);
    }
}

