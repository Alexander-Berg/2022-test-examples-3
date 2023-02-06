package ru.yandex.market.logistics.utilizer.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import ru.yandex.market.logistics.utilizer.config.dbqueue.DbqueueConfig;

@Import({
        DbqueueConfig.class,
        DataBaseConnectionConfig.class
})
@ComponentScan({
        "ru.yandex.market.logistics.utilizer.dbqueue",
        "ru.yandex.market.logistics.utilizer.config.dbqueue"
})
public class DbqueueIntegrationTestConfig {
}
