package ru.yandex.market.jmf.configuration.test.impl;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(ConfigurationTestConfiguration.class)
public class InternalConfigurationTestConfiguration extends AbstractModuleConfiguration {
    public InternalConfigurationTestConfiguration() {
        super("configuration");
    }
}
