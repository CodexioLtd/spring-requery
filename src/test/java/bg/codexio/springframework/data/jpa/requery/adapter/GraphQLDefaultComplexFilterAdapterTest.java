package bg.codexio.springframework.data.jpa.requery.adapter;

import bg.codexio.springframework.data.jpa.requery.payload.FilterGroupRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GraphQLDefaultComplexFilterAdapterTest {

    private ObjectMapper objectMapper;
    private GraphQLDefaultComplexFilterAdapter adapter;

    @BeforeEach
    void setUp() {
        objectMapper = mock(ObjectMapper.class);
        adapter = new GraphQLDefaultComplexFilterAdapter(objectMapper);
    }

    @Test
    void testSupportsReturnsTrue() {
        boolean result = adapter.supports(mock(HttpServletRequest.class));

        assertTrue(result);
    }

    @Test
    void testAdaptSuccess() throws JsonProcessingException {
        var complexFilterJson = "{\"groupOperations\":[], \"nonPriorityGroupOperators\":[], \"rightSideOperands\":null}";
        var filterGroupRequest = new FilterGroupRequest(null, null, null);
        when(objectMapper.readValue(complexFilterJson, FilterGroupRequest.class)).thenReturn(filterGroupRequest);

        var result = adapter.adapt(complexFilterJson);

        assertEquals(filterGroupRequest, result.filterGroupRequest().orElse(null));
    }

    @Test
    void testAdaptInvalidJsonThrowsJsonProcessingException() throws JsonProcessingException {
        // Arrange
        String invalidJson = "invalid json";
        when(objectMapper.readValue(invalidJson, FilterGroupRequest.class)).thenThrow(new JsonProcessingException("Invalid JSON") {
        });

        // Act & Assert
        assertThrows(JsonProcessingException.class, () -> adapter.adapt(invalidJson));
    }
}
