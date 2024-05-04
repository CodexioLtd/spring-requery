package bg.codexio.springframework.data.jpa.requery.config;

import bg.codexio.springframework.data.jpa.requery.specification.joinColumn.types.enumType.JoinColumnEnumeration;
import bg.codexio.springframework.data.jpa.requery.specification.joinColumn.types.enumType.PrimaryKeyProvider;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiFunction;

/**
 * Configuration class to handle the conversion of JSON types to Java types
 * within the application.
 * It supports custom conversions for {@link LocalDateTime},
 * {@link LocalDate}, {@link Boolean},
 * and {@link JoinColumnEnumeration}.
 */
@Configuration
public class FilterJsonTypeConversionConfiguration {
    public FilterJsonTypeConversionConfiguration(
            FilterJsonTypeConverter converter,
            PrimaryKeyProvider primaryKeyProvider
    ) {
        converter.addConversion(
                LocalDateTime.class,
                (input, type) -> LocalDateTime.parse(
                        input,
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                )
        );

        converter.addConversion(
                LocalDate.class,
                (input, type) -> LocalDate.parse(
                        input,
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                )
        );

        BiFunction<String, Class<?>, Boolean> boolConversion =
                (input, type) -> {
            if (input.equalsIgnoreCase("1")) {
                return true;
            }

            if (input.equalsIgnoreCase("0")) {
                return false;
            }

            return Boolean.parseBoolean(input);
        };

        converter.addConversion(
                Boolean.class,
                boolConversion
        );
        converter.addConversion(
                boolean.class,
                boolConversion
        );

        converter.addConversion(
                JoinColumnEnumeration.class,
                (input, actualType) -> JoinColumnEnumeration.fromId(
                        primaryKeyProvider.convertPrimaryKeyFromString(input),
                        (Class<? extends Enum<?>>) actualType
                )
        );
    }
}