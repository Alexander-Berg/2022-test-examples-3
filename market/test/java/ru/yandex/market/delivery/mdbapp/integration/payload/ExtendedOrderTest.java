package ru.yandex.market.delivery.mdbapp.integration.payload;

import org.junit.Assert;
import org.junit.Test;
import steps.logisticsPointSteps.LogisticPointSteps;
import steps.orderSteps.OrderSteps;
import steps.shopSteps.ShopSteps;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;

public class ExtendedOrderTest {
    ExtendedOrder extendedOrder = new ExtendedOrder();

    @Test
    public void getAndSetOrderTest() {
        Order order = OrderSteps.getFilledOrder();
        extendedOrder.setOrder(order);

        Assert.assertEquals("Unexpected order", order, extendedOrder.getOrder());
    }

    @Test
    public void getAndSetShopTest() {
        Shop shop = ShopSteps.getDefaultShop();
        extendedOrder.setShop(shop);

        Assert.assertEquals("Unexpected shop", shop, extendedOrder.getShop());
    }

    @Test
    public void getAndSetOutletTest() {
        LogisticsPoint outlet = LogisticPointSteps.getDefaultOutlet();
        extendedOrder.setOutlet(outlet);

        Assert.assertEquals("Unexpected outlet", outlet, extendedOrder.getOutlet());
    }

    @Test
    public void getAndSetInletTest() {
        LogisticsPoint inlet = LogisticPointSteps.getDefaultOutlet();
        extendedOrder.setInlet(inlet);

        Assert.assertEquals("Unexpected inlet", inlet, extendedOrder.getInlet());
    }

    @Test
    public void getAndSetOrderDataTest() {
        ExtendedOrder.OrderData orderData = new ExtendedOrder.OrderData();
        extendedOrder.setOrderData(orderData);

        Assert.assertEquals("Unexpected orderData", orderData, extendedOrder.getOrderData());
    }
}
