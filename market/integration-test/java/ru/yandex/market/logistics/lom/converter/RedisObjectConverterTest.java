package ru.yandex.market.logistics.lom.converter;

import java.io.Serializable;
import java.util.Optional;

import javax.annotation.Nonnull;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.lms.converter.RedisObjectConverter;

@DisplayName("Конвертация YT-моделей")
class RedisObjectConverterTest extends AbstractContextualTest {

    private static final String SERIALIZED_TEST_ENTITY = "{\"field1\":\"a\",\"field2\":\"b\",\"emptyField\":null}";

    @Autowired
    private RedisObjectConverter redisObjectConverter;

    @Test
    void successfulSerialization() {
        softly.assertThat(redisObjectConverter.serializeToString(buildTestEntity()))
            .isEqualTo(SERIALIZED_TEST_ENTITY);
    }

    @Test
    void successfulDeserialization() {
        softly.assertThat(redisObjectConverter.deserializeToObject(SERIALIZED_TEST_ENTITY, TestEntity.class))
            .isEqualTo(buildTestEntity());
    }

    @Test
    void failedSerialization() {
        softly.assertThatThrownBy(() -> redisObjectConverter.serializeToString(new NonSerializableEntity()))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void failDeserialization() {
        softly.assertThatThrownBy(
                () -> redisObjectConverter.deserializeToObject("{\"field3: \"a\"}", TestEntity.class)
            )
            .isInstanceOf(IllegalStateException.class);
    }

    @Nonnull
    private static TestEntity buildTestEntity() {
        TestEntity entity = new TestEntity();
        entity.setField1("a");
        entity.setField2(Optional.of("b"));
        entity.setEmptyField(Optional.empty());

        return entity;
    }

    @Data
    @NoArgsConstructor
    static class TestEntity implements Serializable {
        private String field1;
        private Optional<String> field2;
        private Optional<String> emptyField;
    }

    static class NonSerializableEntity implements Serializable {
        private String field;
    }
}
