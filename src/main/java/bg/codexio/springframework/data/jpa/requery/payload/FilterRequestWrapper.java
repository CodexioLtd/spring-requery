package bg.codexio.springframework.data.jpa.requery.payload;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class FilterRequestWrapper {
    private List<FilterRequest> filterRequest;
    private FilterGroupRequest filterGroupRequest;

    public FilterRequestWrapper() {
    }

    public FilterRequestWrapper(List<FilterRequest> filterRequest) {
        setFilterRequest(filterRequest);
    }

    public FilterRequestWrapper(FilterGroupRequest filterGroupRequest) {
        setFilterGroupRequest(filterGroupRequest);
    }

    public List<FilterRequest> getFilterRequest() {
        return filterRequest;
    }

    public void setFilterRequest(List<FilterRequest> filterRequest) {
        this.filterRequest = filterRequest;
    }

    public FilterGroupRequest getFilterGroupRequest() {
        return filterGroupRequest;
    }

    public void setFilterGroupRequest(FilterGroupRequest filterGroupRequest) {
        this.filterGroupRequest = filterGroupRequest;
    }

    /**
     * If the wrapper contains a simple filter (FilterRequest), execute the provided consumer.
     *
     * @param consumer the operation to perform on the simple filter
     * @return the current instance of FilterRequestWrapper to chain further operations
     */
    public FilterRequestWrapper ifSimple(Consumer<List<FilterRequest>> consumer) {
        Optional.ofNullable(filterRequest).ifPresent(consumer);
        return this;
    }

    /**
     * If the wrapper contains a complex filter (FilterGroupRequest), execute the provided consumer.
     *
     * @param consumer the operation to perform on the complex filter
     * @return the current instance of FilterRequestWrapper to chain further operations
     */
    public FilterRequestWrapper orComplex(Consumer<FilterGroupRequest> consumer) {
        Optional.ofNullable(filterGroupRequest).ifPresent(consumer);
        return this;
    }
}
