package bg.codexio.springframework.data.jpa.requery.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FilterJsonTypeConverterConfigurationTest {
    @Test
    void filterJsonTypeConverter_ShouldBeCreatedAndInstanceOfCorrectClass() {
        var configuration = new FilterJsonTypeConverterConfiguration();
        var converter = configuration.filterJsonTypeConverter();

        assertNotNull(converter);
        assertInstanceOf(
                FilterJsonTypeConverterImpl.class,
                converter
        );
    }
}