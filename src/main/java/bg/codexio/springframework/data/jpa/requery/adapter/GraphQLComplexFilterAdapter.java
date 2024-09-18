package bg.codexio.springframework.data.jpa.requery.adapter;

import bg.codexio.springframework.data.jpa.requery.payload.FilterGroupRequest;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequestWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * An interface for adapting GraphQL-like complex filters from a JSON string
 * to a {@link FilterRequestWrapper}.
 * Implementations of this interface are responsible for handling and parsing
 * GraphQL complex filters
 * and providing them as {@link FilterRequestWrapper} instances.
 */
public interface GraphQLComplexFilterAdapter {
    /**
     * Determines whether this adapter supports the given
     * {@link HttpServletRequest}.
     * Currently, this method always returns {@code true}, but this behavior
     * can be modified.
     *
     * @param req the HTTP servlet request to check
     * @return {@code true} if the adapter supports the request, {@code false
     * } otherwise
     */
    boolean supports(HttpServletRequest req);

    /**
     * Adapts a JSON string representing a complex filter into a
     * {@link FilterRequestWrapper}.
     * The JSON string is expected to contain the filter definition that can
     * be parsed into a
     * {@link FilterGroupRequest}.
     *
     * @param complexFilterJson the JSON string containing the complex filter
     *                          definition
     * @param <T>               the type parameter for the filter's generic type
     * @return a {@link FilterRequestWrapper} containing the parsed
     * {@link FilterGroupRequest}
     * @throws JsonProcessingException if there is an error during JSON parsing
     */
    <T> FilterRequestWrapper<T> adapt(String complexFilterJson)
            throws JsonProcessingException;
}
