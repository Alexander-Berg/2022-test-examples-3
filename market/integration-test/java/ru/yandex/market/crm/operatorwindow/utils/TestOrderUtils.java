package ru.yandex.market.crm.operatorwindow.utils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.domain.OrderItem;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

@Component
public class TestOrderUtils {
    private static final Long ORDER_ITEM_ID_1 = Randoms.positiveLongValue();
    private static final String ORDER_ITEM_TITLE_1 = Randoms.string();
    private static final Long ORDER_ITEM_ID_2 = ORDER_ITEM_ID_1 + 1;
    private static final String ORDER_ITEM_TITLE_2 = Randoms.string();

    private final OrderTestUtils orderTestUtils;

    public TestOrderUtils(OrderTestUtils orderTestUtils) {
        this.orderTestUtils = orderTestUtils;
    }

    private static Map<String, Object> getDropshippingOrderProps() {
        return Maps.of(
                Order.IS_FULFILMENT, Boolean.FALSE,
                Order.DELIVERY_PARTNER_TYPE, DeliveryPartnerType.YANDEX_MARKET
        );
    }

    public Order createOrder(TestOrder testOrder) {
        Map<String, Object> props = new HashMap<>(Map.of(
                Order.NUMBER, TestOrder.TEST_ORDER_NUMBER,
                Order.PAYMENT_METHOD, testOrder.getPaymentMethod(),
                Order.PAYMENT_TYPE, testOrder.getPaymentType(),
                Order.BUYER_EMAIL, testOrder.getBuyerEmail(),
                Order.BUYER_PHONE, testOrder.getBuyerPhone(),
                Order.BUYER_LAST_NAME, testOrder.getBuyerFullName(),
                Order.BUYER_FIRST_NAME, "",
                Order.BUYER_MIDDLE_NAME, "",
                Order.STATUS, testOrder.getStatus(),
                Order.SUB_STATUS, testOrder.getSubstatus()

        ));
        props.putAll(testOrder.isDropshipping()
                ? getDropshippingOrderProps()
                : getNotDropshippingOrderProps()
        );
        Order order = orderTestUtils.createOrder(props);
        createOrderItem(order, testOrder.isDropshipping(), ORDER_ITEM_ID_1, ORDER_ITEM_TITLE_1);
        createOrderItem(order, testOrder.isDropshipping(), ORDER_ITEM_ID_2, ORDER_ITEM_TITLE_2);
        return order;
    }

    private Map<String, Object> getNotDropshippingOrderProps() {
        return Maps.of(Order.IS_FULFILMENT, Boolean.TRUE);
    }

    private void createOrderItem(Order order, boolean isDropshipping, Long checkouterId, String title) {
        Map<String, Object> props = new HashMap<>(Map.of(
                OrderItem.CHECKOUTER_ID, checkouterId,
                OrderItem.TITLE, title
        ));
        if (!isDropshipping) {
            props.putAll(Maps.of(
                    OrderItem.AT_SUPPLIER_WAREHOUSE, Boolean.FALSE,
                    OrderItem.SUPPLIER_TYPE, SupplierType.FIRST_PARTY
            ));
        }
        orderTestUtils.mockOrderItem(order, props);
    }
}
