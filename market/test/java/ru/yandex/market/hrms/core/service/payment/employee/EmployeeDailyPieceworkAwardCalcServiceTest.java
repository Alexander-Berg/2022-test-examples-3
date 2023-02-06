package ru.yandex.market.hrms.core.service.payment.employee;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.payment.EmployeeDailyQualityFactorCalculateService;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

@DbUnitDataSet(before = "CalculateDailyPieceworkAward.before.csv")
public class EmployeeDailyPieceworkAwardCalcServiceTest extends AbstractCoreTest {

    @Autowired
    EmployeeDailyQualityFactorCalculateService employeeDailyQualityFactorCalculateService;

    @Test
    @DbUnitDataSet(before = "HappyPathWithoutViolations.before.csv", after = "HappyPathWithoutViolations.after.csv")
    void happyPathWithoutViolations() {
        mockClock(LocalDate.of(2022, 2, 1));
        LocalDateInterval interval = new LocalDateInterval(LocalDate.of(2022, 1, 19), LocalDate.of(2022, 1, 24));
        employeeDailyQualityFactorCalculateService.calculateDailyQualityFactor(interval, Set.of(1L, 38L));
    }

    @Test
    @DbUnitDataSet(before = {"HappyPathWithoutViolations.before.csv", "HappyPathWithViolations.before.csv"},
            after = "HappyPathWithViolations.after.csv")
    void happyPathWithViolations() {
        mockClock(LocalDate.of(2022, 2, 1));
        LocalDateInterval interval = new LocalDateInterval(LocalDate.of(2022, 1, 19), LocalDate.of(2022, 1, 24));
        employeeDailyQualityFactorCalculateService.calculateDailyQualityFactor(interval, Set.of(1L, 38L));
    }

    @Test
    @DbUnitDataSet(before = {"HappyPathWithoutViolations.before.csv", "DailyAwardUpdateOld.before.csv"},
            after = "DailyAwardUpdateOld.after.csv")
    void dailyAwardUpdateOld() {
        mockClock(LocalDate.of(2022, 2, 1));
        LocalDateInterval interval = new LocalDateInterval(LocalDate.of(2022, 1, 19), LocalDate.of(2022, 1, 24));
        employeeDailyQualityFactorCalculateService.calculateDailyQualityFactor(interval, Set.of(1L, 38L));
    }

    @Test
    @DbUnitDataSet(before = {"HappyPathWithoutViolations.before.csv",
            "IgnoreViolationsWithoutExplanatoryNote.before.csv"},
            after = "IgnoreViolationsWithoutExplanatoryNote.after.csv")
    void ignoreViolationsWithoutExplanatoryNote() {
        mockClock(LocalDate.of(2022, 2, 1));
        LocalDateInterval interval = new LocalDateInterval(LocalDate.of(2022, 1, 19), LocalDate.of(2022, 1, 24));
        employeeDailyQualityFactorCalculateService.calculateDailyQualityFactor(interval, Set.of(1L, 38L));
    }
}
