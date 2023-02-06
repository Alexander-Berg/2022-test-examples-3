package ru.yandex.market.logistics.logging.appender;

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
import org.junit.jupiter.api.io.TempDir;

import ru.yandex.common.util.date.TestableClock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileCheckerCachedTest {
    private Path filepath;
    private FileCheckerCached fileCheckerCached;
    private TestableClock clock;
    private final long cacheInt = 1000;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        filepath = tempDir.resolve("foo");

        Files.write(filepath, ImmutableList.of("baz"), StandardCharsets.UTF_8);

        clock = new TestableClock();

        fileCheckerCached = new FileCheckerCached(clock, cacheInt, filepath);
    }

    @DisplayName("fileExists = false если файл отсутствует")
    @Test
    void checkAbsent() throws IOException {
        Files.delete(filepath);
        assertFalse(fileCheckerCached.fileExists());
    }

    @DisplayName("fileExists = true если файл существует")
    @Test
    void checkExists() {
        assertTrue(fileCheckerCached.fileExists());
    }

    @Test
    @DisplayName("Проверяем что к файлу не обращались между интервалами кеша")
    void checkFileNotAccessed() throws IOException {
        assertTrue(fileCheckerCached.fileExists());
        Files.delete(filepath);
        assertTrue(fileCheckerCached.fileExists());
    }

    @Test
    @DisplayName("Проверяем сброс кеша через заданный интервал")
    void checkFileAccessed() throws IOException {
        Instant now = Instant.now();
        clock.setFixed(now, ZoneId.systemDefault());
        assertTrue(fileCheckerCached.fileExists());
        Files.delete(filepath);
        clock.setFixed(now.plus(cacheInt, ChronoUnit.MILLIS), ZoneId.systemDefault());
        assertFalse(fileCheckerCached.fileExists());
    }
}
