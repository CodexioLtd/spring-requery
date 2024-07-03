package bg.codexio.springframework.data.jpa.requery.resolver;

import bg.codexio.springframework.data.jpa.requery.payload.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.ValueBindJpaCriteriaParameter;
import org.hibernate.query.sqm.tree.predicate.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

public class ReversibleSpecification<T>
        implements Specification<T> {

    private final Specification<T> delegate;

    private final List<FilterRequest> filterRequests = new ArrayList<>();

    private FilterGroupRequest filterGroups;

    public ReversibleSpecification(
            Specification<T> delegate,
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder
    ) {
        this.delegate = delegate;
        this.toPredicate(
                root,
                query,
                criteriaBuilder
        );
    }

    @Override
    public Predicate toPredicate(
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder
    ) {
        var predicate = this.delegate.toPredicate(
                root,
                query,
                criteriaBuilder
        );
        this.captureFilterCriteria(predicate);

        return predicate;
    }

    public FilterGroupRequest toRequest() {
        if (Objects.nonNull(this.filterGroups)) {
            return this.filterGroups;
        }

        return new FilterGroupRequest(
                this.filterRequests.toArray(FilterRequest[]::new),
                null,
                null
        );
    }

    private Object captureFilterCriteria(Predicate predicate) {
        return switch (predicate) {
            case SqmInListPredicate<?> listPredicate ->
                    this.handleSqmInListPredicate(listPredicate);
            case SqmLikePredicate likePredicate ->
                    this.handleSqmLikePredicate(likePredicate);
            case SqmComparisonPredicate comparisonPredicate ->
                    this.handleSqmComparisonPredicate(comparisonPredicate);
            case SqmNullnessPredicate nullnessPredicate ->
                    this.handleSqmNullnessPredicate(nullnessPredicate);
            case SqmJunctionPredicate junctionPredicate -> {
                var filterGroup =
                        this.handleJunctionPredicate(junctionPredicate);
                this.filterGroups = filterGroup;

                yield filterGroup;
            }
            default -> null;
        };
    }

    private FilterRequest handleSqmInListPredicate(SqmInListPredicate<?> predicate) {
        var testExpression = predicate.getTestExpression();

        if (testExpression instanceof SelfRenderingSqmFunction<?> expression) {
            return this.handleSelfRenderingSqmFunction(expression);
        } else if (testExpression instanceof SqmBasicValuedSimplePath<?> expression) {
            return this.handleSqmBasicValuedSimplePath(
                    expression,
                    predicate
            );
        }

        return null;
    }

    private FilterRequest handleSelfRenderingSqmFunction(SelfRenderingSqmFunction<?> expression) {
        var function = (SelfRenderingSqmFunction<?>) expression.getArguments()
                                                               .getFirst();
        var field = (
                (SqmBasicValuedSimplePath<?>) function.getArguments()
                                                      .getFirst()
        ).getNavigablePath()
         .getLocalName();
        var literal = (SqmLiteral<?>) expression.getArguments()
                                                .get(1);
        var value = literal.getLiteralValue()
                           .toString();
        var operation = this.determineOperation(
                value,
                Boolean.TRUE
        );

        value = value.replace(
                "%",
                ""
        );
        var filterRequest = new FilterRequest(
                field,
                value,
                operation
        );
        this.filterRequests.add(filterRequest);

        return filterRequest;
    }

    private FilterRequest handleSqmBasicValuedSimplePath(
            SqmBasicValuedSimplePath<?> expression,
            SqmInListPredicate<?> predicate
    ) {
        var field = expression.getNavigablePath()
                              .getLocalName();
        var value = predicate.getListExpressions()
                             .stream()
                             .map(obj -> (
                                     (ValueBindJpaCriteriaParameter<?>) obj
                             ).getValue())
                             .toList();
        var operation = predicate.isNegated()
                        ? FilterOperation.NOT_IN
                        : FilterOperation.IN;

        var filterRequest = new FilterRequest(
                field,
                value,
                operation
        );
        this.filterRequests.add(filterRequest);

        return filterRequest;
    }

    private FilterRequest handleSqmLikePredicate(SqmLikePredicate predicate) {
        var operation = (FilterOperation) null;
        var matchExpression = predicate.getMatchExpression();
        var filterRequest = (FilterRequest) null;
        if (matchExpression instanceof SelfRenderingSqmFunction<?> inMatchExpressions) {
            var args = inMatchExpressions.getArguments();

            var path =
                    ((SqmBasicValuedSimplePath<?>) args.getFirst()).getNavigablePath();
            var field = path.getLocalName();
            var value =
                    (String) ((ValueBindJpaCriteriaParameter<?>) predicate.getPattern()).getValue();
            operation = this.determineOperation(
                    value,
                    Boolean.FALSE
            );
            value = value.replace(
                    "%",
                    ""
            );
            filterRequest = new FilterRequest(
                    field,
                    value,
                    operation
            );
            this.filterRequests.add(filterRequest);
        }

        return filterRequest;
    }

    private FilterRequest handleSqmComparisonPredicate(SqmComparisonPredicate predicate) {
        var left =
                (SqmBasicValuedSimplePath<?>) predicate.getLeftHandExpression();
        var field = left.getNavigablePath()
                        .getLocalName();
        var right =
                (ValueBindJpaCriteriaParameter<?>) predicate.getRightHandExpression();
        var value = right.getValue();
        var operator =
                this.determineComparisonOperator(predicate.getSqmOperator()
                                                                 .name());

        var filterRequest = new FilterRequest(
                field,
                value,
                operator
        );
        this.filterRequests.add(filterRequest);

        return filterRequest;
    }

    private FilterRequest handleSqmNullnessPredicate(SqmNullnessPredicate predicate) {
        var expression =
                (SqmBasicValuedSimplePath<?>) predicate.getExpression();
        var field = expression.getNavigablePath()
                              .getLocalName();

        var operation = predicate.isNegated()
                        ? FilterOperation.NOT_EMPTY
                        : FilterOperation.EMPTY;

        var filterRequest = new FilterRequest(
                field,
                null,
                operation
        );
        this.filterRequests.add(filterRequest);

        return filterRequest;
    }

    private FilterGroupRequest handleJunctionPredicate(SqmJunctionPredicate junctionPredicate) {
        var predicates = junctionPredicate.getPredicates();
        var groupOperations = new ArrayList<FilterRequest>();
        var nonPriorityGroupOperators = new ArrayList<FilterLogicalOperator>();
        var rightSideOperands = (UnaryGroupRequest) null;

        for (var i = 0; i < predicates.size(); i++) {
            var currentPredicate = predicates.get(i);
            rightSideOperands = this.processPredicate(
                    junctionPredicate,
                    groupOperations,
                    rightSideOperands,
                    currentPredicate,
                    i,
                    predicates.size()
            );
        }

        this.processNestedJunctionPredicates(
                predicates,
                nonPriorityGroupOperators
        );
        this.adjustOperatorsForGroupOperations(
                groupOperations,
                nonPriorityGroupOperators,
                junctionPredicate
        );

        return new FilterGroupRequest(
                groupOperations.toArray(new FilterRequest[0]),
                nonPriorityGroupOperators.toArray(new FilterLogicalOperator[0]),
                rightSideOperands
        );
    }

    private UnaryGroupRequest processPredicate(
            SqmJunctionPredicate junctionPredicate,
            List<FilterRequest> groupOperations,
            UnaryGroupRequest rightSideOperands,
            Predicate currentPredicate,
            int index,
            int totalPredicates
    ) {
        return currentPredicate instanceof SqmJunctionPredicate nestedPredicate
               ? this.processNestedPredicate(
                junctionPredicate,
                groupOperations,
                rightSideOperands,
                nestedPredicate,
                index,
                totalPredicates
        )
               : this.processSimplePredicate(
                       junctionPredicate,
                       groupOperations,
                       rightSideOperands,
                       currentPredicate,
                       index,
                       totalPredicates
               );
    }

    private UnaryGroupRequest processNestedPredicate(
            SqmJunctionPredicate junctionPredicate,
            List<FilterRequest> groupOperations,
            UnaryGroupRequest rightSideOperands,
            SqmJunctionPredicate nestedPredicate,
            Integer index,
            Integer totalPredicates
    ) {
        if (index == totalPredicates - 1) {
            return new UnaryGroupRequest(
                    handleJunctionPredicate(nestedPredicate),
                    determinePredicateBooleanOperator(junctionPredicate.getOperator())
            );
        } else {
            var nestedGroup = handleJunctionPredicate(nestedPredicate);
            groupOperations.addAll(Arrays.asList(nestedGroup.groupOperations()));
            if (Objects.nonNull(nestedGroup.rightSideOperands())) {
                groupOperations.addAll(Arrays.asList(nestedGroup.rightSideOperands()
                                                                .unaryGroup()
                                                                .groupOperations()));
            }
        }

        return rightSideOperands;
    }

    private UnaryGroupRequest processSimplePredicate(
            SqmJunctionPredicate junctionPredicate,
            List<FilterRequest> groupOperations,
            UnaryGroupRequest rightSideOperands,
            Predicate currentPredicate,
            Integer index,
            Integer totalPredicates
    ) {
        var filterRequest =
                (FilterRequest) this.captureFilterCriteria(currentPredicate);
        if (Objects.isNull(filterRequest)) {
            return rightSideOperands;
        }

        if (index == totalPredicates - 1 && Objects.isNull(rightSideOperands)) {
            return new UnaryGroupRequest(
                    new FilterGroupRequest(
                            new FilterRequest[]{filterRequest},
                            new FilterLogicalOperator[0],
                            null
                    ),
                    determinePredicateBooleanOperator(junctionPredicate.getOperator())
            );
        } else {
            groupOperations.add(filterRequest);
        }

        return rightSideOperands;
    }

    private void processNestedJunctionPredicates(
            List<SqmPredicate> predicates,
            List<FilterLogicalOperator> nonPriorityGroupOperators
    ) {
        var iterator = predicates.getFirst();
        while (iterator instanceof SqmJunctionPredicate jPredicate) {
            nonPriorityGroupOperators.add(determinePredicateBooleanOperator(jPredicate.getOperator()));
            iterator = jPredicate.getPredicates()
                                 .getFirst();
        }
        Collections.reverse(nonPriorityGroupOperators);
    }

    private void adjustOperatorsForGroupOperations(
            List<FilterRequest> groupOperations,
            List<FilterLogicalOperator> nonPriorityGroupOperators,
            SqmJunctionPredicate junctionPredicate
    ) {
        if (groupOperations.size() > 1 && nonPriorityGroupOperators.size()
                < groupOperations.size() - 1) {
            nonPriorityGroupOperators.add(determinePredicateBooleanOperator(junctionPredicate.getOperator()));
        }
        if (groupOperations.size() == 1
                && !nonPriorityGroupOperators.isEmpty()) {
            nonPriorityGroupOperators.clear();
        }
    }

    private FilterLogicalOperator determinePredicateBooleanOperator(Predicate.BooleanOperator operator) {
        return switch (operator) {
            case AND -> FilterLogicalOperator.AND;
            case OR -> FilterLogicalOperator.OR;
        };
    }

    private FilterOperation determineComparisonOperator(String name) {
        return switch (name) {
            case "EQUAL" -> FilterOperation.EQ;
            case "GREATER_THAN" -> FilterOperation.GT;
            case "GREATER_THAN_OR_EQUAL" -> FilterOperation.GTE;
            case "LESS_THAN" -> FilterOperation.LT;
            case "LESS_THAN_OR_EQUAL" -> FilterOperation.LTE;
            default -> null;
        };
    }

    private FilterOperation determineOperation(
            String pattern,
            Boolean caseInsensitive
    ) {
        return switch (pattern) {
            case String p when p.startsWith("%") && p.endsWith("%") ->
                    caseInsensitive
                    ? FilterOperation.CONTAINS_CASEINS
                    : FilterOperation.CONTAINS;
            case String p when p.startsWith("%") -> caseInsensitive
                                                    ?
                                                    FilterOperation.ENDS_WITH_CASEINS
                                                    : FilterOperation.ENDS_WITH;
            case String p when p.endsWith("%") -> caseInsensitive
                                                  ?
                                                  FilterOperation.BEGINS_WITH_CASEINS
                                                  : FilterOperation.BEGINS_WITH;
            default -> null;
        };
    }
}
