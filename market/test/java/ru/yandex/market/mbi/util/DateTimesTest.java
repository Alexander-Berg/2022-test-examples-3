package ru.yandex.market.mbi.util;

import java.time.Month;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.util.DateTimes;

public class DateTimesTest {

    @Test
    void getMonthCasedName() {
        Assertions.assertEquals(DateTimes.getMonthCasedName(Month.JANUARY), "Январе");
        Assertions.assertEquals(DateTimes.getMonthCasedName(Month.FEBRUARY), "Феврале");
        Assertions.assertEquals(DateTimes.getMonthCasedName(Month.MARCH), "Марте");
        Assertions.assertEquals(DateTimes.getMonthCasedName(Month.APRIL), "Апреле");
        Assertions.assertEquals(DateTimes.getMonthCasedName(Month.MAY), "Мае");
        Assertions.assertEquals(DateTimes.getMonthCasedName(Month.JUNE), "Июне");
        Assertions.assertEquals(DateTimes.getMonthCasedName(Month.JULY), "Июле");
        Assertions.assertEquals(DateTimes.getMonthCasedName(Month.AUGUST), "Августе");
        Assertions.assertEquals(DateTimes.getMonthCasedName(Month.SEPTEMBER), "Сентябре");
        Assertions.assertEquals(DateTimes.getMonthCasedName(Month.NOVEMBER), "Ноябре");
        Assertions.assertEquals(DateTimes.getMonthCasedName(Month.DECEMBER), "Декабре");
    }
}
