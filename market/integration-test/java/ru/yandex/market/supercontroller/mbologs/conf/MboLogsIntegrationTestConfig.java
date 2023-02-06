package ru.yandex.market.supercontroller.mbologs.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.yandex.market.mbo.core.dashboard.DashboardClickHouseConfig;

@Configuration
@Import({
    DashboardClickHouseConfig.class,
    MboLogsIntegrationPropertiesConfig.class,
    MboLogsIntegrationTestXmlConfig.class,
})
public class MboLogsIntegrationTestConfig {
}
