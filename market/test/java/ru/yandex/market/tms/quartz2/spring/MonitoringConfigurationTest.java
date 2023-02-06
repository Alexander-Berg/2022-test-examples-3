package ru.yandex.market.tms.quartz2.spring;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.tms.quartz2.model.MonitoringConfigInfo;
import ru.yandex.market.tms.quartz2.model.MonitoringStatus;
import ru.yandex.market.tms.quartz2.service.TmsMonitoringService;
import ru.yandex.market.tms.quartz2.spring.config.MonitoringConfigurationTestConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author otedikova
 */
@SpringJUnitConfig(classes = MonitoringConfigurationTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MonitoringConfigurationTest {
    @Autowired
    protected TmsMonitoringService tmsMonitoringService;

    @Test
    void checkExecutorWithFullMonitoringConfig() {
        MonitoringConfigInfo expected = new MonitoringConfigInfo("executorWithFullConfig",
                5,
                3,
                "SHOPS",
                3600000,
                600000,
                MonitoringStatus.CRIT,
                MonitoringStatus.CRIT,
                Arrays.asList("no_wildcard", " check_space", "mu%ple", "", "s_ngle"),
                false,
                false,
                "");
        checkConfiguration(expected);
    }

    @Test
    void checkExecutorWithPartialMonitoringConfig() {
        MonitoringConfigInfo expected = new MonitoringConfigInfo("executorWithPartialConfig",
                5,
                1,
                "",
                3600000,
                600000,
                MonitoringStatus.CRIT,
                MonitoringStatus.CRIT,
                Arrays.asList("no_wildcard", " check_space", "mu%ple", "", "s_ngle"),
                false,
                false,
                "");
        checkConfiguration(expected);
    }

    @Test
    void checkExecutorWithoutMonitoringConfig() {
        MonitoringConfigInfo expected = new MonitoringConfigInfo("executorWithoutConfig",
                1,
                1,
                "",
                -1,
                -1,
                MonitoringStatus.CRIT,
                MonitoringStatus.CRIT,
                Arrays.asList("no_wildcard", " check_space", "mu%ple", "", "s_ngle"),
                false,
                false,
                "");
        checkConfiguration(expected);
    }

    @Test
    void checkExecutorWithJobStatusesToSkipConfig() {
        MonitoringConfigInfo expected = new MonitoringConfigInfo("executorWithJobStatusesToSkipConfig",
                1,
                1,
                "",
                -1,
                -1,
                MonitoringStatus.CRIT,
                MonitoringStatus.CRIT,
                Arrays.asList("status1", "status2"),
                false,
                false,
                "");
        checkConfiguration(expected);
    }

    @Test
    void checkThrowExceptionIfJobIsToSkipButSkipHealthCheckReasonIsNotSet() {
        assertThrows(IllegalStateException.class, () -> {
                    new MonitoringConfigInfo("executorWithJobStatusesToSkipConfig",
                            1,
                            1,
                            "",
                            -1,
                            -1,
                            MonitoringStatus.CRIT,
                            MonitoringStatus.CRIT,
                            Collections.emptyList(),
                            false,
                            true,
                            null);
                },
                "Expected to throw IllegalStateException, but it didn't");
    }

    @Test
    void checkThrowExceptionIfJobIsToSkipButSkipHealthCheckReasonIsEmpty() {
        assertThrows(IllegalStateException.class, () -> {
                    new MonitoringConfigInfo("executorWithJobStatusesToSkipConfig",
                            1,
                            1,
                            "",
                            -1,
                            -1,
                            MonitoringStatus.CRIT,
                            MonitoringStatus.CRIT,
                            Collections.emptyList(),
                            false,
                            true,
                            "");
                },
                "Expected to throw IllegalStateException, but it didn't");
    }

    @Test
    void checkNotExecutor() {
        MonitoringConfigInfo monitoringConfigInfo =
                tmsMonitoringService.getMonitoringConfig("notExecutor");
        assertNull(monitoringConfigInfo);
    }

    void checkConfiguration(MonitoringConfigInfo expected) {
        MonitoringConfigInfo actual =
                tmsMonitoringService.getMonitoringConfig(expected.getJobName());
        assertNotNull(actual);
        assertEquals(expected.getFailsToCrit(), actual.getFailsToCrit());
        assertEquals(expected.getFailsToWarn(), actual.getFailsToWarn());
        assertEquals(expected.getResponsibleTeam(), actual.getResponsibleTeam());
        assertEquals(expected.getMaxDelayTimeMillis(), actual.getMaxDelayTimeMillis());
        assertEquals(expected.getMaxExecutionTimeMillis(), actual.getMaxExecutionTimeMillis());
        assertEquals(expected.getOnNeverBeenFinishedYetStatus(), actual.getOnNeverBeenFinishedYetStatus());
        assertEquals(expected.getOnNotStartedYetStatus(), actual.getOnNotStartedYetStatus());
        assertEquals(expected.getJobStatusesToSkip(), actual.getJobStatusesToSkip());
        assertEquals(expected.isSkipEmptyJobStatuses(), actual.isSkipEmptyJobStatuses());
    }
}
