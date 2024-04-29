package bg.codexio.springframework.data.jpa.requery.config;

import bg.codexio.springframework.data.jpa.requery.specification.joinColumn.types.enumType.LongTypePrimaryKeyProvider;
import bg.codexio.springframework.data.jpa.requery.test.objects.EnumMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilterJsonTypeConversionConfigurationTest {
    private FilterJsonTypeConverterImpl converter;


    @BeforeEach
    void setup() {
        this.converter = new FilterJsonTypeConverterImpl();
        var primaryKeyProvider = new LongTypePrimaryKeyProvider();
        new FilterJsonTypeConversionConfiguration(
                this.converter,
                primaryKeyProvider
        );
    }

    @Test
    void convert_ShouldParseLocalDateTime_WhenGivenStringWithCustomFormat() {
        var input = "01/01/2023 00:00:00";
        var expected = LocalDateTime.of(
                2023,
                1,
                1,
                0,
                0
        );
        var result = (LocalDateTime) this.converter.convert(
                LocalDateTime.class,
                input
        );

        assertEquals(
                expected,
                result
        );
    }

    @Test
    void convert_ShouldParseLocalDate_WhenGivenStringWithCustomFormat() {
        var input = "01/01/2023";
        var expected = LocalDate.of(
                2023,
                1,
                1
        );
        var result = (LocalDate) this.converter.convert(
                LocalDate.class,
                input
        );

        assertEquals(
                expected,
                result
        );
    }

    @Test
    void convert_ShouldReturnTrue_WhenGivenTrueEquivalentStrings() {
        var trueStrings = List.of(
                "true",
                "True",
                "TRUE",
                "1"
        );
        for (var trueString : trueStrings) {
            var result = (Boolean) this.converter.convert(
                    Boolean.class,
                    trueString
            );

            assertTrue(result);
        }
    }

    @Test
    void convert_ShouldReturnFalse_WhenGivenFalseEquivalentStrings() {
        var falseStrings = List.of(
                "false",
                "False",
                "FALSE",
                "0"
        );
        for (var falseString : falseStrings) {
            var result = (Boolean) this.converter.convert(
                    Boolean.class,
                    falseString
            );

            assertFalse(result);
        }
    }

    @Test
    void convert_ShouldReturnCorrectEnum_WhenGivenPrimaryKeyString() {
        var primaryKeyAsString = "1";
        var enumType = EnumMock.class;
        var result = this.converter.convert(
                enumType,
                primaryKeyAsString
        );

        assertEquals(
                EnumMock.ITEM1,
                result
        );
    }
}