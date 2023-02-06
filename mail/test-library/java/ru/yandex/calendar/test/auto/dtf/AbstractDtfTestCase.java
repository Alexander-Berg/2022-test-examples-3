package ru.yandex.calendar.test.auto.dtf;

import java.util.Map;

import org.joda.time.Chronology;
import org.joda.time.DateTimeZone;
import org.joda.time.chrono.ISOChronology;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.misc.time.TimeUtils;

/**
 * @author ssytnik
 */
public class AbstractDtfTestCase extends CalendarTestBase {
    private static final String[] TZ_IDS = {DateTimeZone.UTC.getID(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE.getID(), "Europe/London"};
    protected static Map<String, Chronology> tzId2ChronoDtfMap = null;
    protected static Chronology utcChrono = null;

    static {
        tzId2ChronoDtfMap = Cf.hashMap();
        for (String tzId : TZ_IDS) {
            Chronology chrono = ISOChronology.getInstance(DateTimeZone.forID(tzId));
            tzId2ChronoDtfMap.put(tzId, chrono);
        } // for
        utcChrono = tzId2ChronoDtfMap.get(DateTimeZone.UTC.getID());
    } // static

}
