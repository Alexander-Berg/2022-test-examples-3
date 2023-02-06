package ru.yandex.market.checkout.checkouter.order.item.removalrules;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemsRemovalPermissionResponse;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.checkout.checkouter.order.itemsremoval.ReasonForNotAbleRemoveItem.NOT_ALLOWED_PROMO;

class PromoItemsRemovalRuleTest extends AbstractRemovalRuleTest {

    private PromoItemsRemovalRule promoRule;

    @Test
    @DisplayName("Ошибка если пропертя со строкой промо акций содержит неизвестную акцию")
    void exceptionWhenPromoIsInvalid() {
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> promoRule = new PromoItemsRemovalRule("invalidPromo"));
        assertThat(exception.getMessage()).contains("Unknown promo type");
    }

    @Test
    @DisplayName("Ок, когда переданная пропертя с промо акциями null или пустая")
    void okWhenPropertyNullOrEmpty() {
        new PromoItemsRemovalRule(null);
        new PromoItemsRemovalRule("");
        new PromoItemsRemovalRule("   ");
    }

    @Test
    @DisplayName("Все товары удалять нельзя. Все участвуют в неразрешенных для удалениях промо акциях")
    void disableRemoveWithDefaultPromo() {
        promoRule = new PromoItemsRemovalRule("BERU_PLUS, CASHBACK");
        Order order = new Order();
        order.setId(123L);
        order.addItem(initItem(1L, List.of(PromoType.BERU_PLUS, PromoType.DIRECT_DISCOUNT)));
        order.addItem(initItem(2L, List.of(PromoType.CASHBACK)));

        OrderItemsRemovalPermissionResponse response = promoRule.apply(order);

        assertAllowedOrderPermission(order, response);
        assertDisabledItemPermission(1, response, NOT_ALLOWED_PROMO);
        assertDisabledItemPermission(2, response, NOT_ALLOWED_PROMO);
    }

    @Test
    @DisplayName("Все товары можно удалять")
    void allRemoveAllowed() {
        promoRule = new PromoItemsRemovalRule("CASHBACK");
        Order order = new Order();
        order.setId(123L);
        order.addItem(initItem(1L, List.of(PromoType.BERU_PLUS, PromoType.DIRECT_DISCOUNT)));
        order.addItem(initItem(2L, Collections.emptyList()));

        OrderItemsRemovalPermissionResponse response = promoRule.apply(order);

        assertAllowedOrderPermission(order, response);
        assertAllowedItemPermission(1, response);
        assertAllowedItemPermission(2, response);
    }

    @Test
    @DisplayName("1 удалить можно, другой нельзя")
    void oneAllowedOneDisabledForRemove() {
        promoRule = new PromoItemsRemovalRule("CASHBACK");
        Order order = new Order();
        order.setId(123L);
        order.addItem(initItem(1L, List.of(PromoType.BERU_PLUS, PromoType.CASHBACK)));
        order.addItem(initItem(2L, List.of(PromoType.BERU_PLUS)));

        OrderItemsRemovalPermissionResponse response = promoRule.apply(order);

        assertAllowedOrderPermission(order, response);
        assertDisabledItemPermission(1, response, NOT_ALLOWED_PROMO);
        assertAllowedItemPermission(2, response);
    }

    @Test
    @DisplayName("товар с CHEAPEST_AS_GIFT удалить можно, если заказ на сборке")
    void cheapestAsGiftOnProcessingAllowedForRemove() {
        promoRule = new PromoItemsRemovalRule(null);
        Order order = new Order();
        order.setStatus(OrderStatus.PROCESSING);
        order.setSubstatus(OrderSubstatus.STARTED);
        order.setId(123L);
        order.setFulfilment(false);
        order.addItem(initItem(1L, List.of(PromoType.CHEAPEST_AS_GIFT)));

        OrderItemsRemovalPermissionResponse response = promoRule.apply(order);

        assertAllowedOrderPermission(order, response);
        assertAllowedItemPermission(1, response);
    }

    @Test
    @DisplayName("товар с CHEAPEST_AS_GIFT удалить можно на SHIPPED для fby заказа")
    void cheapestAsGiftOnProcessingWithShippedAllowedForRemoveFromFbyOrder() {
        promoRule = new PromoItemsRemovalRule(null);
        Order order = new Order();
        order.setStatus(OrderStatus.PROCESSING);
        order.setSubstatus(OrderSubstatus.SHIPPED);
        order.setId(123L);
        order.setFulfilment(true);
        order.addItem(initItem(1L, List.of(PromoType.CHEAPEST_AS_GIFT)));

        OrderItemsRemovalPermissionResponse response = promoRule.apply(order);

        assertAllowedOrderPermission(order, response);
        assertAllowedItemPermission(1, response);
    }

    @ParameterizedTest
    @DisplayName("товар с CHEAPEST_AS_GIFT удалить нельзя, если заказ был собран")
    @CsvSource({"fby", "fbs/dbs"})
    void cheapestAsGiftOnDeliveryForbiddenForRemove(String fulfilmentType) {
        promoRule = new PromoItemsRemovalRule(null);
        Order order = new Order();
        order.setStatus(OrderStatus.DELIVERY);
        order.setSubstatus(OrderSubstatus.DELIVERY_SERVICE_RECEIVED);
        order.setId(123L);
        order.setFulfilment(fulfilmentType.equals("fby"));
        order.addItem(initItem(1L, List.of(PromoType.CHEAPEST_AS_GIFT)));

        OrderItemsRemovalPermissionResponse response = promoRule.apply(order);

        assertAllowedOrderPermission(order, response);
        assertDisabledItemPermission(1, response, NOT_ALLOWED_PROMO);
    }

    @Test
    @DisplayName("Удалить нельзя, так-как товар идет в группе с другим (item.bundledId != null)")
    void disableByBundled() {
        promoRule = new PromoItemsRemovalRule("");
        Order order = new Order();
        order.setId(123L);
        OrderItem orderItem1 = initItem(1L, List.of(PromoType.BERU_PLUS, PromoType.CASHBACK));
        orderItem1.setBundleId("bundle1");
        order.addItem(orderItem1);
        OrderItem orderItem2 = initItem(2L, List.of(PromoType.BERU_PLUS, PromoType.CASHBACK));
        orderItem2.setBundleId("bundle1");
        order.addItem(orderItem2);
        order.addItem(initItem(3L, List.of(PromoType.BERU_PLUS)));

        OrderItemsRemovalPermissionResponse response = promoRule.apply(order);

        assertAllowedOrderPermission(order, response);
        assertDisabledItemPermission(1, response, NOT_ALLOWED_PROMO);
        assertDisabledItemPermission(2, response, NOT_ALLOWED_PROMO);
        assertAllowedItemPermission(3, response);
    }

    private OrderItem initItem(Long itemId, List<PromoType> promoTypes) {
        OrderItem item = new OrderItem();
        item.setId(itemId);
        item.setCount(2);
        item.setOfferItemKey(new OfferItemKey("offerId" + itemId, itemId, null));
        item.setSupplierId(itemId);
        item.setShopSku("sku" + itemId);
        item.setPromos(mockPromos(promoTypes));
        return item;
    }

    private Set<ItemPromo> mockPromos(Collection<PromoType> promoTypes) {
        return promoTypes.stream()
                .map(this::mockPromo)
                .collect(Collectors.toSet());
    }

    private ItemPromo mockPromo(PromoType promoType) {
        ItemPromo itemPromo = Mockito.mock(ItemPromo.class);
        Mockito.when(itemPromo.getType()).thenReturn(promoType);
        return itemPromo;
    }
}
