package ru.yandex.market.checkout.checkouter.checkout;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderProperty;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Alexander Semenov (alxsemn@yandex-team.ru)
 */
public class CheckoutOrderPropertyTest extends AbstractWebTestBase {

    @Test
    public void createOrderWithNullProperties() throws Exception {
        Order orderIn = OrderProvider.getBlueOrder();
        orderIn.addProperty(OrderPropertyType.PLATFORM.create(orderIn.getId(), null));
        orderIn.addProperty(OrderPropertyType.YANDEX_PLUS.create(orderIn.getId(), null));
        orderIn.addProperty(OrderPropertyType.MULTI_ORDER_ID.create(orderIn.getId(), null));
        orderIn.addProperty(OrderPropertyType.UNFREEZE_STOCKS_TIME.create(orderIn.getId(), null));
        orderIn.addProperty(new OrderProperty(orderIn.getId(), "testProperty", null));
        orderIn.addProperty(new OrderProperty(orderIn.getId(), "testProperty1", StringUtils.repeat('s', 2000)));

        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters(orderIn));

        Order savedOrder = orderService.getOrder(order.getId());
        assertEquals(
                Platform.UNKNOWN,
                savedOrder.getProperty(OrderPropertyType.PLATFORM)
        );

        assertNull(savedOrder.getProperty(OrderPropertyType.YANDEX_PLUS));
        assertNull(savedOrder.getProperty(OrderPropertyType.MULTI_ORDER_ID));
        assertNull(savedOrder.getProperty(OrderPropertyType.UNFREEZE_STOCKS_TIME));
        assertNull(savedOrder.getProperty("testProperty"));
        assertEquals(savedOrder.getProperty("testProperty1"), StringUtils.repeat('s', 2000));

        assertNotNull(savedOrder.getPropertiesForJson());
    }
}
