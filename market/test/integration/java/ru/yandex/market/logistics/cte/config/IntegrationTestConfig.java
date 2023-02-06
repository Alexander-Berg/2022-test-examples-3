package ru.yandex.market.logistics.cte.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@Import({
        PostgreSQLContainerConfig.class,
        DataSourceConfig.class,
        LiquibaseConfig.class,
        DbUnitConfig.class,
        DefaultTmsDataSourceConfig.class,
        JacksonConfiguration.class,
        JpaConfig.class,
        ProtoClientConfiguration.class,
        MockConfiguration.class,
        QueueShardConfiguration.class,
        TestClockConfig.class,
        DbQueueLibraryConfig.class,
        RestResponseEntityExceptionHandler.class,
        CacheConfiguration.class,
        ServiceCenterStatusFlowConfig.class,
        FtTestConfig.class
})
@EnableWebMvc
@PropertySource("classpath:test.properties")
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(basePackages = {
        "ru.yandex.market.logistics.cte.service",
        "ru.yandex.market.logistics.cte.repo",
        "ru.yandex.market.logistics.cte.controller",
        "ru.yandex.market.logistics.cte.converters",
        "ru.yandex.market.logistics.cte.dbqueue.*",
        "ru.yandex.market.logistics.cte.dbqueue.property",
        "ru.yandex.market.logistics.cte.dbqueue.task",
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASPECTJ, pattern = "ru.yandex.market.logistics.cte.service.auth.*"))
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, SecurityAutoConfiguration.class})
public class IntegrationTestConfig {
}
