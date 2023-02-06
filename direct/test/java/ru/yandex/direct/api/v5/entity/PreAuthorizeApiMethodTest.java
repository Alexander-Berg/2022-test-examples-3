package ru.yandex.direct.api.v5.entity;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;
import org.springframework.ws.server.endpoint.annotation.Endpoint;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.ws.annotation.ApiMethod;
import ru.yandex.direct.core.security.authorization.PreAuthorizeAgencyRead;
import ru.yandex.direct.core.security.authorization.PreAuthorizeAgencyWrite;
import ru.yandex.direct.core.security.authorization.PreAuthorizeRead;
import ru.yandex.direct.core.security.authorization.PreAuthorizeWrite;

import static org.assertj.core.api.Assertions.assertThat;

@Api5Test
@RunWith(SpringRunner.class)
public class PreAuthorizeApiMethodTest {
    @Autowired
    ApplicationContext applicationContext;

    private static final Set<Class<? extends Annotation>> AUTH_ANNOTATION = Set.of(
            PreAuthorizeRead.class, PreAuthorizeWrite.class,
            PreAuthorizeAgencyRead.class, PreAuthorizeAgencyWrite.class);

    public Collection<Method> allMethods() {
        List<Method> res = new ArrayList<>();
        for (String beanName : applicationContext.getBeanNamesForAnnotation(Endpoint.class)) {
            Object bean = applicationContext.getBean(beanName);
            ReflectionUtils.doWithMethods(
                    bean.getClass(),
                    res::add,
                    m -> AnnotationUtils.getAnnotation(m, ApiMethod.class) != null);
        }
        return res;
    }

    @Test
    public void apiMethodsMustBeAnnotatedWithPreAuthorize() {
        var methodsWithoutAnnotations = allMethods()
                .stream()
                .filter(m -> Arrays.stream(m.getDeclaredAnnotations())
                        .map(Annotation::annotationType)
                        .noneMatch(AUTH_ANNOTATION::contains))
                .collect(Collectors.toList());
        assertThat(methodsWithoutAnnotations).isEmpty();
    }
}
