package ru.yandex.market.fps.module.supplier1p.offers.test;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fps.module.mbo.test.ModuleMboTestConfiguration;
import ru.yandex.market.fps.module.supplier1p.offers.ModuleSupplier1pOffersConfiguration;
import ru.yandex.market.fps.module.supplier1p.test.ModuleSupplier1pTestConfiguration;

@Configuration
@Import({
        ModuleSupplier1pOffersConfiguration.class,
        ModuleMboTestConfiguration.class,
        ModuleSupplier1pTestConfiguration.class
})
public class ModuleSupplier1pOffersTestConfiguration {
}
