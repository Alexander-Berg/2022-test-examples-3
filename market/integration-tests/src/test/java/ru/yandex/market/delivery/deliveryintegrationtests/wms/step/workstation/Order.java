package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.workstation;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.topcontextmenu.TopContextMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.outgoing.outgoingorder.OrderStatusHistoryPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.outgoing.outgoingorder.OutgoingOrderPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.outgoing.outgoingorder.SingleOrderPage;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Resource;

@Resource.Classpath({"wms/infor.properties"})
public class Order extends AbstractWSSteps {
    private static final Logger log = LoggerFactory.getLogger(Order.class);

    private final OutgoingOrderPage outgoingOrderPage;
    private final SingleOrderPage singleOrderPage;
    private final OrderStatusHistoryPage orderStatusHistoryPage;


    public Order(WebDriver drvr) {
        super(drvr);

        topContextMenu = new TopContextMenu(driver);
        outgoingOrderPage = new OutgoingOrderPage(driver);
        singleOrderPage = new SingleOrderPage(driver);
        orderStatusHistoryPage = new OrderStatusHistoryPage(driver);
    }

    @Step("Открыть Заказ {orderId}")
    public void openOrder(String orderId) {
        log.info("Opening Order {}", orderId);

        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().outgoing().outgoingOrder();

        outgoingOrderPage.inputOrderId(orderId);
        outgoingOrderPage.filterButtonClick();
        outgoingOrderPage.openFirstOrder();
    }

    @Step("Проверяем, что статсус заказа {orderId} = {status}")
    public void verifyOrderStatus(String orderId, OrderStatus status) {
        log.info("Verifying order {} status is {}", orderId, status);

        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().outgoing().outgoingOrder();

        outgoingOrderPage.inputOrderId(orderId);
        outgoingOrderPage.filterButtonClick();

        Assertions.assertEquals(status, outgoingOrderPage.getOrderStatus(), "Order status is wrong:");

        outgoingOrderPage.openFirstOrder();

        Assertions.assertEquals(status, singleOrderPage.getOrderStatus(), "Order status is wrong:");
    }
    @Step("Проверяем что статус заказа {orderId} = {status}")
    public void waitOrderStatusIs(String orderId, OrderStatus status) {
        Retrier.retry(() -> {
            verifyOrderStatus(orderId, status);
        }, Retrier.RETRIES_MEDIUM, Retrier.TIMEOUT_SMALL, TimeUnit.SECONDS);
    }

    //TODO Сделать здесь 2 метода получения номеров заказов
    @Step("Получаем первый номер заказа из разделенного заказ")
    public ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order getFirstSplitOrder(String externOrderId) {
        return getSplitOrder(externOrderId, OutgoingOrderPage::getFirstSplitOrder);
    }

    @Step("Получаем второй номер заказа из разделенного заказ")
    public ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order getSecondSplitOrder(String externOrderId) {
        return getSplitOrder(externOrderId, OutgoingOrderPage::getSecondSplitOrder);
    }

    private ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order getSplitOrder(
            String externOrderId,
            Function<OutgoingOrderPage, String> orderPageStringFunction
    ) {
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().outgoing().outgoingOrder();
        outgoingOrderPage.openSplitOrders(externOrderId);
        outgoingOrderPage.filterButtonClick();
        String order = orderPageStringFunction.apply(outgoingOrderPage);
        long externOrder = Long.parseLong(externOrderId);
        return new ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order(externOrder,order) ;
    }

    @Step("Проверяеим историю статусов заказа {orderId}: last = {lastStatus}, secondLast = {secondLastStatus}")
    public void checkOrderStatusHistory(String orderId, OrderStatus lastStatus, OrderStatus secondLastStatus) {
        waitOrderStatusIs(orderId, lastStatus);
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().outgoing().outgoingOrder();

        outgoingOrderPage.inputOrderId(orderId);
        outgoingOrderPage.filterButtonClick();
        outgoingOrderPage.openFirstOrder();

        singleOrderPage.orderStatusHistory();

        Assertions.assertEquals(lastStatus, orderStatusHistoryPage.getLastStatus(),
                "Order status history is wrong:");

        Assertions.assertEquals(secondLastStatus, orderStatusHistoryPage.getSecondLastStatus(),
                "Order status history is wrong:");
    }

    @Step("Проверяем, что заказ {orderId} был в статусе {status}")
    public void checkOrderWasInStatus(String orderId, OrderStatus status) {
        Retrier.retry(() -> {
            topMenu.whSelectorClick().openWarehouse();
            leftMenu.WMS().outgoing().outgoingOrder();

            outgoingOrderPage.inputOrderId(orderId);
            outgoingOrderPage.filterButtonClick();
            outgoingOrderPage.openFirstOrder();

            singleOrderPage.orderStatusHistory();


            orderStatusHistoryPage.checkStatusInHistory(status);
        }, Retrier.RETRIES_TINY, Retrier.TIMEOUT_SMALL, TimeUnit.SECONDS);
    }

    @Step("Проверяем общее число товаров в заказе {orderId} = {expectedQty}")
    public void checkTotalOpenQty(String orderId, BigDecimal expectedQty) {
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().outgoing().outgoingOrder();

        outgoingOrderPage.inputOrderId(orderId);
        outgoingOrderPage.filterButtonClick();
        outgoingOrderPage.openFirstOrder();

        Assertions.assertEquals(expectedQty, singleOrderPage.getTotalOpenQty(), "Total open qty is wrong:");
    }
}
