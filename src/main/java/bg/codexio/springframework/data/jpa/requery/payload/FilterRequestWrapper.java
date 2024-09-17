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
        this(
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
    }

    public FilterRequestWrapper(List<FilterRequest> filterRequest) {
        this(
                Optional.ofNullable(filterRequest),
                Optional.empty(),
                Optional.empty()
        );
    }

    public FilterRequestWrapper(FilterGroupRequest filterGroupRequest) {
        this(
                Optional.empty(),
                Optional.ofNullable(filterGroupRequest),
                Optional.empty()
        );
    }

    public FilterRequestWrapper<T> isSimple(Function<List<FilterRequest>, T> simpleSpecFunction) {
        return this.filterRequests.filter(reqs -> !reqs.isEmpty())
                                  .map(reqs -> new FilterRequestWrapper<T>(
                                          this.filterRequests,
                                          this.filterGroupRequest,
                                          Optional.ofNullable(simpleSpecFunction.apply(reqs))
                                  ))
                                  .orElse(this);
    }

    public FilterRequestWrapper<T> orComplex(Function<FilterGroupRequest, T> complexSpecFunction) {
        return this.filterGroupRequest.map(groupReq -> new FilterRequestWrapper<T>(
                           this.filterRequests,
                           this.filterGroupRequest,
                           Optional.ofNullable(complexSpecFunction.apply(groupReq))
                   ))
                                      .orElse(this);
    }


    public T or(Supplier<T> defaultSupplier) {
        return this.result.orElseGet(defaultSupplier);
    }
}
