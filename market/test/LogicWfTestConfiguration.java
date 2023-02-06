package ru.yandex.market.jmf.logic.wf.test;

import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.configuration.test.impl.ConfigurationTestConfiguration;
import ru.yandex.market.jmf.logic.wf.LogicWfConfiguration;
import ru.yandex.market.jmf.search.ModuleSearchTestConfiguration;

@Import({
        LogicWfConfiguration.class,
        ModuleSearchTestConfiguration.class,
        ConfigurationTestConfiguration.class
})
public class LogicWfTestConfiguration {
}
