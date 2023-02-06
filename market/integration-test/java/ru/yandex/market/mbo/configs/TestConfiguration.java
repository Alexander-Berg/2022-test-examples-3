package ru.yandex.market.mbo.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import ru.yandex.market.mbo.configs.audit.OracleAuditConfig;
import ru.yandex.market.mbo.configs.billing.PersonalBillingReportConfig;
import ru.yandex.market.mbo.configs.category.CategoryMappingConfig;
import ru.yandex.market.mbo.configs.db.MeasureServicesConfig;
import ru.yandex.market.mbo.configs.db.parameter.ParameterLoaderServiceDAOConfig;
import ru.yandex.market.mbo.configs.db.recemmendation.RecommendationServiceDAOConfig;
import ru.yandex.market.mbo.configs.initializers.EtcdTnsnamesPostProcessor;
import ru.yandex.market.mbo.core.dashboard.DashboardClickHouseConfig;
import ru.yandex.market.mbo.core.metrics.MetricsClickHouseConfig;

/**
 * Джава конфигурация аналогичная {@code mbo-core/test-config.xml}.
 *
 * @author s-ermakov
 */
@Configuration
@ImportResource("classpath:mbo-core/test-config.xml")
@Import({
    ParameterLoaderServiceDAOConfig.class,
    AutoUserConfig.class,
    DashboardClickHouseConfig.class,
    TestPropertiesConfiguration.class,
    RecommendationServiceDAOConfig.class,
    MeasureServicesConfig.class,
    OracleAuditConfig.class,
    PersonalBillingReportConfig.class,
    OperatorTaskListConfig.class,
    CategoryMappingConfig.class,
    MetricsClickHouseConfig.class,
    MboCoreAopConfig.class
})
public class TestConfiguration {

    @Bean
    public EtcdTnsnamesPostProcessor etcdTnsnamesPostProcessor() {
        return new EtcdTnsnamesPostProcessor();
    }
}
