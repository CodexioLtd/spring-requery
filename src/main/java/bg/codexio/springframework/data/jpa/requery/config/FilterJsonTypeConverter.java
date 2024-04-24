package bg.codexio.springframework.data.jpa.requery.config;

import java.util.function.BiFunction;

public interface FilterJsonTypeConverter {

    Comparable<?> convert(Class<?> type, String value);

    <T extends Comparable<?>> void addConversion(Class<T> type, BiFunction<String, Class<?>, T> delegate);
}
