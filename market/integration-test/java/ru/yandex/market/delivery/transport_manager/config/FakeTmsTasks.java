package ru.yandex.market.delivery.transport_manager.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.delivery.transport_manager.config.tms.TmsConfig;
import ru.yandex.market.delivery.transport_manager.task.TmsTimer;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;
import ru.yandex.market.tms.quartz2.spring.MonitoringConfig;

@Slf4j
@Configuration
@Import(TmsConfig.class)
@RequiredArgsConstructor
public class FakeTmsTasks {

    @Bean
    @CronTrigger(
        cronExpression = "0 0 19 * * ?",
        description = "Тестовый крон"
    )
    @MonitoringConfig(
        maxDelayTimeMillis = TmsTimer.DAYS_1,
        maxExecutionTimeMillis = TmsTimer.SECONDS_30
    )
    public Executor delayedJob() {
        return context -> {

        };
    }

    @Bean
    @CronTrigger(
        cronExpression = "0 0 19 * * ?",
        description = "Тестовый крон"
    )
    public Executor normalJob() {
        return context -> {

        };
    }

    @Bean
    @CronTrigger(
        cronExpression = "0 0 19 * * ?",
        description = "Тестовый крон"
    )
    public Executor failedJob() {
        return context -> {

        };
    }

    @Bean
    @CronTrigger(
        cronExpression = "0 0 19 * * ?",
        description = "Тестовый крон"
    )
    @MonitoringConfig(
        maxExecutionTimeMillis = TmsTimer.SECONDS_30
    )
    public Executor overdueJob() {
        return context -> {

        };
    }
}
