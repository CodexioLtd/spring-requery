package bg.codexio.springframework.data.jpa.requery.payload;

public record FilterGroupRequest(
        FilterRequest[] groupOperations,
        FilterLogicalOperator[] nonPriorityGroupOperators,
        UnaryGroupRequest rightSideOperands
) {
}