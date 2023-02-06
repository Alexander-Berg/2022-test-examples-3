package ru.yandex.market.tpl.billing.service;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.pvz.client.billing.PvzClient;
import ru.yandex.market.pvz.client.billing.dto.BillingPickupPointWorkingDaysDto;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * Тесты для {@link CalcPvzMonthThresholdService}
 */
class CalcPvzMonthThresholdServiceTest extends AbstractFunctionalTest {

    @Autowired
    private PvzClient pvzClient;

    @Autowired
    private CalcPvzMonthThresholdService calcPvzMonthThresholdService;

    @BeforeEach
    void setup() {
        LocalDate lastDate = LocalDate.of(2021, Month.SEPTEMBER, 5);
        Set<LocalDate> workingDates = lastDate
                .withDayOfMonth(1)
                .datesUntil(lastDate.plusDays(1))
                .collect(Collectors.toSet());

        doReturn(
                new BillingPickupPointWorkingDaysDto(
                        Map.of(1L, workingDates)
                ))
                .when(pvzClient)
                .getPickupPointWorkingDays(any(LocalDate.class), any(LocalDate.class));
    }

    private void setupNotWorkingDays() {
        Set<LocalDate> workingDates = Set.of(
                LocalDate.of(2021, Month.SEPTEMBER, 1),
                LocalDate.of(2021, Month.SEPTEMBER, 2),
                LocalDate.of(2021, Month.SEPTEMBER, 5)
        );

        doReturn(
                new BillingPickupPointWorkingDaysDto(
                        Map.of(1L, workingDates)
                ))
                .when(pvzClient)
                .getPickupPointWorkingDays(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("Ни один из пвз не пробил лимит (потому что нет записей)")
    @DbUnitDataSet(
            before = "/database/service/calcpvzmonththresholdservice/before/calcThresholdForDateWithNoLimitExceed.csv",
            after ="/database/service/calcpvzmonththresholdservice/after/calcThresholdForDateWithNoLimitExceed.csv")
    void calcThresholdForDateWithNoLimitExceed() {
        calcPvzMonthThresholdService.calcThresholdForDate(LocalDate.of(2021, Month.SEPTEMBER, 5));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/calcpvzmonththresholdservice/before/calcThresholdLimitExceedForOnePp.csv",
            after ="/database/service/calcpvzmonththresholdservice/after/calcThresholdLimitExceedForOnePp.csv")
    void calcThresholdLimitExceedForOnePp() {
        calcPvzMonthThresholdService.calcThresholdForDate(LocalDate.of(2021, Month.SEPTEMBER, 5));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/calcpvzmonththresholdservice/before/testRecalculateWithExistingLimit.csv",
            after ="/database/service/calcpvzmonththresholdservice/after/testRecalculateWithExistingLimit.csv")
    void testRecalculateWithExistingLimit() {
        calcPvzMonthThresholdService.calcThresholdForDate(LocalDate.of(2021, Month.SEPTEMBER, 5));
    }

    @Test
    @DisplayName("Начисляем только за дни, когда пвз работал")
    @DbUnitDataSet(
            before = "/database/service/calcpvzmonththresholdservice/before/testRecalculateWithNotWorkingDays.csv",
            after ="/database/service/calcpvzmonththresholdservice/after/testRecalculateWithNotWorkingDays.csv")
    void testRecalculateWithNotWorkingDays() {
        setupNotWorkingDays();
        calcPvzMonthThresholdService.calcThresholdForDate(LocalDate.of(2021, Month.SEPTEMBER, 5));
    }
}
