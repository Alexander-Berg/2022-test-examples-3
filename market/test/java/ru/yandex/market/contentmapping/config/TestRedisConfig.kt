package ru.yandex.market.contentmapping.config

import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.JedisSentinelPool
import ru.yandex.market.contentmapping.utils.RedisHelperMock
import ru.yandex.market.contentmapping.utils.RedisHelper

@Configuration
@Profile("test")
open class TestRedisConfig: RedisConfig() {
    @Bean
    override fun jedisSentinelPool(): JedisSentinelPool {
        return Mockito.mock(JedisSentinelPool::class.java)
    }

    @Bean
    override fun jedisPoolConfig(): JedisPoolConfig {
        return Mockito.mock(JedisPoolConfig::class.java)
    }

    @Bean
    override fun redisHelper(): RedisHelper {
        return RedisHelperMock()
    }
}
