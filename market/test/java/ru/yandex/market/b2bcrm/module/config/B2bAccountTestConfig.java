package ru.yandex.market.b2bcrm.module.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.b2bcrm.module.account.ModuleAccountConfiguration;
import ru.yandex.market.jmf.db.test.TestDefaultDataSourceConfiguration;
import ru.yandex.market.jmf.search.ModuleSearchTestConfiguration;
import ru.yandex.market.ocrm.module.order.ModuleOrderTestConfiguration;

@Import({
        ru.yandex.market.jmf.dataimport.DataImportConfiguration.class,
        ModuleAccountConfiguration.class,
        ModuleOrderTestConfiguration.class,
        ModuleSearchTestConfiguration.class,
        TestDefaultDataSourceConfiguration.class,
})
@ComponentScan("ru.yandex.market.b2bcrm.module.utils")
@PropertySource("classpath:/module_account_test.properties")
public class B2bAccountTestConfig {
}
