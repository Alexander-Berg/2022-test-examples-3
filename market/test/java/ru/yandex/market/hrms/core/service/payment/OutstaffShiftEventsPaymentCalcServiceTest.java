package ru.yandex.market.hrms.core.service.payment;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

@DbUnitDataSet(before = "CalculateCommon.before.csv")
public class OutstaffShiftEventsPaymentCalcServiceTest extends AbstractCoreTest {

    @Autowired
    OutstaffFfcShiftEventsPaymentCalcService outstaffDaySalaryCalcService;

    @Test
    @DbUnitDataSet(before = "OutstaffCalculateSysOper.before.csv",
            after = "OutstaffCalculateSysOper.after.csv")
    void calculateBonus() {
        outstaffDaySalaryCalcService.calculateShiftEventsPayment(new LocalDateInterval(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2021, 11, 23))
        );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffCalculateSysOperWithShelfLife.before.csv",
            after = "OutstaffCalculateSysOperWithShelfLife.after.csv")
    void calculateBonusWithShelfLife() {
        outstaffDaySalaryCalcService.calculateShiftEventsPayment(new LocalDateInterval(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2021, 11, 23))
        );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffCalculateWmsOper.before.csv",
            after = "OutstaffCalculateWmsOper.after.csv")
    void calculateBonusWithNpo() {
        outstaffDaySalaryCalcService.calculateShiftEventsPayment(new LocalDateInterval(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2021, 11, 23))
        );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffCalculateHermesNpo.before.csv",
            after = "OutstaffCalculateHermesNpo.after.csv")
    void calculateBonusWithNpoAndHermes() {
        outstaffDaySalaryCalcService.calculateShiftEventsPayment(new LocalDateInterval(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2021, 11, 23))
        );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffCalculateManyUnits.before.csv",
            after = "OutstaffCalculateManyUnits.after.csv")
    void calculateBonusWithManyUnits() {
        outstaffDaySalaryCalcService.calculateShiftEventsPayment(new LocalDateInterval(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2021, 11, 23))
        );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffCheckTimeSeparation.before.csv",
            after = "OutstaffCheckTimeSeparation.after.csv")
    void checkTimeSeparation() {
        outstaffDaySalaryCalcService.calculateShiftEventsPayment(new LocalDateInterval(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2022, 2, 8)));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffRemoveOld.before.csv",
            after = "OutstaffRemoveOld.after.csv")
    void removeOld() {
        mockClock(LocalDateTime.of(2021, 2, 8, 14, 32, 7));
        outstaffDaySalaryCalcService.calculateShiftEventsPayment(new LocalDateInterval(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2022, 2, 8)));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffUpdatePayments.before.csv",
            after = "OutstaffUpdatePayments.after.csv")
    void updatePayments() {
        mockClock(LocalDateTime.of(2021, 2, 8, 14, 32, 7));
        outstaffDaySalaryCalcService.calculateShiftEventsPayment(new LocalDateInterval(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2022, 2, 8)));
    }

}
