package ru.yandex.market.ff.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.ff.config.AppEventsConfig;
import ru.yandex.market.ff.config.CustomGridValidationRules;
import ru.yandex.market.ff.config.DaasConfig;
import ru.yandex.market.ff.config.HealthConfig;
import ru.yandex.market.ff.config.I18nConfig;
import ru.yandex.market.ff.config.JpaConfig;
import ru.yandex.market.ff.config.JpaReplicaConfig;
import ru.yandex.market.ff.config.LogbrokerObjectMapperConfig;
import ru.yandex.market.ff.config.ParentMvcConfig;
import ru.yandex.market.ff.config.PdfConfig;
import ru.yandex.market.ff.config.PersistenceContextConfig;
import ru.yandex.market.ff.config.ServiceConfiguration;
import ru.yandex.market.ff.config.SpringMvcToTsConfig;
import ru.yandex.market.ff.config.TankerConfig;
import ru.yandex.market.ff.config.TemplateValidationConfiguration;
import ru.yandex.market.ff.config.TicketQueueConfig;
import ru.yandex.market.ff.config.les.LesConfiguration;
import ru.yandex.market.ff.config.metrics.PrometheusConfiguration;

@Configuration
@Import({
        //-Common
        CommonTestConfiguration.class,
        DateTimeTestConfig.class,
        SpringMvcToTsConfig.class,

        //--Database config
        PostgreSQLContainerConfiguration.class,
        JpaConfig.class,
        JpaReplicaConfig.class,
        PersistenceContextConfig.class,
        LiquibaseConfiguration.class,

        //--Service layer config
        ServiceConfiguration.class,
        TemplateValidationConfiguration.class,
        CustomGridValidationRules.class,
        HealthConfig.class,
        I18nConfig.class,
        AppEventsConfig.class,

        //--MVC config
        ParentMvcConfig.class,
        PrimaryConfiguration.class,

        // DaaS service
        DaasConfig.class,

        // Tanker service
        TankerConfig.class,

        // Pdf service
        PdfConfig.class,

        AboIntegrationTestConfiguration.class,

        LogbrokerObjectMapperConfig.class,
        PrometheusConfiguration.class,
        LesConfiguration.class,
        TicketQueueConfig.class,
})
@ComponentScan(basePackages = {"ru.yandex.market.ff.event"})
@ComponentScan({"ru.yandex.market.ff.mvc", })
public class BaseIntegrationTestConfiguration {
}
