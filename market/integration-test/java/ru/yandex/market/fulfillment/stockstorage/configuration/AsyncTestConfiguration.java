package ru.yandex.market.fulfillment.stockstorage.configuration;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import ru.yandex.market.fulfillment.stockstorage.config.AsyncConfiguration;
import ru.yandex.market.fulfillment.stockstorage.util.AsyncWaiterService;

@Configuration
@Import(AsyncConfiguration.class)
public class AsyncTestConfiguration {

    @Bean
    public AsyncWaiterService asyncWaiterServiceTest(List<ThreadPoolTaskExecutor> executors) {
        return new AsyncWaiterService(executors);
    }
}
