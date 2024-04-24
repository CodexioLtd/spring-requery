package bg.codexio.springframework.data.jpa.requery.resolver;

import java.lang.reflect.Field;

record FieldInfo(
        Field field,
        Class<?> type
) {
}
