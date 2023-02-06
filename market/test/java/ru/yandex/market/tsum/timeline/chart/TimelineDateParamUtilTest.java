package ru.yandex.market.tsum.timeline.chart;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 20.08.18
 */
public class TimelineDateParamUtilTest {
    @Test
    public void parseNow() {
        int actualResult = TimelineDateParamUtil.parseDateParam("now/d", true);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        Assert.assertEquals((int) TimeUnit.MILLISECONDS.toSeconds(calendar.getTimeInMillis()), actualResult);
    }

    @Test
    public void parseNowPlusOneDay() {
        int actualResult = TimelineDateParamUtil.parseDateParam("now+1d/d", false);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.add(Calendar.HOUR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Assert.assertEquals((int) TimeUnit.MILLISECONDS.toSeconds(calendar.getTimeInMillis()), actualResult);
    }

    @Test
    public void parseMillis() {
        int actualResult = TimelineDateParamUtil.parseDateParam("1483988504495", false);
        Assert.assertEquals(1483988504, actualResult);
    }
}