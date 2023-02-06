package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class ComplectationMenu extends AbstractTsdPage {

    @Name("0 ЗДЧ отбора")
    @FindBy(xpath = "//button[text() = 'ЗДЧ отбора']")
    private HtmlElement complectationTaskButton;

    public ComplectationMenu(WebDriver driver) {
        super(driver);
    }

    @Step("Меню: ЗДЧ отбора")
    public void complectationTaskButtonClick () {
        complectationTaskButton.click();
    }

}
