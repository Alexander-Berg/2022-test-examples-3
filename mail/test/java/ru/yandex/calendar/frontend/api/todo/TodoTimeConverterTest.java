package ru.yandex.calendar.frontend.api.todo;

import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.time.TimeUtils;

/**
 * @author gutman
 */
public class TodoTimeConverterTest extends CalendarTestBase {

    @Test
    public void convertOnCreate() {
        Instant i = TestDateTimes.moscow(2012, 2, 21, 15, 53);
        DateTimeZone userTz = MoscowTime.TZ;

        Option<Instant> fromTimestamp = TodoTimeConverter.convertOnCreate(
                Option.of(Long.toString(i.getMillis() / 1000)), Option.<String>empty(), userTz);
        Option<Instant> fromDateTime = TodoTimeConverter.convertOnCreate(
                Option.<String>empty(), Option.of("2012-02-21T15:53:00"), userTz);
        Option<Instant> fromDate = TodoTimeConverter.convertOnCreate(
                Option.<String>empty(), Option.of("2012-02-21"), userTz);
        Option<Instant> none = TodoTimeConverter.convertOnCreate(
                Option.<String>empty(), Option.<String>empty(), userTz);

        Assert.equals(i, fromTimestamp.get());
        Assert.equals(i, fromDateTime.get());
        Assert.equals(i.toDateTime(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).withTimeAtStartOfDay().toInstant(), fromDate.get());
        Assert.none(none);
    }

    @Test
    public void convertOnUpdate() {
        DateTimeZone userTz = MoscowTime.TZ;

        Option<Option<Instant>> none = TodoTimeConverter.convertOnUpdate(
                Option.<String>empty(), Option.<String>empty(), userTz);
        Option<Option<Instant>> emptyUnixtime = TodoTimeConverter.convertOnUpdate(
                Option.of(""), Option.<String>empty(), userTz);
        Option<Option<Instant>> emptyDateTime = TodoTimeConverter.convertOnUpdate(
                Option.<String>empty(), Option.of(""), userTz);

        Assert.none(none);
        Assert.none(emptyUnixtime.get());
        Assert.none(emptyDateTime.get());
    }

}
