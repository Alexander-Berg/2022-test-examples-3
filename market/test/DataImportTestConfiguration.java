package ru.yandex.market.jmf.dataimport.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.jmf.dataimport.DataImportConfiguration;
import ru.yandex.market.jmf.module.def.test.ModuleDefaultTestConfiguration;

@Configuration
@Import({
        DataImportConfiguration.class,
        ModuleDefaultTestConfiguration.class,
})
@PropertySource(name = "do_sync_data_import", value = "classpath:/do_sync_data_import.properties")
public class DataImportTestConfiguration {
}
