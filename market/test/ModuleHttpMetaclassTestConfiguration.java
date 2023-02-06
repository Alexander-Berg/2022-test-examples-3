package ru.yandex.market.jmf.module.http.metaclass.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.catalog.items.test.CatalogItemsTestConfiguration;
import ru.yandex.market.jmf.module.http.metaclass.ModuleHttpMetaclassConfiguration;
import ru.yandex.market.jmf.module.http.support.ModuleHttpSupportTestConfiguration;
import ru.yandex.market.jmf.module.properties.test.ModuleContextPropertiesTestConfiguration;

@Import({
        ModuleHttpMetaclassConfiguration.class,
        ModuleHttpSupportTestConfiguration.class,
        ModuleContextPropertiesTestConfiguration.class,
        CatalogItemsTestConfiguration.class,
})
@Configuration
@ComponentScan("ru.yandex.market.jmf.module.http.metaclass.test.utils")
public class ModuleHttpMetaclassTestConfiguration {
}
