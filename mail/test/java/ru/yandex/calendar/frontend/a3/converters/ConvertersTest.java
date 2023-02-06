package ru.yandex.calendar.frontend.a3.converters;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.commune.a3.action.invoke.ActionInvocationContext;
import ru.yandex.commune.a3.action.parameter.convert.ConverterToType;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class ConvertersTest extends CalendarTestBase {
    @Test
    public void convertIsoDateTime() {
        ConverterToType<Instant> converter = new ConverterToInstantIsoDateTime();

        Instant converted = converter.convert("2014-12-15T00:00:00+0300", new ActionInvocationContext());

        DateTimeZone tz = DateTimeZone.forOffsetHours(3);
        Assert.equals(new DateTime(2014, 12, 15, 0, 0, tz), converted.toDateTime(tz));
    }
}
