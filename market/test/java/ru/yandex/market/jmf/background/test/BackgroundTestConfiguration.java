package ru.yandex.market.jmf.background.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.background.BackgroundConfiguration;

@Configuration
@Import({
        BackgroundConfiguration.class,
})
@ComponentScan("ru.yandex.market.jmf.background.test.impl")
public class BackgroundTestConfiguration {
}
