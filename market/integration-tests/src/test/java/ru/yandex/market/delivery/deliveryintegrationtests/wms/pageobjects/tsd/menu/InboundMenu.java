package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class InboundMenu extends AbstractTsdPage {

    @Name("0 Приёмка по местам")
    @FindBy(xpath = "//button[text() = 'Приёмка по местам']")
    private HtmlElement inboundByPlacesButton;

    @Name("1 Приёмка")
    @FindBy(xpath = "//button[text() = 'Приёмка']")
    private HtmlElement inboundButton;

    @Name("2 Приёмка брака")
    @FindBy(xpath = "//button[text() = 'Приёмка брака']")
    private HtmlElement defectButton;

    @Name("3 Возвраты")
    @FindBy(xpath = "//button[text() = 'Возвраты']")
    private HtmlElement returnButton;

    @Name("4 Настройка товара")
    @FindBy(xpath = "//button[text() = 'Настройка товара']")
    private HtmlElement goodsSetupButton;

    @Name("6 Отмена приёмки")
    @FindBy(xpath = "//button[text() = 'Отмена приёмки']")
    private HtmlElement inboundCancelButton;

    public InboundMenu(WebDriver driver) {
        super(driver);
    }

    @Step("Меню: Приёмка по местам")
    public void inboundByPlacesButtonClick() {
        inboundByPlacesButton.click();
    }

    @Step("Меню: Приёмка")
    public void inboundButtonClick() {
        inboundButton.click();
    }

    @Step("Меню: Приёмка брака")
    public void defectButton() {
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

    @Step("Меню: Отмена приёмки")
    public void inboundCancelButton() { inboundCancelButton.click(); }
}
