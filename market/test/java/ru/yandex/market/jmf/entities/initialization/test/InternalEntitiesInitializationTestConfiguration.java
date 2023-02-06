package ru.yandex.market.jmf.entities.initialization.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(EntitiesInitializationTestConfiguration.class)
public class InternalEntitiesInitializationTestConfiguration extends AbstractModuleConfiguration {
    protected InternalEntitiesInitializationTestConfiguration() {
        super("entities/initialization/test");
    }
}
