package ru.yandex.market.hrms.core.domain.employee;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.employee.EmployeeTimexService;
import ru.yandex.market.hrms.core.service.timex.TimexApiFacadeNew;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
@DbUnitDataSet(before = "EmployeeServiceYtDataTest.before.csv")
public class EmployeeTimexServiceTest extends AbstractCoreTest {

    @MockBean
    private TimexApiFacadeNew timexApiFacadeNew;

    @Autowired
    private EmployeeTimexService employeeTimexService;

    @Test
    @DbUnitDataSet(after = "EmployeeTimexServiceTest.DeleteFromTimex.after.csv")
    public void deleteFromTimex() {
        try {
            doNothing().when(timexApiFacadeNew).removeEmployee(any());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mockClock(LocalDate.of(2022, 7, 1));
        employeeTimexService.deleteEmployeeFromTimex(5581L, "kuk");
    }
}
