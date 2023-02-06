package ru.yandex.market.jmf.search;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.logic.def.test.LogicDefaultTestConfiguration;

@Configuration
@ComponentScan({
        "ru.yandex.market.jmf.search.impl"
})
@Import({
        ModuleSearchConfiguration.class,
        LogicDefaultTestConfiguration.class
})
public class ModuleSearchTestConfiguration {
}
