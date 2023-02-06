package ru.yandex.common.cache.memcached.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.common.util.collections.Pair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ParallelMemCachedAgentTest extends BaseAgentTest {
    ParallelMemCachedAgent agent = new ParallelMemCachedAgent();
    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Rule
    public Timeout globalTimeout = new Timeout(3, TimeUnit.SECONDS);

    @Before
    public void setUp() {
        super.setUp();
        agent.setExecutor(executor);
        agent.setMemCachedClient(client);
        agent.afterPropertiesSet();
    }

    @After
    public void tearDown() {
        agent.close();
        executor.shutdownNow();
    }

    @Test
    public void getFromCacheOne() {
        // when
        Object result1 = agent.getFromCache("key");
        Object result2 = agent.getFromCache("key");

        // then
        assertThat(result1).isEqualTo(v);
        assertThat(result2).isEqualTo(result1);
        verify(client, times(1).description("results a cached internally for a short time to reduce load on client"))
                .get("key");
        verifyNoMoreInteractions(client);
    }

    @Test
    public void getFromCacheEmpty() {
        // when
        Map<String, Object> result = agent.getFromCache(Collections.emptyList());

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    public void getFromCacheSingle() {
        // when
        Map<String, Object> result1 = agent.getFromCache(Collections.singleton("key"));
        Map<String, Object> result2 = agent.getFromCache(Collections.singleton("key"));

        // then
        assertThat(result1).isEqualTo(Collections.singletonMap("key", v));
        assertThat(result2).isEqualTo(result1);
        verify(client, times(1).description("results a cached internally for a short time to reduce load on client"))
                .getMany(Lists.newLinkedList(Collections.singleton("key")));
        verifyNoMoreInteractions(client);
    }

    @Test
    public void getFromCacheSingleCacheExpiresShortly() throws InterruptedException {
        // when
        Map<String, Object> result1 = agent.getFromCache(Collections.singleton("key"));
        TimeUnit.MILLISECONDS.sleep(600); // to invalidate local cache
        Map<String, Object> result2 = agent.getFromCache(Collections.singleton("key"));

        // then
        assertThat(result1).isEqualTo(Collections.singletonMap("key", v));
        assertThat(result2).isEqualTo(result1);
        verify(client, times(2).description("internal cache is very short-living"))
                .getMany(Lists.newLinkedList(Collections.singleton("key")));
        verifyNoMoreInteractions(client);
    }

    @Test
    public void getFromCacheLimitDefault() {
        // when
        Map<String, Object> result = agent.getFromCache(Arrays.asList("key1", "key2"));

        // then
        assertThat(result).isEqualTo(Utils.makeMapWithNulls(
                Pair.of("key1", v),
                Pair.of("key2", v)
        ));
        verify(client).getMany(Arrays.asList("key1", "key2"));
        verifyNoMoreInteractions(client);
    }

    @Test
    public void getFromCacheLimitLessThanKeys() {
        // given
        List<String> keys = Arrays.asList("key1", "key2");
        int limit = 1;
        agent.setBulkGetLimit(limit);

        // when
        Map<String, Object> result = agent.getFromCache(keys);

        // then
        assertThat(limit).as("should not queue client if").isLessThan(keys.size());
        assertThat(result).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    public void putInCacheOne() throws InterruptedException {
        // when
        agent.putInCache("key", v, e);
        flushAsyncOperations(executor);

        // then
        verify(client).set("key", v, e);
        verifyNoMoreInteractions(client);
    }

    @Test
    public void putInCacheEmpty() throws InterruptedException {
        // when
        agent.putInCache(Collections.emptyMap(), e);
        flushAsyncOperations(executor);

        // then
        verifyNoInteractions(client);
    }

    @Test
    public void putInCacheSingle() throws InterruptedException {
        // given
        Map<String, Object> request = Collections.singletonMap("key", v);

        // when
        agent.putInCache(request, e);
        flushAsyncOperations(executor);

        // then
        verify(client).set("key", v, e);
        verifyNoMoreInteractions(client);
    }

    @Test
    public void putInCacheLimitDefault() throws InterruptedException {
        // given
        Map<String, Object> request = Utils.makeMapWithNulls(
                Pair.of("key1", null),
                Pair.of("key2", v)
        );

        // when
        agent.putInCache(request, e);
        flushAsyncOperations(executor);

        // then
        verify(client, description("nulls are serialized to singleton")).set("key1", CachedValue.NULL, e);
        verify(client).set("key2", v, e);
        verifyNoMoreInteractions(client);
    }

    @Test
    public void putInCacheLimitGtEqThanKeys() throws InterruptedException {
        // given
        Map<String, Object> request = Utils.makeMapWithNulls(
                Pair.of("key1", null),
                Pair.of("key2", v)
        );
        agent.setBulkPutBatchSize(request.size());

        // when
        agent.putInCache(request, e);
        flushAsyncOperations(executor);

        // then
        verify(client).setMany(Utils.makeMapWithNulls(
                Pair.of("key1", CachedValue.NULL),
                Pair.of("key2", v)
        ), e);
        verifyNoMoreInteractions(client);
    }

    @Test
    public void putInCacheLimitLessThanKeysButGreaterThan1() throws InterruptedException {
        // given
        Map<String, Object> request = Utils.makeMapWithNulls(
                Pair.of("key1", null),
                Pair.of("key2", v),
                Pair.of("key3", v)
        );
        int limit = 2;
        agent.setBulkPutBatchSize(limit);

        // when
        agent.putInCache(request, e);
        flushAsyncOperations(executor);

        // then
        assertThat(limit).as("should be less for representative test").isLessThan(request.size());
        verify(client).setMany(Utils.makeMapWithNulls(
                Pair.of("key1", CachedValue.NULL),
                Pair.of("key2", v)
        ), e);
        verify(client).set("key3", v, e);
        verifyNoMoreInteractions(client);
    }

    @Test
    public void deleteFromCacheEmpty() {
        // when
        agent.deleteFromCache(Collections.emptyList());

        // then
        verifyNoInteractions(client);
    }

    @Test
    public void deleteFromCacheSingle() {
        // when
        agent.deleteFromCache(Collections.singletonList("key"));

        // then
        verify(client).delete("key");
        verifyNoMoreInteractions(client);
    }

    @Test
    public void deleteFromCacheLimitDefault() {
        // when
        agent.deleteFromCache(Arrays.asList("key1", "key2"));

        // then
        verify(client).delete("key1");
        verify(client).delete("key2");
        verifyNoMoreInteractions(client);
    }

    @Test
    public void deleteFromCacheLimitGtEqThanKeys() {
        // given
        Collection<String> keys = Arrays.asList("key1", "key2");
        agent.setBulkDeleteBatchSize(keys.size());

        // when
        agent.deleteFromCache(keys);

        // then
        verify(client).deleteMany(keys);
        verifyNoMoreInteractions(client);
    }

    @Test
    public void deleteFromCacheLimitLessThanKeysButGreaterThan1() {
        // given
        Collection<String> keys = Arrays.asList("key1", "key2", "key3");
        int limit = 2;
        agent.setBulkDeleteBatchSize(limit);

        // when
        agent.deleteFromCache(keys);

        // then
        assertThat(limit).as("should be less for representative test").isLessThan(keys.size());
        verify(client).deleteMany(Arrays.asList("key1", "key2"));
        verify(client).deleteMany(Collections.singletonList("key3"));
        verifyNoMoreInteractions(client);
    }
}
