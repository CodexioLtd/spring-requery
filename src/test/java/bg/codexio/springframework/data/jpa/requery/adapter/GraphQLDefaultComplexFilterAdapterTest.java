package bg.codexio.springframework.data.jpa.requery.adapter;

import bg.codexio.springframework.data.jpa.requery.payload.FilterGroupRequest;
import com.fasterxml.jackson.core.JsonParseException;
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
        this.objectMapper = mock(ObjectMapper.class);
        this.adapter =
                new GraphQLDefaultComplexFilterAdapter(this.objectMapper);
    }

    @Test
    void testSupportsReturnsTrue() {
        var result = this.adapter.supports(mock(HttpServletRequest.class));

        assertTrue(result);
    }

    @Test
    void testAdaptSuccess() throws JsonProcessingException {
        var complexFilterJson = "{\"groupOperations\":[], "
                + "\"nonPriorityGroupOperators\":[], "
                + "\"rightSideOperands\":null}";
        var filterGroupRequest = new FilterGroupRequest(
                null,
                null,
                null
        );
        when(this.objectMapper.readValue(
                complexFilterJson,
                FilterGroupRequest.class
        )).thenReturn(filterGroupRequest);

        var result = this.adapter.adapt(complexFilterJson);

        assertEquals(
                filterGroupRequest,
                result.filterGroupRequest()
                      .orElse(null)
        );
    }

    @Test
    void testAdaptInvalidJsonThrowsJsonProcessingException()
            throws JsonProcessingException {
        var invalidJson = "invalid json";
        when(this.objectMapper.readValue(
                invalidJson,
                FilterGroupRequest.class
        )).thenThrow(new JsonParseException("Invalid JSON"));

        assertThrows(
                JsonProcessingException.class,
                () -> this.adapter.adapt(invalidJson)
        );
    }
}
