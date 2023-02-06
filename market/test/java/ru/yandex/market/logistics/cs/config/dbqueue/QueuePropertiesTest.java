package ru.yandex.market.logistics.cs.config.dbqueue;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.logistics.cs.config.dbqueue.QueuePropertiesTest.PropertiesConfig;
import ru.yandex.money.common.dbqueue.settings.ProcessingMode;
import ru.yandex.money.common.dbqueue.settings.QueueSettings;
import ru.yandex.money.common.dbqueue.settings.TaskRetryType;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PropertiesConfig.class)
class QueuePropertiesTest {
    @Autowired
    private QueueProperties overriddenQueueProperties;
    @Autowired
    private QueueProperties defaultQueueProperties;

    @Test
    void queueSettingsOverriding() {
        QueueSettings queueSettings = overriddenQueueProperties.buildSettings();

        assertEquals(100L, queueSettings.getBetweenTaskTimeout().toMillis());
        assertEquals(200L, queueSettings.getNoTaskTimeout().toMillis());
        assertEquals(300L, queueSettings.getFatalCrashTimeout().toMillis());
        assertEquals(400L, queueSettings.getRetryInterval().toMillis());
        assertEquals(TaskRetryType.LINEAR_BACKOFF, queueSettings.getRetryType());
        assertEquals(500L, queueSettings.getThreadCount());
        assertEquals(ProcessingMode.USE_EXTERNAL_EXECUTOR, queueSettings.getProcessingMode());
    }

    @Test
    void queueSettingsDefaultValuesPerseverance() {
        QueueProperties defaultProperties = new QueueProperties();
        QueueSettings expectedSettings = QueueSettings.builder()
            .withBetweenTaskTimeout(Duration.ofMillis(defaultProperties.getBetweenTaskTimeoutMillis()))
            .withNoTaskTimeout(Duration.ofMillis(defaultProperties.getNoTaskTimeoutMillis()))
            .build();

        assertEquals(expectedSettings, defaultQueueProperties.buildSettings());
    }

    @Configuration
    @EnableConfigurationProperties
    @PropertySource("classpath:/spring/queue-definition.properties")
    public static class PropertiesConfig {

        @Bean
        @ConfigurationProperties(prefix = "dbqueue.queue.overridden-queue")
        public QueueProperties overriddenQueueProperties() {
            return new QueueProperties();
        }

        @Bean
        @ConfigurationProperties(prefix = "dbqueue.queue.default-queue")
        public QueueProperties defaultQueueProperties() {
            return new QueueProperties();
        }
    }
}
