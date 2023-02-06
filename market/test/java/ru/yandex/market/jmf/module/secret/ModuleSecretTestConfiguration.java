package ru.yandex.market.jmf.module.secret;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.logic.def.test.LogicDefaultTestConfiguration;

@Configuration
@Import({
        ModuleSecretConfiguration.class,
        LogicDefaultTestConfiguration.class,
})
public class ModuleSecretTestConfiguration {

}
