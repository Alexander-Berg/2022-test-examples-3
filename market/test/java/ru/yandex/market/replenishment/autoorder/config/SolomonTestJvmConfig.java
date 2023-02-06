package ru.yandex.market.replenishment.autoorder.config;

import java.util.concurrent.CompletableFuture;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.inside.solomon.pusher.SolomonPusher;
import ru.yandex.market.replenishment.autoorder.service.solomon.AutoorderSolomonPushService;

@Configuration
public class SolomonTestJvmConfig {

    @Bean
    public SolomonPusher autoorderSolomonPusher() {
        final SolomonPusher solomonPusher = Mockito.mock(SolomonPusher.class);
        Mockito.when(solomonPusher.push(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));
        return solomonPusher;
    }

    @Bean
    public AutoorderSolomonPushService autoorderSolomonPushService(SolomonPusher autoorderSolomonPusher) {
        return new AutoorderSolomonPushService("test", autoorderSolomonPusher);
    }
}
