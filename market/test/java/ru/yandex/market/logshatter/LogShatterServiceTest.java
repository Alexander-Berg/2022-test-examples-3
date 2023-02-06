package ru.yandex.market.logshatter;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import ru.yandex.market.clickhouse.ClickHouseSource;
import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.health.configs.logshatter.LogBatch;
import ru.yandex.market.health.configs.logshatter.config.ConfigValidationException;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.health.configs.logshatter.useragent.FakeUserAgentDetector;
import ru.yandex.market.logshatter.config.ConfigurationService;
import ru.yandex.market.logshatter.logging.BatchErrorLoggerFactory;
import ru.yandex.market.logshatter.logging.ErrorBoosterLogger;
import ru.yandex.market.logshatter.logging.LogSamplingPropertiesService;
import ru.yandex.market.logshatter.meta.SourceKey;
import ru.yandex.market.logshatter.parser.LogParser;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.parser.ParserContext;
import ru.yandex.market.logshatter.parser.TableDescription;
import ru.yandex.market.logshatter.reader.ReadSemaphore;
import ru.yandex.market.logshatter.reader.SourceContext;

import static org.mockito.Mockito.mock;


public class LogShatterServiceTest {

    public ClickhouseTemplate initClickhouseTemplate(ClickHouseSource clickHouseSource) {
        ClickhouseTemplate clickhouseTemplate = new ClickhouseTemplate();
        clickhouseTemplate.setDb(clickHouseSource);

        clickhouseTemplate.afterPropertiesSet();
        return clickhouseTemplate;
    }

    private ClickHouseSource initClickhouseSource() {
        // ssh -L 8123:health-house-testing.market.yandex.net:8123 welder01gt.market.yandex.net
        ClickHouseSource clickHouseSource = new ClickHouseSource();
        clickHouseSource.setHost(
            "welder01gt.market.yandex.net,welder02gt.market.yandex.net,welder03gt.market.yandex.net");
        clickHouseSource.setCluster("market_health");
        clickHouseSource.setSlbHost("127.0.0.1");
        clickHouseSource.setDb("market");
        clickHouseSource.setReplicated(true);
        clickHouseSource.setUser("clickphite");
        return clickHouseSource;
    }

    @Before
    public void setUp() {
        TestParser.parsedLines = 0;
    }

    @Test
    @Ignore
    public void testRun() throws Exception {
        System.setProperty("java.net.preferIPv6Addresses", "true");

        ApplicationContext cx = new FileSystemXmlApplicationContext("classpath:logshatter.xml");

        LogShatterService logShatterService = cx.getBean(LogShatterService.class);
        Thread.sleep(TimeUnit.MINUTES.toMillis(142));
    }

    @Test
    public void parseBatchSampleRatioNotMinusOne() throws ConfigValidationException {
        runParseBatch(0f);
        Assert.assertEquals(2, TestParser.parsedLines);
    }

    @Test
    public void notParseBatchSampleRatioMinusOne() throws ConfigValidationException {
        runParseBatch(-1f);
        Assert.assertEquals(0, TestParser.parsedLines);
    }

    private LogShatterService createLogShatterService(String tableName, float sampleRatio) {
        Map<String, Float> dataSampling = new HashMap<>();
        dataSampling.put(tableName, sampleRatio);
        LogShatterService logShatterService = new LogShatterService();

        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setUserAgentDetector(new FakeUserAgentDetector());

        logShatterService.setConfigurationService(configurationService);
        logShatterService.setDataSampling(dataSampling);
        logShatterService.setReadSemaphore(mock(ReadSemaphore.class));
        return logShatterService;
    }

    private void runParseBatch(float sampleRatio) throws ConfigValidationException {
        String tableName = "db.testTable";

        LogShatterConfig logshatterConfig = LogShatterConfig.newBuilder()
            .setDataClickHouseTable(new ClickHouseTableDefinitionImpl(tableName, Collections.emptyList(), null))
            .setConfigId("/some/path/file.log")
            .setParserProvider(new LogParserProvider(TestParser.class.getName(), null, null))
            .build();
        TestSourceContext testSourceContext = new TestSourceContext(
            logshatterConfig, null, new BatchErrorLoggerFactory(
            0, 0,
            new ErrorBoosterLogger(false, "test"),
            new LogSamplingPropertiesService(1.0f, Collections.emptyMap())
        )
        );

        LogBatch logBatch = new LogBatch(
            Stream.of("firstLine", "secondLine"), 0, 0, 0,
            Duration.ofMillis(0), Collections.emptyList(), "sourceName", "sourceHost", null
        );

        new LogShatterParserWorker(createLogShatterService(tableName, sampleRatio))
            .parseBatch(testSourceContext, logBatch);
    }

    private static class TestSourceContext extends SourceContext {

        TestSourceContext(LogShatterConfig logShatterConfig, SourceKey sourceKey,
                                 BatchErrorLoggerFactory errorLoggerFactory) {
            super(logShatterConfig, sourceKey, errorLoggerFactory, new ReadSemaphore().getEmptyQueuesCounter());
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public String getHost() {
            return null;
        }

        @Override
        public Path getPath() {
            return null;
        }

        @Override
        public long getDataOffset() {
            return 0;
        }

        @Override
        public void setDataOffset(long dataOffset) {

        }

        @Override
        public long getFileOffset() {
            return 0;
        }

        @Override
        public void setFileOffset(long fileOffset) {

        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public int getInstanceId() {
            return 0;
        }
    }

    public static class TestParser implements LogParser {
        private static int parsedLines = 0;

        @Override
        public TableDescription getTableDescription() {
            return null;
        }

        @Override
        public void parse(String line, ParserContext context) throws Exception {
            parsedLines++;
        }
    }
}
