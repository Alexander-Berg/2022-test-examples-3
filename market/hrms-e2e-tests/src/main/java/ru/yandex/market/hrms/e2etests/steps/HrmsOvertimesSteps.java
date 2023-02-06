package ru.yandex.market.hrms.e2etests.steps;

import java.time.ZonedDateTime;

import io.qameta.allure.Description;
import io.qameta.allure.Step;

import ru.yandex.market.hrms.e2etests.pageobjects.hrms.OvertimesCreateShiftPopup;

public class HrmsOvertimesSteps extends AbstractHrmsStep {
    @Step("Создаем Подработку")
    @Description("""
            - Создаем подработку
            - Проверяем, что подработка появилась в календаре""")
    public void createOvertimeShift(ZonedDateTime shiftStart, ZonedDateTime shiftEnd) {
        open()
                .leftMenu()
                .clickSettings()
                .clickOvertimes()
                .createShiftButtonClick()
                .inputShiftStart(shiftStart)
                .inputShiftEnd(shiftEnd)
                .selectOvertimeReason(OvertimesCreateShiftPopup.OvertimeReason.PROCESS_URGENT_TASKS)
                .clickReadyButton();
    }

    @Step("Проверяем, что есть Подработка с заданным промежутком")
    public void assertOvertimeShiftExists(ZonedDateTime shiftStart, ZonedDateTime shiftEnd) {
        open()
                .leftMenu()
                .clickSettings()
                .clickOvertimes()
                .filterByDate(shiftStart)
                .checkShiftExists(shiftStart, shiftEnd);
    }
}
