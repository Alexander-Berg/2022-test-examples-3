package ru.yandex.market.jmf.http.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.http.HttpConfiguration;
import ru.yandex.market.jmf.module.metric.test.MetricsModuleTestConfiguration;

@Configuration
@Import({
        HttpConfiguration.class,
        MetricsModuleTestConfiguration.class,
})
@ComponentScan("ru.yandex.market.jmf.http.test.impl")
public class HttpTestConfiguration {
}
