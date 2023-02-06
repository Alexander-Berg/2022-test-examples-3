package ru.yandex.market.rotation;

import java.time.Instant;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Aleksei Malygin <a href="mailto:Malygin-Me@yandex-team.ru"></a>
 * Date: 2019-06-05
 */
public class ClickhousePartitionTypeTest {

    @Test
    public void define() {
        Assert.assertEquals(ClickhousePartitionType.TO_YYYYMM, ClickhousePartitionType.define("toYYYYMM(date)"));
        Assert.assertEquals(ClickhousePartitionType.TO_YYYYMMDD, ClickhousePartitionType.define("toYYYYMMDD(date)"));
        Assert.assertEquals(ClickhousePartitionType.TO_MONDAY, ClickhousePartitionType.define("toMonday(mydate)"));
        Assert.assertEquals(ClickhousePartitionType.TUPLE, ClickhousePartitionType.define("tuple()"));
        Assert.assertEquals(ClickhousePartitionType.DATE, ClickhousePartitionType.define("mydate"));

        Assertions.assertThatThrownBy(() -> ClickhousePartitionType.define("my date"))
            .isInstanceOf(RuntimeException.class);
        Assertions.assertThatThrownBy(() -> ClickhousePartitionType.define("toShMonday(mydate)"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void format() {
        Instant date = Instant.ofEpochSecond(1600000000);
        Assert.assertEquals("202009", ClickhousePartitionType.TO_YYYYMM.format(date));
        Assert.assertEquals("20200913", ClickhousePartitionType.TO_YYYYMMDD.format(date));
        Assert.assertEquals("2020-09-13", ClickhousePartitionType.DATE.format(date));
        Assert.assertEquals("2020-09-13", ClickhousePartitionType.TO_MONDAY.format(date));
        Assert.assertEquals("tuple()", ClickhousePartitionType.TUPLE.format(date));
    }

    @Test
    public void getColumnName() {
        Assert.assertEquals(Optional.of("date"), ClickhousePartitionType.TO_YYYYMM.getColumnName("toYYYYMM(date)"));
        Assert.assertEquals(Optional.of("date"), ClickhousePartitionType.TO_YYYYMMDD.getColumnName("toYYYYMMDD(date)"));
        Assert.assertEquals(Optional.of("mydate"), ClickhousePartitionType.TO_MONDAY.getColumnName("toMonday(mydate)"));
        Assert.assertEquals(Optional.of("mydate"), ClickhousePartitionType.DATE.getColumnName("mydate"));
        Assert.assertEquals(Optional.empty(), ClickhousePartitionType.TUPLE.getColumnName("tuple()"));
    }

    @Test
    public void prepareQuotes() {
        Assert.assertEquals("201901", ClickhousePartitionType.prepareQuotes("201901"));
        Assert.assertEquals("20190101", ClickhousePartitionType.prepareQuotes("20190101"));
        Assert.assertEquals("'2019-01-01'", ClickhousePartitionType.prepareQuotes("2019-01-01"));
        Assert.assertEquals("'2019-01-01'", ClickhousePartitionType.prepareQuotes("'2019-01-01'"));
        Assert.assertEquals("tuple()", ClickhousePartitionType.prepareQuotes("tuple()"));
    }

    @Test
    public void isTuple() {
        Assert.assertTrue(ClickhousePartitionType.isTuple("tuple()"));
        Assert.assertFalse(ClickhousePartitionType.isTuple("2019-05-13"));
    }
}
