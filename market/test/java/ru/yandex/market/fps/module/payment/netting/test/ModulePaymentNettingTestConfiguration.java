package ru.yandex.market.fps.module.payment.netting.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fps.module.payment.netting.ModulePaymentNettingConfiguration;
import ru.yandex.market.fps.module.supplier1p.test.ModuleSupplier1pTestConfiguration;
import ru.yandex.market.jmf.dataimport.test.DataImportTestConfiguration;

@Configuration
@ComponentScan("ru.yandex.market.fps.module.payment.netting.test.impl")
@Import({
        ModulePaymentNettingConfiguration.class,
        ModuleSupplier1pTestConfiguration.class,
        DataImportTestConfiguration.class,
})
public class ModulePaymentNettingTestConfiguration {
}
