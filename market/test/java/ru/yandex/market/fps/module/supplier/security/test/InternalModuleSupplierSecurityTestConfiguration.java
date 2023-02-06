package ru.yandex.market.fps.module.supplier.security.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@ComponentScan("ru.yandex.market.fps.module.supplier.security.impl")
@Import({
        ModuleSupplierSecurityTestConfiguration.class,
})
public class InternalModuleSupplierSecurityTestConfiguration extends AbstractModuleConfiguration {
    protected InternalModuleSupplierSecurityTestConfiguration() {
        super("module/supplier/security/test");
    }
}
