package ru.yandex.market.jmf.ui.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(UiTestConfiguration.class)
public class InternalUiTestConfiguration extends AbstractModuleConfiguration {
    protected InternalUiTestConfiguration() {
        super("ui/test");
    }
}
