package ru.yandex.market.fps.module.supplier.specification.test;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fps.module.axapta.test.ModuleAxaptaTestConfiguration;
import ru.yandex.market.fps.module.supplier.specification.ModuleSupplierSpecificationConfiguration;
import ru.yandex.market.fps.module.supplier1p.test.ModuleSupplier1pTestConfiguration;

@Configuration
@Import({
        ModuleSupplierSpecificationConfiguration.class,
        ModuleSupplier1pTestConfiguration.class,
        ModuleAxaptaTestConfiguration.class
})
public class ModuleSupplierSpecificationTestConfiguration {
}
