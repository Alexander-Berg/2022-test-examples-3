package ru.yandex.market.jmf.catalog.items.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.catalog.items.CatalogItemsConfiguration;
import ru.yandex.market.jmf.entities.initialization.test.EntitiesInitializationTestConfiguration;
import ru.yandex.market.jmf.logic.wf.test.LogicWfTestConfiguration;

@Configuration
@Import({
        CatalogItemsConfiguration.class,
        LogicWfTestConfiguration.class,
        EntitiesInitializationTestConfiguration.class,
})
@ComponentScan("ru.yandex.market.jmf.catalog.items.test.impl")
public class CatalogItemsTestConfiguration {
}
