package bg.codexio.springframework.data.jpa.requery.payload;

public record FilterRequest(
        String field,
        Object value,
        FilterOperation operation
) {
}