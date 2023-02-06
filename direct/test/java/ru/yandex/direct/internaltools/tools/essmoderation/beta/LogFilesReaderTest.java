package ru.yandex.direct.internaltools.tools.essmoderation.beta;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.common.log.container.ModerationLogEntry;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.common.log.LogHelper.DATE_TIME_FORMATTER;

@ParametersAreNonnullByDefault
public class LogFilesReaderTest {
    private Path appDir;
    private Path logFile;

    @Before
    public void setUp() throws URISyntaxException {
        appDir = Paths.get(Resources.getResource("").toURI());
        logFile = appDir.resolve("logs/ess_moderation.log.20201008");
    }

    @Test
    public void readLogFile() throws URISyntaxException, IOException {
        LocalDateTime startTime = LocalDateTime.parse("2020-10-08 12:00:00", DATE_TIME_FORMATTER);
        List<ModerationLogEntry<ModerationLogEntryData>> logLines = LogFilesReader.readLogFile(logFile, startTime);

        // должны прочитать все логи за 08.10.2020
        assertThat(logLines, hasSize(3));
    }

    @Test
    public void readLogFile_partial() throws URISyntaxException, IOException {
        LocalDateTime startTime = LocalDateTime.parse("2020-10-08 17:15:00", DATE_TIME_FORMATTER);
        List<ModerationLogEntry<ModerationLogEntryData>> logLines = LogFilesReader.readLogFile(logFile, startTime);

        // должны прочитать часть лога за 08.10.2020 - только записи после 17:15:00
        assertThat(logLines, hasSize(2));
    }

    @Test
    public void readEntries() throws IOException {
        var logReader = new LogFilesReader(appDir, "ess_moderation");

        LocalDateTime time = LocalDateTime.parse("2020-10-07 17:00:00", DATE_TIME_FORMATTER);
        List<ModerationLogEntry<ModerationLogEntryData>> logLines = logReader.readEntries(time);

        // Должны прочитать часть лога за 07.10.2020 (после 17:00:00) и весь лог за 08.10.2020.
        // Файл за 06.10.2020 даже не читаем (в нем кривой контент).
        assertThat(logLines, hasSize(4));
    }

}
