package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.outgoing.outgoingorder;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wave.execution.OrderWithoutWavePage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import java.util.Collections;
import java.util.List;

public class OutgoingOrderPage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(OutgoingOrderPage.class);

    @Name("Кнопка фильтрации")
    @FindBy(xpath = "//input[@id = '$v2ewwt_filterbutton']")
    private HtmlElement filterButton;

    @Name("Поле фильтрации по номеру заказа")
    @FindBy(xpath = "//input[@id = 'It4g6o8']")
    private HtmlElement orderIdField;

    @Name("Поле фильтрации по внешнему номеру заказа")
    @FindBy(xpath = "//input[@id = 'Idwqaoo']")
    private HtmlElement externOrderId;

    @Name("Поле фильтрации по Владельцу")
    @FindBy(xpath = "//input[@id = 'It4g6o8']")
    private HtmlElement vendorIdField;

    @Name("Чебокс первого заказа в списке")
    @FindBy(xpath = "//input[@id = '$v2ewwt_rowChkBox_0']")
    private HtmlElement firstOrderCheckbox;

    @Name("Иконка перехода к первому заказу в списке")
    @FindBy(xpath = "//input[@id = '$v2ewwt_cell_0_0_Img']")
    private HtmlElement firstOrderIcon;

    @Name("Поле Статус первого заказа в списке")
    @FindBy(xpath = "//span[@id = '$v2ewwt_cell_0_7_span']")
    private HtmlElement firstOrderStatusField;

    @Name("Поле Номер первого заказа в списке")
    @FindBy(xpath = "//span[@id = '$v2ewwt_cell_0_1_span']")
    private HtmlElement firstOrderId;

    @Name("Поле Номер второго заказа в списке")
    @FindBy(xpath = "//span[@id = '$v2ewwt_cell_1_1_span']")
    private HtmlElement secondOrderId;

    public OutgoingOrderPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим {0} в поле фильтрации по номеру заказа")
    public OutgoingOrderPage inputOrderId(String inboundId) {
        orderIdField.sendKeys(inboundId);
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Жмем кнопку фильтрации")
    public OutgoingOrderPage filterButtonClick() {
        filterButton.click();
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Получаем статус первого заказа в списке")
    public OrderStatus getOrderStatus() {
        return OrderStatus.get(firstOrderStatusField.getText());
    }

    @Step("Выбираем первый элемент в таблице")
    public OutgoingOrderPage selectFirstResult() {
        firstOrderCheckbox.click();

        return this;
    }

    @Step("Открываем первый элемент в таблице")
    public void openFirstOrder() {
        firstOrderIcon.click();
        overlayBusy.waitUntilHidden();
    }

    @Step("Открываем номера заказов")
    public void openSplitOrders(String externOrder){
        externOrderId.sendKeys(externOrder);

    }
    @Step("Получаем номер перовго заказа")
    public String getFirstSplitOrder(){
        return firstOrderId.getText();
    }
    @Step("Получаем номер второго заказа")
    public String getSecondSplitOrder(){
        return secondOrderId.getText();
    }
}
