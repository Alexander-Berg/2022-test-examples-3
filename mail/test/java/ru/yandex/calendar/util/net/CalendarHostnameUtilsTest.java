package ru.yandex.calendar.util.net;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class CalendarHostnameUtilsTest {
    @Test
    public void getHostId() {
        Assert.some("02f", CalendarHostnameUtils.getHostId("calendar-back02f.tools.yandex.net"));
        Assert.some("01e", CalendarHostnameUtils.getHostId("calendar-worker01e.tools.yandex.net"));
        Assert.some("01f", CalendarHostnameUtils.getHostId("back01f.calendar.yandex.net"));
        Assert.some("182e", CalendarHostnameUtils.getHostId("target182e.load.yandex.net"));
        Assert.some("1", CalendarHostnameUtils.getHostId("calendar1.dev.yandex.net"));
        Assert.none(CalendarHostnameUtils.getHostId("gauss.music.dev.yandex.net"));
        // synthetic
        Assert.some("02z", CalendarHostnameUtils.getHostId("calendar_back02z.tools.yandex.net"));
        Assert.some("02z", CalendarHostnameUtils.getHostId("calendar_back02z.tools.yandex.net"));
        Assert.some("02Z", CalendarHostnameUtils.getHostId("CALENDAR_BACK02Z.TOOLS.YANDEX.NET"));
        Assert.some("02", CalendarHostnameUtils.getHostId("calendar_back02.tools.yandex.net"));
        Assert.some("02z", CalendarHostnameUtils.getHostId("calendar-back02z"));
        Assert.none(CalendarHostnameUtils.getHostId("calendar_back02yz.tools.yandex.net"));
        Assert.none(CalendarHostnameUtils.getHostId("calendar-back02z-some"));
        Assert.none(CalendarHostnameUtils.getHostId("calendar-back02z-some.tools.yandex.net"));
        Assert.none(CalendarHostnameUtils.getHostId("calendar.back02z.tools.yandex.net"));
    }

}
