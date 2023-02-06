package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wms.execution;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class Productivity extends AbstractWsPage {

    @Name("Задача")
    @FindBy(xpath = "//div[@id = '$ovuiid']")
    private HtmlElement task;

    public Productivity(WebDriver driver) {
        super(driver);
    }

    @Step("Выбираем меню: Задача")
    public void task() {
        safeClick(task);
    }
}
