package ru.yandex.market.jmf.db.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.google.common.io.Resources;
import com.netflix.hystrix.util.Exceptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ru.yandex.market.jmf.db.api.test.DbApiTestConfiguration;
import ru.yandex.market.jmf.db.hibernate.HibernateConfiguration;
import ru.yandex.market.jmf.hibernate.HibernateSupportConfiguration;
import ru.yandex.market.jmf.security.test.SecurityTestConfiguration;

@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
@Import({
        HibernateConfiguration.class,
        DbApiTestConfiguration.class,
        SecurityTestConfiguration.class
})
@PropertySource(name = "testHibernateProperties", value = "classpath:/hibernate_test.properties")
public class HibernateTestConfiguration {

    @Bean(name = HibernateSupportConfiguration.PROPERTIES)
    public Properties properties() {
        Properties properties = new Properties();
        try (InputStream inputStream = Resources.asByteSource(Resources.getResource("hibernate_test.properties"))
                .openStream()) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw Exceptions.sneakyThrow(e);
        }
        return properties;
    }

}
