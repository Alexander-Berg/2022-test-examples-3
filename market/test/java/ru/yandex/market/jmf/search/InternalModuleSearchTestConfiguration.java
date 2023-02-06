package ru.yandex.market.jmf.search;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(ModuleSearchTestConfiguration.class)
public class InternalModuleSearchTestConfiguration extends AbstractModuleConfiguration {

    public InternalModuleSearchTestConfiguration() {
        super("module/search/test");
    }
}
