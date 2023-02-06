package ru.yandex.market.hrms.core.service.shift;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.shift.EmployeeShiftResponse;

@RequiredArgsConstructor
@DbUnitDataSet(before = "EmployeeShiftServiceTest.GetEmployeesInShift.csv")
public class EmployeeShiftServiceTest extends AbstractCoreTest {

    @Autowired
    private EmployeeShiftService employeeShiftService;

    @Test
    public void getEmployeesInDayShift()  {
        List<EmployeeShiftResponse> actual = List.of(
                new EmployeeShiftResponse("sof-aleurbobyk", "Кладовщик", Instant.parse("2022-02-24T05:00:00Z"),
                        Instant.parse("2022-02-24T17:00:00Z"), "СОФЬИНО_2/2 ДЕНЬ\\НОЧЬ ПО 11 ЧАСОВ", null, null, null, true),
                new EmployeeShiftResponse("sof-elenavlad", "Кладовщик", Instant.parse("2022-02-24T05:00:00Z"),
                        Instant.parse("2022-02-24T17:00:00Z"), "СОФЬИНО_2/2 ДЕНЬ\\НОЧЬ ПО 11 ЧАСОВ", null, null, null, true),
                new EmployeeShiftResponse("sof-juli78", "Кладовщик", Instant.parse("2022-02-24T05:00:00Z"),
                        Instant.parse("2022-02-24T17:00:00Z"), "СОФЬИНО_2/2 ДЕНЬ\\НОЧЬ ПО 11 ЧАСОВ", null, null, null, true),
                new EmployeeShiftResponse("sof-slav-sam", "Оператор системы управления складом", Instant.parse("2022-02-24T05:15:00Z"),
                        Instant.parse("2022-02-24T17:15:00Z"), "2/2 ПО 11 Ч.", null, null, null, true)
        );

        List<EmployeeShiftResponse> employeesInShifts = employeeShiftService.getEmployeesInShift(
                "SOF", Instant.parse("2022-02-24T05:00:00.00Z"), Instant.parse("2022-02-24T15:00:00.00Z"));
        Assertions.assertTrue(employeesInShifts.size() == actual.size()
                && actual.containsAll(employeesInShifts) && employeesInShifts.containsAll(actual));
    }

}
