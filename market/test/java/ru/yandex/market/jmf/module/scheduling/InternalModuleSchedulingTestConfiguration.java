package ru.yandex.market.jmf.module.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.logic.def.test.LogicDefaultTestConfiguration;
import ru.yandex.market.jmf.module.def.test.ModuleDefaultTestConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Import({
        LogicDefaultTestConfiguration.class,
        ModuleSchedulingTestConfiguration.class,
        ModuleDefaultTestConfiguration.class
})
@Configuration
public class InternalModuleSchedulingTestConfiguration extends AbstractModuleConfiguration {

    public InternalModuleSchedulingTestConfiguration() {
        super("test/jmf/module/scheduling");
    }
}
