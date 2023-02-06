package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.initialReceiving;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;

import static com.codeborne.selenide.Selectors.byXpath;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving.BarcodeInputPage;

import static com.codeborne.selenide.Selenide.$;

public class QualityAttributesPage extends AbstractPage {
    private static final String QUALITY_ATTRIBUTE_XPATH_TEMPLATE = "//button[@data-e2e='%s']";
    private final NotificationDialog notificationDialog;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forwardButton;


    public QualityAttributesPage(WebDriver driver) {
        super(driver);
        this.notificationDialog = new NotificationDialog(driver);
    }

    @Step("Отмечаем атрибут качества с id {attributeId}")
    public QualityAttributesPage chooseQualityAttribute(String attributeId) {
        final By qualityAttribute = byXpath(String.format(QUALITY_ATTRIBUTE_XPATH_TEMPLATE, attributeId));
        $(qualityAttribute).click();

        return this;
    }

    @Step("Нажимаем кнопку \"Сохранить\"")
    public ContainerInputPage clickSaveButton() {
        forwardButton.click();
        Assertions.assertTrue(notificationDialog.IsPresentWithMessageWithCustomTimeout("Тара проверена", 20),
                "Не появилось сообщение об успешной проверке тары");

        return new ContainerInputPage(driver);
    }

    @Step("Нажимаем кнопку \"Сохранить\" на первичке для палетного флоу")
    public ReceiptInputPage clickSaveButtonForInitialPalletFlow() {
        forwardButton.click();
        Assertions.assertTrue(notificationDialog.IsPresentWithMessageWithCustomTimeout("Тара проверена", 20),
                "Не появилось сообщение об успешной проверке тары");

        return new ReceiptInputPage(driver);
    }

    @Step("Нажимаем кнопку \"Сохранить\" на втроичке для палетного флоу")
    public BarcodeInputPage clickSaveButtonForSecondaryPalletFlow() {
        forwardButton.click();
        return new BarcodeInputPage(driver);
    }
}
