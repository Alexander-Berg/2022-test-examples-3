package ru.yandex.market.hrms.e2etests.pageobjects.hrms.leftmenu;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.support.FindBy;

public class LeftMenu {

    @FindBy(xpath = "//aside//button[@title='Настройки']")
    private SelenideElement settings;

    @Step("Кликаем на меню Настройки")
    public LeftMenuSettingsSubmenu clickSettings() {
        settings.click();
        return Selenide.page(LeftMenuSettingsSubmenu.class);
    }
}
