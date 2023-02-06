package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.wave;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;

import static com.codeborne.selenide.Selectors.byXpath;

public class PartReservedListModal extends ModalWindow {
    private NotificationDialog notificationDialog;
    @FindBy(xpath = "//*[@data-e2e='REPLENISHMENT']")
    private SelenideElement replenishButton;
    @FindBy(xpath = "//*[@data-e2e='DELETE']")
    private SelenideElement deleteButton;
    @FindBy(xpath = "//*[@data-e2e='select_all_checkbox']")
    private SelenideElement selectAllCheckBox;

    public PartReservedListModal(WebDriver driver) {
        super(driver);
        this.notificationDialog = new NotificationDialog(driver);
    }

    @Step("Проверяем сообщение")
    public PartReservedListModal checkModalCorrect() {
        final By by = byXpath(String.format("//div[@role='dialog']//span[text()='%s']", "Резерв выполнен частично!"));
        Assertions.assertTrue(isElementPresent(by), "Не появилось сообщение о частичном резервировании волны");
        return this;
    }

    public PartReservedListModal selectReplenishAction() {
        replenishButton.click();
        return this;
    }

    public PartReservedListModal selectAllOrders() {
        selectAllCheckBox.click();
        return this;
    }

    @Step("Выбираем заказы и пополняем")
    public PartReservedListModal proceedReplenish() {
        selectReplenishAction();
        selectAllOrders();
        clickForward();

        Assertions.assertTrue(
                notificationDialog.isPresentWithTitle("Задание на пополнение создано")
                        || notificationDialog.isPresentWithTitle("Пополнение заказов начато"),
                "Не появилось сообщение о пополнении заказов");

        return this;
    }
}
