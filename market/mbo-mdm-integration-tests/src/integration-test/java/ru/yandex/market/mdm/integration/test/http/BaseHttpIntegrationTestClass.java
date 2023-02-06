package ru.yandex.market.mdm.integration.test.http;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Descriptors;
import org.assertj.core.api.Assertions;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.mdm.app.props.AppPropertySourcesContextInitializer;
import ru.yandex.market.mdm.integration.test.CommonConfiguration;

/**
 * Общая конфигурация для интеграционных тестов на ручки МДМ (HTTP и прото).
 * Тесты не поднимают контекст всего mdm-app.
 * Это сделано специально, так как эти интеграционные тесты должны тестировать только ручки.
 *
 * @author s-ermakov
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {CommonConfiguration.class}, initializers = AppPropertySourcesContextInitializer.class)
public abstract class BaseHttpIntegrationTestClass {
    protected static final Logger log = LoggerFactory.getLogger(BaseHttpIntegrationTestClass.class);

    @Autowired
    protected TestRestTemplate restTemplate;

    protected static void assertProtoMessagesEqual(AbstractMessage expected,
                                                   AbstractMessage actual,
                                                   String... fieldNames) {
        Set<String> fieldsSet = new HashSet<>(Arrays.asList(fieldNames));
        Map<Descriptors.FieldDescriptor, Object> expectedFieldsToCompare = expected.getAllFields().entrySet().stream()
            .filter(entry -> fieldsSet.contains(entry.getKey().getJsonName()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<Descriptors.FieldDescriptor, Object> actualFieldsToCompare = actual.getAllFields().entrySet().stream()
            .filter(entry -> fieldsSet.contains(entry.getKey().getJsonName()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Assertions.assertThat(actualFieldsToCompare.entrySet())
            .containsExactlyInAnyOrderElementsOf(expectedFieldsToCompare.entrySet());
    }
}
