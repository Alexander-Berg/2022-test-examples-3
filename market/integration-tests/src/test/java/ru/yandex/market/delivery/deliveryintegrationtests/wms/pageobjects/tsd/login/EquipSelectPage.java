package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.login;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class EquipSelectPage extends AbstractTsdPage {

    @Name("Поле ввода оборудования")
    @FindBy(id="eqid_0")
    private HtmlElement inputField;

    public EquipSelectPage(WebDriver driver) {
        super(driver);
    }

    public void EnterEquipId () {
        EnterEquipId("");
    }

    @Step("Ввод номера оборудования")
    public void EnterEquipId (String id) {
        inputField.sendKeys(id);
        inputField.sendKeys(Keys.ENTER);
        waitSpinnerIfPresent();
    }
}
