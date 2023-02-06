package ru.yandex.market.hrms.e2etests.steps;

import io.qameta.allure.Step;

public class HrmsUiSteps extends AbstractHrmsStep {
    private HrmsOvertimesSteps overtimesSteps = new HrmsOvertimesSteps();

    public HrmsOvertimesSteps overtimes() {
        return overtimesSteps;
    }

    @Step("Проверяем, что мы успешно залогинились")
    public HrmsUiSteps checkLoginWasSuccessfull() {
        open();
        return this;
    }
}
