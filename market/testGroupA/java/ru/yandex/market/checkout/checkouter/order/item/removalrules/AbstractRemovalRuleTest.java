package ru.yandex.market.checkout.checkouter.order.item.removalrules;

import java.util.Collection;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemRemovalPermission;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemsRemovalPermissionResponse;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.ReasonForNotAbleRemoveFromOrder;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.ReasonForNotAbleRemoveItem;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemsRemovalPermissionResponse.MAX_PERCENT_ALLOWED;

public abstract class AbstractRemovalRuleTest {

    protected void assertAllowedOrderPermission(Order order, OrderItemsRemovalPermissionResponse orderPermission) {
        assertThat(orderPermission.getOrderId()).isEqualTo(order.getId());
        assertThat(orderPermission.isRemovalAllowed()).isTrue();
        assertThat(orderPermission.getMaxTotalPercentRemovable()).isEqualTo(MAX_PERCENT_ALLOWED);
        assertThat(orderPermission.getReasons()).isEmpty();
        assertThat(orderPermission.getItemRemovalPermissions()).hasSize(order.getItems().size());
    }

    protected void assertDisabledOrderPermission(
            Order order,
            OrderItemsRemovalPermissionResponse orderPermission,
            ReasonForNotAbleRemoveFromOrder reason
    ) {
        assertThat(orderPermission.getOrderId()).isEqualTo(order.getId());
        assertThat(orderPermission.isRemovalAllowed()).isFalse();
        assertThat(orderPermission.getMaxTotalPercentRemovable()).isEqualTo(MAX_PERCENT_ALLOWED);
        assertThat(orderPermission.getReasons()).containsOnly(reason);
        assertThat(orderPermission.getItemRemovalPermissions()).hasSize(order.getItems().size());
    }

    protected void assertAllowedItemPermission(long itemId, OrderItemsRemovalPermissionResponse response) {
        OrderItemRemovalPermission item3Permission = filterPermissionWithItemId(itemId,
                response.getItemRemovalPermissions());
        assertThat(item3Permission).isNotNull();
        assertThat(item3Permission.isRemovalAllowed()).isTrue();
        assertThat(item3Permission.getReasons()).isEmpty();
    }

    protected void assertDisabledItemPermission(long itemId, OrderItemsRemovalPermissionResponse response,
                                                ReasonForNotAbleRemoveItem reason) {
        OrderItemRemovalPermission item3Permission = filterPermissionWithItemId(itemId,
                response.getItemRemovalPermissions());
        assertThat(item3Permission).isNotNull();
        assertThat(item3Permission.isRemovalAllowed()).isFalse();
        assertThat(item3Permission.getReasons()).containsOnly(reason);
    }


    protected OrderItemRemovalPermission filterPermissionWithItemId(
            long itemId,
            Collection<OrderItemRemovalPermission> itemRemovalPermissions) {
        return itemRemovalPermissions.stream()
                .filter(itemPermission -> itemPermission.getItemId() == itemId)
                .findFirst()
                .orElse(null);
    }
}
