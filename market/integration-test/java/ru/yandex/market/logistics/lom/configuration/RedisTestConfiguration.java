package ru.yandex.market.logistics.lom.configuration;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

@Configuration
@MockBeans({
    @MockBean(name = "jedis", classes = Jedis.class),
    @MockBean(name = "jedisSentinelPool", classes = JedisSentinelPool.class)
})
public class RedisTestConfiguration {

}
