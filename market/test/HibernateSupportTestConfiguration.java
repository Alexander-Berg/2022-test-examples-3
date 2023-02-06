package ru.yandex.market.jmf.hibernate.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.db.test.TestDefaultDataSourceConfiguration;
import ru.yandex.market.jmf.hibernate.HibernateSupportConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import({
        HibernateSupportConfiguration.class,
        TestDefaultDataSourceConfiguration.class
})
public class HibernateSupportTestConfiguration extends AbstractModuleConfiguration {
    protected HibernateSupportTestConfiguration() {
        super("hibernate/support/test");
    }
}
