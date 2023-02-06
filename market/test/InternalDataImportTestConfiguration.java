package ru.yandex.market.jmf.dataimport.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(DataImportTestConfiguration.class)
public class InternalDataImportTestConfiguration extends AbstractModuleConfiguration {
    protected InternalDataImportTestConfiguration() {
        super("test/jmf/dataimport");
    }
}
