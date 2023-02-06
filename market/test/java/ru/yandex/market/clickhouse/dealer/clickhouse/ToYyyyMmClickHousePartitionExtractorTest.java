package ru.yandex.market.clickhouse.dealer.clickhouse;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 16/06/2018
 */
public class ToYyyyMmClickHousePartitionExtractorTest {

    @Test
    public void extract() {
        Assert.assertEquals("201711", ToYyyyMmClickHousePartitionExtractor.INSTANCE.extract("2017-11-12"));
    }
}
