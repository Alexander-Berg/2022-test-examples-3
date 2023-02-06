package ru.yandex.market.jmf.logic.def.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.jmf.logic.def.LogicDefaultConfiguration;
import ru.yandex.market.jmf.module.resizer.test.ModuleResizerTestConfiguration;
import ru.yandex.market.jmf.trigger.test.TriggerTestConfiguration;
import ru.yandex.market.jmf.ui.api.test.UiApiTestConfiguration;

@Configuration
@Import({
        LogicDefaultConfiguration.class,
        TriggerTestConfiguration.class,
        ModuleResizerTestConfiguration.class,
        UiApiTestConfiguration.class,
})
@ComponentScan("ru.yandex.market.jmf.logic.def.test.impl")
public class LogicDefaultTestConfiguration {
    @Bean
    public EnvironmentResolver environmentResolver() {
        return () -> Environment.INTEGRATION_TEST;
    }

}
