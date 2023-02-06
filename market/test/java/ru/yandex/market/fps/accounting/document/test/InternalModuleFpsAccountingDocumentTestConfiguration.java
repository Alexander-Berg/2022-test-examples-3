package ru.yandex.market.fps.accounting.document.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(ModuleFpsAccountingDocumentTestConfiguration.class)
public class InternalModuleFpsAccountingDocumentTestConfiguration extends AbstractModuleConfiguration {
    protected InternalModuleFpsAccountingDocumentTestConfiguration() {
        super("module/fps/accountingDocument/test");
    }
}
