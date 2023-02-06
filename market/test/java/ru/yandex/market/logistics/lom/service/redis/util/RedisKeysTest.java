package ru.yandex.market.logistics.lom.service.redis.util;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.service.yt.dto.YtMigrationModel;

@DisplayName("Тесты на методы получения ключей для redis")
class RedisKeysTest extends AbstractTest {

    private static final String VERSION = "0";
    private static final String LMS_PREFIX = "lms-" + VERSION;
    private static final String YT_PREFIX = "yt-" + VERSION;

    @Test
    @DisplayName("Для всех классов, реализующих интерфейс YtMigrationModel, есть название хэш таблицы")
    void successGetKeyForExistingYtModels() {
        Reflections reflections = new Reflections("ru.yandex.market.logistics.lom");
        Set<Class<? extends YtMigrationModel>> classes = reflections.getSubTypesOf(YtMigrationModel.class);

        for (var ytModel : classes) {
            softly.assertThat(RedisKeys.getHashTableFromYtName(ytModel, VERSION))
                .isNotBlank()
                .startsWith(YT_PREFIX);
        }
    }

    @Test
    @DisplayName("Если для класса не задан ключ хэш таблицы - исключение")
    void failIfUndefinedClassCalling() {
        softly.assertThatCode(
            () -> RedisKeys.getHashTableFromYtName(
                ((YtMigrationModel) () -> "some-incorrect-lambda").getClass(),
                VERSION
            )
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No hashtable for class");
    }
}
