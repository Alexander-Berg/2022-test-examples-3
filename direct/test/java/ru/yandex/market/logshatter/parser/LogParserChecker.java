package ru.yandex.market.logshatter.parser;

import org.junit.Assert;
import org.junit.internal.ExactComparisonCriteria;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.logshatter.LogBatch;
import ru.yandex.market.logshatter.url.PageMatcher;
import ru.yandex.market.logshatter.useragent.FakeUserAgentDetector;
import ru.yandex.market.logshatter.useragent.UserAgentDetector;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LogParserChecker {
    private String origin = "debug";
    private String host = "hostname.test";
    private int instanceId = 123;
    private String logBrokerTopic = null;
    private LogParser logParser;
    private ParserContext context;
    private ArrayList<Date> dateList;
    private ArrayList<Object[]> fieldsList;
    private int lineCount;
    private Path file;
    private Map<String, String> params = new HashMap<>();
    private Map<int[], CustomAssertion> customAssertions = new HashMap<>();

    public LogParserChecker(LogParser logParser) {
        this.logParser = logParser;
        this.context = new TestContext();
    }

    public LogParserChecker(LogParser logParser, PageMatcher pageMatcher) {
        this.logParser = logParser;
        this.context = new TestContext();
        this.context = new MockableTestContext(pageMatcher);
    }

    public void check(String line) throws Exception {
        dateList = new ArrayList<>();
        fieldsList = new ArrayList<>();
        lineCount = 0;
        logParser.parse(line, context);
    }

    public void check(String line, int timestampSeconds, Object... expectedFields) throws Exception {
        check(line, new Date(timestampSeconds * 1000L), expectedFields);
    }

    public void check(String line, Date expectedDate, Object... expectedFields) throws Exception {
        check(line);

        Assert.assertTrue("Nothing is written", dateList.size() > 0);

        int lastIndex = dateList.size() - 1;

        checkOneEntry(dateList.get(lastIndex), fieldsList.get(lastIndex), expectedDate, expectedFields);
    }

    public void check(String line, List<Date> expectedDateList, List<Object[]> expectedFieldsList) throws Exception {
        check(line);

        Assert.assertEquals(
            "expectedDateList and expectedFieldsList sized should be equal",
            expectedDateList.size(),
            expectedFieldsList.size()
        );

        Assert.assertEquals(
            "line count differs",
            expectedDateList.size(),
            lineCount
        );

        for (int i = 0; i < lineCount; i++) {
            checkOneEntry(dateList.get(i), fieldsList.get(i), expectedDateList.get(i), expectedFieldsList.get(i));
        }
    }

    private void checkOneEntry(Date date, Object[] fields, Date expectedDate, Object[] expectedFields) throws Exception {

        checkDate(date, expectedDate);

        List<Column> columns = logParser.getTableDescription().getColumns()
            .stream()
            .skip(2)
            .collect(Collectors.toList());

        Assert.assertEquals(String.format(
            "Fields (%d) and columns (%d) sizes don't correspond (too much fields)",
            fields.length, columns.size()
        ), fields.length, columns.size());

        for (int i = 0; i < fields.length; i++) {
            Object field = fields[i];
            Column column = columns.get(i);
            Assert.assertNotNull("Field #" + i + " is null", field);
            Assert.assertTrue(
                "Field #" + i + "(" + field + ") is not a " + column.getType(),
                column.getType().validate(field)
            );
        }

        HashSet<Integer> customIndices = new HashSet<>();

        for (Map.Entry<int[], CustomAssertion> assertionEntry : customAssertions.entrySet()) {
            int[] indices = assertionEntry.getKey();
            for (int index : indices) {
                customIndices.add(index);
            }

            Object[] expected = Arrays.stream(indices).mapToObj(i -> expectedFields[i]).toArray();
            Object[] actual = Arrays.stream(indices).mapToObj(i -> fields[i]).toArray();
            assertionEntry.getValue().doAssertion(expected, actual);
        }

        Assert.assertEquals("Array length differs", expectedFields.length, fields.length);

        for (int i = 0; i < expectedFields.length; i++) {
            if (customIndices.contains(i)) {
                continue;
            }

            if (expectedFields[i] != null && fields[i] != null &&
                expectedFields[i].getClass().isArray() && fields[i].getClass().isArray()) {
                if (!(fields[i] instanceof Object[])) {
                    Assert.fail(String.format("Arrays must be Object[] (error at index [%d])", i));
                }

                new ExactComparisonCriteria().arrayEquals(
                    String.format("Data differs at index [%d]", i),
                    expectedFields[i],
                    fields[i]);
            } else {
                Assert.assertEquals(
                    String.format("Data differs at index [%d]", i),
                    expectedFields[i],
                    fields[i]);
            }
        }
    }

    public static void checkDate(Date date, Date expectedDate) {

        Assert.assertNotNull("Date cannot be null", date);

        Assert.assertTrue(
            "Date from parser is too old. Probably you mixed up the seconds with the milliseconds. " + date.toString(),
            date.getTime() > Integer.MAX_VALUE
        );

        Assert.assertTrue(
            "Expected date if too old. Probably you mixed up the seconds with the milliseconds. " + date.toString(),
            expectedDate == null || expectedDate.getTime() > Integer.MAX_VALUE
        );

        if (expectedDate != null) {
            //Ignores ms because we doesn't save is to ClickHouse
            Assert.assertEquals(
                "Wrong dates. Expected: " + expectedDate.toString() + " (" + expectedDate.getTime() + "). " +
                    "Actual: " + date.toString() + "(" + date.getTime() + ")",
                expectedDate.toInstant().getEpochSecond(), date.toInstant().getEpochSecond()
            );
        }
    }

    public void checkEmpty(String line) throws Exception {
        check(line);
        Assert.assertEquals(lineCount, 0);
    }

    public Date getDate() {
        return dateList.get(lineCount - 1);
    }

    public List<Date> getDateList() {
        return dateList;
    }

    public Object[] getFields() {
        return fieldsList.get(lineCount - 1);
    }

    public List<Object[]> getFieldList() {
        return fieldsList;
    }

    public String getHost() {
        return host;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setFile(String file) {
        this.file = Paths.get(file);
    }

    public void setParam(String name, String value) {
        this.params.put(name, value);
    }

    public Path getFile() {
        return file;
    }

    public int getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(int instanceId) {
        this.instanceId = instanceId;
    }

    public String getLogBrokerTopic() {
        return logBrokerTopic;
    }

    public void setLogBrokerTopic(String logBrokerTopic) {
        this.logBrokerTopic = logBrokerTopic;
    }

    public void setCustomAssertion(int[] indicies, CustomAssertion customAssertion) {
        customAssertions.put(indicies, customAssertion);
    }

    public class TestContext implements ParserContext {
        private UserAgentDetector userAgentDetector = new FakeUserAgentDetector();
        private final LogBatch logBatch = new LogBatch(Stream.empty(), 0, 0, 0,
            Duration.ZERO, logParser.getTableDescription().getColumns(), "");

        @Override
        public Path getFile() {
            return LogParserChecker.this.getFile() == null ? Paths.get("/tmp/", getHost(), "access.log") :
                LogParserChecker.this.getFile();
        }

        @Override
        public String getHost() {
            return LogParserChecker.this.getHost();
        }

        @Override
        public int getInstanceId() {
            return instanceId;
        }

        @Override
        public String getLogBrokerTopic() {
            return logBrokerTopic;
        }

        @Override
        public String getParam(String name) {
            return params.get(name);
        }

        @Override
        public Map<String, String> getParams() {
            return params;
        }

        @Override
        public String getOrigin() {
            return origin;
        }

        @Override
        public void write(Date date, Object... fields) {
            LogParserChecker.this.dateList.add(date);
            LogParserChecker.this.fieldsList.add(fields);
            lineCount++;

            // MARKETINFRA-4749 Пишем в настоящий LogBatch чтобы проверить что там ничего не падает
            logBatch.write(date, fields);
        }

        @Override
        public PageMatcher getPageMatcher() {
            return (host, httpMethod, url) -> null;
        }

        @Override
        public UserAgentDetector getUserAgentDetector() {
            return userAgentDetector;
        }
    }

    private class MockableTestContext extends TestContext {
        private PageMatcher pageMatcher;

        public MockableTestContext(PageMatcher pageMatcher) {
            this.pageMatcher = pageMatcher;
        }

        @Override
        public PageMatcher getPageMatcher() {
            return pageMatcher;
        }
    }

    public static <T> Map<T, T> arraysToMap(T[] expectedKeys, T[] expectedValues) {
        Map<T, T> expectedMap = new HashMap<>();
        for (int i = 0; i < expectedKeys.length; i++) {
            expectedMap.put(expectedKeys[i], expectedValues[i]);
        }
        return expectedMap;
    }
}
