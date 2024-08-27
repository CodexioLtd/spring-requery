package bg.codexio.springframework.data.jpa.requery.payload;

import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public record FilterRequestWrapper(
        Optional<List<FilterRequest>> filterRequests,
        Optional<FilterGroupRequest> filterGroupRequest) {
    public FilterRequestWrapper(Optional<List<FilterRequest>> filterRequests, Optional<FilterGroupRequest> filterGroupRequest) {
        this.filterRequests = filterRequests;
        this.filterGroupRequest = filterGroupRequest;
    }
    public FilterRequestWrapper() {
        this(Optional.empty(), Optional.empty());
    }

    public FilterRequestWrapper(List<FilterRequest> filterRequest) {
        this(Optional.ofNullable(filterRequest), Optional.empty());
    }

    public FilterRequestWrapper(FilterGroupRequest filterGroupRequest) {
        this(Optional.empty(), Optional.ofNullable(filterGroupRequest));
    }

    public FilterRequestWrapper isSimple(Function<List<FilterRequest>, Specification<Object>> simpleSpecFunction) {
        if (filterRequests.isPresent() && !filterRequests.get().isEmpty()) {
            simpleSpecFunction.apply(filterRequests.get());
        }
        return this;
    }

    public FilterRequestWrapper orComplex(Function<FilterGroupRequest, Specification<Object>> complexSpecFunction) {
        if (filterGroupRequest.isPresent()) {
            complexSpecFunction.apply(filterGroupRequest.get());
        }
        return this;
    }

    public Specification<Object> or(Supplier<Specification<Object>> defaultSpecSupplier) {
        return defaultSpecSupplier.get();
    }
}
