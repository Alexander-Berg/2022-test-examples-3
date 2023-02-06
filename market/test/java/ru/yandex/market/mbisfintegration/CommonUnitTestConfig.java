package ru.yandex.market.mbisfintegration;

import java.util.concurrent.CompletableFuture;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.mbisfintegration.importer.yt.YtMockHelper;
import ru.yandex.yt.ytclient.proxy.YtClient;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 08.02.2022
 */
@Configuration
@ComponentScan("ru.yandex.market.mbisfintegration")
public class CommonUnitTestConfig {

    @Bean
    public YtClient ytClientMock() {
        YtClient ytClient = Mockito.mock(YtClient.class);
        Mockito.when(ytClient.waitProxies()).thenReturn(CompletableFuture.completedFuture(null));
        return ytClient;
    }

    @Bean
    public YtMockHelper ytMockHelper(YtClient ytClient) {
        return new YtMockHelper(ytClient);
    }
}
