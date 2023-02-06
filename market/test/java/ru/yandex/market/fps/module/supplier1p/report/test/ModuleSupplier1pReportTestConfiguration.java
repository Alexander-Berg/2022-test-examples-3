package ru.yandex.market.fps.module.supplier1p.report.test;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fps.module.supplier1p.report.ModuleSupplier1pReportConfiguration;
import ru.yandex.market.fps.module.supplier1p.test.ModuleSupplier1pTestConfiguration;

@Configuration
@Import({
        ModuleSupplier1pReportConfiguration.class,
        ModuleSupplier1pTestConfiguration.class,
})
public class ModuleSupplier1pReportTestConfiguration {
}
