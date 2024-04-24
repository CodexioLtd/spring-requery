package bg.codexio.springframework.data.jpa.requery.payload;

public record UnaryGroupRequest(
        FilterGroupRequest unaryGroup,
        FilterLogicalOperator unaryGroupOperator
) {
}
