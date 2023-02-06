package ru.yandex.calendar.logic.resource;

import org.joda.time.Duration;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.calendar.logic.user.NameI18n;
import ru.yandex.calendar.util.dates.DateInterval;
import ru.yandex.calendar.util.dates.DateOrDateTime;
import ru.yandex.calendar.util.dates.TimeField;
import ru.yandex.calendar.util.dates.TimesInUnit;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class ResourceInaccessibilityTest {

    @Test
    public void readableMessages() {
        Resource resource = new Resource();

        resource.setId(1L);

        resource.setExchangeName("conf_rr");
        resource.setDomain("yandex-team.ru");

        resource.setName("Перег");
        resource.setNameEn("Room");

        Function1V<ResourceInaccessibility> print = inaccessibility -> {
            NameI18n message = inaccessibility.getReadableMessage();

            System.out.println(message.getName(Language.RUSSIAN));
            System.out.println(message.getName(Language.ENGLISH));
        };

        print.apply(ResourceInaccessibility.repetitionDenied(resource));

        print.apply(ResourceInaccessibility.tooLongEvent(resource, Duration.standardMinutes(13)));

        print.apply(ResourceInaccessibility.tooLongEvent(resource, Duration.standardMinutes(1555)));

        print.apply(ResourceInaccessibility.tooFarEvent(resource, MoscowTime.dateTime(2018, 1, 11, 23, 0)));

        print.apply(ResourceInaccessibility.dateRestricted(resource, new DateInterval(
                Option.of(DateOrDateTime.dateTime(new LocalDateTime(2018, 1, 11, 22, 0))),
                Option.of(DateOrDateTime.dateTime(new LocalDateTime(2018, 1, 11, 23, 0))),
                Option.of(new NameI18n("Веселье", "Fun")))));

        print.apply(ResourceInaccessibility.dateRestricted(resource, new DateInterval(
                Option.of(DateOrDateTime.dateTime(new LocalDateTime(2018, 1, 11, 22, 0))),
                Option.of(DateOrDateTime.dateTime(new LocalDateTime(2018, 1, 11, 23, 0))))));

        print.apply(ResourceInaccessibility.dateRestricted(resource, new DateInterval(
                Option.of(DateOrDateTime.dateTime(new LocalDateTime(2018, 1, 11, 22, 0))),
                Option.of(DateOrDateTime.dateTime(new LocalDateTime(2018, 2, 22, 23, 0))))));

        print.apply(ResourceInaccessibility.dateRestricted(resource, new DateInterval(
                Option.of(DateOrDateTime.dateTime(new LocalDateTime(2018, 1, 11, 22, 0))), Option.empty())));

        print.apply(ResourceInaccessibility.dateRestricted(resource, new DateInterval(
                Option.empty(), Option.of(DateOrDateTime.dateTime(new LocalDateTime(2018, 2, 22, 23, 0))))));

        print.apply(ResourceInaccessibility.tooManyEvents(resource, new TimesInUnit(1, TimeField.DAY)));

        print.apply(ResourceInaccessibility.tooManyEvents(resource, new TimesInUnit(3, TimeField.WEEKDAYS)));

        print.apply(ResourceInaccessibility.tooManyEvents(resource, new TimesInUnit(5, TimeField.WEEK)));

        print.apply(ResourceInaccessibility.tooManyEventsByUser(resource, new TimesInUnit(19, TimeField.MONTH)));

        print.apply(ResourceInaccessibility.massageDenied(resource, "Error"));
    }
}
