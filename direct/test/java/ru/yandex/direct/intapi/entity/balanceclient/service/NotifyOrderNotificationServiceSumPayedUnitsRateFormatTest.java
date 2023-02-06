package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderNotificationService.calcSumPayedUnitsRate;

/**
 * Тесты на формат поля sumPayedUnitsRate, которое отправляем в уведомление
 * Проверяется, что округляем число правильно (до трех знаков после запятой) и не оставляем незначащих нолей в конце
 */
@RunWith(Parameterized.class)
public class NotifyOrderNotificationServiceSumPayedUnitsRateFormatTest {

    private static final long SUM_UNITS = 0;
    private static final long PRODUCT_RATE = 1;

    @Parameterized.Parameter()
    public String testDescription;

    @Parameterized.Parameter(1)
    public double sumUnits;

    @Parameterized.Parameter(2)
    public String expectedSumPayedUnitsRateAsString;

    @Parameterized.Parameters(name = "description={0}, sumUnits={1}, expectedSumPayedUnitsRateAsString={2}")
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
                {"без нолей в конце", 1000, "1000"},
                {"без нолей в конце", 1000.100, "1000.1"},
                {"округление вниз", 1.234500, "1.234"},
                {"округление вверх", 1.234501, "1.235"},
        });
    }


    @Test
    public void checkCalcSumPayedUnitsRateAsString() {
        BigDecimal sumPayedUnitsRate = calcSumPayedUnitsRate(BigDecimal.valueOf(sumUnits), SUM_UNITS, PRODUCT_RATE);
        assertThat("метод вернул ожидаемое значение - " + testDescription,
                sumPayedUnitsRate.toPlainString(), equalTo(expectedSumPayedUnitsRateAsString));
    }
}
