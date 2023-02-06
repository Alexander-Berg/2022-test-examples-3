package ru.yandex.direct.api.v5.ws;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.ws.annotation.ApiMethod;
import ru.yandex.direct.api.v5.ws.annotation.ApiRequest;
import ru.yandex.direct.api.v5.ws.annotation.ApiResponse;
import ru.yandex.direct.api.v5.ws.annotation.ApiServiceEndpoint;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяем, что все методы API классов endpoint-ов имеют один параметр помеченный
 * атрибутом {@link ru.yandex.direct.api.v5.ws.annotation.ApiRequest} и возвращаемое
 * значение помеченно атрибутом {@link ru.yandex.direct.api.v5.ws.annotation.ApiResponse}
 */
@Api5Test
@RunWith(SpringRunner.class)
public class CheckApiEndpointMethodCorrectlyAnnotatedTest {
    @Autowired
    ApplicationContext applicationContext;

    Set<Method> methods;

    @Before
    public void setUp() {
        methods = applicationContext.getBeansWithAnnotation(ApiServiceEndpoint.class)
                .values()
                .stream()
                .map(AopProxyUtils::ultimateTargetClass)
                .flatMap(ec -> Stream.of(ec.getMethods()).filter(m -> m.isAnnotationPresent(ApiMethod.class)))
                .collect(Collectors.toSet());
    }

    @Test
    public void allApiMethodsMustHaveApiResponseAnnotaion() {
        var methodsWithoutAnnotation = methods.stream()
                .filter(m -> !m.isAnnotationPresent(ApiResponse.class))
                .collect(Collectors.toSet());
        assertThat(methodsWithoutAnnotation)
                .as("Method must be annotated with @ApiResponse")
                .isEmpty();
    }

    @Test
    public void allApiMethodsMustHaveOnlyOneParameter() {
        var methodsWithNotOneParameter = methods.stream()
                .map(Method::getParameters)
                .filter(param -> param.length != 1)
                .collect(Collectors.toSet());

        assertThat(methodsWithNotOneParameter)
                .as("ApiMethod must have only one parameter")
                .isEmpty();
    }

    @Test
    public void allApiMethodsParametersMustHaveApiRequestAnnotation() {
        var methodsWithoutAnnotation = methods.stream()
                .map(Method::getParameters)
                .filter(param -> param.length == 1)
                .map(param -> param[0])
                .filter(p -> !p.isAnnotationPresent(ApiRequest.class))
                .collect(Collectors.toSet());

        assertThat(methodsWithoutAnnotation)
                .as("ApiMethod must have parameter annotated with @ApiRequest")
                .isEmpty();
    }
}
