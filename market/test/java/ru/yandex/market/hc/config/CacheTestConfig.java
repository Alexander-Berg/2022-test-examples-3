package ru.yandex.market.hc.config;

import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.spy.memcached.MemcachedClientIF;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.hc.entity.DegradationModes;
import ru.yandex.market.hc.service.MemcachedService;
import ru.yandex.market.hc.stubs.MemcachedClientStub;

/**
 * Created by aproskriakov on 9/2/21
 */
@Configuration
public class CacheTestConfig {

    @Bean
    public MemcachedClientIF memcachedClient() {
        return new MemcachedClientStub();
    }

    @Bean
    public MemcachedService memcachedService(MemcachedClientIF memcachedClient) {
        return new MemcachedService(memcachedClient, new ObjectMapper(),
                new DegradationModes(new ConcurrentHashMap<>()));
    }
}
