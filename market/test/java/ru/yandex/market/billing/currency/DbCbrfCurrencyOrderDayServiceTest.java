package ru.yandex.market.billing.currency;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.calendar.CalendarType;
import ru.yandex.market.core.calendar.DayType;
import ru.yandex.market.currency.CbrfCurrencyOrderDayService;
import ru.yandex.market.currency.DbCbrfCurrencyOrderDayService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Тесты для {@link DbCbrfCurrencyOrderDayService}.
 *
 * @author vbudnev
 */
class DbCbrfCurrencyOrderDayServiceTest extends FunctionalTest {

    private static final LocalDate DATE_2018_05_01 = LocalDate.of(2018, 5, 1);
    private static final LocalDate HOLIDAY_2018_08_07 = LocalDate.of(2018, 8, 7);
    private static final LocalDate WORKING_2018_08_08 = LocalDate.of(2018, 8, 8);
    private static final LocalDate HOLIDAY_2018_09_06 = LocalDate.of(2018, 9, 6);
    private static final LocalDate HOLIDAY_2018_08_09 = LocalDate.of(2018, 8, 9);
    private static final LocalDate WORKING_2018_09_20 = LocalDate.of(2018, 9, 20);
    private static final LocalDate WORKING_2018_09_21 = LocalDate.of(2018, 9, 21);
    private static final LocalDate HOLIDAY_2018_09_22 = LocalDate.of(2018, 9, 22);
    private static final LocalDate HOLIDAY_2018_11_23 = LocalDate.of(2018, 11, 23);
    private static final LocalDate MISSING_TYPE_DAY_2018_09_26 = LocalDate.of(2018, 9, 26);

    @Autowired
    private CbrfCurrencyOrderDayService currencyExchangeDaysTracer;

    private static Stream<Arguments> argsForFails() {
        return Stream.of(
                Arguments.of(
                        "В рассматриваемый интервал не удалось получить никакой информации о типе дней",
                        DATE_2018_05_01,
                        "Failed to load any dayType data. date=2018-05-01"
                ),
                Arguments.of(
                        "Не удалось получить информацию о типе дня для запрашиваемой даты",
                        MISSING_TYPE_DAY_2018_09_26,
                        "No dayType for target day. date=2018-09-26"
                ),
                Arguments.of(
                        "В рассматриваемый интервал не нашлось ни одного рабочего дня",
                        HOLIDAY_2018_11_23,
                        "No working day found on interval. date=2018-11-23"
                )
        );
    }

    private static Stream<Arguments> argsPositive() {
        return Stream.of(
                Arguments.of(
                        "Рабочий день, следующий за одиночным рабочим днем",
                        WORKING_2018_09_21,
                        WORKING_2018_09_21
                ),
                Arguments.of(
                        "Рабочий день, следующий за одиночным выходным днем",
                        WORKING_2018_08_08,
                        HOLIDAY_2018_08_07
                ),
                Arguments.of(
                        "Рабочий день, следующий за выходным интервалом",
                        WORKING_2018_09_20,
                        HOLIDAY_2018_08_09
                ),
                Arguments.of(
                        "Выходной день, следующий за выходным интервалом",
                        HOLIDAY_2018_09_06,
                        HOLIDAY_2018_08_09
                ),
                Arguments.of(
                        "Выходной день, следующий за рабочим",
                        HOLIDAY_2018_09_22,
                        HOLIDAY_2018_09_22
                )
        );
    }

    @DisplayName("Поиск ближайщего рабочего дня.Ошибки")
    @MethodSource("argsForFails")
    @ParameterizedTest(name = "{0}")
    @DbUnitDataSet(before = "db/calendars.csv")
    void test_getOrderDay_errors(String description, LocalDate passedDate, String expectedExcMessage) {
        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> currencyExchangeDaysTracer.getOrderDateForDay(passedDate)
        );

        Assertions.assertEquals(expectedExcMessage, ex.getMessage());
    }

    @DisplayName("Поиск ближайшего рабочего дня")
    @MethodSource("argsPositive")
    @DbUnitDataSet(before = "db/calendars.csv")
    @ParameterizedTest(name = "{0}")
    void test_getOrderDay(String descritpion, LocalDate passedDate, LocalDate expectedDate) {
        final LocalDate actual = currencyExchangeDaysTracer.getOrderDateForDay(passedDate);
        Assertions.assertEquals(expectedDate, actual);
    }

    /**
     * Если вдруг {@link DayType} будет расширен каким то дополнительным типом дня (выходным или рабочим),
     * это следует не забыть учесть в логике {@link DbCbrfCurrencyOrderDayService}.
     */
    @DisplayName("Проверка консистентности с DayType")
    @Test
    void test_dayTypeConsistency() {
        final Set<DayType> actualRegionHolidayDayTypes = Arrays.stream(DayType.values())
                .filter(x -> x.isMemberOf(CalendarType.REGION_HOLIDAYS))
                .collect(Collectors.toSet());

        assertThat(
                actualRegionHolidayDayTypes, containsInAnyOrder(
                        DayType.REGION_WORKDAY,
                        DayType.REGION_WORKING_WEEKEND,
                        DayType.REGION_MOVED_WEEKEND,
                        DayType.REGION_HOLIDAY,
                        DayType.REGION_WEEKEND
                )
        );
    }

}