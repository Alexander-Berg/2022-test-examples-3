package ru.yandex.market.deepmind.common.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.deepmind.common.TestYqlConfig;
import ru.yandex.market.yql_query_service.config.QueryServiceConfiguration;
import ru.yandex.market.yql_test.YqlTestConfiguration;

@TestConfiguration
@Import({
    PostgresSQLBeanPostProcessor.class,
    TestDeepmindSqlDatasourceConfig.class,
    TestRemoteServicesConfig.class,
    TestMbocAdapterConfig.class,
    TestAuditConfig.class,
    TestYqlConfig.class,
    TestYqlOverPgDatasourceConfig.class,
    DeepmindRepositoryConfig.class,
    DbMonitoringConfig.class,
    TrackerApproverRepositoryConfig.class,
    QueryServiceConfiguration.class,
    YqlTestConfiguration.class,
    AvailabilityTaskQueueConfig.class,
    DeepmindKeyValueConfig.class
})
@PropertySource(
    value = {
        "classpath:test-common.properties",
        "classpath:test-app.properties",
        "classpath:test-tms.properties"
    },
    ignoreResourceNotFound = true
)
public class DeepmindDbTestConfiguration {
}
