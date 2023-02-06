package ru.yandex.market.hrms.core.domain.employee;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.employee.EmployeeService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EmployeeServiceTest extends AbstractCoreTest {

    @Autowired
    private EmployeeService employeeService;


    @Test
    @DbUnitDataSet(before = "EmployeeServiceYtDataTest.before.csv")
    public void testReturnsView() {
        mockClock(LocalDate.of(2021, 4, 1));
        List<EmployeeDataYtView> employeeDataForYt = employeeService.getEmployeeDataForYt();
        EmployeeDataYtView expected = EmployeeDataYtView.builder()
                .id(5581)
                .name("Табунчик Виктория")
                .staffLogin("gorshkova-vi")
                .wmsLogin("sof-vikalgorsh")
                .position("Кладовщик")
                .shiftId("3")
                .domainId(1L)
                .domainName("Склад Софьино")
                .flowGroupId(124L)
                .flowGroupName("Смены 3-4")
                .directionGroupId(null)
                .directionGroupName(null)
                .build();
        Assertions.assertFalse(employeeDataForYt.isEmpty());
        EmployeeDataYtView result = employeeDataForYt.get(0);
        EmployeeDataYtView resultSecond = employeeDataForYt.get(1);
        Assertions.assertAll(
                () -> assertEquals(expected.getId(), result.getId()),
                () -> assertEquals(expected.getName(), result.getName()),
                () -> assertEquals(expected.getStaffLogin(), result.getStaffLogin()),
                () -> assertEquals(expected.getWmsLogin(), result.getWmsLogin()),
                () -> assertEquals(expected.getPosition(), result.getPosition()),
                () -> assertEquals(expected.getShiftId(), result.getShiftId()),
                () -> assertEquals(expected.getDomainId(), result.getDomainId()),
                () -> assertEquals(expected.getDomainName(), result.getDomainName()),
                () -> assertEquals(expected.getFlowGroupId(), result.getFlowGroupId()),
                () -> assertEquals(expected.getFlowGroupName(), result.getFlowGroupName()),
                () -> assertEquals(expected.getDirectionGroupId(), result.getDirectionGroupId()),
                () -> assertEquals(expected.getDirectionGroupName(), result.getDirectionGroupName()),

                //в случае если группа удаленна структура не должна быть возвращена
                () -> assertNull(resultSecond.getFlowGroupId()),
                () -> assertNull(resultSecond.getFlowGroupName()),
                () -> assertNull(resultSecond.getDirectionGroupId()),
                () -> assertNull(resultSecond.getDirectionGroupName())

        );
    }
}
