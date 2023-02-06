package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class MeasuringDevicePage extends AbstractTsdPage {

    @Name("Поле ввода устройства обмера")
    @FindBy(id="dimhost_0")
    private HtmlElement inputField;

    public MeasuringDevicePage(WebDriver driver) {
        super(driver);
    }

    public void enterEquipId () {
        enterEquipId("");
    }

    @Step("Ввод номера устройства обмера")
    public void enterEquipId (String id) {
        inputField.sendKeys(id);
        inputField.sendKeys(Keys.ENTER);
        waitSpinnerIfPresent();
    }
}
