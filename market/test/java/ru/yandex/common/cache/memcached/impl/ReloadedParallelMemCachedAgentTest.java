package ru.yandex.common.cache.memcached.impl;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.common.cache.memcached.client.MemCachedClusterFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ReloadedParallelMemCachedAgentTest extends BaseAgentTest {
    @Test
    public void fallsBackToMockClientOnInitializationError() {
        // given
        MemCachedClusterFactory clientFactory = Mockito.mock(MemCachedClusterFactory.class);
        when(clientFactory.makeClusterClient(any()))
                .thenThrow(RuntimeException.class)
                .thenThrow(RuntimeException.class)
                .thenReturn(client)
                .thenThrow(RuntimeException.class);
        ReloadedParallelMemCachedAgent agent = new ReloadedParallelMemCachedAgent();
        agent.setMemCachedClient(null); // just to be sure
        agent.setMemCachedClusterFactory(clientFactory);

        // when-then
        agent.afterPropertiesSet();
        assertThat(agent.getMemCachedClient()).isInstanceOf(StubMemCachedClient.class);

        agent.reload();
        assertThat(agent.getMemCachedClient()).isInstanceOf(StubMemCachedClient.class);

        agent.reload();
        assertThat(agent.getMemCachedClient()).isSameAs(client);

        agent.reload();
        assertThat(agent.getMemCachedClient())
                .as("reload не делает ничего после успешной попытки подключения")
                .isSameAs(client);

        // then
        agent.close();
    }
}
