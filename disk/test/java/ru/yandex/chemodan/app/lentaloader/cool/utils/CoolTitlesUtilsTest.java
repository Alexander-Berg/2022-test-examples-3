package ru.yandex.chemodan.app.lentaloader.cool.utils;


import org.joda.time.DateTime;
import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author tolmalev
 */
public class CoolTitlesUtilsTest {

    @Test
    public void isCrossYear() {
        Assert.isTrue(CoolTitlesUtils.isCrossYear(
                DateTime.parse("2018-12-31T00:00:00.00+0300"),
                IntervalType.WEEK
        ));

        Assert.isFalse(CoolTitlesUtils.isCrossYear(
                DateTime.parse("2019-01-07T00:00:00.00+0300"),
                IntervalType.WEEK
        ));

        Assert.isTrue(CoolTitlesUtils.isCrossYear(
                DateTime.parse("2018-12-01T00:00:00.00+0300"),
                IntervalType.SEASON
        ));

        Assert.isFalse(CoolTitlesUtils.isCrossYear(
                DateTime.parse("2018-09-01T00:00:00.00+0300"),
                IntervalType.SEASON
        ));
        Assert.isFalse(CoolTitlesUtils.isCrossYear(
                DateTime.parse("2018-06-01T00:00:00.00+0300"),
                IntervalType.SEASON
        ));
        Assert.isFalse(CoolTitlesUtils.isCrossYear(
                DateTime.parse("2018-03-01T00:00:00.00+0300"),
                IntervalType.SEASON
        ));
    }

    @Test
    public void isCrossMonth() {
        Assert.isTrue(CoolTitlesUtils.isCrossMonth(
                DateTime.parse("2018-12-31T00:00:00.00+0300"),
                IntervalType.WEEK
        ));

        Assert.isFalse(CoolTitlesUtils.isCrossMonth(
                DateTime.parse("2019-01-07T00:00:00.00+0300"),
                IntervalType.WEEK
        ));

        Assert.isTrue(CoolTitlesUtils.isCrossMonth(
                DateTime.parse("2018-06-30T00:00:00.00+0300"),
                IntervalType.WEEKEND
        ));
    }
}
