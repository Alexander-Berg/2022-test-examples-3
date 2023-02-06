package ru.yandex.market.core.trace;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.common.cache.memcached.client.MemCachedClient;
import ru.yandex.market.request.trace.Module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class TracingMemCachedClientDecoratorTest {
    static final Date e = new Date(0L);
    static final String key = "key";
    static final Collection<String> keys = List.of("a", "b");
    MemCachedClient client = mock(MemCachedClient.class);
    MemCachedClient decorator = new TracingMemCachedClientDecorator(client, Module.MEMCACHED);

    @Test
    void selfWrappingIsForbidden() {
        // when-then
        assertThrows(
                IllegalArgumentException.class,
                () -> new TracingMemCachedClientDecorator(decorator, Module.MEMCACHED)
        );
    }

    @Test
    void get() {
        // given
        var expected = "some";
        given(client.get(anyString())).willReturn(expected);

        // when
        Object result = decorator.get(key);

        // then
        assertThat(result).isEqualTo(expected);
        verify(client).get(key);
        verifyNoMoreInteractions(client);
    }

    @Test
    void getMany() {
        // given
        Map<String, Object> expected = Map.of("a", 1L);
        given(client.getMany(anyCollection())).willReturn(expected);

        // when
        Object result = decorator.getMany(keys);

        // then
        assertThat(result).isEqualTo(expected);
        verify(client).getMany(keys);
        verifyNoMoreInteractions(client);
    }

    @Test
    void add() {
        // given
        var value = 1L;
        given(client.add(anyString(), any(), any())).willReturn(true);

        // when
        var result = decorator.add(key, value, e);

        // then
        assertThat(result).isTrue();
        verify(client).add(key, value, e);
        verifyNoMoreInteractions(client);
    }

    @Test
    void set() {
        // given
        var value = 1L;

        // when
        decorator.set(key, value, e);

        // then
        verify(client).set(key, value, e);
        verifyNoMoreInteractions(client);
    }

    @Test
    void setMany() {
        // given
        Map<String, Object> keyResults = Map.of(key, 1L);

        // when
        decorator.setMany(keyResults, e);

        // then
        verify(client).setMany(keyResults, e);
        verifyNoMoreInteractions(client);
    }

    @Test
    void delete() {
        // when
        decorator.delete(key);

        // then
        verify(client).delete(key);
        verifyNoMoreInteractions(client);
    }

    @Test
    void deleteMany() {
        // when
        decorator.deleteMany(keys);

        // then
        verify(client).deleteMany(keys);
        verifyNoMoreInteractions(client);
    }

    @Test
    void incr() {
        // given
        long expected = 1L;
        given(client.incr(anyString(), anyLong())).willReturn(expected);

        // when
        Object result = decorator.incr(key, 2L);

        // then
        assertThat(result).isEqualTo(expected);
        verify(client).incr(key, 2L);
        verifyNoMoreInteractions(client);
    }

    @Test
    void prepareKeys() {
        assertThat(TracingMemCachedClientDecorator.prepareKeys("p:", null))
                .isEqualTo("p:keys_empty");
        assertThat(TracingMemCachedClientDecorator.prepareKeys("p:", List.of()))
                .isEqualTo("p:keys_empty");
        assertThat(TracingMemCachedClientDecorator.prepareKeys("p:", List.of("K")))
                .isEqualTo("p:key_first:K;keys_size:1");
        assertThat(TracingMemCachedClientDecorator.prepareKeys("set_many:", List.of("K1", "K2")))
                .isEqualTo("set_many:key_first:K1;keys_size:2");
    }
}
