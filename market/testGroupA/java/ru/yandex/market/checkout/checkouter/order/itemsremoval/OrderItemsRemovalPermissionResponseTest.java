package ru.yandex.market.checkout.checkouter.order.itemsremoval;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemsRemovalPermissionResponse.MAX_PERCENT_ALLOWED;
import static ru.yandex.market.checkout.checkouter.order.itemsremoval.ReasonForNotAbleRemoveFromOrder.NOT_ALLOWED_PAYMENT_TYPE;
import static ru.yandex.market.checkout.checkouter.order.itemsremoval.ReasonForNotAbleRemoveItem.NOT_ALLOWED_BY_ORDER;

class OrderItemsRemovalPermissionResponseTest {

    @Test
    @DisplayName("Объединять пустую коллекцию нельзя")
    void exceptionWhenMergeNull() {
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> OrderItemsRemovalPermissionResponse.mergeAll(Collections.emptyList()));
        assertThat(exception.getMessage()).contains("Collection for merge must contains at least one permission");
    }

    @Test
    @DisplayName("Нельзя объединять разметки от разных заказов")
    void exceptionWhenMergePermissionsWithDifferentOrderIds() {
        OrderItemsRemovalPermissionResponse permission1 = OrderItemsRemovalPermissionResponse.Builder
                .initAllowed(1L)
                .build();
        OrderItemsRemovalPermissionResponse permission2 = OrderItemsRemovalPermissionResponse.Builder
                .initAllowed(2L)
                .build();

        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> OrderItemsRemovalPermissionResponse.mergeAll(List.of(permission1, permission2)));
        assertThat(exception.getMessage()).contains("Only permissions with equals order's ids can merge");
    }

    @Test
    @DisplayName("Объединение одной разрешающей разметки")
    void mergeWithOneAllowedPermission() {
        OrderItemsRemovalPermissionResponse permission = OrderItemsRemovalPermissionResponse.Builder
                .initAllowed(1L)
                .addItemPermission(OrderItemRemovalPermission.initAllowed(10L))
                .build();
        OrderItemsRemovalPermissionResponse result = OrderItemsRemovalPermissionResponse.mergeAll(List.of(permission));

        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getMaxTotalPercentRemovable()).isEqualTo(new BigDecimal("99.9"));
        assertThat(result.isRemovalAllowed()).isTrue();
        assertThat(result.getItemRemovalPermissions())
                .hasSize(1)
                .extracting(OrderItemRemovalPermission::getItemId)
                .containsExactly(10L);
        assertThat(result.getReasons()).isEmpty();
    }

    @Test
    @DisplayName("Объединение одной запрещающей разметки")
    void mergeWithOneDisabledPermission() {
        OrderItemsRemovalPermissionResponse permission = OrderItemsRemovalPermissionResponse.Builder
                .initDisable(1L, NOT_ALLOWED_PAYMENT_TYPE)
                .build();
        OrderItemsRemovalPermissionResponse result = OrderItemsRemovalPermissionResponse.mergeAll(List.of(permission));

        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getMaxTotalPercentRemovable()).isEqualTo(MAX_PERCENT_ALLOWED);
        assertThat(result.isRemovalAllowed()).isFalse();
        assertThat(result.getItemRemovalPermissions()).isEmpty();
        assertThat(result.getReasons()).containsOnly(NOT_ALLOWED_PAYMENT_TYPE);
    }

    @Test
    @DisplayName("Объединение запрещающий + разрешающий разметки")
    void mergeWithAllowedPlusDisabledPermissions() {
        OrderItemsRemovalPermissionResponse permission1 = OrderItemsRemovalPermissionResponse.Builder
                .initAllowed(1L)
                .build();
        OrderItemsRemovalPermissionResponse permission2 = OrderItemsRemovalPermissionResponse.Builder
                .initDisable(1L, NOT_ALLOWED_PAYMENT_TYPE)
                .addItemPermission(OrderItemRemovalPermission.initAllowed(10L))
                .build();

        OrderItemsRemovalPermissionResponse result = OrderItemsRemovalPermissionResponse.mergeAll(List.of(permission1,
                permission2));

        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getMaxTotalPercentRemovable()).isEqualTo(MAX_PERCENT_ALLOWED);
        assertThat(result.isRemovalAllowed()).isFalse();
        assertThat(result.getItemRemovalPermissions())
                .hasSize(1);
        OrderItemRemovalPermission itemPermission = result.getItemRemovalPermissions().iterator().next();
        assertThat(itemPermission.getItemId()).isEqualTo(10L);
        assertThat(itemPermission.isRemovalAllowed()).isFalse();
        assertThat(itemPermission.getReasons()).contains(NOT_ALLOWED_BY_ORDER);
        assertThat(result.getReasons()).containsOnly(NOT_ALLOWED_PAYMENT_TYPE);
    }

    @Test
    @DisplayName("Игнорирование null при дополнении разметки для пропущенных товаров")
    void initAllowedForMissingItemWithNull() {
        OrderItemsRemovalPermissionResponse permission = OrderItemsRemovalPermissionResponse.Builder
                .initAllowed(1L)
                .build();

        permission.initAllowedPermissionsForMissingItems(null);

        assertThat(permission.getOrderId()).isEqualTo(1L);
        assertThat(permission.getMaxTotalPercentRemovable()).isEqualTo(MAX_PERCENT_ALLOWED);
        assertThat(permission.isRemovalAllowed()).isTrue();
        assertThat(permission.getItemRemovalPermissions()).isEmpty();
        assertThat(permission.getReasons()).isEmpty();
    }

    @Test
    @DisplayName("Ошибка если при дополнении разметки был указан не заказ, на основе которого разметка была создана")
    void exceptionWhenInitAllowedForMissingItemWithInvalidOrder() {
        OrderItemsRemovalPermissionResponse permission = OrderItemsRemovalPermissionResponse.Builder
                .initAllowed(1L)
                .build();

        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> permission.initAllowedPermissionsForMissingItems(initOrder(2L)));
        assertThat(exception.getMessage()).contains("Order and OrderRemovalPermission have different ids");
    }

    @Test
    @DisplayName("Дополнение разметки с пропущенным разрешением на 1 товар")
    void initAllowedForMissingItemWithOneMissingItem() {
        Order order = OrderProvider.orderBuilder()
                .id(1L)
                .item(initItem(10L))
                .item(initItem(11L))
                .build();
        OrderItemsRemovalPermissionResponse permission = OrderItemsRemovalPermissionResponse.Builder
                .initAllowed(1L)
                .addItemPermission(OrderItemRemovalPermission.initAllowed(10L))
                .build();

        permission.initAllowedPermissionsForMissingItems(order);

        assertThat(permission.getItemRemovalPermissions())
                .extracting(OrderItemRemovalPermission::getItemId)
                .containsExactlyInAnyOrder(10L, 11L);
        OrderItemRemovalPermission missingPermission = permission.getItemRemovalPermissions()
                .stream().filter(itemPermission -> itemPermission.getItemId().equals(11L))
                .findAny()
                .orElseThrow();
        assertThat(missingPermission.isRemovalAllowed()).isTrue();
    }

    @Test
    @DisplayName("Дополнение разметки без пропущенных разрешений на товары")
    void initAllowedForMissingItemWithoutMissingItem() {
        Order order = OrderProvider.orderBuilder()
                .id(1L)
                .item(initItem(10L))
                .item(initItem(11L))
                .build();
        OrderItemsRemovalPermissionResponse permission = OrderItemsRemovalPermissionResponse.Builder
                .initAllowed(1L)
                .addItemPermission(OrderItemRemovalPermission.initAllowed(10L))
                .addItemPermission(OrderItemRemovalPermission.initAllowed(11L))
                .build();

        permission.initAllowedPermissionsForMissingItems(order);

        assertThat(permission.getItemRemovalPermissions())
                .extracting(OrderItemRemovalPermission::getItemId)
                .containsExactlyInAnyOrder(10L, 11L);
    }

    private Order initOrder(Long orderId) {
        Order order = new Order();
        order.setId(orderId);
        return order;
    }

    private OrderItem initItem(Long itemId) {
        OrderItem item = new OrderItem();
        item.setId(itemId);
        item.setCount(1);
        item.setOfferItemKey(new OfferItemKey("offerId", itemId, "bundleId" + itemId));
        item.setSupplierId(itemId);
        item.setShopSku("sku" + itemId);
        return item;
    }
}
