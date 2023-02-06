package ru.yandex.market.delivery.transport_manager.controller;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import one.util.streamex.EntryStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.util.testdata.ObjectTestFieldValuesUtils;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ControllerDtoTest extends AbstractContextualTest {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest(name = "(De)serialization of {0}")
    @MethodSource("testData")
    public <T> void testDtoDeserialization(Class<T> dtoClass) throws IOException {
        T value = ObjectTestFieldValuesUtils.createAndFillInstance(dtoClass, true);

        String json = objectMapper.writeValueAsString(value);
        T actualValue = objectMapper.readValue(json, dtoClass);
        Assertions.assertEquals(value, actualValue);
    }

    public Stream<Arguments> testData() {
        return EntryStream.of(requestMappingHandlerMapping.getHandlerMethods())
            .filterKeys(k -> {
                Set<RequestMethod> methods = k.getMethodsCondition().getMethods();
                return methods.contains(RequestMethod.POST) || methods.contains(RequestMethod.PUT);
            })
            .mapValues(HandlerMethod::getMethod)
            .mapToValue((k, v) -> getRequestBodyParamIndex(v.getParameterAnnotations())
                .map(idx -> v.getParameterTypes()[idx])
                .orElse(null))
            .filterValues(Objects::nonNull)

            .filterValues(this::isInControllerPackage)

            .removeValues(Class::isPrimitive)
            .removeValues(String.class::isAssignableFrom)
            .removeValues(LocalDate.class::isAssignableFrom)
            .removeValues(LocalDateTime.class::isAssignableFrom)
            .removeValues(Duration.class::isAssignableFrom)
            .removeValues(Instant.class::isAssignableFrom)

            .values()
            .distinct()
            .map(Arguments::of);
    }

    private boolean isInControllerPackage(Class<?> c) {
        return c.getPackage().getName().startsWith("ru.yandex.market.delivery.transport_manager") ||
            c.getPackage().getName().startsWith("ru.yandex.market.delivery.gruzin");
    }

    private Optional<Integer> getRequestBodyParamIndex(Annotation[][] annotations) {
        for (int i = 0; i < annotations.length; i++) {
            Annotation[] annotationsOfParam = annotations[i];
            for (Annotation a : annotationsOfParam) {
                if (a.annotationType().equals(RequestBody.class)) {
                    return Optional.of(i);
                }
            }
        }
        return Optional.empty();
    }
}
