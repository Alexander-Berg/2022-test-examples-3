package ru.yandex.market.logistic.gateway.config;

import java.util.Locale;

import javax.annotation.PostConstruct;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres;

@Configuration
@EnableZonkyEmbeddedPostgres
@TestPropertySource({"classpath:application-integration-test.properties"})
@ComponentScan(basePackages = "ru.yandex.market.logistic.gateway")
@Import(value = {DbUnitTestConfiguration.class})
@AutoConfigureDataJpa
public class IntegrationTestConfiguration {

    @Autowired
    private DatabaseConfigBean dbUnitDatabaseConfig;

    @PostConstruct
    private void init() {
        dbUnitDatabaseConfig.setAllowEmptyFields(true);
        dbUnitDatabaseConfig.setEscapePattern("\"?\"");
        Locale.setDefault(Locale.ENGLISH);
    }
}
