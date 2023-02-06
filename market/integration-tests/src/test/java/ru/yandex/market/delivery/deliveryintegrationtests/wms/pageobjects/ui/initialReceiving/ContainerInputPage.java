package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.initialReceiving;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;
import com.codeborne.selenide.SelenideElement;

public class ContainerInputPage extends AbstractInputPage {

    @FindBy(xpath = "//button[@datae2e='end-receiving']")
    private SelenideElement endReceivingButton;

    @FindBy(xpath = "//button[@datae2e='bad-container']")
    private SelenideElement undefinedBoxButton;

    @Override
    protected String getUrl() {
        return "containerInput";
    }

    public ContainerInputPage (WebDriver driver) {
        super(driver);
    }

    @Step("Вводим номер тары")
    public QualityAttributesPage enterContainerId (String containerId) {
        super.performInput(containerId);

        return new QualityAttributesPage(driver);
    }

    @Step("Вводим номер дропшиповой коробки")
    public void enterNotAllowedContainerId (String containerId) {
        super.performInput(containerId);
        Assertions.assertTrue(notificationDialog.isPresentWithTitle("Невозможно принять коробку."),
                "Не появилось сообщение о дропшип коробке");
    }

    @Step("Вводим номер неизвестной коробки")
    public ContainerInputPage enterUndefinedContainerId (String containerId) {
        super.performInput(containerId);
        Assertions.assertTrue(notificationDialog.isPresentWithTitle("Тара не найдена в поставке"),
                "Не появилось сообщение о неизвестной коробке");
        return new ContainerInputPage(driver);
    }

    @Step("Нажимаем кнопку \"Неизвестная коробка\"")
    public ContainerInputPage clickUndefinedBoxButton() {
        undefinedBoxButton.click();
        Assertions.assertTrue(notificationDialog.IsPresentWithMessageWithCustomTimeout("Она будет принята в допоставке", 20),
                "Не появилось сообщение о допоставке неизвестной коробки");

        return new ContainerInputPage(driver);
    }

    @Step("Нажимаем на кнопку \"Завершить приёмку\"")
    public ReceiptInputPage endInitialReceiving() {
        endReceivingButton.click();
        ModalWindow modalWindow = new ModalWindow(driver);
        modalWindow.waitModalVisible();
        modalWindow.clickForward();
        return new ReceiptInputPage(driver);
    }
}
