package ru.yandex.market.clickhouse.dealer.clickhouse;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 16/06/2018
 */
public class ClickHousePartitionParserTest {

    @Test(expected = Exception.class)
    public void testUnsupported() {
        ClickHousePartitionParser.parsePartitionSystem("toStartOfYear(date)", "date");
    }

    @Test(expected = Exception.class)
    public void testUnsupported2() {
        ClickHousePartitionParser.parsePartitionSystem("toYYYYMM(date)", null);
    }

    @Test(expected = Exception.class)
    public void testUnsupported3() {
        ClickHousePartitionParser.parsePartitionSystem("toYYYYMM(date)", "otherField");
    }

    @Test
    public void testParse() {
        testClass("toYYYYMM(date)", "date", ToYyyyMmClickHousePartitionExtractor.class);
        testClass("toMonday(date)", "date", ToMondayHousePartitionExtractor.class);
        testClass("tuple()", null, SingletonClickHousePartitionExtractor.class);
    }

    private static void testClass(String partitionBy, String addDateField, Class expectedClass) {
        Assert.assertEquals(
            expectedClass,
            ClickHousePartitionParser.parsePartitionSystem(partitionBy, addDateField).getClass()
        );
    }

    @Test
    public void testDatePartitionParse() {
        Assert.assertEquals("201810", extract("toYYYYMM(date)", "date", "2018-10-06"));
        Assert.assertEquals("'2018-10-01'", extract("toMonday(date)", "date", "2018-10-06"));
        Assert.assertEquals("tuple()", extract("tuple()", "date", "2018-10-06"));
        Assert.assertEquals("'2018-10-06'", extract("date", "date", "2018-10-06"));
    }

    private String extract(String partitionBy, String dateField, String date) {
        return ClickHousePartitionParser.parsePartitionSystem(partitionBy, dateField).extract(date);
    }
}