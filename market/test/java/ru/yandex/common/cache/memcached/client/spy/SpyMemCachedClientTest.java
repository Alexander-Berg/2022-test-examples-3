package ru.yandex.common.cache.memcached.client.spy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationException;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.common.cache.memcached.impl.Utils;
import ru.yandex.common.util.collections.Pair;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class SpyMemCachedClientTest {
    Date e = new Date(0);
    MemcachedClientIF backend = Mockito.mock(MemcachedClientIF.class);
    SpyMemCachedJMXMetricsCollector metricCollector = Mockito.mock(SpyMemCachedJMXMetricsCollector.class);
    SpyMemCachedClient client = new SpyMemCachedClient(backend, metricCollector);

    @Rule
    public Timeout globalTimeout = new Timeout(3, TimeUnit.SECONDS);

    @Before
    public void setUp() {
        setupOperations(null);
    }

    @After
    public void tearDown() {
        client.close();
        verify(metricCollector).close();
    }

    @Test
    public void setManyEmpty() {
        // when
        client.setMany(Collections.emptyMap(), e);

        // then
        verifyNoInteractions(backend);
    }

    @Test
    public void setManySingle() {
        // when
        client.setMany(Collections.singletonMap("key1", 1), e);

        // then
        verify(backend).set("key1", 0, 1);
        verifyNoMoreInteractions(backend);
    }

    @Test
    public void setManySingleError() {
        // given
        setupOperations("boom");

        // when
        client.setMany(Collections.singletonMap("key1", 1), e); // should not throw by default

        // then
        verify(backend).set("key1", 0, 1);
        verifyNoMoreInteractions(backend);
    }

    @Test
    public void setMany() {
        // when
        client.setMany(Utils.makeMapWithNulls(
                Pair.of("key11", 11),
                Pair.of("key12", 12)
        ), e);

        // then
        verify(backend).set("key11", 0, 11);
        verify(backend).set("key12", 0, 12);
        verifyNoMoreInteractions(backend);
    }

    @Test
    public void setManyError() {
        // given
        setupOperations("boom");

        // when
        client.setMany(Utils.makeMapWithNulls(
                Pair.of("key11", 11),
                Pair.of("key12", 12)
        ), e); // should not throw by default

        // then
        verify(backend).set("key11", 0, 11);
        verify(backend).set("key12", 0, 12);
        verifyNoMoreInteractions(backend);
    }

    @Test
    public void deleteManyEmpty() {
        // when
        client.deleteMany(Collections.emptyList());

        // then
        verifyNoInteractions(backend);
    }

    @Test
    public void deleteManySingle() {
        // when
        client.deleteMany(Collections.singleton("key1"));

        // then
        verify(backend).delete("key1");
        verifyNoMoreInteractions(backend);
    }

    @Test
    public void deleteManySingleError() {
        // given
        setupOperations("boom");

        // when
        client.deleteMany(Collections.singleton("key1")); // should not throw by default

        // then
        verify(backend).delete("key1");
        verifyNoMoreInteractions(backend);
    }

    @Test
    public void deleteMany() {
        // when
        client.deleteMany(Arrays.asList("key11", "key12"));

        // then
        verify(backend).delete("key11");
        verify(backend).delete("key12");
        verifyNoMoreInteractions(backend);
    }

    @Test
    public void deleteManyError() {
        // given
        setupOperations("boom");

        // when
        client.deleteMany(Arrays.asList("key11", "key12")); // should not throw by default

        // then
        verify(backend).delete("key11");
        verify(backend).delete("key12");
        verifyNoMoreInteractions(backend);
    }

    public void setupOperations(@Nullable String error) {
        Answer<Object> answer = invocation -> makeOpFuture(invocation.getArgument(0), error);
        given(backend.set(anyString(), anyInt(), any())).will(answer);
        given(backend.delete(anyString())).will(answer);
    }

    /**
     * @param key   op for
     * @param error if any
     * @return already completed operation
     */
    Future<Boolean> makeOpFuture(String key, @Nullable String error) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        if (StringUtils.isBlank(error)) {
            result.complete(true);
        } else {
            result.completeExceptionally(new OperationException(OperationErrorType.GENERAL, key + " failed: " + error));
        }
        return result;
    }
}
