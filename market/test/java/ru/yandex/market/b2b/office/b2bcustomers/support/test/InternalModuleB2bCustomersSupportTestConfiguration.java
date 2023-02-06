package ru.yandex.market.b2b.office.b2bcustomers.support.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(ModuleB2bCustomersSupportTestConfiguration.class)
public class InternalModuleB2bCustomersSupportTestConfiguration extends AbstractModuleConfiguration {
    protected InternalModuleB2bCustomersSupportTestConfiguration() {
        super("b2b/office/b2bcustomerssupport/test");
    }
}
