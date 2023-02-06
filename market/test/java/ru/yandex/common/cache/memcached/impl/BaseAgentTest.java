package ru.yandex.common.cache.memcached.impl;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.mockito.Mockito;

import ru.yandex.common.cache.memcached.client.MemCachedClient;
import ru.yandex.common.util.collections.Pair;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

public class BaseAgentTest {
    Object v = new Object(); // dummy value
    Date e = new Date(0);
    MemCachedClient client = Mockito.mock(MemCachedClient.class);

    @Before
    public void setUp() {
        given(client.get(anyString())).willReturn(v);
        given(client.getMany(anyCollection()))
                .willAnswer(invocation -> ((Collection<String>) invocation.getArgument(0)).stream()
                        .map(k -> Pair.of(k, v))
                        .collect(Utils.toMapWithNulls())
                );
    }

    public static void invalidateLocalCache() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(600);
    }

    public static void flushAsyncOperations(ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(3L, TimeUnit.SECONDS);
    }
}
