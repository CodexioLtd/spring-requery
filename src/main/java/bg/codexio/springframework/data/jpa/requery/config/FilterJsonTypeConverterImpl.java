package bg.codexio.springframework.data.jpa.requery.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Implementation of {@link FilterJsonTypeConverter} that provides mechanisms
 * to convert JSON string values into Java {@link Comparable} types.
 */
public class FilterJsonTypeConverterImpl
        implements FilterJsonTypeConverter {
    private final Map<Class<?>, BiFunction<String, Class<?>, ?
            extends Comparable<?>>> conversions = new HashMap<>();

    private final BiFunction<String, Class<?>, ? extends Comparable<?>> defaultConversion = (x, y) -> x;

    /**
     * Converts the given string value to a Java type as specified by the
     * class parameter using the registered conversion function.
     *
     * @param type  the {@link Class} of the type to which the value should
     *              be converted
     * @param value the string value to convert
     * @return the converted value as {@link Comparable}, or the input string
     * itself if no specific conversion can be applied.
     */
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

    /**
     * Registers a new conversion function that will be used to convert
     * strings to the specified type.
     * This method can be used to extend the converter's capabilities by
     * adding custom conversions.
     *
     * @param type     the {@link Class} representing the type to which the
     *                 conversion applies
     * @param delegate the conversion function that takes a string and a
     *                 {@link Class} and returns an instance of the type
     */
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