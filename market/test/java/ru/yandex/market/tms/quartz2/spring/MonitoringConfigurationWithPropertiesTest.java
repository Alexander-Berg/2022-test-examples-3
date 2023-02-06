package ru.yandex.market.tms.quartz2.spring;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.tms.quartz2.model.MonitoringConfigInfo;
import ru.yandex.market.tms.quartz2.model.MonitoringStatus;
import ru.yandex.market.tms.quartz2.spring.config.MonitoringConfigurationTestConfig;

/**
 * @author otedikova
 */
@SpringJUnitConfig(classes = MonitoringConfigurationWithPropertiesTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MonitoringConfigurationWithPropertiesTest extends MonitoringConfigurationTest {
    @Test
    void checkExecutorWithFullMonitoringConfig() {
        MonitoringConfigInfo expected = new MonitoringConfigInfo("executorWithFullConfig",
                5,
                3,
                "SHOPS",
                3600000,
                600000,
                MonitoringStatus.OK,
                MonitoringStatus.WARN,
                Arrays.asList("property test", "some status"),
                false,
                false,
                "");
        checkConfiguration(expected);
    }

    @Test
    void checkExecutorWithPartialMonitoringConfig() {
        MonitoringConfigInfo expected = new MonitoringConfigInfo("executorWithPartialConfig",
                5,
                6,
                "DEFAULT_TEAM",
                3600000,
                600000,
                MonitoringStatus.OK,
                MonitoringStatus.WARN,
                Arrays.asList("property test", "some status"),
                false,
                false,
                "");
        checkConfiguration(expected);
    }

    @Test
    void checkExecutorWithoutMonitoringConfig() {
        MonitoringConfigInfo expected = new MonitoringConfigInfo("executorWithoutConfig",
                9,
                6,
                "DEFAULT_TEAM",
                123456000,
                234567000,
                MonitoringStatus.OK,
                MonitoringStatus.WARN,
                Arrays.asList("property test", "some status"),
                false,
                false,
                "");
        checkConfiguration(expected);
    }

    @Test
    void checkExecutorWithJobStatusesToSkipConfig() {
        MonitoringConfigInfo expected = new MonitoringConfigInfo("executorWithJobStatusesToSkipConfig",
                9,
                6,
                "DEFAULT_TEAM",
                123456000,
                234567000,
                MonitoringStatus.OK,
                MonitoringStatus.WARN,
                Arrays.asList("status1", "status2"),
                false,
                false,
                "");
        checkConfiguration(expected);
    }

    @Test
    void checkExecutorWithCustomMonitoringStatuses() {
        MonitoringConfigInfo expected = new MonitoringConfigInfo("executorWithCustomMonitoringStatuses",
                9,
                6,
                "DEFAULT_TEAM",
                123456000,
                234567000,
                MonitoringStatus.WARN,
                MonitoringStatus.OK,
                Arrays.asList("property test", "some status"),
                false,
                false,
                "");
        checkConfiguration(expected);
    }

    @Configuration
    @PropertySource("classpath:/ru/yandex/market/tms/quartz2/spring/tms-monitoring.properties")
    @Import({
            MonitoringConfigurationTestConfig.class
    })
    public static class Config {
    }
}
