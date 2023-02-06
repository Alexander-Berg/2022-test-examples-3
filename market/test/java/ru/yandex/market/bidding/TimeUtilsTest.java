package ru.yandex.market.bidding;

import java.util.Date;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

class TimeUtilsTest {

    @Test
    void testGetUnixTime() {
        int expectedEpoch = 1400679913;
        assertEquals(expectedEpoch, TimeUtils.getUnixTime(new Date(expectedEpoch * 1000L)));
        assertEquals(expectedEpoch, TimeUtils.getUnixTime(new Date(expectedEpoch * 1000L + 500L)));
        assertEquals(expectedEpoch, TimeUtils.getUnixTime(new Date(expectedEpoch * 1000L + 501L)));
        assertEquals(expectedEpoch, TimeUtils.getUnixTime(new Date(expectedEpoch * 1000L + 999L)));
        assertEquals(expectedEpoch + 1, TimeUtils.getUnixTime(new Date(expectedEpoch * 1000L + 1000L)));
    }

    @Test
    void testDateFromUnixTime() {
        int expectedEpoch = 1400679913;
        Date expectedDate = new Date(expectedEpoch * 1000L);
        assertEquals(expectedDate, TimeUtils.dateFromUnixTime(expectedEpoch));
        assertEquals(new DateTime(expectedDate).plusSeconds(1).toDate(), TimeUtils.dateFromUnixTime(expectedEpoch + 1));
    }
}