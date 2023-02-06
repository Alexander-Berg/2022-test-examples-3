package ru.yandex.market.jmf.module.relation.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(ModuleRelationTestConfiguration.class)
public class InternalModuleRelationTestConfiguration extends AbstractModuleConfiguration {
    public InternalModuleRelationTestConfiguration() {
        super("module/relation/test");
    }
}
