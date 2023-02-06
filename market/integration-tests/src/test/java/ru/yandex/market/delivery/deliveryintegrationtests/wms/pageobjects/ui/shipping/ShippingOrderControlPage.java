package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shipping;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.TablePreloader;

public class ShippingOrderControlPage extends AbstractPage {

    @FindBy(xpath = "//button[@data-e2e='button_row_0_header']")
    private SelenideElement shipButton;
    @FindBy(xpath = "//div[@data-e2e='orderKey_filter']//input")
    private SelenideElement orderKeyFilterSelector;
    @FindBy(xpath = "//td[starts-with(@data-e2e,'orderKey_cell')]//span")
    private SelenideElement orderKey;
    @FindBy(xpath = "//td[starts-with(@data-e2e,'checkbox_cell')]//input")
    private SelenideElement orderCheckbox;


    private final NotificationDialog notificationDialog;
    private final TablePreloader tablePreloader;

    public ShippingOrderControlPage(WebDriver driver) {
        super(driver);
        wait.until(ExpectedConditions.urlMatches("orderShipping"));
        notificationDialog = new NotificationDialog(driver);
        tablePreloader = new TablePreloader(driver);
    }

    @Step("Отгружаем заказ {orderKey}")
    public void shipOrderByOrderKey(String orderKey) {
        inputValueIntoFieldByPath(orderKey);
        enableCheckboxForOrder();
        shipOrder(orderKey);
    }

    @Step("Вводим значение '{value}' в фильтр по номеру заказа")
    private void inputValueIntoFieldByPath(String value) {
        orderKeyFilterSelector.click();
        performInputInActiveElement(orderKeyFilterSelector, value);
        tablePreloader.waitUntilHidden();
        Assertions.assertEquals(value, getOrderKey(), "Найден неверный заказ");
    }

    @Step("Помечаем найденный заказ для отгрузки")
    private void enableCheckboxForOrder() {
        if (orderCheckbox.getAttribute("checked") == null) {
            orderCheckbox.click();
        }

        Assertions.assertNotNull(orderCheckbox.getAttribute("checked"),
                "Нет отмеченного для отгрузки заказа");
    }

    @Step("Отгружаем выбранный заказ")
    private void shipOrder(String orderKey) {
        shipButton.click();

        ModalWindow modalWindow = new ModalWindow(driver);
        modalWindow.waitModalVisible();
        modalWindow.clickSubmit();

        Assertions.assertTrue(notificationDialog.isPresentWithTitle("Отгрузка " + orderKey + " прошла успешно"),
                "Не появилось сообщения о успешной отгрузке");
    }

    private String getOrderKey() {
        return orderKey.getText().trim();
    }
}
