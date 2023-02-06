package ru.yandex.market.pharmatestshop.config;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;
import ru.yandex.market.tms.quartz2.spring.MonitoringConfig;
import ru.yandex.market.tms.quartz2.util.QrtzLogTableCleaner;

@Configuration
    @RequiredArgsConstructor
    @Import(QuartzConfiguration.class)
    @ComponentScan("ru.yandex.market.pharmatestshop.executor")
    public class QuartzTasksConfiguration {

        @Bean
        @CronTrigger(
                cronExpression = "0 0 4 1/1 * ?",
                description = "Задача очистки TMS логов в базе данных"
        )
        @MonitoringConfig(maxDelayTimeMillis = 90_000_000) // > 1 day
        public Executor tmsLogsCleanupExecutor(QrtzLogTableCleaner qrtzLogTableCleaner) {
            return context -> qrtzLogTableCleaner.clean();
        }

    }
