package ru.yandex.market.hrms.core.service.employeebonus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.domain.repo.Domain;
import ru.yandex.market.hrms.core.service.payment.EmployeeEventsPieceworkAwardCalculateService;
import ru.yandex.market.hrms.model.domain.DomainType;

@DbUnitDataSet(before = {"schedules.csv", "CalculateCommon.before.csv"})
public class EmployeeDayBonusFfcCalculatorServiceTest extends AbstractCoreTest {

    @SpyBean
    EmployeeEventsPieceworkAwardCalculateService employeeEventsPieceworkAwardCalculateService;

    @AfterEach
    void init() {
        Mockito.reset(employeeEventsPieceworkAwardCalculateService);
    }

    @Test
    @DbUnitDataSet(before = "CalculateSysOper.before.csv",
            after = "CalculateSysOper.after.csv")
    void calculateBonus() {
        employeeEventsPieceworkAwardCalculateService.calculateEventsPieceworkAward(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2021, 11, 23),
                employeeEventsPieceworkAwardCalculateService.getDomains(),
                false
        );
    }

    @Test
    @DbUnitDataSet(before = "CalculateSysOperWithShelfLife.before.csv",
            after = "CalculateSysOperWithShelfLife.after.csv")
    void calculateBonusWithShelfLife() {
        employeeEventsPieceworkAwardCalculateService.calculateEventsPieceworkAward(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2021, 11, 23),
                employeeEventsPieceworkAwardCalculateService.getDomains(),
                false
        );
    }

    @Test
    @DbUnitDataSet(before = "CalculateWmsOper.before.csv",
            after = "CalculateWmsOper.after.csv")
    void calculateBonusWithNpo() {
        employeeEventsPieceworkAwardCalculateService.calculateEventsPieceworkAward(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2021, 11, 23),
                employeeEventsPieceworkAwardCalculateService.getDomains(),
                false
        );
    }

    @Test
    @DbUnitDataSet(before = "CalculateHermes.before.csv",
            after = "CalculateHermes.after.csv")
    void calculateBonusWithNpoAndHermes() {
        employeeEventsPieceworkAwardCalculateService.calculateEventsPieceworkAward(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2021, 11, 23),
                employeeEventsPieceworkAwardCalculateService.getDomains(),
                false
        );
    }

    @Test
    @DbUnitDataSet(before = "CalculateManyUnits.before.csv",
            after = "CalculateManyUnits.after.csv")
    void calculateBonusWithManyUnits() {
        employeeEventsPieceworkAwardCalculateService.calculateEventsPieceworkAward(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2021, 11, 23),
                employeeEventsPieceworkAwardCalculateService.getDomains(),
                false
        );
    }

    @Test
    @DbUnitDataSet(before = "CheckShiftTypeSof.before.csv",
            after = "CheckShiftTypeSof.after.csv")
    void checkShiftTypeSof() {

        Mockito.when(employeeEventsPieceworkAwardCalculateService.getDomains())
                .thenReturn(Map.of(Domain.builder()
                        .id(1L)
                        .timezone(ZoneId.of("Europe/Moscow"))
                        .type(DomainType.FFC)
                        .localStartTime(LocalTime.parse("08:00:00"))
                        .build(), LocalDate.MIN));

        employeeEventsPieceworkAwardCalculateService.calculateEventsPieceworkAward(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2021, 11, 23),
                employeeEventsPieceworkAwardCalculateService.getDomains(),
                false
        );
    }

    @Test
    @DbUnitDataSet(before = "CheckShiftTypeNsk.before.csv",
            after = "CheckShiftTypeNsk.after.csv")
    void checkShiftTypeNsk() {
        Mockito.when(employeeEventsPieceworkAwardCalculateService.getDomains())
                .thenReturn(Map.of(
                        Domain.builder()
                                .id(38L)
                                .timezone(ZoneId.of("Asia/Novosibirsk"))
                                .type(DomainType.FFC)
                                .localStartTime(LocalTime.parse("08:00:00"))
                                .build(), LocalDate.MIN,
                        Domain.builder()
                                .id(1L)
                                .timezone(ZoneId.of("Europe/Moscow"))
                                .type(DomainType.FFC)
                                .localStartTime(LocalTime.parse("08:00:00"))
                                .build(), LocalDate.MIN
                ));

        employeeEventsPieceworkAwardCalculateService.calculateEventsPieceworkAward(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2021, 11, 23),
                employeeEventsPieceworkAwardCalculateService.getDomains(),
                false
        );
    }

    @Test
    @DbUnitDataSet(before = "CheckTimeSeparation.before.csv",
            after = "CheckTimeSeparation.after.csv")
    void checkTimeSeparation() {
        Mockito.when(employeeEventsPieceworkAwardCalculateService.getDomains())
                .thenReturn(Map.of(
                        Domain.builder()
                                .id(1L)
                                .timezone(ZoneId.of("Europe/Moscow"))
                                .type(DomainType.FFC)
                                .localStartTime(LocalTime.parse("08:00:00"))
                                .build(), LocalDate.MIN
                ));

        employeeEventsPieceworkAwardCalculateService.calculateEventsPieceworkAward(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2022, 2, 8),
                employeeEventsPieceworkAwardCalculateService.getDomains(),
                false
        );
    }

    @Test
    @DbUnitDataSet(before = "IgnorePosition.before.csv",
            after = "IgnorePositions.after.csv")
    void checkPositionIgnoreBounds() {
        Mockito.when(employeeEventsPieceworkAwardCalculateService.getDomains())
                .thenReturn(Map.of(
                        Domain.builder()
                                .id(1L)
                                .timezone(ZoneId.of("Europe/Moscow"))
                                .type(DomainType.FFC)
                                .localStartTime(LocalTime.parse("08:00:00"))
                                .build(), LocalDate.MIN
                ));

        employeeEventsPieceworkAwardCalculateService.calculateEventsPieceworkAward(
                LocalDate.of(2021, 10, 1),
                LocalDate.of(2022, 4, 1),
                employeeEventsPieceworkAwardCalculateService.getDomains(),
                false
        );
    }

    @Test
    @DbUnitDataSet(before = "CalculateWmsNpoByHour.before.csv",
            after = "CalculateWmsNpoByHour.after.csv")
    void calculateBonusForWmsNpoByHour() {
        employeeEventsPieceworkAwardCalculateService.calculateEventsPieceworkAward(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2021, 11, 23),
                employeeEventsPieceworkAwardCalculateService.getDomains(),
                false
        );
    }

    @Test
    @DbUnitDataSet(before = "CalculateWmsNpoByHourWithHermesNpo.before.csv",
            after = "CalculateWmsNpoByHourWithHermesNpo.after.csv")
    void calculateBonusForWmsNpoByHourWithHermesNpo() {
        employeeEventsPieceworkAwardCalculateService.calculateEventsPieceworkAward(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2021, 11, 23),
                employeeEventsPieceworkAwardCalculateService.getDomains(),
                false
        );
    }

    @Test
    @DbUnitDataSet(before = "CutExtraWmsNpo.before.csv",
            after = "CutExtraWmsNpo.after.csv")
    void calculateBonusForWmsNpoByHourCutExtraHours() {
        employeeEventsPieceworkAwardCalculateService.calculateEventsPieceworkAward(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2021, 11, 23),
                employeeEventsPieceworkAwardCalculateService.getDomains(),
                false
        );
    }

    @Test
    @DbUnitDataSet(before = "CutExtraHermesNpo.before.csv",
            after = "CutExtraHermesNpo.after.csv")
    void calculateBonusForHermesNpoCutExtraHours() {
        employeeEventsPieceworkAwardCalculateService.calculateEventsPieceworkAward(
                LocalDate.of(2021, 11, 23),
                LocalDate.of(2021, 11, 23),
                employeeEventsPieceworkAwardCalculateService.getDomains(),
                false
        );
    }
}
