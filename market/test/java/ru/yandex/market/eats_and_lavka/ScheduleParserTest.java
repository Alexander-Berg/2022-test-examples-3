package ru.yandex.market.eats_and_lavka;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.eats_and_lavka.ScheduleParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

public class ScheduleParserTest {

    @ParameterizedTest
    @MethodSource
    public void parseTest(String sheduleJson, String shortSchedule) {
        String shedule = ScheduleParser.parse(sheduleJson);
        assertEquals(
                shedule,
                shortSchedule
        );
    }

    private static Stream<Arguments> parseTest() {
        return Stream.of(
                Arguments.of(getString(ScheduleParserTest.class, "schedule/ScheduleEatPartner.json"),
                        "Пн-Чт, Вс 12:00 - 24:00, Пт-Сб 12:00 - 0:30"),
                Arguments.of(getString(ScheduleParserTest.class, "schedule/ScheduleEatPartner1.json"),
                        "Пн, Ср-Чт, Вс 12:00 - 24:00, Вт, Пт-Сб 12:00 - 0:30"),
                Arguments.of(getString(ScheduleParserTest.class, "schedule/ScheduleEatPartner2.json"),
                        "Пн, Ср-Чт 12:00 - 24:00, Вт, Пт-Вс 12:00 - 0:30"),
                Arguments.of(getString(ScheduleParserTest.class, "schedule/ScheduleEatPartner3.json"),
                        "Пн, Ср 12:00 - 24:00, Вт, Чт-Сб 12:00 - 0:30"),
                Arguments.of(getString(ScheduleParserTest.class, "schedule/ScheduleEatPartner4.json"),
                        "Пн-Чт, Вс 8:00 - 0:30, Пт-Сб 8:00 - 1:00"),
                Arguments.of(getString(ScheduleParserTest.class, "schedule/ScheduleEatPartner5.json"),
                        "Пн-Вт 12:00 - 24:00, Ср-Пт круглосуточно")
        );
    }
}
