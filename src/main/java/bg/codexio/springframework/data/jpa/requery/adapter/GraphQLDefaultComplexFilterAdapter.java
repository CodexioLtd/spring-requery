package bg.codexio.springframework.data.jpa.requery.adapter;

import bg.codexio.springframework.data.jpa.requery.payload.FilterGroupRequest;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequestWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link GraphQLComplexFilterAdapter} for adapting
 * complex filters from a JSON string to a {@link FilterRequestWrapper}.
 * This implementation is used when no other implementation of
 * {@link GraphQLComplexFilterAdapter} is provided.
 */
@Component
@ConditionalOnMissingBean(GraphQLComplexFilterAdapter.class)
public class GraphQLDefaultComplexFilterAdapter
        implements GraphQLComplexFilterAdapter {
    private final ObjectMapper objectMapper;

    /**
     * Constructor for {@code GraphQLDefaultComplexFilterAdapter} that
     * injects an {@link ObjectMapper}.
     *
     * @param objectMapper the {@link ObjectMapper} to be used for JSON
     *                     processing
     */
    public GraphQLDefaultComplexFilterAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(HttpServletRequest req) {
        //fix later
        return true;
    }

    /**
     * Adapts a JSON string representing a complex filter into a
     * {@link FilterRequestWrapper}.
     * It deserializes the JSON into a {@link FilterGroupRequest} object.
     *
     * @param complexFilterJson the JSON string containing the complex filter
     *                          definition
     * @param <T>               the type parameter for the filter's generic type
     * @return a {@link FilterRequestWrapper} containing the parsed
     * {@link FilterGroupRequest}
     * @throws JsonProcessingException if there is an error during JSON parsing
     */
    @Override
    public <T> FilterRequestWrapper<T> adapt(String complexFilterJson)
            throws JsonProcessingException {
        var filterGroupRequest = this.objectMapper.readValue(
                complexFilterJson,
                FilterGroupRequest.class
        );

        return new FilterRequestWrapper<>(filterGroupRequest);
    }
}
