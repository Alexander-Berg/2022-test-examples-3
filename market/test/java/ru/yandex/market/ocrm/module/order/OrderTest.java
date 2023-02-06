package ru.yandex.market.ocrm.module.order;

import java.util.List;

import javax.inject.Inject;

import jdk.jfr.Description;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.common.report.model.Color;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.domain.OrderItem;
import ru.yandex.market.ocrm.module.order.domain.OrderItemMarker;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ModuleOrderTestConfiguration.class)
public class OrderTest {

    @Inject
    OrderTestUtils orderTestUtils;
    @Inject
    EntityStorageService entityStorageService;

    @BeforeEach
    void setUp() {
        orderTestUtils.clearCheckouterAPI();
    }

    /**
     * Проверяем, что не развалились при загрузке конфигурации модуля
     */
    @Test
    public void checkConfiguration() {
        // do nothing
    }

    @Test
    @Description("https://testpalm.yandex-team.ru/testcase/ocrm-789")
    public void orderItem_marker_1P() {
        Order order = orderTestUtils.createOrder(Maps.of(Order.IS_FULFILMENT, Boolean.TRUE));
        orderTestUtils.mockOrderItem(order, Maps.of(
                OrderItem.AT_SUPPLIER_WAREHOUSE, Boolean.FALSE,
                OrderItem.SUPPLIER_TYPE, SupplierType.FIRST_PARTY
        ));
        orderTestUtils.mockGetOrderHistory(order, null);

        List<OrderItem> items = entityStorageService.list(Query.of(OrderItem.FQN)
                .withFilters(Filters.eq(OrderItem.PARENT, order)));
        OrderItem item = items.get(0);
        OrderItemMarker marker = item.getMarker();

        Assertions.assertNotNull(marker);
        Assertions.assertEquals("1P", marker.getCode());
    }

    @Test
    @Description("https://testpalm.yandex-team.ru/testcase/ocrm-789")
    public void orderItem_marker_FBB() {
        Order order = orderTestUtils.createOrder(Maps.of(Order.IS_FULFILMENT, Boolean.TRUE));
        orderTestUtils.mockOrderItem(order, Maps.of(
                OrderItem.AT_SUPPLIER_WAREHOUSE, Boolean.FALSE,
                OrderItem.SUPPLIER_TYPE, SupplierType.THIRD_PARTY
        ));
        orderTestUtils.mockGetOrderHistory(order, null);

        List<OrderItem> items = entityStorageService.list(Query.of(OrderItem.FQN)
                .withFilters(Filters.eq(OrderItem.PARENT, order)));
        OrderItem item = items.get(0);
        OrderItemMarker marker = item.getMarker();

        Assertions.assertNotNull(marker);
        Assertions.assertEquals("FBB", marker.getCode());
    }

    @Test
    @Description("https://testpalm.yandex-team.ru/testcase/ocrm-789")
    public void orderItem_marker_Crossdocking() {
        Order order = orderTestUtils.createOrder(Maps.of(Order.IS_FULFILMENT, Boolean.TRUE));
        orderTestUtils.mockOrderItem(order, Maps.of(
                OrderItem.AT_SUPPLIER_WAREHOUSE, Boolean.TRUE)
        );
        orderTestUtils.mockGetOrderHistory(order, null);

        List<OrderItem> items = entityStorageService.list(Query.of(OrderItem.FQN)
                .withFilters(Filters.eq(OrderItem.PARENT, order)));
        OrderItem item = items.get(0);
        OrderItemMarker marker = item.getMarker();

        Assertions.assertNotNull(marker);
        Assertions.assertEquals("CROSS_DOCKING", marker.getCode());
    }

    @Test
    @Description("https://testpalm.yandex-team.ru/testcase/ocrm-789")
    public void orderItem_marker_Dropshipping() {
        Order order = orderTestUtils.createOrder(Maps.of(
                Order.IS_FULFILMENT, Boolean.FALSE,
                Order.DELIVERY_PARTNER_TYPE, DeliveryPartnerType.YANDEX_MARKET
        ));
        orderTestUtils.mockOrderItem(order, Maps.of());
        orderTestUtils.mockGetOrderHistory(order, null);

        List<OrderItem> items = entityStorageService.list(Query.of(OrderItem.FQN)
                .withFilters(Filters.eq(OrderItem.PARENT, order)));
        OrderItem item = items.get(0);
        OrderItemMarker marker = item.getMarker();

        Assertions.assertNotNull(marker);
        Assertions.assertEquals("DROPSHIPPING", marker.getCode());
    }

    @Test
    @Description("https://testpalm.yandex-team.ru/testcase/ocrm-789")
    public void orderItem_marker_ClickCollect() {
        Order order = orderTestUtils.createOrder(Maps.of(
                Order.IS_FULFILMENT, Boolean.FALSE,
                Order.DELIVERY_PARTNER_TYPE, DeliveryPartnerType.SHOP,
                Order.COLOR, Color.BLUE
        ));
        orderTestUtils.mockOrderItem(order, Maps.of());
        orderTestUtils.mockGetOrderHistory(order, null);

        List<OrderItem> items = entityStorageService.list(Query.of(OrderItem.FQN)
                .withFilters(Filters.eq(OrderItem.PARENT, order)));
        OrderItem item = items.get(0);
        OrderItemMarker marker = item.getMarker();

        Assertions.assertNotNull(marker);
        Assertions.assertEquals("CLICK_COLLECT", marker.getCode());
    }

    @Test
    @Description("https://testpalm.yandex-team.ru/testcase/ocrm-789")
    public void orderItem_marker_DSBS() {
        Order order = orderTestUtils.createOrder(Maps.of(
                Order.IS_FULFILMENT, Boolean.FALSE,
                Order.DELIVERY_PARTNER_TYPE, DeliveryPartnerType.SHOP,
                Order.COLOR, Color.WHITE
        ));
        orderTestUtils.mockOrderItem(order, Maps.of());
        orderTestUtils.mockGetOrderHistory(order, null);

        List<OrderItem> items = entityStorageService.list(Query.of(OrderItem.FQN)
                .withFilters(Filters.eq(OrderItem.PARENT, order)));
        OrderItem item = items.get(0);
        OrderItemMarker marker = item.getMarker();

        Assertions.assertNotNull(marker);
        Assertions.assertEquals("DSBS", marker.getCode());
    }

    @Test
    public void createOrder() {
        Order o = orderTestUtils.createOrder(Maps.of());
        Assertions.assertNotNull(o, "Проверяем создание заказа утилитарным классом");
    }
}
