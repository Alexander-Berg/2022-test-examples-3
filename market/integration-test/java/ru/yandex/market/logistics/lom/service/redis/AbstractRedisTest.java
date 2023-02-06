package ru.yandex.market.logistics.lom.service.redis;

import java.util.Set;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.lms.converter.RedisObjectConverter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AbstractRedisTest extends AbstractContextualTest {
    @Autowired
    protected Jedis migrationJedis;

    @Autowired
    protected Jedis clientJedis;

    @Autowired
    protected JedisSentinelPool jedisMigrationPool;

    @Autowired
    protected JedisSentinelPool jedisClientPool;

    @Autowired
    protected RedisObjectConverter redisObjectConverter;

    @BeforeEach
    protected void setUp() {
        doReturn(migrationJedis).when(jedisMigrationPool).getResource();
        doReturn(clientJedis).when(jedisClientPool).getResource();
    }

    @AfterEach
    protected void tearDown() {
        verify(migrationJedis, atLeast(0)).close();
        verify(clientJedis, atLeast(0)).close();
        verifyNoMoreInteractions(migrationJedis, clientJedis);
    }

    @SneakyThrows
    protected String convertToString(Object object) {
        return objectMapper.writeValueAsString(object);
    }

    protected void verifyMultiGetArguments(String tableName, Set<String> multiGetRequest) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(clientJedis).hmget(eq(tableName), captor.capture());
        softly.assertThat(captor.getAllValues()).containsExactlyInAnyOrderElementsOf(multiGetRequest);
    }
}
