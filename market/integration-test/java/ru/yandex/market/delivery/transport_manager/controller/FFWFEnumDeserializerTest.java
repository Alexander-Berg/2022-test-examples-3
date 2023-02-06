package ru.yandex.market.delivery.transport_manager.controller;

import java.io.IOException;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.config.ffwf.FulfillmentWorkflowConfig;

public class FFWFEnumDeserializerTest extends AbstractContextualTest {

    private static final Set<String> IGNORED_CLASS_NAMES = Set.of(
        "ru.yandex.market.ff.client.enums.TransferStatusType"
        // в этом классе результаты JsonValue не подходят по формату в JsonCreator
    );

    private ObjectMapper mapper = new FulfillmentWorkflowConfig().objectMapper();

    @SneakyThrows
    private static Stream<Arguments> testData() {
        String packageName = "ru.yandex.market.ff.client.enums";
        return getClasses(packageName).stream()
            .sorted(Comparator.comparing(Class::getName))
            .filter(cl -> !IGNORED_CLASS_NAMES.contains(cl.getName()))
            .map(Arguments::of);
    }

    static Set<Class<? extends Enum>> getClasses(String packageName) {
        var refs = new Reflections(packageName, new SubTypesScanner(false));
        return refs.getSubTypesOf(Enum.class);
    }

    @ParameterizedTest(name = "Enum parsed: {0}")
    @MethodSource("testData")
    public <T extends Enum<?>> void testDtoDeserialization(Class<T> dtoClass)
        throws ReflectiveOperationException, IOException {
        T value = dtoClass.getEnumConstants()[0];
        String json = mapper.writeValueAsString(value);
        T actualValue = mapper.readValue(json, dtoClass);
        Assertions.assertEquals(value, actualValue);
    }
}
