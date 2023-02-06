package ru.yandex.market.fps.module.supplier.security.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fps.module.supplier.security.ModuleSupplierSecurityConfiguration;
import ru.yandex.market.fps.module.supplier1p.test.ModuleSupplier1pTestConfiguration;
import ru.yandex.market.jmf.blackbox.support.test.BlackBoxSupportTestConfiguration;
import ru.yandex.market.jmf.catalog.items.test.CatalogItemsTestConfiguration;

@Configuration
@ComponentScan("ru.yandex.market.fps.module.supplier.security.test.impl")
@Import({
        ModuleSupplierSecurityConfiguration.class,
        ModuleSupplier1pTestConfiguration.class,
        CatalogItemsTestConfiguration.class,
        BlackBoxSupportTestConfiguration.class,
})
public class ModuleSupplierSecurityTestConfiguration {
}
