package bg.codexio.springframework.data.jpa.requery.payload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class FilterRequestWrapperTest {

    private List<FilterRequest> sampleFilterRequests;
    private FilterGroupRequest sampleFilterGroupRequest;
    private Specification<String> simpleSpecResult;
    private Specification<String> complexSpecResult;
    private Supplier<Specification<String>> defaultSupplier;

    @BeforeEach
    void setUp() {
        sampleFilterRequests = List.of(
                new FilterRequest("name", "Ivan", FilterOperation.EQ),
                new FilterRequest("age", 25, FilterOperation.GTE)
        );

        sampleFilterGroupRequest = new FilterGroupRequest(
                new FilterRequest[]{
                        new FilterRequest("adminStatus", "ACTIVE", FilterOperation.EQ)
                },
                new FilterLogicalOperator[]{FilterLogicalOperator.AND},
                null
        );

        simpleSpecResult = (root, query, cb) -> cb.equal(root.get("name"), "Ivan");
        complexSpecResult = (root, query, cb) -> cb.and(cb.equal(root.get("adminStatus"), "ACTIVE"));

        defaultSupplier = () -> (root, query, cb) -> cb.isTrue(cb.literal(true));
    }

    @Test
    void testIsSimpleWithFilterRequests() {
        FilterRequestWrapper<Specification<String>> wrapper = new FilterRequestWrapper<>(sampleFilterRequests);

        FilterRequestWrapper<Specification<String>> result = wrapper.isSimple(_ -> simpleSpecResult);

        assertTrue(result.result().isPresent(), "Result should be present when filterRequests are provided.");
        assertEquals(simpleSpecResult, result.result().get(), "Result should match the simpleSpecResult.");
    }

    @Test
    void testIsSimpleWithoutFilterRequests() {
        FilterRequestWrapper<Specification<String>> wrapper = new FilterRequestWrapper<>(Optional.empty(), Optional.empty(), Optional.empty());

        FilterRequestWrapper<Specification<String>> result = wrapper.isSimple(filters -> simpleSpecResult);

        assertFalse(result.result().isPresent(), "Result should be absent when filterRequests are not provided.");
    }

    @Test
    void testOrComplexWithFilterGroupRequest() {
        FilterRequestWrapper<Specification<String>> wrapper = new FilterRequestWrapper<>(sampleFilterGroupRequest);

        FilterRequestWrapper<Specification<String>> result = wrapper.orComplex(_ -> complexSpecResult);

        assertTrue(result.result().isPresent(), "Result should be present when filterGroupRequest is provided.");
        assertEquals(complexSpecResult, result.result().get(), "Result should match the complexSpecResult.");
    }

    @Test
    void testOrComplexWithoutFilterGroupRequest() {
        FilterRequestWrapper<Specification<String>> wrapper = new FilterRequestWrapper<>(Optional.empty(), Optional.empty(), Optional.empty());

        FilterRequestWrapper<Specification<String>> result = wrapper.orComplex(group -> complexSpecResult);

        assertFalse(result.result().isPresent(), "Result should be absent when filterGroupRequest is not provided.");
    }

    @Test
    void testOrWithResultPresent() {
        FilterRequestWrapper<Specification<String>> wrapper = new FilterRequestWrapper<>(Optional.empty(), Optional.empty(), Optional.of(simpleSpecResult));

        Specification<String> result = wrapper.or(defaultSupplier);

        assertEquals(simpleSpecResult, result, "Result should match the already present result.");
    }

    @Test
    void testOrWithNoResultPresent() {
        FilterRequestWrapper<Specification<String>> wrapper = new FilterRequestWrapper<>(Optional.empty(), Optional.empty(), Optional.empty());

        Specification<String> result = wrapper.or(defaultSupplier);

        assertNotNull(result, "Result should not be null.");
        assertSame(defaultSupplier.get(), result, "Result should match the default result provided by the supplier.");
    }
}

