package ru.yandex.market.hrms.core.domain.bonus.repo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.bonus.EmployeeEfficiencyEntity;
import ru.yandex.market.hrms.core.service.yt.EmployeeEfficiencyService;

class SaveEmployeeEfficiencyTest extends AbstractCoreTest {
    private static final LocalDate DATE = LocalDate.parse("2022-06-12");

    @Autowired
    private EmployeeEfficiencyService employeeEfficiencyService;

    @Test
    @DbUnitDataSet(before = "SaveEmployeeEfficiencyTest.before.csv", after = "SaveEmployeeEfficiencyTest.after.csv")
    void shouldDeleteOldRecordsAndInsertNew() {
        mockClock(DATE);
        employeeEfficiencyService.saveAllRemovingOldRecords(List.of(
                EmployeeEfficiencyEntity.builder()
                        .employeeId(2L)
                        .operationDate(DATE)
                        .efficiencyRate(BigDecimal.ONE)
                        .efficiencyScore(BigDecimal.valueOf(123))
                        .build(),
                EmployeeEfficiencyEntity.builder()
                        .employeeId(3L)
                        .operationDate(DATE)
                        .efficiencyRate(BigDecimal.valueOf(3))
                        .efficiencyScore(BigDecimal.valueOf(777))
                        .build()
        ));
    }
}