package ru.yandex.market.jmf.module.metric.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.module.metric.MetricsModuleConfiguration;
import ru.yandex.market.jmf.utils.UtilsTestConfiguration;

@Configuration
@ComponentScan("ru.yandex.market.jmf.module.metric.test.impl")
@Import({
        MetricsModuleConfiguration.class,
        UtilsTestConfiguration.class,
})
public class MetricsModuleTestConfiguration {
}
