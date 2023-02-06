package ru.yandex.market.logistics.lom.service.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.exceptions.JedisException;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.JedisLomProperties;
import ru.yandex.market.logistics.lom.service.redis.enums.JedisPoolType;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Сервис работы с redis")
class RedisConnectionServiceImplTest extends AbstractContextualTest {

    private static final int REDIS_RETRIES_AMOUNT = 5;

    @Autowired
    private RedisConnectionService redisConnectionService;

    @Autowired
    private JedisSentinelPool jedisMigrationPool;

    @Autowired
    private JedisLomProperties jedisLomProperties;

    @BeforeEach
    void setUp() {
        doThrow(new JedisException("Some jedis exception"))
            .when(jedisMigrationPool).getResource();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(jedisMigrationPool);
        jedisLomProperties.getPoolConfig().get(JedisPoolType.MIGRATION).setRetriesAmount(1);
    }

    @Test
    @DisplayName("Достигнуто максимальное число попыток выполнить операцию в редисе")
    void maxRetryAttemptsReached() {
        jedisLomProperties.getPoolConfig().get(JedisPoolType.MIGRATION).setRetriesAmount(REDIS_RETRIES_AMOUNT);

        softly.assertThatCode(
                () -> redisConnectionService.performAction(
                    jedis -> jedis.set("test-key", "test-value"),
                    JedisPoolType.MIGRATION
                )
            )
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Connection retries to redis limit exceeded");

        verify(jedisMigrationPool, times(REDIS_RETRIES_AMOUNT)).getResource();
        softly.assertThat(backLogCaptor.getResults().stream()
            .filter(
                result -> result.contains("tags=REDIS_CONNECTION_ERROR")
                    && result.contains("Exception while calling redis")
                    && result.contains("JedisException: Some jedis exception")
            )
            .count()).isEqualTo(REDIS_RETRIES_AMOUNT);
    }
}
