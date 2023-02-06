package ru.yandex.market.fps.module.export.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fps.module.export.ModuleExportConfiguration;
import ru.yandex.market.jmf.ui.test.UiTestConfiguration;

@Configuration
@Import({
        ModuleExportConfiguration.class,
        UiTestConfiguration.class,
})
@ComponentScan("ru.yandex.market.fps.module.export.test.impl")
public class ModuleExportTestConfiguration {
}
