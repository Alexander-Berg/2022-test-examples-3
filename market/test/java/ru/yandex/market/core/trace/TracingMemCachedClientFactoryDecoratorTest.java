package ru.yandex.market.core.trace;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.cache.memcached.client.MemCachedClient;
import ru.yandex.common.cache.memcached.client.MemCachedClientFactory;
import ru.yandex.market.request.trace.Module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TracingMemCachedClientFactoryDecoratorTest {
    MemCachedClient client = mock(MemCachedClient.class);
    MemCachedClientFactory factory = mock(MemCachedClientFactory.class);
    MemCachedClientFactory decorator = new TracingMemCachedClientFactoryDecorator(factory, Module.MEMCACHED);

    @BeforeEach
    void setUp() {
        given(factory.newClient(any(), anyList())).willReturn(client);
    }

    @Test
    void selfWrappingIsForbidden() {
        // when-then
        assertThrows(
                IllegalArgumentException.class,
                () -> new TracingMemCachedClientFactoryDecorator(decorator, Module.MEMCACHED)
        );
    }

    @Test
    void newClient() {
        // when
        var result = decorator.newClient(List.of("whatever"));
        result.delete("key");

        // then
        assertThat(result).isInstanceOf(TracingMemCachedClientDecorator.class);
        verify(client).delete("key");
    }
}
