package bg.codexio.springframework.data.jpa.requery.specification.joinColumn.types.enumType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JoinColumnEnumerationTest {
    @Test
    void whenFromIdIsCalledWithEnumMissingGetId_thenReflectiveOperationExceptionIsThrown() {
        enum NonCompliantEnum {
            TEST
        }

        assertThrows(
                RuntimeException.class,
                () -> JoinColumnEnumeration.fromId(
                        1,
                        NonCompliantEnum.class
                )
        );
    }
}