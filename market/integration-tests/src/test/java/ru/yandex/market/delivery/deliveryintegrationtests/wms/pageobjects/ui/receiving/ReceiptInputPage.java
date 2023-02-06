package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.initialReceiving.QualityAttributesPage;

import com.codeborne.selenide.SelenideElement;

public class ReceiptInputPage extends AbstractInputPage {

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    @FindBy(xpath = "//button[@data-e2e='with-default-button']")
    private SelenideElement defaultCartButton;

    public ReceiptInputPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим паллету")
    public ReceiptInputPage enterPallet(String pallet) {
        performInput(pallet);
        return this;
    }

    @Step("Вводим ШК грузоместа или номер поставки")
    public ReceiptInputPage enterExternalRequestId(String externalRequestId) {
        performInput(externalRequestId);
        return this;
    }

    @Step("Нажимаем кнопку: Без дефолтной тары")
    public BarcodeInputPage clickForwardButton() {
        forward.click();
        return new BarcodeInputPage(driver);
    }

    @Step("Нажимаем кнопку: Без дефолтной тары при попалетной первичке")
    public QualityAttributesPage clickForwardButtonForPalletFlow() {
        forward.click();
        return new QualityAttributesPage(driver);
    }

    @Step("Нажимаем кнопку: С Дефолтной тарой")
    public DefaultContainerInputPage clickDefaultCartButton() {
        defaultCartButton.click();
        return new DefaultContainerInputPage(driver);
    }

    @Override
    protected String getUrl() {
        return "receiptInput";
    }
}
