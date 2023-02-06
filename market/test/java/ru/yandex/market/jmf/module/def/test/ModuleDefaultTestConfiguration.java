package ru.yandex.market.jmf.module.def.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.SpringInfrastructureTestConfiguration;
import ru.yandex.market.jmf.catalog.items.test.CatalogItemsTestConfiguration;
import ru.yandex.market.jmf.module.def.ModuleDefaultConfiguration;
import ru.yandex.market.jmf.module.http.support.ModuleHttpSupportTestConfiguration;
import ru.yandex.market.jmf.module.transfer.test.ModuleDataTransferTestConfiguration;
import ru.yandex.market.jmf.startrek.support.test.StartrekSupportTestConfiguration;

@Configuration
@Import({
        ModuleDefaultConfiguration.class,
        CatalogItemsTestConfiguration.class,
        StartrekSupportTestConfiguration.class,
        ModuleHttpSupportTestConfiguration.class,
        ModuleDataTransferTestConfiguration.class,
        SpringInfrastructureTestConfiguration.class,
})
@ComponentScan("ru.yandex.market.jmf.module.def.test.impl")
public class ModuleDefaultTestConfiguration {

}
