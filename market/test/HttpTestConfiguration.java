package ru.yandex.market.jmf.http.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.http.HttpConfiguration;
import ru.yandex.market.jmf.queue.retry.RetryServiceTestConfiguration;

@Configuration
@Import({
        HttpConfiguration.class,
        RetryServiceTestConfiguration.class,
})
@ComponentScan("ru.yandex.market.jmf.http.test.impl")
public class HttpTestConfiguration {
}
