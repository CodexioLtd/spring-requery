package bg.codexio.springframework.data.jpa.requery.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

public class FilterJsonTypeConverterImpl
        implements FilterJsonTypeConverter {
    private final Map<Class<?>, BiFunction<String, Class<?>, ?
            extends Comparable<?>>> conversions = new HashMap<>();

    private final BiFunction<String, Class<?>, ? extends Comparable<?>> defaultConversion = (x, y) -> x;

    @Override
    public Comparable<?> convert(
            Class<?> type,
            String value
    ) {
        var conversionDelegate = this.conversions.getOrDefault(
                type,
                null
        );

        if (conversionDelegate == null) {
            conversionDelegate = this.conversions.entrySet()
                                                 .stream()
                                                 .filter(kvp -> kvp.getKey()
                                                                   .isAssignableFrom(type))
                                                 .map(Map.Entry::getValue)
                                                 .findFirst()
                                                 .orElse(null);
        }

        return Objects.requireNonNullElse(
                              conversionDelegate,
                              this.defaultConversion
                      )
                      .apply(
                              value,
                              type
                      );
    }

    @Override
    public <T extends Comparable<?>> void addConversion(
            Class<T> type,
            BiFunction<String, Class<?>, T> delegate
    ) {
        this.conversions.put(
                type,
                delegate
        );
    }
}
