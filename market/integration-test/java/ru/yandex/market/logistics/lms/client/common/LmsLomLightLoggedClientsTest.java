package ru.yandex.market.logistics.lms.client.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.lms.client.LmsLomRedisClient;
import ru.yandex.market.logistics.lom.lms.client.LmsLomYtClient;
import ru.yandex.market.logistics.lom.lms.model.logging.enums.LmsLomLoggingCode;

@DisplayName("Проверка тегов для логов клиентов для данных лмс")
class LmsLomLightLoggedClientsTest extends AbstractContextualTest {

    @Autowired
    private LmsLomRedisClient redisClient;

    @Autowired
    private LmsLomYtClient ytClient;

    @Test
    @DisplayName("Проверка настроек клиента редиса")
    void checkRedisClient() {
        softly.assertThat(redisClient.getLoggingCode())
            .isEqualTo(LmsLomLoggingCode.LMS_LOM_REDIS);

        softly.assertThatCode(
                () -> redisClient.getScheduleDay(1L)
            )
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Method getScheduleDay by id is not supported for redis client");
    }

    @Test
    @DisplayName("Проверка настроек клиента yt")
    void checkYtClient() {
        softly.assertThat(ytClient.getLoggingCode())
            .isEqualTo(LmsLomLoggingCode.LMS_LOM_YT);
    }
}
