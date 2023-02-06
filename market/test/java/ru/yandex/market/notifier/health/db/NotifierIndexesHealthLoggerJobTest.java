package ru.yandex.market.notifier.health.db;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.jobs.db.maintenance.NotifierIndexesHealthLoggerJob;

import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.notifier.util.TestAppender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NotifierIndexesHealthLoggerJobTest extends AbstractServicesTestBase {

    @Autowired
    private NotifierIndexesHealthLoggerJob indexesHealthLoggerTmsJob;

    private final TestAppender appender = new TestAppender();

    @BeforeEach
    public void setup() {
        Logger logger = ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("key-value.log"));
        logger.addAppender(appender);
    }

    @AfterEach
    public void tearDown() {
        Logger logger = ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("key-value.log"));
        logger.detachAppender(appender);
    }

    @Test
    public void shouldExecuteCorrectly() {
        // Act
        indexesHealthLoggerTmsJob.doJob(null);

        // Assert
        final List<String> healthMessages = appender.getLog().stream()
                .map(ILoggingEvent::getMessage)
                .filter(m -> m.contains("db_indexes_health"))
                .collect(Collectors.toList());
        assertEquals(10, healthMessages.size());

        List<String> messages = healthMessages.stream()
                .filter(m -> m.contains("invalid_indexes"))
                .collect(Collectors.toList());
        assertEquals(1, messages.size());
        String message = messages.get(0);
        assertTrue(message.contains("invalid_indexes\t0.0"), message);

        messages = healthMessages.stream()
                .filter(m -> m.contains("duplicated_indexes"))
                .collect(Collectors.toList());
        assertEquals(1, messages.size());
        message = messages.get(0);
        assertTrue(message.contains("duplicated_indexes\t0.0"), message);

        messages = healthMessages.stream()
                .filter(m -> m.contains("intersected_indexes"))
                .collect(Collectors.toList());
        assertEquals(1, messages.size());
        message = messages.get(0);
        assertTrue(message.contains("intersected_indexes\t0.0"), message);

        messages = healthMessages.stream()
                .filter(m -> m.contains("unused_indexes"))
                .collect(Collectors.toList());
        assertEquals(1, messages.size());
        message = messages.get(0);
        // В тестах не все запросы вызываются, соответственно не все индексы используются
        assertTrue(message.contains("unused_indexes\t"), message);

        messages = healthMessages.stream()
                .filter(m -> m.contains("foreign_keys_without_index"))
                .collect(Collectors.toList());
        assertEquals(1, messages.size());
        message = messages.get(0);
        assertTrue(message.contains("foreign_keys_without_index\t0.0"), message);

        messages = healthMessages.stream()
                .filter(m -> m.contains("tables_with_missing_indexes"))
                .collect(Collectors.toList());
        assertEquals(1, messages.size());
        message = messages.get(0);
        // Вообще неясно, как используются индексы в тестах, поэтому конкретное значение не проверяем
        assertTrue(message.contains("tables_with_missing_indexes\t"), message);

        messages = healthMessages.stream()
                .filter(m -> m.contains("tables_without_primary_key"))
                .collect(Collectors.toList());
        assertEquals(1, messages.size());
        message = messages.get(0);
        assertTrue(message.contains("tables_without_primary_key\t0.0"), message);

        messages = healthMessages.stream()
                .filter(m -> m.contains("indexes_with_null_values"))
                .collect(Collectors.toList());
        assertEquals(1, messages.size());
        message = messages.get(0);
        assertTrue(message.contains("indexes_with_null_values\t0.0"), message);

        messages = healthMessages.stream()
                .filter(m -> m.contains("indexes_bloat"))
                .collect(Collectors.toList());
        assertEquals(1, messages.size());
        message = messages.get(0);
        assertTrue(message.contains("indexes_bloat\t0.0"), message);

        messages = healthMessages.stream()
                .filter(m -> m.contains("tables_bloat"))
                .collect(Collectors.toList());
        assertEquals(1, messages.size());
        message = messages.get(0);
        assertTrue(message.contains("tables_bloat\t0.0"), message);
    }
}
