package bg.codexio.springframework.data.jpa.requery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterJsonTypeConverterConfiguration {
    @Bean
    public FilterJsonTypeConverter filterJsonTypeConverter() {
        return new FilterJsonTypeConverterImpl();
    }
}