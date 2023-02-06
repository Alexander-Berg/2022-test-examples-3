package ru.yandex.common.cache.memcached.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.common.util.collections.Pair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class TransactionalMemCachedAgentTest extends BaseAgentTest {
    TransactionalMemCachedAgent agent = new TransactionalMemCachedAgent();
    ExecutorService executor = Executors.newSingleThreadExecutor();

//    @Rule
    public Timeout globalTimeout = new Timeout(3, TimeUnit.SECONDS);

    @Before
    public void setUp() {
        super.setUp();
        agent.setMemCachedClient(client);
        agent.setExecutor(executor);
        agent.afterPropertiesSet();
    }

    @After
    public void tearDown() {
        agent.close();
        executor.shutdownNow();
    }

    @Test
    public void getFromCacheOneTx() throws InterruptedException {
        // when
        agent.beginTransaction();
        Object result1 = agent.getFromCache("key");
        invalidateLocalCache();
        Object result2 = agent.getFromCache("key");

        // then
        assertThat(result1).isEqualTo(v);
        assertThat(result2).isEqualTo(result1);
        verify(client, times(2).description("results a NOT cached in tx until being put"))
                .get("key");
        verifyNoMoreInteractions(client);
    }

    @Test
    public void getFromCacheEmptyTx() {
        // when
        agent.beginTransaction();
        Map<String, Object> result = agent.getFromCache(Collections.emptyList());

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    public void getFromCacheMultipleTx() throws InterruptedException {
        // when
        agent.beginTransaction();
        Map<String, Object> result1 = agent.getFromCache(Collections.singleton("key"));
        invalidateLocalCache();
        Map<String, Object> result2 = agent.getFromCache(Collections.singleton("key"));

        // then
        assertThat(result1).isEqualTo(Collections.singletonMap("key", v));
        assertThat(result2).isEqualTo(result1);
        verify(client, times(2).description("results a NOT cached in tx until being put"))
                .getMany(Lists.newLinkedList(Collections.singleton("key")));
        verifyNoMoreInteractions(client); // results should be cached locally in tx after first call
    }

    @Test
    public void putInCacheOneTxCommit() throws InterruptedException {
        // when
        agent.beginTransaction();
        Object result1 = agent.getFromCache("key");
        assertThat(result1).isEqualTo(v);
        verify(client).get("key");

        agent.putInCache("key", "another");
        invalidateLocalCache();

        Object result2 = agent.getFromCache("key");
        assertThat(result2).isEqualTo("another");
        verifyNoMoreInteractions(client); // mutations are postponed until commit

        commit();
        verify(client).set(eq("key"), eq("another"), any());
    }

    @Test
    public void putInCacheOneTxRollback() throws InterruptedException {
        // when
        agent.beginTransaction();
        Object result1 = agent.getFromCache("key");
        agent.putInCache("key", "another");
        Object result2 = agent.getFromCache("key");
        rollback();

        // then
        assertThat(result1).isEqualTo(v);
        assertThat(result2).isEqualTo("another");
        verify(client).get("key");
        verifyNoMoreInteractions(client); // mutations are postponed until commit
    }

    @Test
    public void putInCacheMultipleTxCommit() throws InterruptedException {
        // given
        agent.setBulkPutBatchSize(10);
        Collection<String> keys = Arrays.asList("key1", "key2");

        // when
        agent.beginTransaction();
        Map<String, Object> result1 = agent.getFromCache(keys);
        assertThat(result1).isEqualTo(Utils.makeMapWithNulls(
                Pair.of("key1", v),
                Pair.of("key2", v)
        ));
        verify(client).getMany(keys);

        agent.putInCache(Utils.makeMapWithNulls(
                Pair.of("key2", "another"), // change
                Pair.of("key3", 3)
        ), e);
        invalidateLocalCache();

        Map<String, Object> result2 = agent.getFromCache(keys);
        assertThat(result2).isEqualTo(Utils.makeMapWithNulls(
                Pair.of("key1", v),
                Pair.of("key2", "another")
        ));
        verify(client, description("key2 was modified in tx cache")).getMany(Collections.singletonList("key1"));
        verifyNoMoreInteractions(client); // mutations are postponed until commit

        commit();
        verify(client).setMany(eq(Utils.makeMapWithNulls(
                Pair.of("key2", "another"),
                Pair.of("key3", 3)
        )), any());
    }

    @Test
    public void putInCacheMultipleTxRollback() throws InterruptedException {
        // given
        agent.setBulkPutBatchSize(10);
        Collection<String> keys = Arrays.asList("key1", "key2");

        // when
        agent.beginTransaction();
        Map<String, Object> result1 = agent.getFromCache(keys);
        agent.putInCache(Utils.makeMapWithNulls(
                Pair.of("key2", "another"), // change
                Pair.of("key3", 3)
        ), e);
        Map<String, Object> result2 = agent.getFromCache(keys);
        rollback();

        // then
        assertThat(result1).isEqualTo(Utils.makeMapWithNulls(
                Pair.of("key1", v),
                Pair.of("key2", v)
        ));
        assertThat(result2).isEqualTo(Utils.makeMapWithNulls(
                Pair.of("key1", v),
                Pair.of("key2", "another")
        ));
        verify(client).getMany(keys);
        verifyNoMoreInteractions(client); // mutations are postponed until commit
    }

    @Test
    public void deleteFromCacheOneTxCommit() throws InterruptedException {
        // when
        agent.beginTransaction();
        Object result1 = agent.getFromCache("key");
        assertThat(result1).isEqualTo(v);
        verify(client).get("key");

        agent.deleteFromCache("key");
        Object result2 = agent.getFromCache("key");
        assertThat(result2).isNull();
        verifyNoMoreInteractions(client); // mutations are postponed until commit

        commit();
        verify(client).delete("key");
    }

    @Test
    public void deleteFromCacheOneTxRollback() throws InterruptedException {
        // when
        agent.beginTransaction();
        Object result1 = agent.getFromCache("key");
        agent.deleteFromCache("key");
        Object result2 = agent.getFromCache("key");
        rollback();

        // then
        assertThat(result1).isEqualTo(v);
        assertThat(result2).isNull();
        verify(client).get("key");
        verifyNoMoreInteractions(client); // mutations are postponed until commit
    }

    public void commit() throws InterruptedException {
        agent.endTransaction(true);
        flushAsyncOperations(executor);
    }

    public void rollback() throws InterruptedException {
        agent.endTransaction(false);
        flushAsyncOperations(executor);
    }

}
