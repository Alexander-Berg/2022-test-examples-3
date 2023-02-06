package ru.yandex.market.tsum.pipelines.common.jobs.health;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;


public class AutoGraphConfigFactoryTest {
    static final String AG_CONFIG_EXPECTED_FILE = "expected_ag_config.json";
    static final String AG_CLICKPHITE_EXPECTED_FILE = "expected_ag_clickphite_config.json";

    @Test
    public void verifyNewConfigFormattedWithApplicationInfo() throws IOException {
        HealthMonitoringConfigFactory.ApplicationInfo applicationInfo =
            HealthMonitoringConfigFactoryTest.getApplicationInfo();
        String newVersionContent = AutoGraphConfigFactory.createConfig(
            applicationInfo.getApplicationName() + "_auto_graph",
            applicationInfo.getApplicationName(),
            applicationInfo.getSolomonProjectId(),
            HealthMonitoringConfigFactoryTest.TEST_ABC_GROUP
        );
        HealthMonitoringConfigFactoryTest.assertContentNotContainsPlaceholders(newVersionContent);
        HealthMonitoringConfigFactoryTest.assertContentContainsAbcGroup(newVersionContent);
        HealthMonitoringConfigFactoryTest.assertExpectedContent(newVersionContent, AG_CONFIG_EXPECTED_FILE);
    }

    @Test
    public void verifyClickphiteConfigFormattedWithApplicationInfoAndAbcGroup() throws IOException {
        HealthMonitoringConfigFactory.ApplicationInfo applicationInfo =
            HealthMonitoringConfigFactoryTest.getApplicationInfo();
        String clickphiteId = HealthMonitoringConfigFactoryTest.CLICKPHITE_ID;
        String clickphiteConfig = AutoGraphConfigFactory.createConfigVersion(
            clickphiteId,
            null,
            applicationInfo.getApplicationName(),
            applicationInfo.getSolomonProjectId(),
            HealthMonitoringConfigFactoryTest.TEST_ABC_GROUP,
            HealthMonitoringComponent.CLICKPHITE,
            clickphiteId + "_nginx",
            Collections.emptyMap()
        );
        HealthMonitoringConfigFactoryTest.assertContentNotContainsPlaceholders(clickphiteConfig);
        HealthMonitoringConfigFactoryTest.assertContentContainsApplicationName(clickphiteConfig);
        HealthMonitoringConfigFactoryTest.assertContentContainsProjectNameWithDefaultPrefix(clickphiteConfig);
        HealthMonitoringConfigFactoryTest.assertContentContainsAbcGroup(clickphiteConfig);
        HealthMonitoringConfigFactoryTest.assertExpectedContent(
            clickphiteConfig,
            HealthMonitoringConfigFactoryTest.CLICKPHITE_EXPECTED_FILE
        );
    }

    @Test
    public void verifyClickphiteConfigFormattedWithMapping() throws IOException {
        HealthMonitoringConfigFactory.ApplicationInfo applicationInfo =
            HealthMonitoringConfigFactoryTest.getApplicationInfo();
        String clickphiteConfig = AutoGraphConfigFactory.createConfigVersion(
            applicationInfo.getApplicationName() + "_auto_graph",
            null,
            applicationInfo.getApplicationName(),
            applicationInfo.getSolomonProjectId(),
            HealthMonitoringConfigFactoryTest.TEST_ABC_GROUP,
            HealthMonitoringComponent.CLICKPHITE,
            applicationInfo.getApplicationName() + "_nginx",
            Map.of(
                "host", "new_host",
                "environment", "new_environment",
                "http_code", "new_http_code",
                "page_id", "new_page_id",
                "http_method", "new_http_method",
                "url", "new_url",
                "resptime_ms", "new_resptime_ms"
            )
        );
        HealthMonitoringConfigFactoryTest.assertContentNotContainsPlaceholders(clickphiteConfig);
        HealthMonitoringConfigFactoryTest.assertContentContainsApplicationName(clickphiteConfig);
        HealthMonitoringConfigFactoryTest.assertContentContainsProjectNameWithDefaultPrefix(clickphiteConfig);
        HealthMonitoringConfigFactoryTest.assertContentContainsAbcGroup(clickphiteConfig);
        HealthMonitoringConfigFactoryTest.assertExpectedContent(clickphiteConfig, AG_CLICKPHITE_EXPECTED_FILE);
    }
}
