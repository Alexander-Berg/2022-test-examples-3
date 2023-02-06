package ru.yandex.market.hrms.e2etests.pageobjects.hrms.leftmenu;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.hrms.e2etests.pageobjects.hrms.OvertimesPage;

public class LeftMenuSettingsSubmenu {

    @FindBy(xpath = "//aside//a[@role='button' and @title='Подработки']")
    private SelenideElement overtimes;

    @Step("Кликаем на меню Подработки")
    public OvertimesPage clickOvertimes() {
        overtimes.click();
        return Selenide.page(OvertimesPage.class);
    }
}
