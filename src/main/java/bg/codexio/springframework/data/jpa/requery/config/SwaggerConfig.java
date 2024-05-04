package bg.codexio.springframework.data.jpa.requery.config;

import bg.codexio.springframework.data.jpa.requery.payload.FilterOperation;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Configuration class for customizing Swagger-generated API documentation.
 * This class provides beans that modify the behavior and presentation of API
 * operations within the Swagger UI, particularly for filtering and
 * pagination functionality in the application.
 */
@Configuration
public class SwaggerConfig {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Customizes operations in the Swagger UI related to
     * {@link Specification}-based filtering. This method configures a custom
     * {@link OperationCustomizer} that adds detailed description and example
     * for using filter parameters dynamically based on the presence of
     * {@link Specification} parameters in the method signature.
     *
     * @return an {@link OperationCustomizer} for enhancing API operation
     * documentation where filtering is applicable.
     */
    @Bean
    public OperationCustomizer customizeFiltering() {
        return (operation, handlerMethod) -> {
            var spec = Arrays.stream(handlerMethod.getMethodParameters())
                             .filter(p -> p.getParameterType()
                                     == Specification.class)
                             .findFirst()
                             .orElse(null);

            if (!tryRemove(
                    operation,
                    spec
            )) {
                return operation;
            }

            var example = new FilterRequest[]{
                    new FilterRequest(
                            "id",
                            "2",
                            FilterOperation.GT
                    ), new FilterRequest(
                    "id",
                    "5",
                    FilterOperation.LT
            ),
                    };

            var rawExample = "";
            try {
                rawExample = new ObjectMapper().writeValueAsString(example);
            } catch (JsonProcessingException e) {
                this.logger.error(
                        e.getMessage(),
                        e
                );
            }

            return operation.addParametersItem(new Parameter().required(false)
                                                              .allowReserved(false)
                                                              .style(Parameter.StyleEnum.SIMPLE)
                                                              .in("query")
                                                              .name("filter")
                                                              .description(
                                                                      "Use this param to filter with logical AND the dataset. Possible operations: "
                                                                              + Arrays.stream(FilterOperation.values())
                                                                                      .map(FilterOperation::name)
                                                                                      .collect(Collectors.joining(", "))
                                                                              + "<br/><br/>The example below must be URL Encoded"
                                                                              + "(Swagger will do it automatically after 'Send')")
                                                              .example(rawExample));
        };
    }

    /**
     * Customizes Swagger UI documentation for pageable API operations. This
     * customizer adds parameters for page, size, and sort to operations that
     * accept a {@link Pageable} argument.
     *
     * @return an {@link OperationCustomizer} for enhancing API operation
     * documentation where pagination is applicable.
     */
    @Bean
    public OperationCustomizer customizePageable() {
        return (operation, handlerMethod) -> {
            var pageable = Arrays.stream(handlerMethod.getMethodParameters())
                                 .filter(p -> p.getParameterType()
                                         == Pageable.class)
                                 .findFirst()
                                 .orElse(null);

            if (!tryRemove(
                    operation,
                    pageable
            )) {
                return operation;
            }

            return operation.addParametersItem(new Parameter().in("query")
                                                              .name("page")
                                                              .example(0)
                                                              .required(false))
                            .addParametersItem(new Parameter().in("query")
                                                              .name("size")
                                                              .example(10)
                                                              .required(false))
                            .addParametersItem(new Parameter().in("query")
                                                              .name("sort")
                                                              .example("id,asc")
                                                              .required(false));
        };
    }

    /**
     * Attempts to remove a parameter from the Swagger operation based on the
     * method parameter if it exists. This utility method supports the
     * customizers by removing default parameter documentation when custom
     * parameters are added.
     *
     * @param operation the Swagger {@link Operation} to modify
     * @param spec      the {@link MethodParameter} that may need to be
     *                  removed from Swagger UI documentation
     * @return {@code true} if the parameter was removed, {@code false}
     * otherwise
     */
    private boolean tryRemove(
            Operation operation,
            MethodParameter spec
    ) {
        if (spec == null) {
            return false;
        }

        var params = operation.getParameters();

        return params.removeIf(p -> p.getName()
                                     .equals(spec.getParameter()
                                                 .getName()));
    }
}