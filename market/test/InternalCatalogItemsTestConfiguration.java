package ru.yandex.market.jmf.catalog.items.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(CatalogItemsTestConfiguration.class)
public class InternalCatalogItemsTestConfiguration extends AbstractModuleConfiguration {
    protected InternalCatalogItemsTestConfiguration() {
        super("catalog/items/test");
    }
}
