package ru.yandex.market.hrms.e2etests.overtimes;

import io.qameta.allure.Description;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.hrms.e2etests.AbstractSelenuimTest;
import ru.yandex.market.hrms.e2etests.tools.DateUtil;

@DisplayName("HRMS e2e Tests")
public class OvertimesTest extends AbstractSelenuimTest {

    @Test
    @DisplayName("Создание заявки на подработку")
    @Description("""
            - Создаем подработку
            - Проверяем, что подработка с заданным промежутком появилась в календаре""")
    public void CreateOvertimeTaskTest() {
        var shiftStart = DateUtil.now();
        var shiftEnd = DateUtil.now().plusDays(1);

        hrmsUi.overtimes().createOvertimeShift(shiftStart, shiftEnd);
        hrmsUi.overtimes().assertOvertimeShiftExists(shiftStart, shiftEnd);
    }
}
