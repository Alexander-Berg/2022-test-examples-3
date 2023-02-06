package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.complectation;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common.WarningDialog;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class AreaSelectPage extends AbstractTsdPage {

    private WarningDialog warningDialog = new WarningDialog(driver);

    @Name("Участок")
    @FindBy(id="area1_0")
    private HtmlElement areaInputField;

    public AreaSelectPage(WebDriver driver) {
        super(driver);
    }

    public void enterArea() {
        enterArea("");
    }

    @Step("Вводим Участок")
    public void enterArea(String area) {
        areaInputField.sendKeys(area);
        areaInputField.sendKeys(Keys.ENTER);
        waitSpinner();
    }

    @Step("Проверка: Появился ли диалог 'Нет действий для выполнения'")
    public boolean noComplectationStartedWarningPresent() {
        return warningDialog.IsPresentWithMessage("Нет действий для выполнения");
    }
}
