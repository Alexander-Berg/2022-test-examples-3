package ru.yandex.market.mbo.gwt.server.remote;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author sergtru
 * @since 02.06.2017
 */
@SuppressWarnings("checkstyle:magicNumber")
public class DashboardRemoteServiceImplTest {
    @Test
    public void checkSessionDateParser() throws Exception {
        Date expected = new GregorianCalendar(2017, Calendar.JUNE, 1, 16, 57, 0).getTime();
        Assert.assertEquals(expected, DashboardRemoteServiceImpl.sessionToDate("20170601_1657"));
    }
}
