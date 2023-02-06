package ru.yandex.market.logshatter.config;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.health.configs.logshatter.LogBatch;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.health.configs.logshatter.url.PageMatcher;
import ru.yandex.market.health.configs.logshatter.useragent.FakeUserAgentDetector;
import ru.yandex.market.health.configs.logshatter.useragent.UserAgentDetector;
import ru.yandex.market.logshatter.config.ddl.UpdateDDLService;
import ru.yandex.market.logshatter.parser.LogParser;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.parser.ParserContext;
import ru.yandex.market.logshatter.parser.TableDescription;

import static org.mockito.Mockito.when;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 02/11/2017
 */
@RunWith(Parameterized.class)
public class LogshatterConfigJsonTests {
    private static final String TEST_CONF_PATH = "market/infra/market-health/config-cs-logshatter/src/conf.d/test";

    private ConfigurationService configurationService;

    private static final String TIMESTAMP_SECONDS_FIELD = "timestampSeconds";
    private static final Gson GSON = new Gson();

    private static Map<String, LogShatterConfig> fileToConfig;

    private File testFile;

    @Mock
    private UpdateDDLService updateDDLServiceMock;

    public LogshatterConfigJsonTests(File testFile, String testName) {
        this.testFile = testFile;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        configurationService = TestServiceBuilder.buildConfigurationService();
        configurationService.setUpdateDDLService(updateDDLServiceMock);
        when(updateDDLServiceMock.getClickhouseDdlService()).thenReturn(TestServiceBuilder.buildDdlService());
        fileToConfig = configurationService.readAndValidateConfigurationFromFiles().stream()
            .collect(Collectors.toMap(LogShatterConfig::getConfigId, Function.identity()));
    }

    @Parameterized.Parameters(name = "{index}:{1}")
    public static Collection<Object[]> data() {
        File testConfigDir = new File(ru.yandex.devtools.test.Paths.getSourcePath(TEST_CONF_PATH));
        return Stream.of(testConfigDir.listFiles(f -> f.getName().toLowerCase().endsWith(".json")))
            .sorted()
            .map(f -> new Object[]{f, f.getName()})
            .collect(Collectors.toList());
    }

    @Test
    public void test() throws Exception {
        try (FileReader reader = new FileReader(testFile)) {
            TestConfig test = GSON.fromJson(reader, TestConfig.class);
            runConfigTest(testFile, test);
        }
    }

    public void runConfigTest(File file, TestConfig testConfig) throws Exception {
        LogShatterConfig config = fileToConfig.get(testConfig.getConfig());
        Preconditions.checkArgument(
            config != null,
            "No config file '%s' found for test %s", testConfig.getConfig(), file.getName()
        );
        File input = new File(ru.yandex.devtools.test.Paths.getSourcePath(TEST_CONF_PATH), testConfig.getInput());
        Preconditions.checkArgument(
            input.exists(),
            "File '%s' with input data not found for config %s", testConfig.getInput(), file.getName()
        );
        List<String> lines = FileUtils.readLines(input, Charset.defaultCharset())
            .stream()
            .filter(l -> !l.isEmpty())
            .collect(Collectors.toList());
        List<JsonObject> expectedResults = testConfig.getExpectedResults();

        LogParser parser = config.createParser();
        LogParserProvider parserProvider = config.getParserProvider();
        String dateFormat = null;
        if (parserProvider != null && parserProvider.getParserConfig() != null) {
            dateFormat = parserProvider.getParserConfig().getDateFormat();
        }


        InMemoryParsingContext parsingContext = new InMemoryParsingContext(parser.getTableDescription());
        for (String line : lines) {
            parser.parse(line, parsingContext);
        }

        checkLines(parser.getTableDescription(), parsingContext.getLinesWritten(), expectedResults, dateFormat);
    }

    private void checkLines(TableDescription tableDescription,
                            List<ParsedLine> parsedLines,
                            List<JsonObject> expectedResults,
                            String dateFormatString) throws Exception {
        DateFormat dateFormat = dateFormatString == null ? null : new SimpleDateFormat(dateFormatString);

        Preconditions.checkArgument(
            parsedLines.size() == expectedResults.size(),
            "Expected results count (%s) in test config != lines count (%s) in file with test data after parsing",
            expectedResults.size(), parsedLines.size()
        );

        for (int i = 0; i < parsedLines.size(); i++) {
            checkLine(tableDescription, parsedLines.get(i), expectedResults.get(i), dateFormat);
        }
    }

    private void checkLine(TableDescription tableDescription, ParsedLine parsedLine,
                           JsonObject expected, DateFormat dateFormat) {
        List<Column> columns = tableDescription.getColumns().stream()
            .filter(c -> !c.equals(TableDescription.DATE_COLUMN))
            .filter(c -> !c.equals(TableDescription.TIMESTAMP_COLUMN))
            .collect(Collectors.toList());

        boolean emptyExpected = (expected.entrySet().size() == 0);

        if (!emptyExpected) {
            checkResultSchema(columns, expected);
        }

        Date expectedDate = new Date(TimeUnit.SECONDS.toMillis(expected.get(TIMESTAMP_SECONDS_FIELD).getAsInt()));
        LogParserChecker.checkDate(parsedLine.date, expectedDate);

        for (int i = 0; i < parsedLine.fields.length; i++) {
            Object value = parsedLine.fields[i];
            Column column = columns.get(i);
            JsonElement expectedElement = expected.get(column.getName());
            Assert.assertNotNull("No value for column " + column.getName() + ". " +
                "Did you forget to add \"" + column.getName() + "\": null to expected result?", expectedElement);
            if (expectedElement.isJsonNull()) {
                continue;
            }
            Object expectedValue = null;
            try {
                expectedValue = column.getType().parseValue(expectedElement.getAsString(), dateFormat);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    String.format(
                        "Failed to parse expected value '%s' for column %s(%s)",
                        expectedElement.getAsString(),
                        column.getName(),
                        column.getType()
                    ),
                    e
                );
            }
            if (expectedValue.getClass().isArray()) {
                Object[] expectedArray = (Object[]) expectedValue;
                Object[] valueArray = (Object[]) value;
                Assert.assertEquals(
                    String.format("Different values array size for column '%s'", column.getName()),
                    expectedArray.length, valueArray.length
                );

                for (int j = 0; j < expectedArray.length; j++) {
                    Assert.assertEquals(
                        String.format("Different values on position %s for column '%s'", j, column.getName()),
                        expectedArray[j].toString(), valueArray[j].toString()
                    );
                }
            } else {
                Assert.assertEquals(
                    String.format("Different values for column '%s'", column.getName()),
                    expectedValue.toString(), value.toString()
                );
            }
        }


    }

    private void checkResultSchema(List<Column> columns, JsonObject expected) {
        Assert.assertTrue(
            String.format("No %s field in expected result: %s", TIMESTAMP_SECONDS_FIELD, expected),
            expected.has(TIMESTAMP_SECONDS_FIELD)
        );

        Set<String> expectedResultKeys = expected.entrySet()
            .stream()
            .map(Map.Entry::getKey)
            .filter(s -> !s.equals(TIMESTAMP_SECONDS_FIELD))
            .collect(Collectors.toSet());

        Set<String> notProvidedValues = new HashSet<>();
        for (Column column : columns) {
            if (!expectedResultKeys.remove(column.getName())
                && column.getDefaultValue() == null
                && column.getDefaultExpr() == null) {
                notProvidedValues.add(column.getName());
            }
        }

        Assert.assertTrue(
            String.format("Values for columns '%s' not provided in expected result: %s", notProvidedValues, expected),
            notProvidedValues.isEmpty()
        );

        Assert.assertTrue(
            String.format(
                "Found unexpected keys %s (allowed %s) in expected result: %s", expectedResultKeys, columns, expected
            ),
            expectedResultKeys.isEmpty()
        );
    }

    private static class InMemoryParsingContext implements ParserContext {
        private final LogBatch logBatch;
        private List<ParsedLine> linesWritten = new ArrayList<>();

        InMemoryParsingContext(TableDescription tableDescription) {
            this.logBatch = new LogBatch(Stream.empty(), 0, 0, 0,
                Duration.ZERO, tableDescription.getColumns(), "", "", null);
        }

        @Override
        public Path getFile() {
            return Paths.get("/var/log/some/filename.log");
        }

        @Override
        public String getHost() {
            return "HOSTNAME";
        }

        @Override
        public int getInstanceId() {
            return 0;
        }

        @Override
        public String getLogBrokerTopic() {
            return null;
        }

        @Override
        public String getOrigin() {
            return null; //TODO
        }

        @Override
        public PageMatcher getPageMatcher() {
            return null; //TODO
        }

        @Override
        public UserAgentDetector getUserAgentDetector() {
            return new FakeUserAgentDetector();
        }

        @Override
        public void logException(Exception exception, String line) {
        }

        @Override
        public void write(Date date, Object... fields) {
            linesWritten.add(new ParsedLine(date, fields));
            // MARKETINFRA-4749 Пишем в настоящий LogBatch чтобы проверить что там ничего не падает
            logBatch.write(date, fields);
        }

        public List<ParsedLine> getLinesWritten() {
            return linesWritten;
        }
    }

    private static class ParsedLine {
        private Date date;
        private Object[] fields;

        ParsedLine(Date date, Object[] fields) {
            this.date = date;
            this.fields = fields;
        }
    }

}
