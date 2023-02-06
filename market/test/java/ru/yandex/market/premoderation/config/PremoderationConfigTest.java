package ru.yandex.market.premoderation.config;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.premoderation.storage.QueryToStorageExecutor;
import ru.yandex.market.shop.FunctionalTest;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.params.provider.Arguments.of;

@TestInstance(PER_CLASS)
class PremoderationConfigTest extends FunctionalTest {
    @Autowired
    private Map<String, QueryToStorageExecutor> executors;

    Stream<Arguments> executors() {
        return executors.entrySet().stream().map(entry -> of(entry.getKey(), entry.getValue()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("executors")
    @DisplayName("Проверка корректности sql скриптов")
    void testPremoderationConfigTest(String name, QueryToStorageExecutor executor) {
        try {
            executor.doJob(null);
        } catch (RuntimeException e) {
            fail(String.format("Failed premoderation executor [%s]", name), e);
        }
    }
}
