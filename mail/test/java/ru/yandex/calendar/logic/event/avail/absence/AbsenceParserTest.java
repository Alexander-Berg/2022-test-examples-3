package ru.yandex.calendar.logic.event.avail.absence;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.io.ByteArrayInputStreamSource;
import ru.yandex.misc.io.ClassPathResourceInputStreamSource;
import ru.yandex.misc.test.Assert;

public class AbsenceParserTest extends AbstractConfTest {
    @Test
    public void parseLocalJson() {
        String json = "{"
                + "\"persons\": {"
                + "  \"calendartestuser\": ["
                + "    {"
                + "      \"comment\": \"Missing for whole day\","
                + "      \"workflow\": \"absence\","
                + "      \"work_in_absence\": false,"
                + "      \"date_from\": \"2016-07-26T00:00:00\","
                + "      \"date_to\": \"2016-07-27T00:00:00\","
                + "      \"full_day\": true,"
                + "      \"id\": 453748"
                + "    },"
                + "    {"
                + "      \"comment\": \"Missing for few hours\","
                + "      \"workflow\": \"trip\","
                + "      \"work_in_absence\": true,"
                + "      \"date_from\": \"2016-07-26T17:00:00\","
                + "      \"date_to\": \"2016-07-26T20:00:00\","
                + "      \"full_day\": false,"
                + "      \"id\": 453749"
                + "    }"
                + "  ]}}";

        Assert.equals(Cf.list(
                new Absence(
                        453748, new PassportLogin("calendartestuser"),
                        new LocalDate(2016, 7, 26), new LocalDate(2016, 7, 27),
                        AbsenceType.ABSENCE, "Missing for whole day", false),
                new Absence(
                        453749, new PassportLogin("calendartestuser"),
                        new LocalDateTime(2016, 7, 26, 17, 0), new LocalDateTime(2016, 7, 26, 20, 0), false,
                        AbsenceType.TRIP, "Missing for few hours", true)),

                AbsenceParser.parse(new ByteArrayInputStreamSource(json.getBytes())));
    }

    @Test
    public void parseLocalJson2() {
        ListF<Absence> absences = AbsenceParser.parse(
                new ClassPathResourceInputStreamSource(AbsenceParserTest.class, "planner-export.json"));
        Assert.A.hasSize(4408, absences);
    }
}
