package ru.yandex.market.jmf.configuration.test.impl;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.configuration.ConfigurationConfiguration;
import ru.yandex.market.jmf.logic.def.test.LogicDefaultTestConfiguration;

@Configuration
@Import({
        ConfigurationConfiguration.class,
        LogicDefaultTestConfiguration.class,
})
public class ConfigurationTestConfiguration {
}
