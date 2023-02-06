package ru.yandex.market.hrms.e2etests.steps;

import com.codeborne.selenide.Selenide;
import io.qameta.allure.Step;

import ru.yandex.market.hrms.e2etests.pageobjects.hrms.ProductionCalendarPage;

public abstract class AbstractHrmsStep {
    @Step("Заходим в HRMS под тестовым пользователем")
    protected ProductionCalendarPage open() {
        return Selenide.open("https://lms-admin.tst.market.yandex-team.ru/hrms/",
                ProductionCalendarPage.class
        );
    }
}
