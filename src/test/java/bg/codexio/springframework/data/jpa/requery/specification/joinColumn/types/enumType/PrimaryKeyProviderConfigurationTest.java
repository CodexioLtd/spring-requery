package bg.codexio.springframework.data.jpa.requery.specification.joinColumn.types.enumType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PrimaryKeyProviderConfigurationTest {

    @Test
    void testPrimaryKeyProvider() {
        var configuration = new PrimaryKeyProviderConfiguration();
        var primaryKeyProvider = configuration.primaryKeyProvider();

        assertNotNull(primaryKeyProvider);
    }
}