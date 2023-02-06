package ru.yandex.market.jmf.module.automation.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.module.automation.ModuleAutomationRuleConfiguration;
import ru.yandex.market.jmf.module.ou.test.ModuleOuTestConfiguration;
import ru.yandex.market.jmf.module.scheduling.ModuleSchedulingTestConfiguration;

@Configuration
@Import({
        ModuleAutomationRuleConfiguration.class,
        ModuleOuTestConfiguration.class,
        ModuleSchedulingTestConfiguration.class,
})
@ComponentScan("ru.yandex.market.jmf.module.automation.test.utils")
public class ModuleAutomationRuleTestConfiguration {
}
