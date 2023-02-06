package ru.yandex.market.common.logging.logback;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.date.TestableClock;

import static junit.framework.Assert.assertFalse;

@ExtendWith(MockitoExtension.class)
class RotationBasedTriggeringPolicyTest {

    private RotationBasedTriggeringPolicy policy;
    private TestableClock clock;
    private Path filepath;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        clock = new TestableClock();
        policy = new RotationBasedTriggeringPolicy(clock);
        filepath = tempDir.resolve("foo");

        Files.write(filepath, ImmutableList.of("baz"), StandardCharsets.UTF_8);
    }


    @DisplayName("получаем статус из кеша")
    @Test
    void isTriggeringEventCached() throws IOException {
        Instant now = Instant.now();
        clock.setFixed(now, ZoneId.systemDefault());
        File filefake = Mockito.mock(File.class);
        Mockito.when(filefake.toPath()).thenReturn(filepath);

        assertFalse(policy.isTriggeringEvent(filefake, new Object()));
        Files.delete(filepath);
        assertFalse(policy.isTriggeringEvent(filefake, new Object()));
    }

    @DisplayName("получаем статус из кеша - смена файла")
    @Test
    void isTriggeringEventResetOnFileChange() throws IOException {
        Instant now = Instant.now();
        clock.setFixed(now, ZoneId.systemDefault());
        File filefake = Mockito.mock(File.class);
        Mockito.when(filefake.toPath()).thenReturn(filepath);

        Path filepath2 = tempDir.resolve("bar");
        Files.write(filepath2, ImmutableList.of("baz2"), StandardCharsets.UTF_8);
        File filefake2 = Mockito.mock(File.class);
        Mockito.when(filefake2.toPath()).thenReturn(filepath2);

        assertFalse(policy.isTriggeringEvent(filefake, new Object()));
        clock.setFixed(now.plus(policy.getCheckCachePeriod(), ChronoUnit.MILLIS), ZoneId.systemDefault());
        assertFalse(policy.isTriggeringEvent(filefake2, new Object()));
        Files.delete(filepath2);
        assertFalse(policy.isTriggeringEvent(filefake2, new Object()));
    }
}
