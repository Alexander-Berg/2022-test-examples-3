package ru.yandex.reminders.util;

import org.junit.Test;

import ru.yandex.misc.test.Assert;
import ru.yandex.misc.test.TestBase;

/**
 * @author ssytnik
 */
public class HostnameUtilsTest extends TestBase {

    @Test
    public void getHostId() {
        Assert.some("02f", HostnameUtils.getHostId("calendar-back02f.tools.yandex.net"));
        Assert.some("01e", HostnameUtils.getHostId("calendar-worker01e.tools.yandex.net"));
        Assert.some("01f", HostnameUtils.getHostId("back01f.calendar.yandex.net"));
        Assert.some("182e", HostnameUtils.getHostId("target182e.load.yandex.net"));
        Assert.some("1", HostnameUtils.getHostId("calendar1.dev.yandex.net"));
        Assert.none(HostnameUtils.getHostId("gauss.music.dev.yandex.net"));
        // synthetic
        Assert.some("02z", HostnameUtils.getHostId("calendar_back02z.tools.yandex.net"));
        Assert.some("02z", HostnameUtils.getHostId("calendar_back02z.tools.yandex.net"));
        Assert.some("02Z", HostnameUtils.getHostId("CALENDAR_BACK02Z.TOOLS.YANDEX.NET"));
        Assert.some("02", HostnameUtils.getHostId("calendar_back02.tools.yandex.net"));
        Assert.some("02z", HostnameUtils.getHostId("calendar-back02z"));
        Assert.none(HostnameUtils.getHostId("calendar_back02yz.tools.yandex.net"));
        Assert.none(HostnameUtils.getHostId("calendar-back02z-some"));
        Assert.none(HostnameUtils.getHostId("calendar-back02z-some.tools.yandex.net"));
        Assert.none(HostnameUtils.getHostId("calendar.back02z.tools.yandex.net"));
    }

    @Test
    public void looksLikeHostname() {
        Assert.isTrue(HostnameUtils.looksLikeHostname("yandex.ru"));
        Assert.isTrue(HostnameUtils.looksLikeHostname("www.film.ru"));
        Assert.isFalse(HostnameUtils.looksLikeHostname("Yandex.Calendar"));
    }

}
