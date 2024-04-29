package bg.codexio.springframework.data.jpa.requery.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class SwaggerConfigTest {

    private SwaggerConfig swaggerConfig;
    private Operation operationMock;
    private HandlerMethod handlerMethodMock;
    private MethodParameter methodParameterMock;
    private ArgumentCaptor<Parameter> parameterCaptor;

    @BeforeEach
    void setup() {
        this.swaggerConfig = new SwaggerConfig();
        this.operationMock = mock(Operation.class);
        this.handlerMethodMock = mock(HandlerMethod.class);
        this.methodParameterMock = mock(MethodParameter.class);
        this.parameterCaptor = ArgumentCaptor.forClass(Parameter.class);

        var existingParameter = new Parameter();
        existingParameter.setName("existingParameterName");

        var parametersList = new ArrayList<Parameter>();
        parametersList.add(existingParameter);
        when(this.operationMock.getParameters()).thenReturn(parametersList);

        var reflectedParameterMock = mock(java.lang.reflect.Parameter.class);
        when(reflectedParameterMock.getName()).thenReturn(
                "existingParameterName");
        when(this.methodParameterMock.getParameter()).thenReturn(reflectedParameterMock);
        when(this.methodParameterMock.getParameterType()).thenAnswer(_ -> Pageable.class);
        when(this.operationMock.addParametersItem(any(Parameter.class))).thenReturn(this.operationMock);
        when(this.handlerMethodMock.getMethodParameters()).thenReturn(new MethodParameter[]{
                this.methodParameterMock
        });
    }

    @Test
    public void customizeFiltering_ShouldAddFilterParameter_WhenSpecificationParameterPresent() {
        when(this.methodParameterMock.getParameterType()).thenAnswer(_ -> Specification.class);

        var customizer = this.swaggerConfig.customizeFiltering();
        customizer.customize(
                this.operationMock,
                this.handlerMethodMock
        );

        verify(
                this.operationMock,
                atLeastOnce()
        ).addParametersItem(any(Parameter.class));
        verify(this.operationMock).addParametersItem(this.parameterCaptor.capture());
        var addedParameter = this.parameterCaptor.getValue();

        assertNotNull(addedParameter);
        assertEquals(
                "filter",
                addedParameter.getName()
        );
        assertEquals(
                Parameter.StyleEnum.SIMPLE,
                addedParameter.getStyle()
        );
        assertEquals(
                "query",
                addedParameter.getIn()
        );
    }

    @Test
    void customizePageable_ShouldAddPaginationParameters_WhenPageableParameterPresent() {
        when(this.methodParameterMock.getParameterType()).thenAnswer(_ -> Pageable.class);

        var customizer = this.swaggerConfig.customizePageable();
        customizer.customize(
                this.operationMock,
                this.handlerMethodMock
        );

        verify(
                this.operationMock,
                atLeast(3)
        ).addParametersItem(any(Parameter.class));
    }

    @Test
    public void customizeFiltering_ShouldReturnOriginalOperation_WhenNoSpecificationParameter() {
        var customizer = this.swaggerConfig.customizeFiltering();
        var result = customizer.customize(
                this.operationMock,
                this.handlerMethodMock
        );

        assertEquals(
                this.operationMock,
                result
        );
    }

    @Test
    void customizeFiltering_ShouldReturnOriginalOperation_WhenNoMatchingParameter() {
        class UnsupportedType {}
        when(this.methodParameterMock.getParameterType()).thenAnswer(_ -> UnsupportedType.class);

        var customizer = this.swaggerConfig.customizeFiltering();
        var result = customizer.customize(
                this.operationMock,
                this.handlerMethodMock
        );

        assertEquals(
                this.operationMock,
                result
        );
    }

    @Test
    void customizePageable_ShouldReturnOriginalOperation_WhenNoMatchingParameter() {
        class UnsupportedType {}
        when(this.methodParameterMock.getParameterType()).thenAnswer(_ -> UnsupportedType.class);

        var customizer = this.swaggerConfig.customizePageable();
        var result = customizer.customize(
                this.operationMock,
                this.handlerMethodMock
        );

        assertEquals(
                this.operationMock,
                result
        );
    }

    @Test
    void customizeFiltering_ShouldLogError_WhenJsonProcessingExceptionOccurs() {
        try (
                var mockedLoggerFactory = mockStatic(LoggerFactory.class)
        ) {
            var loggerMock = mock(Logger.class);
            mockedLoggerFactory.when(() -> LoggerFactory.getLogger(SwaggerConfig.class))
                               .thenReturn(loggerMock);

            var tempSwaggerConfig = new SwaggerConfig();

            when(this.methodParameterMock.getParameterType()).thenAnswer(_ -> Specification.class);

            try (
                    var ignored = Mockito.mockConstruction(
                            ObjectMapper.class,
                            (mock, _) -> when(mock.writeValueAsString(any())).thenThrow(new JsonProcessingException("Test exception") {})
                    )
            ) {
                var customizer = tempSwaggerConfig.customizeFiltering();
                customizer.customize(
                        operationMock,
                        handlerMethodMock
                );
            }

            verify(loggerMock).error(
                    anyString(),
                    any(Throwable.class)
            );
        }
    }
}