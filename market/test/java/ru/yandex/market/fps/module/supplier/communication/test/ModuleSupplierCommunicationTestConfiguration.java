package ru.yandex.market.fps.module.supplier.communication.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fps.module.supplier.communication.ModuleSupplierCommunicationConfiguration;
import ru.yandex.market.fps.module.supplier1p.test.ModuleSupplier1pTestConfiguration;

@Configuration
@Import({
        ModuleSupplierCommunicationConfiguration.class,
        ModuleSupplier1pTestConfiguration.class,
})
public class ModuleSupplierCommunicationTestConfiguration {
}
