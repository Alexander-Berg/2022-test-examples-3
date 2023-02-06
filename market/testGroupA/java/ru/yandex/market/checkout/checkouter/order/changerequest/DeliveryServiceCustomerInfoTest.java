package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.ANOTHER_MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_INTAKE_DELIVERY_SERVICE_ID;

public class DeliveryServiceCustomerInfoTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    private YandexMarketDeliveryHelper.MarDoOrderBuilder orderBuilder;

    @BeforeEach
    void init() {
        orderBuilder = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
    }

    @Test
    void shouldReturnDeliveryServiceCustomerInfoWithAllFields() {
        DeliveryServiceCustomerInfo expectedInfo = new DeliveryServiceCustomerInfo(
                "MOCK_DELIVERY_SERVICE",
                Arrays.asList("+7-(912)-345-67-89", "+7-(912)-345-67-88"),
                "www.partner100501-site.ru", TrackOrderSource.ORDER_NO,
                DeliveryServiceSubtype.CONTRACT_COURIER);

        Order order = orderBuilder.build();

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Set.of(order.getId()), ClientRole.SYSTEM, 0L, List.of(Color.BLUE));

        assertThat(orderEditPossibilityList, hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility.getOrderId(), equalTo(order.getId()));
        assertThat(orderEditPossibility.getEditPossibilities(), hasSize(9));
        DeliveryServiceCustomerInfo actualInfo = orderEditPossibility.getDeliveryServiceCustomerInfo();
        assertNotNull(actualInfo);
        assertEquals(expectedInfo, actualInfo);
    }

    @Test
    void shouldReturnDeliveryServiceCustomerInfoWithoutPhonesAndSite() {
        DeliveryServiceCustomerInfo expectedInfo = new DeliveryServiceCustomerInfo(
                "ANOTHER_MOCK_DELIVERY_SERVICE",
                Collections.emptyList(),
                null, TrackOrderSource.ORDER_NO, DeliveryServiceSubtype.MARKET_COURIER);

        orderBuilder.withDeliveryServiceId(ANOTHER_MOCK_DELIVERY_SERVICE_ID);
        Order order = orderBuilder.build();

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Set.of(order.getId()), ClientRole.SYSTEM, 0L, List.of(Color.BLUE));

        assertThat(orderEditPossibilityList, hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility.getOrderId(), equalTo(order.getId()));
        assertThat(orderEditPossibility.getEditPossibilities(), hasSize(6));
        DeliveryServiceCustomerInfo actualInfo = orderEditPossibility.getDeliveryServiceCustomerInfo();
        assertNotNull(actualInfo);
        assertEquals(expectedInfo, actualInfo);
    }

    @Test
    void notReturnDeliveryServiceCustomerInfo() {
        orderBuilder.withDeliveryServiceId(MOCK_INTAKE_DELIVERY_SERVICE_ID);
        Order order = orderBuilder.build();

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Set.of(order.getId()), ClientRole.SYSTEM, 0L, List.of(Color.BLUE));

        assertThat(orderEditPossibilityList, hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility.getOrderId(), equalTo(order.getId()));
        assertThat(orderEditPossibility.getEditPossibilities(), hasSize(4));
        DeliveryServiceCustomerInfo actualInfo = orderEditPossibility.getDeliveryServiceCustomerInfo();
        assertNull(actualInfo);
    }

    @Test
    void shouldReturnDeliveryServiceCustomerInfoWithoutPhonesAndSiteWhenPossibilityDisabled() {
        DeliveryServiceCustomerInfo expectedInfo = new DeliveryServiceCustomerInfo(
                "MOCK_DELIVERY_SERVICE",
                List.of(),
                null, TrackOrderSource.ORDER_NO, DeliveryServiceSubtype.CONTRACT_COURIER);

        Order order = orderBuilder.build();
        orderStatusHelper.proceedOrderFromUnpaidToCancelled(order);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Set.of(order.getId()), ClientRole.SYSTEM, 0L, List.of(Color.BLUE));

        assertThat(orderEditPossibilityList, hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility.getOrderId(), equalTo(order.getId()));
        assertThat(orderEditPossibility.getEditPossibilities(), hasSize(7));
        DeliveryServiceCustomerInfo actualInfo = orderEditPossibility.getDeliveryServiceCustomerInfo();
        assertNotNull(actualInfo);
        assertEquals(expectedInfo, actualInfo);
    }
}
