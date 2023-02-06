package ru.yandex.market.delivery.mdbapp.components.logging.json;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.stream.Collectors;

import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public abstract class AbstractJsonLoggerTest<X, T extends AbstractJsonLogger<X>> {
    private static final LoggerContext LOGGER_CONTEXT = (LoggerContext) LoggerFactory.getILoggerFactory();
    private static final File LOG_PATH = new File(LOGGER_CONTEXT.getProperty("LOG_PATH"));

    protected final T logger = createLogger();
    private final File logFile = FileUtils.getFile(LOG_PATH.getAbsoluteFile(), getFilePath());
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    private final Class<X> recordClass;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    protected abstract String[] getFilePath();

    @Before
    public void setUp() throws Exception {
        logFile.delete();
    }

    @After
    public void tearDown() {
        LOGGER_CONTEXT.reset();
    }

    @Test
    public void doTest() throws IOException {
        List<X> data = recordForTest();
        data.forEach(logger::logRecord);

        try (Reader r = new FileReader(logFile)) {
            List<String> lines = IOUtils.readLines(r);
            softly.assertThat(lines.size()).isEqualTo(data.size());
            List<X> actualData = lines.stream()
                .map(this::parseMessage)
                .collect(Collectors.toList());
            softly.assertThat(actualData).isEqualTo(data);
        }
    }

    @SneakyThrows
    private X parseMessage(String line) {
        return objectMapper.readValue(line, recordClass);
    }

    protected abstract T createLogger();

    protected abstract List<X> recordForTest();
}
