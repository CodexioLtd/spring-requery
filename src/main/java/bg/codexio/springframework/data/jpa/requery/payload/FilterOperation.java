package bg.codexio.springframework.data.jpa.requery.payload;

public enum FilterOperation {
    EMPTY,
    NOT_EMPTY,
    EQ,
    GT,
    GTE,
    LT,
    LTE,
    BEGINS_WITH,
    ENDS_WITH,
    CONTAINS,
    IN,
    NOT_IN,
    BEGINS_WITH_CASEINS,
    ENDS_WITH_CASEINS,
    CONTAINS_CASEINS
}
