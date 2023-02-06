package ru.yandex.market.tms.quartz2.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.tms.quartz2.service.TmsMonitoringService;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;
import ru.yandex.market.tms.quartz2.spring.MonitoringConfig;

/**
 * @author otedikova
 */
@Import(JdbcTestConfig.class)
@Configuration
public class MonitoringConfigurationTestConfig {
    @Autowired
    protected TmsMonitoringService tmsMonitoringService;

    @Bean
    @CronTrigger(
            cronExpression = "0 0 12 1 * ? 2042",
            description = "Executor with full monitoring config"
    )
    @MonitoringConfig(
            failsToCritCount = 5,
            failsToWarnCount = 3,
            responsibleTeam = "SHOPS",
            maxDelayTimeMillis = 3600000,
            maxExecutionTimeMillis = 600000
    )
    public Executor executorWithFullConfig() {
        return context -> {
        };
    }

    @Bean
    @CronTrigger(
            cronExpression = "0 0 12 1 * ? 2042",
            description = "Executor with partial monitoring config"
    )
    @MonitoringConfig(
            failsToCritCount = 5,
            maxDelayTimeMillis = 3600000,
            maxExecutionTimeMillis = 600000
    )
    public Executor executorWithPartialConfig() {
        return context -> {
        };
    }

    @Bean
    @CronTrigger(
            cronExpression = "0 0 12 1 * ? 2042",
            description = "Executor without monitoring config"
    )
    public Executor executorWithoutConfig() {
        return context -> {
        };
    }

    @Bean
    @MonitoringConfig(
            failsToCritCount = 5,
            maxDelayTimeMillis = 3600000,
            maxExecutionTimeMillis = 600000
    )
    public Executor notExecutor() {
        return context -> {
        };
    }

    @Bean
    @CronTrigger(
            cronExpression = "0 0 12 1 * ? 2042",
            description = "Executor with full monitoring config"
    )
    @MonitoringConfig(
            jobStatusesToSkip = {"status1", "status2"}
    )
    public Executor executorWithJobStatusesToSkipConfig() {
        return context -> {
        };
    }

    @Bean
    @CronTrigger(
            cronExpression = "0 0 12 1 * ? 2042",
            description = "Executor with custom monitoring statuses"
    )
    @MonitoringConfig(
            onNotStartedYetStatus = "WARN",
            onNeverBeenFinishedYetStatus = "OK"
    )
    public Executor executorWithCustomMonitoringStatuses() {
        return context -> {
        };
    }
}
