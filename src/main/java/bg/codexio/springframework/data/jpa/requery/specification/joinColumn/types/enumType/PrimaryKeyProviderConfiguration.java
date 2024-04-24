package bg.codexio.springframework.data.jpa.requery.specification.joinColumn.types.enumType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrimaryKeyProviderConfiguration {

    @Bean
    public PrimaryKeyProvider primaryKeyProvider() {
        return new LongTypePrimaryKeyProvider();
    }
}