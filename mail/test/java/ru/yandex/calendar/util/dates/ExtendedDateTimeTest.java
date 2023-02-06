package ru.yandex.calendar.util.dates;

import org.junit.Test;

import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.TimeUtils;

/**
 * @author gutman
 */
public class ExtendedDateTimeTest {

    @Test
    public void toInstantAtSmth() {
        ExtendedDateTime d = new ExtendedDateTime(
                TestDateTimes.moscow(2011, 9, 15, 1, 11), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);

        Assert.A.equals(TestDateTimes.moscow(2011, 9, 15, 0, 0), d.toInstantAtStartOfDay());
        Assert.A.equals(TestDateTimes.moscow(2011, 9, 16, 0, 0), d.toInstantAtEndOfDay());
        Assert.A.equals(TestDateTimes.moscow(2011, 9, 12, 0, 0), d.toInstantAtStartOfWeek());
        Assert.A.equals(TestDateTimes.moscow(2011, 9, 19, 0, 0), d.toInstantAtEndOfWeek());
    }

}
