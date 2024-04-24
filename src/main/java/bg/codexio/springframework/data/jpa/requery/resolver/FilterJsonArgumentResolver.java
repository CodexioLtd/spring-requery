package bg.codexio.springframework.data.jpa.requery.resolver;

import bg.codexio.springframework.data.jpa.requery.config.FilterJsonTypeConverter;
import bg.codexio.springframework.data.jpa.requery.payload.FilterGroupRequest;
import bg.codexio.springframework.data.jpa.requery.payload.FilterLogicalOperator;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequest;
import bg.codexio.springframework.data.jpa.requery.resolver.function.CaseInsensitiveLikeSQLFunction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;

@Component
public class FilterJsonArgumentResolver
        implements HandlerMethodArgumentResolver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ObjectMapper objectMapper;

    private final FilterJsonTypeConverter converter;

    public FilterJsonArgumentResolver(
            ObjectMapper objectMapper,
            FilterJsonTypeConverter converter
    ) {
        this.objectMapper = objectMapper;
        this.converter = converter;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameter()
                        .getType()
                        .equals(Specification.class);
    }

    @Override
    public Object resolveArgument(
            @NotNull MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            @NotNull NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) throws Exception {
        String filterJson = ((ServletWebRequest) webRequest).getParameter(
                "filter");
        String complexFilterJson =
                ((ServletWebRequest) webRequest).getParameter("complexFilter");

        var genericType =
                (Class<?>) ((ParameterizedType) parameter.getGenericParameterType()).getActualTypeArguments()[0];

        if (filterJson != null) {
            return this.getSimpleFilterSpecification(
                    filterJson,
                    genericType
            );
        }

        if (complexFilterJson != null) {
            return this.getComplexFilterSpecification(
                    complexFilterJson,
                    genericType
            );
        }

        return this.noFilterSpecification();
    }

    private Specification<Object> noFilterSpecification() {
        return Specification.where(null);
    }

    private Specification<Object> getComplexFilterSpecification(
            String complexFilterJson,
            Class<?> genericType
    ) throws JsonProcessingException {
        return this.computeRecursiveRightLeftSideQuery(
                this.noFilterSpecification(),
                FilterLogicalOperator.AND,
                this.objectMapper.readValue(
                        complexFilterJson,
                        FilterGroupRequest.class
                ),
                genericType
        );
    }

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
                FilterLogicalOperator.AND
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
                leftSide,
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

    private Specification<Object> getSimpleFilterSpecification(
            String filterJson,
            Class<?> genericType
    ) throws JsonProcessingException {
        var specification = noFilterSpecification();

        if (!filterJson.startsWith("[")) {
            return this.getSpecification(
                    specification,
                    this.objectMapper.readValue(
                            filterJson,
                            FilterRequest.class
                    ),
                    genericType,
                    FilterLogicalOperator.AND
            );
        }

        for (var filter : this.objectMapper.readValue(
                filterJson,
                FilterRequest[].class
        )) {
            specification = getSpecification(
                    specification,
                    filter,
                    genericType,
                    FilterLogicalOperator.AND
            );
        }

        return specification;
    }

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
                (root, cq, cb) -> this.getFilterPredicate(filter,
                                                          genericType,
                                                          value,
                                                          root,
                                                          cb
                )
        );
    }

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
            case IN -> this.in(filter,
                               genericType,
                               root
            );
            case NOT_IN -> this.notIn(filter,
                                      genericType,
                                      root
            );
            case BEGINS_WITH_CASEINS -> this.caseInsensitiveLikeFunction(filter,
                                                                         root,
                                                                         cb,
                                                                         filter.value()
                                                                                 + "%"
            );
            case ENDS_WITH_CASEINS -> this.caseInsensitiveLikeFunction(filter,
                                                                       root,
                                                                       cb,
                                                                       "%"
                                                                               + filter.value()
            );
            case CONTAINS_CASEINS -> this.caseInsensitiveLikeFunction(filter,
                                                                      root,
                                                                      cb,
                                                                      "%"
                                                                              + filter.value()
                                                                              + "%"
            );
        };
    }

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

    private List<? extends Comparable> convertValue(
            String field,
            Class<?> type,
            List<? extends Comparable> value
    ) {
        try {
            return value.stream()
                        .map(v -> {
                            try {
                                var fieldInfo = this.getFieldInfo(
                                        field,
                                        type
                                );

                                return this.converter.convert(
                                        fieldInfo.type(),
                                        v.toString()
                                );
                            } catch (ReflectiveOperationException e) {
                                throw new RuntimeException(e);
                            }
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

    private Path joinOrGet(
            Path path,
            String field
    ) {
        return path.get(field);
    }

    private FieldInfo getFieldInfo(
            String fieldPath,
            Class<?> type
    ) throws ReflectiveOperationException {
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

    private Predicate in(
            FilterRequest filter,
            Class<?> genericType,
            Root<Object> root
    ) {
        return this.getPath(
                           root,
                           filter
                   )
                   .in(this.convertValue(filter.field(),
                                         genericType,
                                         (List<? extends Comparable>) filter.value()
                   ));
    }

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