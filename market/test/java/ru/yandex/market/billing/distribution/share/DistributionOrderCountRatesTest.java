package ru.yandex.market.billing.distribution.share;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для {@link DistributionOrderCountRates}.
 *
 * @author vbudnev
 */
class DistributionOrderCountRatesTest {
    private static final LocalDate DATE_2019_01_01 = LocalDate.of(2019, 1, 1);
    private static final LocalDate DATE_2019_01_09 = LocalDate.of(2019, 1, 9);
    private static final LocalDate DATE_2019_01_10 = LocalDate.of(2019, 1, 10);
    private static final LocalDate DATE_2019_01_11 = LocalDate.of(2019, 1, 11);
    private static final LocalDate DATE_2018_12_10 = LocalDate.of(2018, 12, 10);

    private static final BigDecimal RATE_5 = BigDecimal.valueOf(5).movePointLeft(2);
    private static final BigDecimal RATE_11 = BigDecimal.valueOf(11).movePointLeft(2);
    private static final BigDecimal RATE_12 = BigDecimal.valueOf(12).movePointLeft(2);
    private static final BigDecimal RATE_15 = BigDecimal.valueOf(15).movePointLeft(2);

    private static Stream<Arguments> argsDefRampUp() {
        return Stream.of(
                //тестовый период
                Arguments.of(0, DATE_2018_12_10, RATE_15),
                Arguments.of(100, DATE_2018_12_10, RATE_15),
                Arguments.of(1000, DATE_2018_12_10, RATE_15),
                //тарифная сетка
                Arguments.of(0, DATE_2019_01_01, RATE_5),
                Arguments.of(50, DATE_2019_01_01, RATE_5),
                Arguments.of(50, DATE_2019_01_10, RATE_5),
                Arguments.of(99, DATE_2019_01_01, RATE_5),
                Arguments.of(100, DATE_2019_01_01, RATE_11),
                Arguments.of(150, DATE_2019_01_01, RATE_11),
                Arguments.of(499, DATE_2019_01_01, RATE_11),
                Arguments.of(500, DATE_2019_01_01, RATE_12),
                Arguments.of(550, DATE_2019_01_01, RATE_12),
                Arguments.of(999, DATE_2019_01_01, RATE_12),
                Arguments.of(1000, DATE_2019_01_01, RATE_15),
                Arguments.of(2000, DATE_2019_01_01, RATE_15),
                Arguments.of(Integer.MAX_VALUE, DATE_2019_01_01, RATE_15)
        );
    }

    private static Stream<Arguments> argsExplicitRampUp() {
        return Stream.of(
                //тестовый период
                Arguments.of(50, DATE_2019_01_01, RATE_15),
                Arguments.of(100, DATE_2019_01_09, RATE_15),
                //тарифная сетка
                Arguments.of(50, DATE_2019_01_10, RATE_5),
                Arguments.of(100, DATE_2019_01_11, RATE_11),
                Arguments.of(1000, DATE_2019_01_11, RATE_15)
        );
    }

    private static Stream<Arguments> argsConstRate() {
        return Stream.of(
                Arguments.of(0, DATE_2018_12_10, RATE_15),
                Arguments.of(100, DATE_2018_12_10, RATE_15),
                Arguments.of(1000, DATE_2018_12_10, RATE_15),
                Arguments.of(0, DATE_2019_01_01, RATE_15),
                Arguments.of(50, DATE_2019_01_01, RATE_15),
                Arguments.of(50, DATE_2019_01_10, RATE_15),
                Arguments.of(99, DATE_2019_01_01, RATE_15),
                Arguments.of(100, DATE_2019_01_01, RATE_15),
                Arguments.of(150, DATE_2019_01_01, RATE_15),
                Arguments.of(499, DATE_2019_01_01, RATE_15),
                Arguments.of(500, DATE_2019_01_01, RATE_15),
                Arguments.of(550, DATE_2019_01_01, RATE_15),
                Arguments.of(999, DATE_2019_01_01, RATE_15),
                Arguments.of(1000, DATE_2019_01_01, RATE_15),
                Arguments.of(2000, DATE_2019_01_01, RATE_15),
                Arguments.of(Integer.MAX_VALUE, DATE_2019_01_01, RATE_15)
        );
    }

    @DisplayName("rates при явно заданной границе тестового периода")
    @ParameterizedTest(name = "rate for {0} and date {1}")
    @MethodSource("argsExplicitRampUp")
    void test_ratesByCount(long count, LocalDate billingDate, BigDecimal expectedRate) {
        final DistributionOrderCountRates.TariffService tariffService
                = new DistributionOrderCountRates().buildTariffService(DATE_2019_01_10);
        assertEquals(expectedRate, tariffService.calcMultiplier(count, billingDate, 0));
    }

    @DisplayName("rates при дефолтной границе тестового периода")
    @ParameterizedTest(name = "rate for {0} and date {1}")
    @MethodSource("argsDefRampUp")
    void test_ratesByCountWithDefaultRampUp(long count, LocalDate billingDate, BigDecimal expectedRate) {
        final DistributionOrderCountRates.TariffService tariffService
                = new DistributionOrderCountRates().buildTariffService();
        assertEquals(expectedRate, tariffService.calcMultiplier(count, billingDate, 0));
    }

    @DisplayName("rates для особого клида 2339849 с постоянной границей 15%")
    @ParameterizedTest(name = "rate for {0} and date {1}")
    @MethodSource("argsConstRate")
    void test_ratesByCountWithSpecialClid(long count, LocalDate billingDate, BigDecimal expectedRate) {
        final DistributionOrderCountRates.TariffService tariffService
                = new DistributionOrderCountRates().buildTariffService();
        assertEquals(expectedRate, tariffService.calcMultiplier(count, billingDate, 2339849));
    }
}