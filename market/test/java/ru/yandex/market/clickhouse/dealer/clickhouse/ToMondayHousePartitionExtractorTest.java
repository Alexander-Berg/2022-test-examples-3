package ru.yandex.market.clickhouse.dealer.clickhouse;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 16/06/2018
 */
public class ToMondayHousePartitionExtractorTest {

    @Test
    public void extract() {
        Assert.assertEquals("'2018-06-11'", ToMondayHousePartitionExtractor.INSTANCE.extract("2018-06-16"));
        Assert.assertEquals("'2018-05-28'", ToMondayHousePartitionExtractor.INSTANCE.extract("2018-06-01"));
        Assert.assertEquals("'2016-12-26'", ToMondayHousePartitionExtractor.INSTANCE.extract("2017-01-01"));
    }
}
