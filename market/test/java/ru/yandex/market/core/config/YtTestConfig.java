package ru.yandex.market.core.config;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.core.yt.YtRpcClientFactory;
import ru.yandex.market.yt.client.YtClientProxy;
import ru.yandex.market.yt.client.YtDynamicTableClientFactory;
import ru.yandex.yt.ytclient.bus.BusConnector;
import ru.yandex.yt.ytclient.proxy.YtClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@Configuration
public class YtTestConfig {
    @Bean({
            "mockYtBusConnector",
            "ytBusConnector",
            "busConnector",
    })
    public BusConnector mockYtBusConnector() {
        return mock(BusConnector.class);
    }

    @Bean
    @Primary
    public YtRpcClientFactory mockYtRpcClientFactory(
            @Nullable @Qualifier("ytClientFactory") YtRpcClientFactory factory
    ) {
        if (factory == null) {
            return factory; // no-op null bean
        }
        factory = spy(factory);
        doReturn(mock(YtClient.class))
                .when(factory)
                .build(any());
        return factory;
    }

    @Bean
    @Primary
    public YtDynamicTableClientFactory mockYtDynamicTableClientFactory(
            @Nullable @Qualifier("ytDynamicTableClientFactory") YtDynamicTableClientFactory factory
    ) {
        if (factory == null) {
            return factory; // no-op null bean
        }
        factory = spy(factory);
        doReturn(mock(YtClientProxy.class))
                .when(factory)
                .makeClient(anyString(), any());
        return factory;
    }
}
