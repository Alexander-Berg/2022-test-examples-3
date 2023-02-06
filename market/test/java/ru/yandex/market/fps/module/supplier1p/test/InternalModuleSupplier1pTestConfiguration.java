package ru.yandex.market.fps.module.supplier1p.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@ComponentScan("ru.yandex.market.fps.module.supplier1p.impl")
@Import({
        ModuleSupplier1pTestConfiguration.class,
})
public class InternalModuleSupplier1pTestConfiguration extends AbstractModuleConfiguration {
    protected InternalModuleSupplier1pTestConfiguration() {
        super("module/supplier1p/test");
    }
}
