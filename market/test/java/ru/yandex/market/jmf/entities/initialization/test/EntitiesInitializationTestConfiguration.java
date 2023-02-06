package ru.yandex.market.jmf.entities.initialization.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.entities.initialization.EntitiesInitializationConfiguration;
import ru.yandex.market.jmf.logic.def.test.LogicDefaultTestConfiguration;

@Configuration
@Import({
        EntitiesInitializationConfiguration.class,
        LogicDefaultTestConfiguration.class,
})
@ComponentScan("ru.yandex.market.jmf.entities.initialization.test.impl")
public class EntitiesInitializationTestConfiguration {
}
