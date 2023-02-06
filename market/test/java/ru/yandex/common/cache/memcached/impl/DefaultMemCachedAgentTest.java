package ru.yandex.common.cache.memcached.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.common.util.collections.Pair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class DefaultMemCachedAgentTest extends BaseAgentTest {
    DefaultMemCachedAgent agent = new DefaultMemCachedAgent();

    @Before
    public void setUp() {
        super.setUp();
        agent.setMemCachedClient(client);
        agent.afterPropertiesSet();
    }

    @After
    public void tearDown() {
        agent.close();
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
        Map<String, Object> result = agent.getFromCache(Collections.singleton("key"));

        // then
        assertThat(result).isEqualTo(Collections.singletonMap("key", v));
        verify(client).getMany(Collections.singleton("key"));
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
    public void putInCacheEmpty() {
        // when
        agent.putInCache(Collections.emptyMap(), e);

        // then
        verifyNoInteractions(client);
    }

    @Test
    public void putInCacheSingle() {
        // given
        Map<String, Object> request = Collections.singletonMap("key", v);

        // when
        agent.putInCache(request, e);

        // then
        verify(client).set("key", v, e);
        verifyNoMoreInteractions(client);
    }

    @Test
    public void putInCacheLimitDefault() {
        // given
        Map<String, Object> request = Utils.makeMapWithNulls(
                Pair.of("key1", null),
                Pair.of("key2", v)
        );

        // when
        agent.putInCache(request, e);

        // then
        verify(client, description("nulls are serialized to singleton")).set("key1", CachedValue.NULL, e);
        verify(client).set("key2", v, e);
        verifyNoMoreInteractions(client);
    }

    @Test
    public void putInCacheLimitGtEqThanKeys() {
        // given
        Map<String, Object> request = Utils.makeMapWithNulls(
                Pair.of("key1", null),
                Pair.of("key2", v)
        );
        agent.setBulkPutBatchSize(request.size());

        // when
        agent.putInCache(request, e);

        // then
        verify(client).setMany(Utils.makeMapWithNulls(
                Pair.of("key1", CachedValue.NULL),
                Pair.of("key2", v)
        ), e);
        verifyNoMoreInteractions(client);
    }

    @Test
    public void putInCacheLimitLessThanKeysButGreaterThan1() {
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

        // then
        assertThat(limit).as("should be less for representative test").isLessThan(request.size());
        verify(client).setMany(Utils.makeMapWithNulls(
                Pair.of("key1", CachedValue.NULL),
                Pair.of("key2", v)
        ), e);
        verify(client).setMany(Utils.makeMapWithNulls(
                Pair.of("key3", v)
        ), e);
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

    @Test
    public void limitLogKeys() {
        assertThat(agent.limitLogKeys(Collections.emptyList())).isEqualTo("(empty)");
        assertThat(agent.limitLogKeys(Collections.singletonList("1"))).isEqualTo("1");
        assertThat(agent.limitLogKeys(Arrays.asList("1", "2"))).isEqualTo("1,2");
        assertThat(agent.limitLogKeys(IntStream.range(0, 11)
                .mapToObj(Objects::toString)
                .collect(Collectors.toList())
        ))
                .as("trims to first 10 keys")
                .isEqualTo("0,1,2,3,4,5,6,7,8,9... total=11");
    }
}
