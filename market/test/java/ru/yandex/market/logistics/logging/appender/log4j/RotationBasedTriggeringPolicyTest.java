package ru.yandex.market.logistics.logging.appender.log4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;

import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.date.TestableClock;

import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class RotationBasedTriggeringPolicyTest {
    private RotationBasedTriggeringPolicy policy;
    private TestableClock clock;
    private Path filepath;
    @TempDir
    Path tempDir;

    private FileSystem fs;

    @BeforeEach
    void setUp() throws IOException {
        clock = new TestableClock();
        policy = new RotationBasedTriggeringPolicy(clock, 1000);

        filepath = tempDir.resolve("foo");

        Files.write(filepath, ImmutableList.of("baz"), StandardCharsets.UTF_8);

        RollingFileManager fakeManager = Mockito.mock(RollingFileManager.class);
        Mockito.when(fakeManager.getFileName()).thenReturn(filepath.toString());

        policy.initialize(fakeManager);
        policy.setFilePath(filepath);
    }

    @DisplayName("получаем статус из кеша")
    @Test
    void isTriggeringEventCached() throws IOException {
        Instant now = Instant.now();
        clock.setFixed(now, ZoneId.systemDefault());

        LogEvent fakeEvent = Mockito.mock(LogEvent.class);
        assertFalse(policy.isTriggeringEvent(fakeEvent));
        Files.delete(filepath);
        assertFalse(policy.isTriggeringEvent(fakeEvent));
    }
}
