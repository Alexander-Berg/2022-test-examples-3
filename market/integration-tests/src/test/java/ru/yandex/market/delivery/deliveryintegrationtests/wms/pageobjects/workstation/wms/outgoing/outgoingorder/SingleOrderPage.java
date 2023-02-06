package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.outgoing.outgoingorder;

import java.math.BigDecimal;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class SingleOrderPage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(SingleOrderPage.class);

    @Name("Поле Статус заказа")
    @FindBy(xpath = "//input[@id = 'Iti2whs']")
    private HtmlElement orderStatusField;

    @Name("Таб История статуса")
    @FindBy(xpath = "//div[@id = 'tab_EB450881F46C420DB39B1ABD625ECAC4:0~0~1~0~1_split_7']")
    private HtmlElement orderStatusHistoryTab;

    @Name("Поле Всего")
    @FindBy(xpath = "//input[@id = 'I19ik8o']")
    private HtmlElement totalOpenQtyField;

    public SingleOrderPage(WebDriver driver) {
        super(driver);
    }

    @Step("Получаем статус заказа")
    public OrderStatus getOrderStatus() {
        return OrderStatus.get(orderStatusField.getAttribute("value"));
    }

    @Step("Открываем табу История статуса")
    public void orderStatusHistory() {
        orderStatusHistoryTab.click();
        overlayBusy.waitUntilHidden();
    }

    @Step("Получаем общее кол-во товаров в заказе")
    public BigDecimal getTotalOpenQty() {
        return new BigDecimal(totalOpenQtyField.getAttribute("value"));
    }

}
