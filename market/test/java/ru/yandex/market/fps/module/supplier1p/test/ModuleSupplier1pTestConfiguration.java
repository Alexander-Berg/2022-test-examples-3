package ru.yandex.market.fps.module.supplier1p.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fps.module.balance.test.ModuleBalanceTestConfiguration;
import ru.yandex.market.fps.module.mbi.test.ModuleMbiTestConfiguration;
import ru.yandex.market.fps.module.supplier1p.ModuleSupplier1pConfiguration;
import ru.yandex.market.jmf.module.startrek.ModuleStartrekTestConfiguration;

@Configuration
@ComponentScan("ru.yandex.market.fps.module.supplier1p.test.impl")
@Import({
        ModuleSupplier1pConfiguration.class,
        ModuleMbiTestConfiguration.class,
        ModuleBalanceTestConfiguration.class,
        ModuleStartrekTestConfiguration.class
})
public class ModuleSupplier1pTestConfiguration {
}
