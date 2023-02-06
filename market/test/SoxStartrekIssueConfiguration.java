package ru.yandex.market.jmf.module.def.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Import({
        ModuleDefaultTestConfiguration.class,
})
@Configuration
public class SoxStartrekIssueConfiguration extends AbstractModuleConfiguration {
    protected SoxStartrekIssueConfiguration() {
        super("module/default/test");
    }
}
