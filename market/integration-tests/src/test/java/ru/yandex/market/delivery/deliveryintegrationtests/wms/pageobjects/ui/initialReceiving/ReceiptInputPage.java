package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.initialReceiving;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class ReceiptInputPage extends AbstractInputPage {

    @Override
    protected String getUrl() {
        return "receiptInput";
    }

    public ReceiptInputPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим номер поставки")
    public QtyInputPage enterInboundId(String fulfillmentId) {
        super.performInput(fulfillmentId);

        return new QtyInputPage(driver);
    }

    @Step("Вводим номер возвратной поставки")
    public ContainerInputPage enterReturnInboundId(String fulfillmentId) {
        super.performInput(fulfillmentId);

        return new ContainerInputPage(driver);
    }

    @Step("Вводим номер грузоместа")
    public QualityAttributesPage enterContainerId(String containerId) {
        super.performInput(containerId);
        return new QualityAttributesPage(driver);
    }

    @Step("Вводим номер поставки в Аксапте")
    public QtyInputPage enterExternalRequestId(String externalRequestId) {
        super.performInput(externalRequestId);
        return new QtyInputPage(driver);
    }

    @Step("Вводим номер грузоместа при закрытой первичке")
    public ReceiptInputPage enterContainerIdWithInitiallyClosedReceipt(String fulfillmentId, String containerId) {
        super.performInput(containerId);
        Assertions.assertTrue(notificationDialog.isPresentWithTitle("Отсканирован неверный тип грузоместа"),
                "Не появился диалог с заголовком \"Отсканирован неверный тип грузоместа\"");
        return new ReceiptInputPage(driver);
    }

    @Step("Вводим номер задублированного в бд грузоместа")
    public ReceiptInputPage enterDuplicateContainerId(String containerId) {
        super.performInput(containerId);
        Assertions.assertTrue(
                notificationDialog.isPresentWithTitle("Не удалось определить поставку по номеру грузоместа"),
                "Не появился диалог с заголовком \"Не удалось определить поставку по номеру грузоместа\"");
        return new ReceiptInputPage(driver);
    }
}
