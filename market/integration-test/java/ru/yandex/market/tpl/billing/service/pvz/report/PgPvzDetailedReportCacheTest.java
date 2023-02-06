package ru.yandex.market.tpl.billing.service.pvz.report;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.util.DateTimeUtil;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для {@link PgPvzDetailedReportCache}
 */
public class PgPvzDetailedReportCacheTest extends AbstractFunctionalTest {
    @Autowired
    private PvzDetailedReportCache pgPvzDetailedReportCache;

    @Autowired
    private TestableClock clock;

    @DbUnitDataSet(before = "/database/service/pvz/report/PgPvzDetailedReportCache/before/testGetReports.csv")
    @ParameterizedTest(name = "[{index}]: {4}")
    @MethodSource("testGetReportsData")
    void testGetReports(
            long partnerId,
            YearMonth targetMonth,
            LocalDate today,
            boolean expectedReportExist,
            String description
    ) {
        clock.setFixed(today.atStartOfDay().toInstant(DateTimeUtil.DEFAULT_ZONE_ID), DateTimeUtil.DEFAULT_ZONE_ID);
        Optional<byte[]> report = pgPvzDetailedReportCache.getReport(
                partnerId, targetMonth
        );
        assertEquals(expectedReportExist, report.isPresent());
    }

    @Test
    @DbUnitDataSet(before = "/database/service/pvz/report/PgPvzDetailedReportCache/before/testStoreReportWithOverwrite.csv")
    @DisplayName("Тест на перегенерацию существующего отчета")
    void testStoreReportWithOverwrite() {
        clock.setFixed(Instant.parse("2022-02-11T12:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        YearMonth targetMonth = YearMonth.of(2022, Month.FEBRUARY);
        pgPvzDetailedReportCache.storeReport(1L, new byte[]{1, 2, 3}, targetMonth);

        Optional<byte[]> reportO = pgPvzDetailedReportCache.getReport(1L, targetMonth);
        assertTrue(reportO.isPresent());
        assertArrayEquals(new byte[]{1, 2, 3}, reportO.get());
    }

    private static Stream<Arguments> testGetReportsData() {
        return Stream.of(
                Arguments.of(
                        1L,
                        YearMonth.of(2022, Month.FEBRUARY),
                        LocalDate.of(2022, Month.FEBRUARY, 10),
                        true,
                        "Отчет за февраль, который был сгенерирован сегодня, существует"
                ),
                Arguments.of(
                        1L,
                        YearMonth.of(2022, Month.FEBRUARY),
                        LocalDate.of(2022, Month.FEBRUARY, 11),
                        false,
                        "Отчет за февраль, который НЕ был сгенерирован сегодня, НЕ существует"
                ),
                Arguments.of(
                        1L,
                        YearMonth.of(2022, Month.JANUARY),
                        LocalDate.of(2022, Month.FEBRUARY, 11),
                        true,
                        "Отчет за январь (запрашивается в феврале), который был сгенерирован 5 февраля, существует"
                ),
                Arguments.of(
                        2L,
                        YearMonth.of(2022, Month.JANUARY),
                        LocalDate.of(2022, Month.FEBRUARY, 11),
                        true,
                        "Отчет за январь (запрашивается в феврале), который был сгенерирован 1 февраля, существует"
                ),
                Arguments.of(
                        3L,
                        YearMonth.of(2022, Month.JANUARY),
                        LocalDate.of(2022, Month.FEBRUARY, 11),
                        false,
                        "Отчет за январь (запрашивается в феврале), который был сгенерирован 31 января, НЕ существует"
                )
        );
    }
}
