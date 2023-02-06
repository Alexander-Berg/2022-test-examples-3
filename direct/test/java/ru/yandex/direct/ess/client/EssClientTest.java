package ru.yandex.direct.ess.client;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.ess.client.repository.EssAdditionalObjectsRepository;
import ru.yandex.direct.ess.common.models.BaseEssConfig;
import ru.yandex.direct.ess.common.models.BaseLogicObject;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EssClientTest {
    private EssAdditionalObjectsRepository essAdditionalObjectsRepository;

    private static final String TEST_LOGIC_PROCESSOR = "test_logic_processor";

    @BeforeEach
    void before() {
        essAdditionalObjectsRepository = mock(EssAdditionalObjectsRepository.class);
    }

    @Test
    void constructorWithoutExceptionsTest() {
        assertThatCode(() -> new EssClient(essAdditionalObjectsRepository)).doesNotThrowAnyException();
    }

    @Test
    void getLogicProcessorsNamesTest() {
        EssClient essClient = new EssClient(essAdditionalObjectsRepository);
        assertThat(essClient.getLogicProcessorsNames()).isNotEmpty();
    }

    @Test
    void getLogicProcessorsNamesForCustomConfigTest() {
        var essClient = new EssClient(essAdditionalObjectsRepository, this.getClass().getPackageName());
        assertThat(essClient.getLogicProcessorsNames()).containsExactlyInAnyOrder(TEST_LOGIC_PROCESSOR);
    }

    @SuppressWarnings("unchecked")
    @Test
    void addLogicObjectsForProcessorForCustomClassTest() {
        var essClient = new EssClient(essAdditionalObjectsRepository, this.getClass().getPackageName());
        var testLogicObjectsToAdd = List.of(
            new TestObject(23),
            new TestObject(40)
        );
        essClient.addLogicObjectsForProcessor(1, TEST_LOGIC_PROCESSOR, JsonUtils.toJson(testLogicObjectsToAdd), false);
        ArgumentCaptor<List> logicObjectCapture =
            ArgumentCaptor.forClass(List.class);
        verify(essAdditionalObjectsRepository).addLogicObjectsForProcessor(anyInt(), eq(TEST_LOGIC_PROCESSOR),
            logicObjectCapture.capture());

        var gotSerializedObjects = logicObjectCapture.getAllValues();
        var expectedSerializeObjects = testLogicObjectsToAdd.stream()
            .map(JsonUtils::toJson)
            .collect(toList());

        assertThat(gotSerializedObjects).containsExactlyInAnyOrder(expectedSerializeObjects);
    }

    @SuppressWarnings("unchecked")
    @Test
    void addLogicObjectsForProcessorForCustomClass_WithDeleteTest() {
        var essClient = new EssClient(essAdditionalObjectsRepository, this.getClass().getPackageName());
        var testLogicObjectsToAdd = List.of(
            new TestObject(23),
            new TestObject(40)
        );
        essClient.addLogicObjectsForProcessor(1, TEST_LOGIC_PROCESSOR, JsonUtils.toJson(testLogicObjectsToAdd), true);
        ArgumentCaptor<List> logicObjectCapture =
            ArgumentCaptor.forClass(List.class);
        verify(essAdditionalObjectsRepository).addLogicObjectsForProcessor(anyInt(), eq(TEST_LOGIC_PROCESSOR),
            logicObjectCapture.capture());
        verify(essAdditionalObjectsRepository).clearLogicObjects(anyInt(), eq(2));

        var gotSerializedObjects = logicObjectCapture.getAllValues();
        var expectedSerializeObjects = testLogicObjectsToAdd.stream()
            .map(JsonUtils::toJson)
            .collect(toList());

        assertThat(gotSerializedObjects).containsExactlyInAnyOrder(expectedSerializeObjects);
    }

    @Test
    void addErrorJsonLogicObjectsForCustomClassTest() {
        var essClient = new EssClient(essAdditionalObjectsRepository, this.getClass().getPackageName());
        var errorJson = "[{\"i\":5}]";
        assertThatThrownBy(() -> essClient.addLogicObjectsForProcessor(1, TEST_LOGIC_PROCESSOR, errorJson, false))
            .isInstanceOf(IllegalStateException.class);
    }

    @SuppressWarnings("unused")
    static class TestConfig extends BaseEssConfig {
        @Override
        public String getLogicProcessName() {
            return TEST_LOGIC_PROCESSOR;
        }

        @Override
        public String getTopic() {
            return "test_topic";
        }

        @Override
        public Class<? extends BaseLogicObject> getLogicObject() {
            return TestObject.class;
        }

        @Override
        public Duration getCriticalEssProcessTime() {
            return null;
        }

        @Override
        public Duration getTimeToReadThreshold() {
            return null;
        }

        @Override
        public int getRowsThreshold() {
            return 0;
        }

        @Override
        public boolean processReshardingEvents() {
            // выставлено явно при замене умолчания в базовом классе: DIRECT-171006
            // необязательно означает, что для этого процесса нужно обрабатывать события от решардинга, просто настройку добавили позже: DIRECT-122901
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class TestObject extends BaseLogicObject {
        @JsonProperty("id")
        private Long id;

        @JsonCreator
        TestObject(@JsonProperty(value = "id", required = true) long id) {
            this.id = id;
        }
    }
}
