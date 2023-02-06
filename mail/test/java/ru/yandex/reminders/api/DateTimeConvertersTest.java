package ru.yandex.reminders.api;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import ru.yandex.bolts.function.Function;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class DateTimeConvertersTest {

    @Test
    public void raspDateTimeConverter() {
        DateTime dt = DateTimeConverters.raspDateTimeWithTzConverter.parse("2014-11-16 06:15:00 +0400");
        Assert.equals(new LocalDateTime(2014, 11, 16, 6, 15), dt.toLocalDateTime());
    }

    @Test
    public void hotelDateParser() {
        Function<String, LocalDateTime> parse = DateTimeConverters.hotelDateParser::apply;

        Assert.equals(new LocalDateTime(2015, 2, 21, 0, 5), parse.apply("21.2.2015 0:5:0"));
        Assert.equals(new LocalDateTime(2015, 2, 22, 14, 5), parse.apply("2015-2-22 14:5:0"));
        Assert.equals(new LocalDateTime(2015, 2, 23, 1, 5), parse.apply("2015-02-23 01:05:00"));
    }
}
