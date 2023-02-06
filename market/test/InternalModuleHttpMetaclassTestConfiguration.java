package ru.yandex.market.jmf.module.http.metaclass.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(ModuleHttpMetaclassTestConfiguration.class)
public class InternalModuleHttpMetaclassTestConfiguration extends AbstractModuleConfiguration {

    protected InternalModuleHttpMetaclassTestConfiguration() {
        super("test/module/http/metaclass");
    }
}
