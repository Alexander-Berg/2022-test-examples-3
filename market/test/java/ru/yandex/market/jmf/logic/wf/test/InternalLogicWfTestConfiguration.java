package ru.yandex.market.jmf.logic.wf.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(LogicWfTestConfiguration.class)
public class InternalLogicWfTestConfiguration extends AbstractModuleConfiguration {
    protected InternalLogicWfTestConfiguration() {
        super("logic/wf/test");
    }
}
