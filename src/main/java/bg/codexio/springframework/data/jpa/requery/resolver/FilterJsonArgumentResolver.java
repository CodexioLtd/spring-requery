package bg.codexio.springframework.data.jpa.requery.resolver;

import bg.codexio.springframework.data.jpa.requery.adapter.HttpFilterAdapter;
import bg.codexio.springframework.data.jpa.requery.config.FilterJsonTypeConverter;
import bg.codexio.springframework.data.jpa.requery.payload.FilterGroupRequest;
import bg.codexio.springframework.data.jpa.requery.payload.FilterLogicalOperator;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequest;
import bg.codexio.springframework.data.jpa.requery.resolver.function.CaseInsensitiveLikeSQLFunction;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;

/**
 * A Spring MVC argument resolver for converting JSON-encoded filter criteria
 * into {@link Specification} objects. This resolver allows for complex
 * filtering strategies to be applied to JPA entity queries based on JSON
 * input from web requests, supporting both simple and complex structured
 * filters.
 */
@Component
public class FilterJsonArgumentResolver
        implements HandlerMethodArgumentResolver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final FilterJsonTypeConverter converter;

    private final HttpFilterAdapter httpFilterAdapter;

    public FilterJsonArgumentResolver(
            FilterJsonTypeConverter converter,
            HttpFilterAdapter httpFilterAdapter
    ) {
        this.converter = converter;
        this.httpFilterAdapter = httpFilterAdapter;
    }

    /**
     * Determines if this resolver is applicable for the method parameter,
     * specifically checking if the parameter is of type {@link Specification}.
     *
     * @param parameter the method parameter to check
     * @return true if the parameter is a {@link Specification}, false otherwise
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameter()
                        .getType()
                        .equals(Specification.class);
    }

    /**
     * Resolves a method parameter into an argument value from a given web
     * request.
     *
     * @param parameter  the method parameter to resolve
     * @param webRequest the {@link NativeWebRequest} being handled
     * @return the resolved {@link Specification} object, or {@code null} if
     * no filters are provided
     * @throws Exception if an error occurs during argument resolution
     */
    @Override
    public Object resolveArgument(
            @NotNull MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            @NotNull NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) throws Exception {
        var request = webRequest.getNativeRequest(HttpServletRequest.class);
        var filterWrapper = this.httpFilterAdapter.adapt(request);

        var genericType =
                (Class<?>) ((ParameterizedType) parameter.getGenericParameterType()).getActualTypeArguments()[0];


        return filterWrapper.isSimple(simpleFilter -> getSimpleFilterSpecification(simpleFilter, genericType))
                .orComplex(complexFilter -> getComplexFilterSpecification(complexFilter, genericType))
                .or(this::noFilterSpecification);
    }

    /**
     * Provides a default {@link Specification} that applies no filtering to
     * the query.
     *
     * @return A {@link Specification} that does not alter the query.
     */
    private Specification<Object> noFilterSpecification() {
        return Specification.where(null);
    }

    /**
     * Parses a JSON string representing complex filtering criteria into a
     * {@link Specification}. This method handles the deserialization of
     * complex structured JSON into {@link FilterGroupRequest} and
     * recursively builds a composite {@link Specification} based on the
     * logical operations and groupings defined within the filter request.
     *
     * @param complexFilter The adapted {@link FilterGroupRequest} from the request
     * @param genericType   The entity class type on which the filter
     *                      will be applied.
     * @return A {@link Specification} representing the complex filtering
     * criteria.
     * @throws JsonProcessingException If there is an error parsing the JSON
     *                                 string.
     */
    private Specification<Object> getComplexFilterSpecification(
            FilterGroupRequest complexFilter,
            Class<?> genericType) {
        return this.computeRecursiveRightLeftSideQuery(
                this.noFilterSpecification(),
                FilterLogicalOperator.AND,
                complexFilter,
                genericType
        );
    }

    /**
     * Recursively computes a {@link Specification} by navigating through
     * nested groups of filter operations. This method starts with a base
     * specification and applies a series of logical operations and filter
     * criteria, constructing a tree of specifications that reflect the
     * complex structure of the input filter groups.
     *
     * @param startingSpecification The initial {@link Specification} to
     *                              which subsequent filters are applied.
     * @param startingOperator      The logical operator (e.g., AND, OR) to
     *                              apply between groups.
     * @param startingGroup         The {@link FilterGroupRequest} defining
     *                              the initial set of filters and nested
     *                              groups.
     * @param genericType           The entity class type on which the filter
     *                              will be applied.
     * @return A {@link Specification} that represents the combination of all
     * applied filters and operations.
     */
    private Specification<Object> computeRecursiveRightLeftSideQuery(
            Specification<Object> startingSpecification,
            FilterLogicalOperator startingOperator,
            FilterGroupRequest startingGroup,
            Class<?> genericType
    ) {
        var leftSide = this.getSpecification(
                this.noFilterSpecification(),
                startingGroup.groupOperations()[0],
                genericType,
                startingOperator
        );

        for (var i = 1; i < startingGroup.groupOperations().length; i++) {
            var leftSideOperation = startingGroup.groupOperations()[i];
            var leftSideOperator = startingGroup.nonPriorityGroupOperators()[i
                    - 1];

            leftSide = this.getSpecification(
                    leftSide,
                    leftSideOperation,
                    genericType,
                    leftSideOperator
            );
        }

        if (startingGroup.rightSideOperands() == null) {
            return this.rightLeftSideByOperator(
                    startingSpecification,
                    startingOperator,
                    leftSide
            );
        }

        var rightSide = this.computeRecursiveRightLeftSideQuery(
                noFilterSpecification(),
                startingGroup.rightSideOperands()
                             .unaryGroupOperator(),
                startingGroup.rightSideOperands()
                             .unaryGroup(),
                genericType
        );

        return this.rightLeftSideByOperator(
                leftSide,
                startingGroup.rightSideOperands()
                             .unaryGroupOperator(),
                rightSide
        );
    }

    /**
     * Parses a JSON string representing simple or list-based filter criteria
     * into a {@link Specification}. This method supports either a single
     * JSON object or an array of objects defining filter criteria, which are
     * then transformed into a {@link Specification} based on the type of
     * entity being filtered.
     *
     * @param simpleRequest The adapted simple {@link FilterRequest} from the request
     * @param genericType   The class type of the entities being filtered.
     * @return A {@link Specification} that represents the filter criteria
     * provided.
     */
    private Specification<Object> getSimpleFilterSpecification(
            List<FilterRequest> simpleRequest,
            Class<?> genericType) {
        var specification = this.noFilterSpecification();
        for (var filter : simpleRequest) {
            specification = getSpecification(
                    specification,
                    filter,
                    genericType,
                    FilterLogicalOperator.AND
            );
        }

        return specification;
    }

    /**
     * Constructs a {@link Specification} from a given filter request,
     * applying the specified logical operator. This method centralizes the
     * creation of specifications based on individual filter criteria,
     * handling value conversion and predicate creation based on the filter's
     * operation.
     *
     * @param specification The starting specification to which the new
     *                      condition will be added.
     * @param filter        The filter criteria to apply.
     * @param genericType   The type of entity being filtered.
     * @param operator      The logical operator to apply with the existing
     *                      specification.
     * @return A new {@link Specification} that includes the condition
     * derived from the filter.
     */
    private Specification<Object> getSpecification(
            Specification<Object> specification,
            FilterRequest filter,
            Class<?> genericType,
            FilterLogicalOperator operator
    ) {

        var value = this.convertValue(
                filter.field(),
                genericType,
                filter.value()
                      .toString()
        );

        return this.rightLeftSideByOperator(
                specification,
                operator,
                (root, cq, cb) -> this.getFilterPredicate(
                        filter,
                        genericType,
                        value,
                        root,
                        cb
                )
        );
    }

    /**
     * Combines two specifications using the specified logical operator.
     *
     * @param leftSide  The left-hand specification.
     * @param operator  The logical operator to use (AND, OR).
     * @param rightSide The right-hand specification.
     * @return A new {@link Specification} that represents the combination of
     * both sides using the logical operator.
     */
    private Specification<Object> rightLeftSideByOperator(
            Specification<Object> leftSide,
            FilterLogicalOperator operator,
            Specification<Object> rightSide
    ) {
        return switch (operator) {
            case AND -> leftSide.and(rightSide);
            case OR -> leftSide.or(rightSide);
        };
    }

    /**
     * Creates a JPA {@link Predicate} based on a filter request, translating
     * the filter's operation into a query condition.
     *
     * @param filter      The filter request defining the condition to apply.
     * @param genericType The type of entity being filtered.
     * @param value       The value to compare or match against, properly
     *                    converted.
     * @param root        The root of the query from which paths are derived.
     * @param cb          The {@link CriteriaBuilder} used to construct the
     *                    query predicates.
     * @return A {@link Predicate} representing the filter condition.
     */
    private Predicate getFilterPredicate(
            FilterRequest filter,
            Class<?> genericType,
            Comparable value,
            Root<Object> root,
            CriteriaBuilder cb
    ) {
        return switch (filter.operation()) {
            case EMPTY -> cb.isNull(this.getPath(
                    root,
                    filter
            ));
            case NOT_EMPTY -> cb.isNotNull(this.getPath(
                    root,
                    filter
            ));
            case EQ -> cb.equal(
                    this.getPath(
                            root,
                            filter
                    ),
                    value
            );
            case GT -> cb.greaterThan(
                    this.getPath(
                            root,
                            filter
                    ),
                    value
            );
            case GTE -> cb.greaterThanOrEqualTo(
                    this.getPath(
                            root,
                            filter
                    ),
                    value
            );
            case LT -> cb.lessThan(
                    this.getPath(
                            root,
                            filter
                    ),
                    value
            );
            case LTE -> cb.lessThanOrEqualTo(
                    this.getPath(
                            root,
                            filter
                    ),
                    value
            );
            case BEGINS_WITH -> cb.like(
                    this.getPath(
                                root,
                                filter
                        )
                        .as(String.class),
                    filter.value() + "%"
            );
            case ENDS_WITH -> cb.like(
                    this.getPath(
                                root,
                                filter
                        )
                        .as(String.class),
                    "%" + filter.value()
            );
            case CONTAINS -> cb.like(
                    this.getPath(
                                root,
                                filter
                        )
                        .as(String.class),
                    "%" + filter.value() + "%"
            );
            case IN -> this.in(
                    filter,
                    genericType,
                    root
            );
            case NOT_IN -> this.notIn(
                    filter,
                    genericType,
                    root
            );
            case BEGINS_WITH_CASEINS -> this.caseInsensitiveLikeFunction(
                    filter,
                    root,
                    cb,
                    filter.value() + "%"
            );
            case ENDS_WITH_CASEINS -> this.caseInsensitiveLikeFunction(
                    filter,
                    root,
                    cb,
                    "%" + filter.value()
            );
            case CONTAINS_CASEINS -> this.caseInsensitiveLikeFunction(
                    filter,
                    root,
                    cb,
                    "%" + filter.value() + "%"
            );
        };
    }

    /**
     * Converts a string value to its corresponding Java type based on the
     * entity's field type. This method uses the
     * {@link FilterJsonTypeConverter} to ensure that string values are
     * correctly converted to the appropriate type, facilitating type-safe
     * queries.
     *
     * @param field The name of the field whose type should guide the
     *              conversion.
     * @param type  The Java class of the entity being queried.
     * @param value The string value to be converted.
     * @return A {@link Comparable} representing the converted value or the
     * original value if conversion fails.
     */
    private Comparable convertValue(
            String field,
            Class<?> type,
            String value
    ) {
        try {
            var fieldInfo = this.getFieldInfo(
                    field,
                    type
            );

            return this.converter.convert(
                    fieldInfo.type(),
                    value
            );
        } catch (Exception e) {
            this.logger.error(
                    e.getMessage(),
                    e
            );

            return value;
        }
    }

    /**
     * Converts a list of string values to their corresponding Java types
     * based on the entity's field type. This method processes each string in
     * the list individually, applying the same type conversion as
     * {@link #convertValue(String, Class, String)} to each element.
     *
     * @param field The name of the field whose type should guide the
     *              conversion.
     * @param type  The Java class of the entity being queried.
     * @param value The list of values to be converted.
     * @return A list of {@link Comparable} representing the converted
     * values, or the original list if conversion fails.
     */
    private List<? extends Comparable> convertValue(
            String field,
            Class<?> type,
            List<? extends Comparable> value
    ) {
        try {
            return value.stream()
                        .map(v -> {
                            var fieldInfo = this.getFieldInfo(
                                    field,
                                    type
                            );

                            return this.converter.convert(
                                    fieldInfo.type(),
                                    v.toString()
                            );
                        })
                        .toList();
        } catch (RuntimeException e) {
            this.logger.error(
                    e.getMessage(),
                    e
            );

            return value;
        }
    }

    /**
     * Retrieves a {@link Path} for a specified entity field from a
     * {@link Root} object, potentially navigating nested properties based on
     * dot-separated paths.
     *
     * @param root   The {@link Root} object from which to start the path
     *               retrieval.
     * @param filter The {@link FilterRequest} containing the field
     *               information, which may include nested properties.
     * @return A {@link Path} object representing the location of the field
     * within the entity model.
     */
    private Path getPath(
            Root<Object> root,
            FilterRequest filter
    ) {
        var paths = filter.field()
                          .split("\\.");
        var path = this.joinOrGet(
                root,
                paths[0]
        );
        for (var i = 1; i < paths.length; i++) {
            path = this.joinOrGet(
                    path,
                    paths[i]
            );
        }

        return path;
    }

    /**
     * Joins a path to the next segment or retrieves the next segment if it
     * already exists. This method assists in navigating nested paths in
     * entity models.
     *
     * @param path  The current {@link Path} from which the next segment
     *              should be retrieved or joined.
     * @param field The field name representing the next segment of the path.
     * @return The updated {@link Path} including the next segment.
     */
    private Path joinOrGet(
            Path path,
            String field
    ) {
        return path.get(field);
    }

    /**
     * Retrieves detailed information about a field specified by a
     * dot-separated path within a class hierarchy. This method recursively
     * resolves each segment of the field path to determine the final field
     * and its type, which can be used for type conversion and query
     * generation purposes.
     *
     * @param fieldPath The dot-separated path to the field in the class
     *                  hierarchy.
     * @param type      The starting class from which to resolve the field path.
     * @return A {@link FieldInfo} object containing the resolved
     * {@link Field} and its type.
     */
    private FieldInfo getFieldInfo(
            String fieldPath,
            Class<?> type
    ) {
        var fields = fieldPath.split("\\.");
        var field = (Field) null;
        var currentType = type;
        var index = 0;

        do {
            field = this.getFieldFromTypeChain(
                    fields[index++],
                    currentType
            );
            currentType = Collection.class.isAssignableFrom(field.getType())
                          ?
                          (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]
                          : field.getType();
        } while (index < fields.length);

        return new FieldInfo(
                field,
                currentType
        );
    }

    /**
     * Recursively retrieves a {@link Field} from a class or its superclass
     * hierarchy based on the field name provided. This method attempts to
     * find the field in the given class and, if not found, continues to
     * search in the superclass, recursively moving up the class hierarchy
     * until the field is found or no superclass remains.
     *
     * @param field The name of the field to retrieve.
     * @param type  The class from which to start the search.
     * @return The {@link Field} object corresponding to the specified field
     * name.
     * @throws NoSuchFieldException if the field cannot be found in the class
     *                              hierarchy.
     */
    private Field getFieldFromTypeChain(
            String field,
            Class<?> type
    ) {
        try {
            return type.getDeclaredField(field);
        } catch (NoSuchFieldException ignored) {
            return this.getFieldFromTypeChain(
                    field,
                    type.getSuperclass()
            );
        }
    }

    /**
     * Creates a JPA {@link Predicate} representing an 'IN' clause for a
     * specified field and value list.
     *
     * @param filter      The filter criteria containing the field and value
     *                    list.
     * @param genericType The class type of the entities being queried.
     * @param root        The root of the query from which the field path is
     *                    derived.
     * @return A {@link Predicate} for the 'IN' condition.
     */
    private Predicate in(
            FilterRequest filter,
            Class<?> genericType,
            Root<Object> root
    ) {
        return this.getPath(
                           root,
                           filter
                   )
                   .in(this.convertValue(
                           filter.field(),
                           genericType,
                           (List<? extends Comparable>) filter.value()
                   ));
    }

    /**
     * Creates a JPA {@link Predicate} representing a 'NOT IN' clause for a
     * specified field and value list.
     *
     * @param filter      The filter criteria containing the field and value
     *                    list.
     * @param genericType The class type of the entities being queried.
     * @param root        The root of the query from which the field path is
     *                    derived.
     * @return A {@link Predicate} for the 'NOT IN' condition.
     */
    private Predicate notIn(
            FilterRequest filter,
            Class<?> genericType,
            Root<Object> root
    ) {
        return this.in(
                           filter,
                           genericType,
                           root
                   )
                   .not();
    }

    /**
     * Creates a JPA {@link Predicate} using a custom SQL function for
     * case-insensitive LIKE matching.
     *
     * @param filter  The filter criteria containing the field and value.
     * @param root    The root of the query from which the field path is
     *                derived.
     * @param cb      The {@link CriteriaBuilder} used to create the predicate.
     * @param literal The literal value to be matched in a case-insensitive
     *                manner.
     * @return A {@link Predicate} that applies the custom case-insensitive
     * LIKE SQL function.
     */
    private Predicate caseInsensitiveLikeFunction(
            FilterRequest filter,
            Root<Object> root,
            CriteriaBuilder cb,
            String literal
    ) {
        return cb.function(
                         CaseInsensitiveLikeSQLFunction.FUNC_NAME,
                         Boolean.class,
                         this.getPath(
                                     root,
                                     filter
                             )
                             .as(String.class),
                         cb.literal(literal)
                 )
                 .in(true);
    }
}