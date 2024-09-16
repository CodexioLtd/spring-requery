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
        var result = adapter.supports(mock(HttpServletRequest.class));

        assertTrue(result);
    }

    @Test
    void testAdaptSingleConditionSimpleFilter() throws JsonProcessingException {
        // Arrange
        var filterJson = "{\"field\": \"firstName\", \"operation\": \"EQ\", \"value\": \"John\"}";
        var request = mock(HttpServletRequest.class);
        when(request.getParameter("filter")).thenReturn(filterJson);

        var expectedFilterRequest = new FilterRequest("firstName", "John", FilterOperation.EQ);
        var expectedWrapper = new FilterRequestWrapper<>(List.of(expectedFilterRequest));

        when(objectMapper.readValue(filterJson, FilterRequest.class)).thenReturn(expectedFilterRequest);

        // Act
        var result = adapter.adapt(request);

        // Assert
        assertEquals(expectedWrapper, result);
    }

    @Test
    void testAdaptMultipleConditionsSimpleFilter() throws JsonProcessingException {
        // Arrange
        var filterJson = "[{\"field\": \"lastName\", \"operation\": \"CONTAINS\", \"value\": \"Doe\"}, {\"field\": \"age\", \"operation\": \"GTE\", \"value\": 25}]";
        var request = mock(HttpServletRequest.class);
        when(request.getParameter("filter")).thenReturn(filterJson);

        var filterRequests = new FilterRequest[]{
                new FilterRequest("lastName", "Doe", FilterOperation.CONTAINS),
                new FilterRequest("age", 25, FilterOperation.GTE)
        };
        var expectedWrapper = new FilterRequestWrapper<>(List.of(filterRequests));

        when(objectMapper.readValue(filterJson, FilterRequest[].class)).thenReturn(filterRequests);

        // Act
        var result = adapter.adapt(request);

        // Assert
        assertEquals(expectedWrapper, result);
    }

    @Test
    void testAdaptComplexFilter() throws JsonProcessingException {
        // Arrange
        var complexFilterJson = "{ \"groupOperations\": [{\"field\": \"email\", \"operation\": \"CONTAINS\", \"value\": \"example.com\"}], \"nonPriorityGroupOperators\": [\"AND\"], \"rightSideOperands\": { \"unaryGroupOperator\": \"OR\", \"unaryGroup\": { \"groupOperations\": [{\"field\": \"firstName\", \"operation\": \"IN\", \"value\": [\"John\", \"Jane\"]}, {\"field\": \"lastName\", \"operation\": \"BEGINS_WITH_CASEINS\", \"value\": \"Doe\"}], \"nonPriorityGroupOperators\": [\"OR\"], \"rightSideOperands\": { \"unaryGroupOperator\": \"AND\", \"unaryGroup\": { \"groupOperations\": [{\"field\": \"age\", \"operation\": \"GT\", \"value\": 25}], \"nonPriorityGroupOperators\": [] } } } }";
        var request = mock(HttpServletRequest.class);
        when(request.getParameter("complexFilter")).thenReturn(complexFilterJson);

        // Define the nested FilterGroupRequest structure
        var innerMostGroupRequest = new FilterGroupRequest(
                new FilterRequest[]{new FilterRequest("age", 25, FilterOperation.GT)},
                new FilterLogicalOperator[]{},
                null  // No rightSideOperands for the innermost group
        );

        var middleGroupRequest = new FilterGroupRequest(
                new FilterRequest[]{
                        new FilterRequest("firstName", List.of("John", "Jane"), FilterOperation.IN),
                        new FilterRequest("lastName", "Doe", FilterOperation.BEGINS_WITH_CASEINS)
                },
                new FilterLogicalOperator[]{FilterLogicalOperator.OR},
                new UnaryGroupRequest(innerMostGroupRequest, FilterLogicalOperator.AND)
        );

        var outerGroupRequest = new FilterGroupRequest(
                new FilterRequest[]{
                        new FilterRequest("email", "example.com", FilterOperation.CONTAINS)
                },
                new FilterLogicalOperator[]{FilterLogicalOperator.AND},
                new UnaryGroupRequest(middleGroupRequest, FilterLogicalOperator.OR)
        );

        var expectedWrapper = new FilterRequestWrapper<>(outerGroupRequest);
        when(objectMapper.readValue(complexFilterJson, FilterGroupRequest.class)).thenReturn(outerGroupRequest);

        // Act
        var result = adapter.adapt(request);

        // Assert
        assertEquals(expectedWrapper, result);
    }

    @Test
    void testAdaptNoFilterParameters() throws JsonProcessingException {
        // Arrange
        var request = mock(HttpServletRequest.class);
        when(request.getParameter("filter")).thenReturn(null);
        when(request.getParameter("complexFilter")).thenReturn(null);

        // Act
        var result = adapter.adapt(request);

        // Assert
        assertEquals(new FilterRequestWrapper<>(), result);
    }

    @Test
    void testAdaptInvalidFilterParameter() throws JsonProcessingException {
        // Arrange
        var request = mock(HttpServletRequest.class);
        when(request.getParameter("filter")).thenReturn("invalid filter");
        when(request.getParameter("complexFilter")).thenReturn(null);

        // Act
        var result = adapter.adapt(request);

        // Assert
        assertEquals(new FilterRequestWrapper<>(), result);
    }
}

