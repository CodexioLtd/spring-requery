package bg.codexio.springframework.data.jpa.requery.payload;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public record FilterRequestWrapper<T>(
        Optional<List<FilterRequest>> filterRequests,
        Optional<FilterGroupRequest> filterGroupRequest,
        Optional<T> result
) {
    public FilterRequestWrapper {
    }

    public FilterRequestWrapper() {
        this(Optional.empty(), Optional.empty(), Optional.empty());
    }

    public FilterRequestWrapper(List<FilterRequest> filterRequest) {
        this(Optional.ofNullable(filterRequest), Optional.empty(), Optional.empty());
    }

    public FilterRequestWrapper(FilterGroupRequest filterGroupRequest) {
        this(Optional.empty(), Optional.ofNullable(filterGroupRequest), Optional.empty());
    }

    public FilterRequestWrapper<T> isSimple(Function<List<FilterRequest>, T> simpleSpecFunction) {
        if (filterRequests.isPresent() && !filterRequests.get().isEmpty()) {
            var result = filterRequests.map(simpleSpecFunction);
            return new FilterRequestWrapper<T>(filterRequests, filterGroupRequest, result);
        }
        return this;
    }

    public FilterRequestWrapper<T> orComplex(Function<FilterGroupRequest, T> complexSpecFunction) {
        if (filterGroupRequest.isPresent()) {
            var result = filterGroupRequest.map(complexSpecFunction);
            return new FilterRequestWrapper<T>(filterRequests, filterGroupRequest, result);
        }
        return this;
    }

    public T or(Supplier<T> defaultSupplier) {
        return result.orElseGet(defaultSupplier);
    }
}
