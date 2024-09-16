package bg.codexio.springframework.data.jpa.requery.resolver;

import bg.codexio.springframework.data.jpa.requery.adapter.JsonHttpFilterAdapter;
import bg.codexio.springframework.data.jpa.requery.config.FilterJsonTypeConverter;
import bg.codexio.springframework.data.jpa.requery.payload.FilterGroupRequest;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequest;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequestWrapper;
import bg.codexio.springframework.data.jpa.requery.resolver.function.CaseInsensitiveLikeSQLFunction;
import bg.codexio.springframework.data.jpa.requery.test.objects.ChildMock;
import bg.codexio.springframework.data.jpa.requery.test.objects.ParentMock;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FilterJsonArgumentResolverTest {
    private MethodParameter methodParameterMock;
    private java.lang.reflect.Parameter reflectParameter;
    private NativeWebRequest nativeWebRequestMock;
    private HttpServletRequest httpServletRequestMock;
    private ModelAndViewContainer modelAndViewContainerMock;
    private WebDataBinderFactory webDataBinderFactoryMock;
    private FilterJsonArgumentResolver filterJsonArgumentResolver;
    private CriteriaQuery criteriaQueryMock;
    private CriteriaBuilder mockCriteriaBuilder;
    private Root mockRoot;
    private static ObjectMapper objectMapperMock;
    private JsonHttpFilterAdapter httpFilterAdapterMock;
    private Predicate mockPredicate;
    private FilterJsonTypeConverter filterJsonTypeConverterMock;

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

    private static FilterRequestWrapper<Object> createMockSimpleFilterRequestWrapper(String filterJson) {
        try {
            if (filterJson == null || filterJson.isEmpty() || filterJson.isBlank()) {
                return new FilterRequestWrapper<>();
            }
            if (!filterJson.startsWith("[")) {
                var filterRequest = objectMapperMock.readValue(
                        filterJson,
                        FilterRequest.class
                );
                if (filterRequest == null) {
                    return new FilterRequestWrapper<>();
                }
                return new FilterRequestWrapper<>(List.of(filterRequest));
            }

            return new FilterRequestWrapper<>(List.of(objectMapperMock.readValue(filterJson, FilterRequest[].class)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static FilterRequestWrapper<Object> createMockComplexFilterRequestWrapper(String filterJson) {
        if (filterJson == null || filterJson.isEmpty() || filterJson.isBlank()) {
            return new FilterRequestWrapper<>();
        }
        try {
            return new FilterRequestWrapper<>(objectMapperMock.readValue(filterJson, FilterGroupRequest.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setup() {
        var mockPath = mock(Path.class);
        var mockExpression = mock(Expression.class);
        var mockParameterizedType = mock(ParameterizedType.class);

        this.filterJsonTypeConverterMock = mock(FilterJsonTypeConverter.class);
        this.objectMapperMock = new ObjectMapper();
        this.httpFilterAdapterMock = mock(JsonHttpFilterAdapter.class);
        this.methodParameterMock = mock(MethodParameter.class);
        this.reflectParameter = mock(java.lang.reflect.Parameter.class);
        this.nativeWebRequestMock = mock(NativeWebRequest.class);
        this.httpServletRequestMock = mock(HttpServletRequest.class);
        this.modelAndViewContainerMock = mock(ModelAndViewContainer.class);
        this.webDataBinderFactoryMock = mock(WebDataBinderFactory.class);
        this.filterJsonArgumentResolver = new FilterJsonArgumentResolver(
                filterJsonTypeConverterMock,
                this.httpFilterAdapterMock
        );

        this.mockCriteriaBuilder = mock(CriteriaBuilder.class);
        this.mockRoot = mock(Root.class);
        this.criteriaQueryMock = mock(CriteriaQuery.class);
        this.mockPredicate = mock(Predicate.class);

        when(this.methodParameterMock.getGenericParameterType()).thenReturn(mockParameterizedType);
        when(mockParameterizedType.getActualTypeArguments()).thenReturn(new Type[]{
                ParentMock.class
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
                this.filterJsonArgumentResolver.supportsParameter(this.methodParameterMock);

        assertTrue(result);
    }

    @Test
    public void resolveArgument_ShouldReturnSpecification_WhenProcessingMultipleSimpleOperations()
            throws Exception {
        var filterJson =
                "[{\"operation\":\"CONTAINS\", \"field\":\"name\", \"value\":\"John\"}, "
                        + "{\"operation\":\"EQ\", \"field\":\"age\", \"value\":21}, "
                        + "{\"operation\":\"EMPTY\", \"field\":\"grades\", \"value\":[6]}, "
                        + "{\"operation\":\"NOT_EMPTY\", \"field\":\"email\", \"value\":\"emailAddres@example.com\"}]";
        var filterWrapperMock = createMockSimpleFilterRequestWrapper(filterJson);
        when(this.nativeWebRequestMock.getParameter("filter")).thenReturn(filterJson);
        doReturn(filterWrapperMock).when(httpFilterAdapterMock).adapt(this.httpServletRequestMock);

        var namePredicate = mock(Predicate.class);
        var agePredicate = mock(Predicate.class);
        var gradesPredicate = mock(Predicate.class);
        var emailPredicate = mock(Predicate.class);
        var nameANDAgePredicate = mock(Predicate.class);
        var nameANDAgeANDGradesPredicate = mock(Predicate.class);
        var nameANDAgeANDGradesANDEmailPredicate = mock(Predicate.class);

        var namePath = mock(Path.class);
        var agePath = mock(Path.class);
        var gradesPath = mock(Path.class);
        var emailPath = mock(Path.class);

        when(mockRoot.get("name")).thenReturn(namePath);
        when(mockRoot.get("age")).thenReturn(agePath);
        when(mockRoot.get("grades")).thenReturn(gradesPath);
        when(mockRoot.get("email")).thenReturn(emailPath);
        when(namePath.as(String.class)).thenReturn(namePath);

        when(mockCriteriaBuilder.like(eq(namePath), eq("%John%"))).thenReturn(namePredicate);
        when(mockCriteriaBuilder.equal(eq(agePath), eq(21))).thenReturn(agePredicate);
        when(mockCriteriaBuilder.isNull(eq(gradesPath))).thenReturn(gradesPredicate);
        when(mockCriteriaBuilder.isNotNull(eq(emailPath))).thenReturn(emailPredicate);
        when(mockCriteriaBuilder.and(namePredicate, agePredicate)).thenReturn(nameANDAgePredicate);
        when(mockCriteriaBuilder.and(nameANDAgePredicate, gradesPredicate)).thenReturn(nameANDAgeANDGradesPredicate);
        when(mockCriteriaBuilder.and(nameANDAgeANDGradesPredicate, emailPredicate)).thenReturn(nameANDAgeANDGradesANDEmailPredicate);

        doReturn("John").when(filterJsonTypeConverterMock).convert(eq(String.class), eq("John"));
        doReturn(21).when(filterJsonTypeConverterMock).convert(eq(Long.class), eq("21"));

        var result =
                (Specification<?>) this.filterJsonArgumentResolver.resolveArgument(
                        this.methodParameterMock,
                        this.modelAndViewContainerMock,
                        this.nativeWebRequestMock,
                        this.webDataBinderFactoryMock
                );

        var resultPredicate = result.toPredicate(mockRoot, criteriaQueryMock, mockCriteriaBuilder);

        assertNotNull(resultPredicate);
        assertEquals(nameANDAgeANDGradesANDEmailPredicate.getExpressions(), resultPredicate.getExpressions());

        verify(mockRoot).get("name");
        verify(mockRoot).get("age");
        verify(mockCriteriaBuilder).like(eq(namePath), eq("%John%"));
        verify(mockCriteriaBuilder).equal(eq(agePath), eq(21));
        verify(mockCriteriaBuilder).isNull(eq(gradesPath));
        verify(mockCriteriaBuilder).isNotNull(eq(emailPath));
        verify(mockCriteriaBuilder).and(namePredicate, agePredicate);
        verify(mockCriteriaBuilder).and(nameANDAgePredicate, gradesPredicate);
        verify(mockCriteriaBuilder).and(nameANDAgeANDGradesPredicate, emailPredicate);
    }

    @Test
    public void resolveArgument_ShouldReturnSpecification_WhenProcessingComplexGroupFilter()
            throws Exception {
        var filterJson = "{\"groupOperations\":["
                + "{\"field\":\"role\",\"operation\":\"CONTAINS\",\"value\":\"operator\"}],"
                + "\"nonPriorityGroupOperators\":[\"AND\"],"
                + "\"rightSideOperands\":{\"unaryGroupOperator\":\"OR\","
                + "\"unaryGroup\":{\"groupOperations\":["
                + "{\"field\":\"grades\",\"operation\":\"IN\",\"value\":[100,95]},"
                + "{\"field\":\"name\",\"operation\":\"BEGINS_WITH\",\"value\":\"Doe\"}],"
                + "\"nonPriorityGroupOperators\":[\"OR\"],"
                + "\"rightSideOperands\":{\"unaryGroupOperator\":\"AND\","
                + "\"unaryGroup\":{\"groupOperations\":["
                + "{\"field\":\"age\",\"operation\":\"GT\",\"value\":25}],"
                + "\"nonPriorityGroupOperators\":[]}}}}}";
        var filterWrapperMock = createMockComplexFilterRequestWrapper(filterJson);
        when(this.nativeWebRequestMock.getParameter("complexFilter")).thenReturn(filterJson);
        doReturn(filterWrapperMock).when(httpFilterAdapterMock).adapt(this.httpServletRequestMock);

        // Mock predicates for each field
        var rolePredicate = mock(Predicate.class);
        var gradesPredicate = mock(Predicate.class);
        var namePredicate = mock(Predicate.class);
        var agePredicate = mock(Predicate.class);
        var secondLevel = mock(Predicate.class);
        var secondANDThirdLevel = mock(Predicate.class);
        var finalPredicate = mock(Predicate.class);

        var namePath = mock(Path.class);
        var gradesPath = mock(Path.class);
        var agePath = mock(Path.class);
        var rolePath = mock(Path.class);

        when(mockRoot.get("name")).thenReturn(namePath);
        when(mockRoot.get("grades")).thenReturn(gradesPath);
        when(mockRoot.get("age")).thenReturn(agePath);
        when(mockRoot.get("role")).thenReturn(rolePath);
        when(rolePath.as(String.class)).thenReturn(rolePath);
        when(namePath.as(String.class)).thenReturn(namePath);

        when(mockCriteriaBuilder.like(eq(rolePath), eq("%operator%"))).thenReturn(rolePredicate);
        when(gradesPath.in(Arrays.asList(100, 95))).thenReturn(gradesPredicate);
        when(mockCriteriaBuilder.like(eq(namePath), eq("Doe%"))).thenReturn(namePredicate);
        when(mockCriteriaBuilder.greaterThan(eq(agePath), eq(25))).thenReturn(agePredicate);

        when(mockCriteriaBuilder.or(gradesPredicate, namePredicate)).thenReturn(secondLevel);
        when(mockCriteriaBuilder.and(secondLevel, agePredicate)).thenReturn(secondANDThirdLevel);
        when(mockCriteriaBuilder.or(rolePredicate, secondANDThirdLevel)).thenReturn(finalPredicate);

        doReturn("Doe").when(filterJsonTypeConverterMock).convert(eq(String.class), eq("Doe"));
        doReturn("operator").when(filterJsonTypeConverterMock).convert(eq(String.class), eq("operator"));
        doReturn(25).when(filterJsonTypeConverterMock).convert(eq(Long.class), eq("25"));
        doReturn(100).when(filterJsonTypeConverterMock).convert(eq(Integer.class), eq("100"));
        doReturn(95).when(filterJsonTypeConverterMock).convert(eq(Integer.class), eq("95"));
        doReturn("[100, 95]").when(filterJsonTypeConverterMock).convert(eq(Integer.class), eq("[100, 95]"));

        var result =
                (Specification<?>) this.filterJsonArgumentResolver.resolveArgument(
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


        var predicate = result.toPredicate(
                this.mockRoot,
                this.criteriaQueryMock,
                this.mockCriteriaBuilder
        );

        verify(mockRoot).get("role");
        verify(mockRoot).get("grades");
        verify(mockRoot).get("name");
        verify(mockRoot).get("age");

        verify(mockCriteriaBuilder).like(eq(rolePath), eq("%operator%"));
        verify(gradesPath).in(eq(Arrays.asList(100, 95)));
        verify(mockCriteriaBuilder).like(eq(namePath), eq("Doe%"));
        verify(mockCriteriaBuilder).greaterThan(eq(agePath), eq(25));

        verify(mockCriteriaBuilder).or(gradesPredicate, namePredicate);
        verify(mockCriteriaBuilder).and(secondLevel, agePredicate);
        verify(mockCriteriaBuilder).or(rolePredicate, secondANDThirdLevel);

        verify(filterJsonTypeConverterMock).convert(eq(String.class), eq("operator"));
        verify(filterJsonTypeConverterMock).convert(eq(Integer.class), eq("100"));
        verify(filterJsonTypeConverterMock).convert(eq(Integer.class), eq("95"));
        verify(filterJsonTypeConverterMock).convert(eq(String.class), eq("Doe"));
        verify(filterJsonTypeConverterMock).convert(eq(Long.class), eq("25"));

        assertNotNull(predicate);
        assertEquals(finalPredicate.getExpressions(), predicate.getExpressions());
    }

    @Test
    void testResolveArgumentWithNoFilters() throws Exception {
        var filterRequestWrapperMock = new FilterRequestWrapper<>();
        when(this.httpServletRequestMock.getParameter("filter")).thenReturn(null);
        when(this.httpServletRequestMock.getParameter("complexFilter")).thenReturn(null);
        doReturn(filterRequestWrapperMock).when(httpFilterAdapterMock).adapt(this.httpServletRequestMock);

        // Act
        var result = filterJsonArgumentResolver.resolveArgument(
                methodParameterMock, null, nativeWebRequestMock, null
        );

        // Assert
        assertEquals(noFilterSpecification(), result);
    }

    private Specification<ChildMock> noFilterSpecification() {
        return Specification.where(null);
    }

    @ParameterizedTest
    @MethodSource("simpleFilterDataProvider")
    void resolveArgument_ShouldReturnSpecification_ForVariousFilterTypes(String filterJson)
            throws Exception {
        var filterRequestWrapperMock = createMockSimpleFilterRequestWrapper(filterJson);
        when(this.nativeWebRequestMock.getParameter("filter")).thenReturn(filterJson);
        doReturn(filterRequestWrapperMock).when(httpFilterAdapterMock).adapt(this.httpServletRequestMock);

        var result =
                (Specification<?>) this.filterJsonArgumentResolver.resolveArgument(
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