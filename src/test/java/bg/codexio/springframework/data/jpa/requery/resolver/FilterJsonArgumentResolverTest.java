package bg.codexio.springframework.data.jpa.requery.resolver;

import bg.codexio.springframework.data.jpa.requery.adapter.JsonHttpFilterAdapter;
import bg.codexio.springframework.data.jpa.requery.config.FilterJsonTypeConverterImpl;
import bg.codexio.springframework.data.jpa.requery.resolver.function.CaseInsensitiveLikeSQLFunction;
import bg.codexio.springframework.data.jpa.requery.test.objects.ChildMock;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FilterJsonArgumentResolverTest {
    private MethodParameter methodParameterMock;
    private java.lang.reflect.Parameter reflectParameter;
    private NativeWebRequest nativeWebRequestMock;
    private HttpServletRequest httpServletRequestMock;
    private ModelAndViewContainer modelAndViewContainerMock;
    private WebDataBinderFactory webDataBinderFactoryMock;
    private FilterJsonArgumentResolver filterJsonArgumentResolverMock;
    private CriteriaQuery criteriaQueryMock;
    private CriteriaBuilder mockCriteriaBuilder;
    private Root mockRoot;

    private static Stream<Arguments> simpleFilterDataProvider() {
        return Stream.of(
                Arguments.of(
                        null,
                        null
                ),
                Arguments.of(simpleFilterTemplate("EQ")),
                Arguments.of(simpleFilterTemplate("GT")),
                Arguments.of(simpleFilterTemplate("GTE")),
                Arguments.of(simpleFilterTemplate("LT")),
                Arguments.of(simpleFilterTemplate("LTE")),
                Arguments.of(simpleFilterTemplate("BEGINS_WITH")),
                Arguments.of(simpleFilterTemplate("ENDS_WITH")),
                Arguments.of(simpleFilterTemplate("CONTAINS")),
                Arguments.of(simpleFilterTemplate("BEGINS_WITH_CASEINS")),
                Arguments.of(simpleFilterTemplate("ENDS_WITH_CASEINS")),
                Arguments.of(simpleFilterTemplate("CONTAINS_CASEINS")),
                Arguments.of(simpleFilterTemplate(
                        "IN",
                        "nonExisting.field",
                        Boolean.TRUE
                )),
                Arguments.of(simpleFilterTemplate(
                        "NOT_IN",
                        Boolean.TRUE
                ))
        );
    }

    private static String simpleFilterTemplate(String operation) {
        return simpleFilterTemplate(
                operation,
                Boolean.FALSE
        );
    }

    private static String simpleFilterTemplate(
            String operation,
            Boolean multiValue
    ) {
        return simpleFilterTemplate(
                operation,
                null,
                multiValue
        );
    }

    private static String simpleFilterTemplate(
            String operation,
            String field,
            Boolean multiValue
    ) {
        return STR."""
        {
            "operation": "\{operation}",
            "field": "\{Objects.isNull(field)
                        ? "name"
                        : field}",
            "value": \{multiValue
                       ? "[\"John\", \"Jane\"]"
                       : "\"John\""}
        }
        """;
    }

    @BeforeEach
    void setup() {
        var objectMapperMock = new ObjectMapper();
        var filterJsonTypeConverterMock = new FilterJsonTypeConverterImpl();
        var httpFilterAdapterMock = new JsonHttpFilterAdapter(objectMapperMock);
        var mockPath = mock(Path.class);
        var mockExpression = mock(Expression.class);
        var mockPredicate = mock(Predicate.class);
        var mockParameterizedType = mock(ParameterizedType.class);

        this.methodParameterMock = mock(MethodParameter.class);
        this.reflectParameter = mock(java.lang.reflect.Parameter.class);
        this.nativeWebRequestMock = mock(NativeWebRequest.class);
        this.httpServletRequestMock = mock(HttpServletRequest.class);
        this.modelAndViewContainerMock = mock(ModelAndViewContainer.class);
        this.webDataBinderFactoryMock = mock(WebDataBinderFactory.class);
        this.filterJsonArgumentResolverMock = new FilterJsonArgumentResolver(
                filterJsonTypeConverterMock,
                httpFilterAdapterMock
        );

        this.criteriaQueryMock = mock(CriteriaQuery.class);
        this.mockCriteriaBuilder = mock(CriteriaBuilder.class);
        this.mockRoot = mock(Root.class);

        when(this.methodParameterMock.getGenericParameterType()).thenReturn(mockParameterizedType);
        when(mockParameterizedType.getActualTypeArguments()).thenReturn(new Type[]{
                ChildMock.class
        });
        when(this.mockRoot.get(anyString())).thenReturn(mockPath);
        when(mockPath.get(anyString())).thenReturn(mockPath);
        when(this.mockCriteriaBuilder.function(
                eq(CaseInsensitiveLikeSQLFunction.FUNC_NAME),
                eq(Boolean.class),
                any(),
                any()
        )).thenReturn(mockExpression);
        when(mockPath.in(any(Collection.class))).thenReturn(mockPredicate);
        when(this.nativeWebRequestMock.getNativeRequest(HttpServletRequest.class)).thenReturn(this.httpServletRequestMock);
    }

    @Test
    public void supportsParameter_ShouldReturnTrue_WhenParameterIsSpecification() {
        when(this.methodParameterMock.getParameter()).thenReturn(this.reflectParameter);
        when(this.reflectParameter.getType()).thenAnswer(_ -> Specification.class);

        var result =
                this.filterJsonArgumentResolverMock.supportsParameter(this.methodParameterMock);

        assertTrue(result);
    }

    @Test
    public void resolveArgument_ShouldReturnSpecification_WhenProcessingMultipleSimpleOperations()
            throws Exception {
        var filterJson =
                "[{\"operation\":\"NOT_EMPTY\", " + "\"field\":\"name.first\", "
                        + "\"value\":\"John\"}, " + "{\"operation\":\"EMPTY\", "
                        + "\"field\":\"age\", " + "\"value\":\"21\"}]";
        when(this.nativeWebRequestMock.getParameter("filter")).thenReturn(filterJson);
        var result =
                (Specification<?>) this.filterJsonArgumentResolverMock.resolveArgument(
                this.methodParameterMock,
                this.modelAndViewContainerMock,
                this.nativeWebRequestMock,
                this.webDataBinderFactoryMock
        );

        assertNotNull(result);
        assertInstanceOf(
                Specification.class,
                result
        );
        result.toPredicate(
                this.mockRoot,
                this.criteriaQueryMock,
                this.mockCriteriaBuilder
        );
    }

    @Test
    public void resolveArgument_ShouldReturnSpecification_WhenProcessingComplexGroupFilter()
            throws Exception {
        var filterJson = "{\"groupOperations\":[" + "{\"operation\":\"EQ\", "
                + "\"field\":\"name\", " + "\"value\":\"John\"},"
                + "{\"operation\":\"IN\", " + "\"field\":\"grades\", "
                + "\"value\":[100, 95, 90]}]," + "\"nonPriorityGroupOperators"
                + "\":[\"AND\"]," + "\"rightSideOperands\":{"
                + "\"unaryGroupOperator\": \"OR\"," + "\"unaryGroup\": {"
                + "\"groupOperations\":[" + "{\"operation\":\"GTE\", "
                + "\"field\":\"age\"," + " \"value\":18}],"
                + "\"nonPriorityGroupOperators\":[], "
                + "\"rightSideOperands\": " + "null" + "}}}";
        when(this.nativeWebRequestMock.getParameter("complexFilter")).thenReturn(filterJson);
        var result =
                (Specification<?>) this.filterJsonArgumentResolverMock.resolveArgument(
                this.methodParameterMock,
                this.modelAndViewContainerMock,
                this.nativeWebRequestMock,
                this.webDataBinderFactoryMock
        );

        assertInstanceOf(
                Specification.class,
                result
        );
    }

    @ParameterizedTest
    @MethodSource("simpleFilterDataProvider")
    void resolveArgument_ShouldReturnSpecification_ForVariousFilterTypes(String filterJson)
            throws Exception {
        when(this.nativeWebRequestMock.getParameter("filter")).thenReturn(filterJson);
        var result =
                (Specification<?>) this.filterJsonArgumentResolverMock.resolveArgument(
                this.methodParameterMock,
                this.modelAndViewContainerMock,
                this.nativeWebRequestMock,
                this.webDataBinderFactoryMock
        );

        assertNotNull(result);
        assertInstanceOf(
                Specification.class,
                result
        );
        result.toPredicate(
                this.mockRoot,
                this.criteriaQueryMock,
                this.mockCriteriaBuilder
        );
    }
}