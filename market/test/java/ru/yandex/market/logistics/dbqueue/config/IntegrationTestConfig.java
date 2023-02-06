package ru.yandex.market.logistics.dbqueue.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.LiquibaseTestConfiguration;

@Configuration
@Import({
        TestDataSourceConfiguration.class,
        LiquibaseTestConfiguration.class,
        DbUnitTestConfiguration.class,
        DbQueueAppInfoConfig.class,
        LibrarySetupConfig.class,
        DataBaseConnectionConfig.class,
        ObjectMapperConfig.class
})
@EnableWebMvc
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan({
        "ru.yandex.market.logistics.dbqueue.*"
})

@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class})
public class IntegrationTestConfig {
}
