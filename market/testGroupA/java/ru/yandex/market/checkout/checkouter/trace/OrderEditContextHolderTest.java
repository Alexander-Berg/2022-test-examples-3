package ru.yandex.market.checkout.checkouter.trace;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.checkouter.order.OrderEditRequest.EditRequestType.ORDER_CANCELLATION;
import static ru.yandex.market.checkout.checkouter.order.OrderEditRequest.EditRequestType.REMOVE;

class OrderEditContextHolderTest {

    private final OrderEditContextHolder.OrderEditContextAttributesHolder holder;

    OrderEditContextHolderTest() {
        holder = new OrderEditContextHolder.OrderEditContextAttributesHolder();
    }

    @BeforeEach
    void setUp() {
        holder.clear();
    }

    @Test
    void simpleCases() {
        OrderEditContextHolder.setRequestTypes(List.of());
        assertThat(holder.getAttributes()).isEmpty();

        OrderEditContextHolder.setRequestTypes(List.of(REMOVE));
        assertThat(holder.getAttributes()).containsOnly(Map.entry("editRequestTypes", "REMOVE"));

        OrderEditContextHolder.setRequestTypes(List.of(REMOVE, ORDER_CANCELLATION));
        assertThat(holder.getAttributes()).containsOnly(Map.entry("editRequestTypes", "REMOVE,ORDER_CANCELLATION"));
    }

    @Test
    void withOrderProperties() {
        Order order = new Order();
        order.setId(123L);
        order.setFulfilment(true);
        order.setRgb(Color.BLUE);
        order.setStatus(OrderStatus.PROCESSING);
        order.setShopId(321L);
        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        delivery.setDeliveryServiceId(11L);
        delivery.setType(DeliveryType.DELIVERY);
        delivery.addFeatures(Set.of(DeliveryFeature.ON_DEMAND, DeliveryFeature.ON_DEMAND_YALAVKA));
        order.setDelivery(delivery);

        OrderEditContextHolder.setRequestTypes(List.of(REMOVE));
        OrderEditContextHolder.defineOrderProperty(order);
        OrderEditContextHolder.setDeliveryDatesChange(true);
        OrderEditContextHolder.setChangeRequestStatus(ChangeRequestStatus.REJECTED);
        OrderEditContextHolder.setToStatus(OrderStatus.CANCELLED);
        OrderEditContextHolder.setToSubstatus(OrderSubstatus.USER_UNREACHABLE);
        OrderEditContextHolder.setClientRole(ClientRole.SHOP);
        OrderEditContextHolder.setUserUnreachableValidationPassed(true);

        assertThat(holder.getAttributes()).containsEntry("editRequestTypes", "REMOVE");
        assertThat(holder.getAttributes()).containsEntry("orderId", 123L);
        assertThat(holder.getAttributes()).containsEntry("isFulfilment", true);
        assertThat(holder.getAttributes()).containsEntry("orderColor", Color.BLUE);
        assertThat(holder.getAttributes()).containsEntry("deliveryPartnerType", DeliveryPartnerType.YANDEX_MARKET);
        assertThat(holder.getAttributes()).containsEntry("orderStatus", OrderStatus.PROCESSING);
        assertThat(holder.getAttributes()).containsEntry("deliveryType", DeliveryType.DELIVERY);
        assertThat(holder.getAttributes()).containsEntry("deliveryServiceId", 11L);
        assertThat(holder.getAttributes()).containsEntry("isDeliveryDatesChange", true);
        assertThat(holder.getAttributes()).containsEntry("changeRequestStatus", ChangeRequestStatus.REJECTED);
        assertThat(holder.getAttributes()).hasEntrySatisfying("deliveryFeatures", df -> {
            assertNotNull(df);
            assertThat(df).isInstanceOf(String.class);
            assertThat((String) df).contains("ON_DEMAND");
            assertThat((String) df).contains("ON_DEMAND_YALAVKA");
        });
        assertThat(holder.getAttributes()).containsEntry("shopId", 321L);
        assertThat(holder.getAttributes()).containsEntry("toStatus", OrderStatus.CANCELLED);
        assertThat(holder.getAttributes()).containsEntry("toSubstatus", OrderSubstatus.USER_UNREACHABLE);
        assertThat(holder.getAttributes()).containsEntry("clientRole", ClientRole.SHOP);
        assertThat(holder.getAttributes()).containsEntry("userUnreachableValidationPassed", true);
        assertThat(holder.getAttributes()).hasSize(16);
    }

    @Test
    void withEmptyOrNull() {
        OrderEditContextHolder.defineOrderProperty(null);
        OrderEditContextHolder.setRequestTypes(null);
        OrderEditContextHolder.setChangeRequestStatus(null);
        OrderEditContextHolder.setDeliveryDatesChange(null);

        assertThat(holder.getAttributes()).isEmpty();
    }

}
