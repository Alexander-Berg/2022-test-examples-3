package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.MenuPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;

public class Nok {
    private WebDriver driver;
    private MenuPage menuPage;
    private NotificationDialog notificationDialog;

    public Nok(WebDriver driver) {
        this.driver = driver;
        this.menuPage = new MenuPage(driver);
        this.notificationDialog = new NotificationDialog(driver);
    }

    @Step("Создаем транспортный ордер для тары с приемки без ТО на первый этаж")
    public void recreateTransportOrderForReceiving(String inboundTable, String totId) {
        menuPage.inputNokPath()
                .recreateTask()
                .inputTable(inboundTable)
                .inputContainer(totId)
                .recreateToForReceiving()
                .chooseFirstFloorZone();

        Assertions.assertTrue(notificationDialog.isPresentWithTitle("Задание конвейера приёмки пересоздано"),
                "Не появился диалог об успешном пересоздании транспортного ордера");

        notificationDialog.waitUntilHidden();
    }

    @Step("Создаем транспортный ордер для тары из зоны мезонина")
    public void recreateTransportOrderFromMezonin(String placementBufForFirstFloor, String totId) {
        menuPage.inputNokPath()
                .recreateTask()
                .inputBufferLoc(placementBufForFirstFloor)
                .inputContainer(totId)
                .recreateToForReceiving()
                .chooseSecondFloorZone();

        Assertions.assertTrue(notificationDialog.isPresentWithTitle("Задание конвейера приёмки пересоздано"),
                "Не появился диалог об успешном пересоздании транспортного ордера");

        notificationDialog.waitUntilHidden();
    }

    @Step("Перемещаем флипбокс в пустой родительский контейнер")
    public void replaceFlipBetweenParentEmptyContainer(String totId, String flipboxId) {
        menuPage.inputNokPath()
                .changeParent()
                .enterParentCart(totId)
                .enterFlip(flipboxId);

        Assertions.assertTrue(notificationDialog.isPresentWithTitle("Товар привязан к таре " + totId),
                "Не появился диалог об успешной привязке к таре "+ totId);

        notificationDialog.waitUntilHidden();
    }

}
