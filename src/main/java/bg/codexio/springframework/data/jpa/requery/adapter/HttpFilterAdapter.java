package bg.codexio.springframework.data.jpa.requery.adapter;

import bg.codexio.springframework.data.jpa.requery.payload.FilterRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;

/**
 * An adapter interface for handling filter requests from
 * {@link HttpServletRequest}.
 * Implementations of this interface are responsible for adapting request
 * parameters into a {@link FilterRequestWrapper}.
 */
public interface HttpFilterAdapter {

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
     * Adapts the filter parameters from the given {@link HttpServletRequest}
     * into a {@link FilterRequestWrapper}.
     *
     * @param req the HTTP servlet request containing filter parameters
     * @param <T> the type of the result in the {@link FilterRequestWrapper}
     * @return a {@link FilterRequestWrapper} containing the adapted filter
     * requests
     */
    <T> FilterRequestWrapper<T> adapt(HttpServletRequest req);
}
