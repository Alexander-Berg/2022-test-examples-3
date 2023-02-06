package ru.yandex.market.logshatter.config;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.devtools.test.Paths;
import ru.yandex.market.logshatter.parser.LogParser;
import ru.yandex.market.logshatter.parser.ParserContext;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logshatter.config.ConfigurationService.getConfigObject;
import static ru.yandex.market.logshatter.config.ConfigurationService.getParserProvider;
import static ru.yandex.market.logshatter.config.ConfigurationService.readParams;

@RunWith(Parameterized.class)
public class ExampleLogsAreParsedSuccessfullyTest {
    private static final String CONF_PATH_PREFIX = "market/infra/market-health/config-cs-logshatter/src/";

    private enum TestCase {
        STATELOG_CHANGES("logs/statelog_changes.log", "conf.d/statelog_changes.json"),
        DJ_RECOMMENDER_TRACE("logs/DJ_recommender-trace.log", "conf.d/trace.json");

        private final String logFile;
        private final String configFile;

        TestCase(String logFile, String configFile) {
            this.logFile = logFile;
            this.configFile = configFile;
        }
    }

    private final ParserContext context;
    private final LogParser parser;
    private final List<String> lines;

    public ExampleLogsAreParsedSuccessfullyTest(TestCase testCase) throws Exception {
        context = mock(ParserContext.class);
        when(context.getHost()).thenReturn("host");
        when(context.getFile()).thenReturn(FileSystems.getDefault().getPath(testCase.configFile));

        JsonObject jsonConfig = getConfigObject(readConfig(testCase.configFile));
        Map<String, String> params = readParams(jsonConfig);
        this.parser = getParserProvider(jsonConfig).createParser(params);

        lines = readLogFile(testCase.logFile);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Stream.of(TestCase.values())
            .map(testCase -> new Object[]{testCase})
            .collect(Collectors.toList());
    }

    @Test
    public void parse() throws Exception {
        for (String line : lines) {
            parser.parse(line, context);
        }
    }

    private static String readConfig(String name) throws IOException {
        return FileUtils.readFileToString(
            new File(Paths.getSourcePath(CONF_PATH_PREFIX + name)),
            StandardCharsets.UTF_8
        );
    }

    private static List<String> readLogFile(String name) throws IOException, URISyntaxException {
        return FileUtils.readLines(getResourceFile(name), StandardCharsets.UTF_8);
    }

    @NotNull
    private static File getResourceFile(String name) throws URISyntaxException {
        URL resource = ExampleLogsAreParsedSuccessfullyTest.class.getClassLoader().getResource(name);
        checkNotNull(resource, "file not found: %s", name);
        return new File(resource.toURI());
    }
}
