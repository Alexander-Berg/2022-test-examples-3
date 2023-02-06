package ru.yandex.market.checkout.checkouter.order.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.StatusAndSubstatus;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderStatusRoutersTest extends AbstractWebTestBase {

    private static final long SHOP_ID = 12345;

    @Autowired
    private OrderStatusRouters orderStatusRouters;

    @Autowired
    private ShopService shopService;

    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;

    @BeforeEach
    void init() {
        b2bCustomersMockConfigurer.mockIsClientCanOrder(BuyerProvider.UID,
                B2bCustomersTestProvider.BUSINESS_BALANCE_ID, true);
    }

    @AfterEach
    void resetMocks() {
        b2bCustomersMockConfigurer.resetAll();
    }

    @Test
    public void testOrderAutoAccept() {
        Order order = orderCreateHelper.createOrder(new Parameters());
        order.setStatus(OrderStatus.UNPAID);
        order.setShopId(SHOP_ID);
        order.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        shopService.updateMeta(SHOP_ID, ShopMetaDataBuilder.create().withOrderAutoAcceptEnabled(true).build());
        StatusAndSubstatus nextStatus = orderStatusRouters.nextStatusAndSubstatus(order);
        assertEquals(OrderStatus.PROCESSING, nextStatus.getStatus());
    }

    @Test
    @Disabled
    public void testOrderNoAutoAccept() {
        Order order = orderCreateHelper.createOrder(new Parameters());
        order.setStatus(OrderStatus.UNPAID);
        order.setShopId(SHOP_ID);
        order.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        shopService.updateMeta(SHOP_ID, ShopMetaDataBuilder.create().withOrderAutoAcceptEnabled(false).build());
        StatusAndSubstatus nextStatus = orderStatusRouters.nextStatusAndSubstatus(order);
        assertEquals(OrderStatus.PENDING, nextStatus.getStatus());
    }

    @Test
    public void testOrderBusinessClient() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getBuyer().setBusinessBalanceId(123L);

        Order order = orderCreateHelper.createOrder(parameters);
        order.setStatus(OrderStatus.UNPAID);
        order.setShopId(SHOP_ID);
        order.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        shopService.updateMeta(SHOP_ID, ShopMetaDataBuilder.create().withOrderAutoAcceptEnabled(true).build());
        StatusAndSubstatus nextStatus = orderStatusRouters.nextStatusAndSubstatus(order);

        assertEquals(OrderStatus.PENDING, nextStatus.getStatus());
        assertEquals(OrderSubstatus.AWAIT_CONFIRMATION, nextStatus.getSubstatus());
    }
}
