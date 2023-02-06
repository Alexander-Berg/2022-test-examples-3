package ru.yandex.market.logistics.lms.client.controller.redis;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistics.lom.service.redis.AbstractRedisTest;
import ru.yandex.market.logistics.lom.service.redis.util.RedisKeys;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Ручки для тестирования текущего актуального поколения в redis")
public class LmsLomRedisControllerGetActualVersionTest extends AbstractRedisTest {

    private static final String GET_VERSION_PATH = "/lms/test-redis/actual-version";

    @AfterEach
    public void tearDown() {
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        super.tearDown();
    }

    @Test
    @SneakyThrows
    @DisplayName("В redis нет актуальной версии")
    void noVersionInRedis() {
        getActualVersion()
            .andExpect(status().isOk())
            .andExpect(content().string("\"-1000000000-01-01T00:00:00Z\""));
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение актуальной версии")
    void successGetVersion() {
        String version = "2022-08-06T13:40:00Z";
        doReturn(version)
            .when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        getActualVersion()
            .andExpect(status().isOk())
            .andExpect(content().string("\"" + version + "\""));
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка парсинга значения актуальной версии")
    void parsingError() {
        String version = "wrong version format";
        doReturn(version)
            .when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        softly.assertThatCode(this::getActualVersion)
            .isInstanceOf(NestedServletException.class)
            .hasMessage(
                "Request processing failed; nested exception is java.time.format.DateTimeParseException: "
                    + "Text 'wrong version format' could not be parsed at index 0"
            );
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка обращения к redis")
    void redisError() {
        doThrow(new RuntimeException("some redis exception"))
            .when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        softly.assertThatCode(this::getActualVersion)
            .isInstanceOf(NestedServletException.class)
            .hasMessage(
                "Request processing failed; nested exception is java.lang.RuntimeException: "
                    + "java.lang.RuntimeException: Connection retries to redis limit exceeded"
            );
    }

    @Nonnull
    @SneakyThrows
    private ResultActions getActualVersion() {
        return mockMvc.perform(get(GET_VERSION_PATH));
    }
}
