package ru.yandex.market.jmf.attributes.test.date;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.util.Randoms;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class DateDescriptorWrapTest extends AbstractDateDescriptorTest {

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void wrap(@SuppressWarnings("unused") String testName, Object rawValue, LocalDate expected) {
        LocalDate result = descriptor.wrap(attribute, type, rawValue);
        Assertions.assertEquals(expected, result);
    }

    private static Stream<Arguments> data() {
        var randomValue = Randoms.date();

        return Stream.of(
                arguments("null", null, null),
                arguments("random", randomValue, randomValue),
                arguments("date", new Date(120, Calendar.FEBRUARY, 22, 13, 17), LocalDate.parse("2020-02-22")),
                arguments("long", 1582367486000L, LocalDate.parse("2020-02-22")),
                arguments("instant", OffsetDateTime.parse("2020-02-22T07:13+03:00").toInstant(), LocalDate.parse(
                        "2020-02-22")),
                arguments("string", "2020-02-22", LocalDate.parse("2020-02-22"))
        );
    }
}
