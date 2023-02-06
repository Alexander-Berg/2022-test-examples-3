package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class InboundMenu extends AbstractTsdPage {

    @Name("1 Приёмка")
    @FindBy(xpath = "//button[text() = 'Приёмка']")
    public HtmlElement inboundButton;

    @Name("2 Приёмка брака")
    @FindBy(xpath = "//button[text() = 'Приёмка брака']")
    public HtmlElement defectButton;

    @Name("3 Возвраты")
    @FindBy(xpath = "//button[text() = 'Возвраты']")
    public HtmlElement returnButton;

    @Name("4 Настройка товара")
    @FindBy(xpath = "//button[text() = 'Настройка товара']")
    public HtmlElement goodsSetupButton;

    public InboundMenu(WebDriver driver) {
        super(driver);
    }

    @Step("Меню: Приёмка")
    public void inboundButtonClick () {
        inboundButton.click();
    }

    @Step("Меню: Приёмка брака")
    public void defectButton () {
        defectButton.click();
    }

    @Step("Меню: Возвраты")
    public void returnButton () {
        returnButton.click();
    }

    @Step("Меню: Настройка товара")
    public void goodsSetupButton() {
        goodsSetupButton.click();
    }
}
