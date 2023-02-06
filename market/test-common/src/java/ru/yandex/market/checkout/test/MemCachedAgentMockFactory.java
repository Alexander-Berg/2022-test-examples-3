package ru.yandex.market.checkout.test;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ru.yandex.common.cache.memcached.MemCachedAgent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MemCachedAgentMockFactory {

    private final Map<String, Object> cache = Collections.synchronizedMap(new HashMap<>());

    public MemCachedAgent createMemCachedAgentMock() {
        MemCachedAgent agent = mock(MemCachedAgent.class);
        when(agent.addInCache(anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    String key = (String) invocation.getArguments()[0];
                    Object value = invocation.getArguments()[1];
                    cache.put(key, value);
                    return true;
                });

        when(agent.getFromCache(anyCollection()))
                .thenAnswer(invocation -> {
                    Map<String, Object> map = new HashMap<>();
                    Collection<String> keys = (Collection<String>) invocation.getArguments()[0];
                    for (String next : keys) {
                        if (cache.containsKey(next)) {
                            map.put(next, cache.get(next));
                        }
                    }
                    return map;
                });

        when(agent.getFromCache(anyString()))
                .thenAnswer(invocation -> cache.get(invocation.getArguments()[0]));

        doAnswer(invocation -> {
            cache.remove(invocation.getArguments()[0]);
            return null;
        }).when(agent).deleteFromCache(anyString());

        doAnswer(invocation -> {
            String key = (String) invocation.getArguments()[0];
            Object value = invocation.getArguments()[1];
            cache.put(key, value);
            return true;
        }).when(agent).putInCache(anyString(), any(), any());

        doAnswer(invocation -> {
            String key = (String) invocation.getArguments()[0];
            Object value = invocation.getArguments()[1];
            cache.put(key, value);
            return true;
        }).when(agent).putInCache(anyString(), any());

        doAnswer(invocation -> {
            String key = (String) invocation.getArguments()[0];
            Object currentValue = cache.get(key);
            if (currentValue == null) {
                cache.put(key, Integer.toString(1));
            } else if (currentValue instanceof String) {
                try {
                    Integer intValue = Integer.valueOf((String) currentValue);
                    cache.put(key, Integer.toString(intValue + 1));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
            return Long.parseLong((String) cache.get(key));
        }).when(agent).incrementInCache(anyString(), any(Date.class));

        doAnswer(invocation -> {
            String key = (String) invocation.getArguments()[0];
            long value = (long) invocation.getArguments()[1];
            Object currentValue = cache.get(key);
            if (currentValue == null) {
                cache.put(key, Integer.toString(1));
            } else if (currentValue instanceof String) {
                try {
                    Integer intValue = Integer.valueOf((String) currentValue);
                    cache.put(key, Long.toString(intValue + value));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
            return Long.parseLong((String) cache.get(key));
        }).when(agent).incrementInCache(anyString(), anyLong(), any(Date.class));
        return agent;
    }

    public synchronized void resetMemCachedAgentMock(MemCachedAgent agent) {
        cache.clear();
    }
}
