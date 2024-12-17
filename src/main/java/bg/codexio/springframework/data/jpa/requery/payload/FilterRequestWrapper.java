package bg.codexio.springframework.data.jpa.requery.payload;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A record that encapsulates filter requests, filter group requests, and a
 * result of type {@code T}.
 * Provides methods to handle these requests and produce results.
 *
 * @param <T> the type of the result
 */
public record FilterRequestWrapper<T>(
        Optional<List<FilterRequest>> filterRequests,
        Optional<FilterGroupRequest> filterGroupRequest,
        Optional<T> result
) {
    /**
     * Constructs a {@code FilterRequestWrapper} with the given filter
     * requests, filter group request, and result.
     * Filters out empty filter requests.
     *
     * @param filterRequests     the filter requests
     * @param filterGroupRequest the filter group request
     * @param result             the result
     */
    public FilterRequestWrapper {
        filterRequests = filterRequests.filter(list -> !list.isEmpty());
    }

    /**
     * Constructs an empty {@code FilterRequestWrapper}.
     * Initializes the filter requests, filter group request, and result as
     * empty.
     */
    public FilterRequestWrapper() {
        this(
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
    }

    /**
     * Constructs a {@code FilterRequestWrapper} with the given list of
     * filter requests.
     * Initializes the filter group request and result as empty.
     *
     * @param filterRequest a list of filter requests
     */
    public FilterRequestWrapper(List<FilterRequest> filterRequest) {
        this(
                Optional.ofNullable(filterRequest),
                Optional.empty(),
                Optional.empty()
        );
    }

    /**
     * Constructs a {@code FilterRequestWrapper} with the given filter group
     * request.
     * Initializes the filter requests and result as empty.
     *
     * @param filterGroupRequest a filter group request
     */
    public FilterRequestWrapper(FilterGroupRequest filterGroupRequest) {
        this(
                Optional.empty(),
                Optional.ofNullable(filterGroupRequest),
                Optional.empty()
        );
    }

    /**
     * Applies the given {@code simpleSpecFunction} to the filter requests if
     * they are present and non-empty.
     * Returns a new {@code FilterRequestWrapper} with the result of the
     * function, or the current instance if filter requests are absent.
     *
     * @param simpleSpecFunction a function that takes a list of filter
     *                           requests and returns a result of type {@code T}
     * @return a new {@code FilterRequestWrapper} with the result of the
     * function or the current instance
     */
    public FilterRequestWrapper<T> isSimple(Function<List<FilterRequest>, T> simpleSpecFunction) {
        return this.filterRequests.map(simpleSpecFunction)
                                  .map(r -> new FilterRequestWrapper<>(
                                          this.filterRequests,
                                          this.filterGroupRequest,
                                          Optional.of(r)
                                  ))
                                  .orElse(this);
    }

    /**
     * Applies the given {@code complexSpecFunction} to the filter group
     * request if it is present.
     * Returns a new {@code FilterRequestWrapper} with the result of the
     * function, or the current instance if filter group request is absent.
     *
     * @param complexSpecFunction a function that takes a filter group
     *                            request and returns a result of type {@code T}
     * @return a new {@code FilterRequestWrapper} with the result of the
     * function or the current instance
     */
    public FilterRequestWrapper<T> orComplex(Function<FilterGroupRequest, T> complexSpecFunction) {
        return this.filterGroupRequest.map(complexSpecFunction)
                                      .map(r -> new FilterRequestWrapper<>(
                                              this.filterRequests,
                                              this.filterGroupRequest,
                                              Optional.of(r)
                                      ))
                                      .orElse(this);
    }

    /**
     * Returns the result if present, or uses the {@code defaultSupplier} to
     * supply a default value.
     *
     * @param defaultSupplier a supplier that provides a default value if the
     *                        result is not present
     * @return the result if present, or the default value from the supplier
     */
    public T or(Supplier<T> defaultSupplier) {
        return this.result.orElseGet(defaultSupplier);
    }
}
