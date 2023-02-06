package ru.yandex.market.ff.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({
        "ru.yandex.market.ff.dbqueue.producer",
        "ru.yandex.market.ff.dbqueue.service",
})
public class DbQueueWorkersConfigWithoutConsumers {
}
