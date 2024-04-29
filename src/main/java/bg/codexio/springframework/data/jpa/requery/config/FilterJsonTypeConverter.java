package bg.codexio.springframework.data.jpa.requery.config;

import java.util.function.BiFunction;

/**
 * Interface defining the contract for a type converter that converts string
 * values from JSON into Java {@link Comparable} types.
 *
 * <p>The converter is intended to be used where JSON data needs to be
 * converted into specific
 * Java types that implement the {@link Comparable} interface.</p>
 */
public interface FilterJsonTypeConverter {

    Comparable<?> convert(
            Class<?> type,
            String value
    );

    <T extends Comparable<?>> void addConversion(
            Class<T> type,
            BiFunction<String, Class<?>, T> delegate
    );
}