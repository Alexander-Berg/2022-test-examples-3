package ru.yandex.market.fps.module.export.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(ModuleExportTestConfiguration.class)
public class InternalModuleExportTestConfiguration extends AbstractModuleConfiguration {
    protected InternalModuleExportTestConfiguration() {
        super("module/export/test");
    }
}
