package ru.yandex.market.logistics.logging.appender.log4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.date.TestableClock;

@ExtendWith(MockitoExtension.class)
class RotatableRollingFileAppenderTest {
    private RollingFileAppender appender;
    private TestableClock clock;
    private int cacheInt = 1000;
    @TempDir
    Path tempDir;

    @DisplayName("Проверка интеграции стратегии и тригера")
    @Test
    void test() {
        clock = new TestableClock();
        Instant now = Instant.now();
        clock.setFixed(now, ZoneId.systemDefault());

        Path logFile = tempDir.resolve("foo.log");

        appender = RollingFileAppender.newBuilder()
            .withStrategy(NoopRollingStrategy.newBuilder().build())
            .withPolicy(
                RotationBasedTriggeringPolicy
                .newBuilder()
                .withCheckCachePeriod(cacheInt)
                .withClock(clock)
                .build()
            )
            .setName("test")
            .withFilePattern("test")
            .withFileName(logFile.toString())
            .build();

        appender.initialize();
        appender.start();

        LogEvent fakeEvent = Mockito.mock(LogEvent.class);
        appender.append(fakeEvent);
    }

    @Test
    @DisplayName("Продолжаем писать в старый файл между интервалами")
    void test2() throws IOException {
        clock = new TestableClock();
        Instant now = Instant.now();
        clock.setFixed(now, ZoneId.systemDefault());

        Path logFile = tempDir.resolve("foo.log");
        Path logFile2 = tempDir.resolve("foo2.log");

        appender = RollingFileAppender.newBuilder()
            .withStrategy(NoopRollingStrategy.newBuilder().build())
            .withPolicy(
                RotationBasedTriggeringPolicy
                    .newBuilder()
                    .withCheckCachePeriod(cacheInt)
                    .withClock(clock)
                    .build()
            )
            .setName("test")
            .withFilePattern("test")
            .withFileName(logFile.toString())
            .build();

        appender.initialize();
        appender.start();

        LogEvent event1 = Log4jLogEvent.newBuilder().setMessage(new SimpleMessage("test")).build();
        appender.append(event1);
        long size1 = Files.size(logFile);
        Files.move(logFile, logFile2);
        appender.append(event1);
        long size2 = Files.size(logFile2);
        Assertions.assertThat(size1).isLessThan(size2);
    }

    @Test
    @DisplayName("Создали файл после интервала")
    void test3() throws IOException {
        clock = new TestableClock();
        Instant now = Instant.now();
        clock.setFixed(now, ZoneId.systemDefault());

        Path logFile = tempDir.resolve("foo.log");
        Path logFile2 = tempDir.resolve("foo2.log");

        appender = RollingFileAppender.newBuilder()
            .withStrategy(NoopRollingStrategy.newBuilder().build())
            .withPolicy(
                RotationBasedTriggeringPolicy
                    .newBuilder()
                    .withCheckCachePeriod(cacheInt)
                    .withClock(clock)
                    .build()
            )
            .setName("test")
            .withFilePattern("test")
            .withFileName(logFile.toString())
            .build();

        appender.initialize();
        appender.start();

        LogEvent event1 = Log4jLogEvent.newBuilder().setMessage(new SimpleMessage("test")).build();
        appender.append(event1);
        Files.move(logFile, logFile2);
        clock.setFixed(now.plus(cacheInt, ChronoUnit.MILLIS), ZoneId.systemDefault());
        appender.append(event1);
        Assertions.assertThat(Files.exists(logFile));
    }
}
