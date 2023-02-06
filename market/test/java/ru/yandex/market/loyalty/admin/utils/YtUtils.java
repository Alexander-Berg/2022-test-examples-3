package ru.yandex.market.loyalty.admin.utils;

import ru.yandex.market.loyalty.admin.yt.model.Promo3pYt;

import static org.junit.Assert.assertEquals;

/**
 * @author dinyat
 * 03/08/2017
 */
public class YtUtils {

    public static void assertYtEquals(Promo3pYt expected, Promo3pYt actual) {
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getEndDate(), actual.getEndDate());
        assertEquals(expected.getStartDate(), actual.getStartDate());
        assertEquals(expected.getRegions(), actual.getRegions());
    }
}
