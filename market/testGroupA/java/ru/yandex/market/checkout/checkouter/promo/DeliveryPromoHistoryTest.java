package ru.yandex.market.checkout.checkouter.promo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.qameta.allure.Epic;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItems;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;

import static com.google.common.collect.Lists.newArrayList;
import static java.math.BigDecimal.ZERO;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryResultProvider.SHIPMENT_DAY;

//ignored until 'remove items' is refactored and supported in production
@Disabled
public class DeliveryPromoHistoryTest extends AbstractPromoTestBase {

    @Autowired
    private OrderPayHelper payHelper;

    private static void assertDeliveryIsFreeWithYandexEmployee(Order order) {
        Delivery delivery = order.getDelivery();
        assertTrue(order.getPromos().stream().anyMatch(promo -> promo.getPromoDefinition().getType()
                .equals(PromoType.YANDEX_EMPLOYEE)));
        assertThat(delivery.getPrice(), comparesEqualTo(ZERO));
        Set<? extends ItemPromo> deliveryPromos = delivery.getPromos();
        assertThat(deliveryPromos, hasSize(1));
        assertThat(deliveryPromos.iterator().next().getPromoDefinition().getType(), equalTo(PromoType.YANDEX_EMPLOYEE));
    }

    @Test
    @DisplayName("Проверяем, что история по deliveryPromo нормально работает после изменения состава заказа")
    @Epic(Epics.CHANGE_ORDER)
    public void testDeliveryPromoOnChangeOrderItems() {
        OrderItem orderItem1 = OrderItemProvider.defaultOrderItem();
        OrderItem orderItem2 = OrderItemProvider.getAnotherOrderItem();

        Parameters parameters = createParameters(params -> {
            params.getOrder().setItems(asList(orderItem1, orderItem2));
            params.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
            params.setDeliveryType(DeliveryType.DELIVERY);
            params.getReportParameters().setActualDelivery(
                    ActualDeliveryProvider.builder().addDelivery(MOCK_DELIVERY_SERVICE_ID, SHIPMENT_DAY).build()
            );
            params.setMockLoyalty(true);
            params.getLoyaltyParameters().addDeliveryDiscount(
                    ru.yandex.market.loyalty.api.model.delivery.DeliveryType.COURIER,
                    new LoyaltyDiscount(new BigDecimal(Integer.MAX_VALUE), PromoType.YANDEX_EMPLOYEE)
            );
        });

        //настраиваем бесплатную доставку по yandexemployee
        //чтобы сходить в обход кеша
        parameters.configureMultiCart(multiCart -> multiCart.getCarts()
                .forEach(cart -> {
                    cart.setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
                    cart.getDelivery().setBuyerAddress(AddressProvider.getAddress(
                            a -> a.setStreet(a.getStreet() + UUID.randomUUID().toString())));
                }));

        //координаты, по которым бесплатная доставка
        parameters.getGeocoderParameters().setGps("60.590898 56.835586");
        parameters.setYandexEmployee(true);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        payHelper.payForOrder(createdOrder);
        Assertions.assertTrue(createdOrder.getBuyer().getYandexEmployee());
        assertDeliveryIsFreeWithYandexEmployee(createdOrder);

        //оставляем один из двух айтемов
        List<OrderItem> items = newArrayList(createdOrder.getItems().iterator().next());
        client.putOrderItems(createdOrder.getId(), new OrderItems(items),
                ClientRole.CALL_CENTER_OPERATOR, nextLong());

        // проверяем что во всех ивентах у ордера присутствует промо на бесплатную доставку yandexEmployee
        PagedEvents orderHistoryEvents = client.orderHistoryEvents().getOrderHistoryEvents(createdOrder.getId(),
                ClientRole.SYSTEM, RandomUtils.nextLong(), 0, 100);
        orderHistoryEvents.getItems().forEach(event -> {
            assertDeliveryIsFreeWithYandexEmployee(event.getOrderAfter());
            if (event.getOrderBefore() != null) {
                assertDeliveryIsFreeWithYandexEmployee(event.getOrderBefore());
            }
        });
    }
}
