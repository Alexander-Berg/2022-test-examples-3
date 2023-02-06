package ru.yandex.market.wms.radiator.core.redis;

import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.wms.radiator.cache.config.RedisConfiguration;
import ru.yandex.market.wms.radiator.cache.config.StringByteCodec;
import ru.yandex.market.wms.radiator.core.config.properties.WarehouseMapProperties;

@TestConfiguration
public class TestRedisConfiguration {

    @Bean(destroyMethod = "close")
    @Primary
    public RedisConfiguration.RedisConnections testRedisConnections(WarehouseMapProperties warehousesProperties) {
        RedisConfiguration.RedisConnections result = new RedisConfiguration.RedisConnections();
        warehousesProperties.getWarehousesMap().forEach((key, p) -> {
            int db = p.getRedisProperties().getDb();
            result.put(p.getId(), EmbeddedRedis.newRedisClient(db).connect(StringByteCodec.INSTANCE));
        });
        return result;
    }

    @Bean(destroyMethod = "close")
    @Primary
    public RedisConfiguration.RedisReadConnections testRedisReadConnections(WarehouseMapProperties warehousesProperties) {
        RedisConfiguration.RedisReadConnections result = new RedisConfiguration.RedisReadConnections();
        warehousesProperties.getWarehousesMap().forEach((key, p) -> {
            int db = p.getRedisProperties().getDb();
            result.put(p.getId(), EmbeddedRedis.newRedisClient(db).connect(StringByteCodec.INSTANCE));
        });
        return result;
    }

    @Bean(destroyMethod = "close")
    @Primary
    public StatefulRedisConnection<String, String> testRedisSharedDbConnection() {
        return EmbeddedRedis.getRedisClient().connect();
    }
}
