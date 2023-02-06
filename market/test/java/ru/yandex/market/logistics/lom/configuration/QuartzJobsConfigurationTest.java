package ru.yandex.market.logistics.lom.configuration;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import ru.yandex.market.logistics.lom.AbstractTest;

class QuartzJobsConfigurationTest extends AbstractTest {
    @Test
    @DisplayName("Не дублируются имена бинов в джобах")
    void distinctNames() {
        Map<String, Integer> beanNames = Arrays.stream(QuartzJobsConfiguration.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(Bean.class))
            .map(Method::getName)
            .collect(Collectors.toMap(Function.identity(), (s) -> 1, Integer::sum));

        for (Map.Entry<String, Integer> entry : beanNames.entrySet()) {
            softly.assertThat(entry.getValue())
                .as(String.format("Bean \"%s\" has more than 1 occurrence", entry.getKey()))
                .isEqualTo(1);
        }
    }
}
