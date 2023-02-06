package ru.yandex.market.pers.test.common;

import com.google.common.cache.Cache;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.common.cache.memcached.client.MemCachedClusterFactory;
import ru.yandex.common.cache.memcached.impl.DefaultMemCachedAgent;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 25.11.2021
 */
public class MemCachedMockUtils {

    public static MemCachedAgent buildMemCachedAgentMock(Cache<String, Object> cache) {
        MemCachedClusterFactory factory = new MemCachedClusterFactory();
        factory.setClusterServers("1");
        factory.setClientFactory((clientId, servers) -> new MemCachedClientOverGuava(cache));

        DefaultMemCachedAgent agent = new DefaultMemCachedAgent();
        agent.setMemCachedClusterFactory(factory);
        agent.afterPropertiesSet();
        return agent;
    }
}
