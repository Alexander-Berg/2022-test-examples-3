package ru.yandex.direct.currency;

import java.time.LocalDate;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.qatools.allure.annotations.Description;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static ru.yandex.direct.currency.CurrencyCode.EUR;
import static ru.yandex.direct.currency.CurrencyCode.YND_FIXED;
import static ru.yandex.direct.currency.FixedRates.BEGIN_OF_TIME;

@RunWith(JUnitParamsRunner.class)
public class RateUtilsTest {

    public static Object provideCurrencyCodes() {
        return CurrencyCode.values();
    }

    @Test
    @Parameters(method = "provideCurrencyCodes")
    @Description("Проверка что для каждой валюты есть курс на текущий момент")
    public void getAll(CurrencyCode currencyCode) {
        assertNotNull(RateUtils.get(currencyCode));
    }

    @Test
    @Description("Проверка что курс берется на указанную дату, если она задана")
    public void getRateByExistingDate() throws Exception {
        Rate rate = RateUtils.get(EUR, LocalDate.of(2015, 4, 1));
        assertEquals(new Rate(LocalDate.of(2015, 4, 1), 0.39, 0.39), rate);
    }

    @Test
    @Description("Проверка что курс берется на ближайшую меньшую дату")
    public void getRateByLowerDate() throws Exception {
        Rate rate = RateUtils.get(EUR, LocalDate.of(2015, 3, 31));
        assertEquals(new Rate(LocalDate.of(2014, 12, 1), 0.45, 0.45), rate);
    }

    @Test
    @Description("Проверка что при отсутствии курса возвращается null")
    public void getNullRate() throws Exception {
        Rate rate = RateUtils.get(EUR, LocalDate.of(1969, 1, 1));
        assertNull(rate);
    }

    @Test
    @Description("Проверка что курс YND_FIXED равен 1")
    public void getYnd() throws Exception {
        Rate rate = RateUtils.get(YND_FIXED);
        assertEquals(new Rate(BEGIN_OF_TIME, 1, 1), rate);
    }
}
