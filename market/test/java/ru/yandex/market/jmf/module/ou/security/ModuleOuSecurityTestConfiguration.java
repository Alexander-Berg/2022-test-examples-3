package ru.yandex.market.jmf.module.ou.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.blackbox.support.test.BlackBoxSupportTestConfiguration;
import ru.yandex.market.jmf.module.ou.test.ModuleOuTestConfiguration;
import ru.yandex.market.jmf.ui.test.UiTestConfiguration;

// Конфигурация, которая может использоваться из других модулей
@Configuration
@Import({
        ModuleOuSecurityConfiguration.class,
        UiTestConfiguration.class,
        ModuleOuTestConfiguration.class,
        BlackBoxSupportTestConfiguration.class,
})
public class ModuleOuSecurityTestConfiguration {
}
