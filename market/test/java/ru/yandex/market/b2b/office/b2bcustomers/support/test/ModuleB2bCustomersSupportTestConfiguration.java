package ru.yandex.market.b2b.office.b2bcustomers.support.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.b2b.office.b2bcustomers.support.B2bCustomersClient;
import ru.yandex.market.b2b.office.b2bcustomers.support.ModuleB2bCustomersSupportConfiguration;
import ru.yandex.market.jmf.utils.UtilsTestConfiguration;

@Configuration
@ComponentScan("ru.yandex.market.b2b.office.b2bcustomers.support.test.impl")
@Import({
        ModuleB2bCustomersSupportConfiguration.class,
        UtilsTestConfiguration.class,
})
public class ModuleB2bCustomersSupportTestConfiguration {
    @Bean
    public B2bCustomersClient mockB2bCustomersClient() {
        return Mockito.mock(B2bCustomersClient.class);
    }
}
