package ru.yandex.market.fps.module.supplier.communication.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import({
        ModuleSupplierCommunicationTestConfiguration.class,
})
public class InternalModuleSupplierCommunicationTestConfiguration extends AbstractModuleConfiguration {
    protected InternalModuleSupplierCommunicationTestConfiguration() {
        super("module/supplier/communication/test");
    }
}
