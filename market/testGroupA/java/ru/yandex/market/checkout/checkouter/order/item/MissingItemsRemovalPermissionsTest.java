package ru.yandex.market.checkout.checkouter.order.item;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemRemovalPermission;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemsRemovalPermissionResponse;
import ru.yandex.market.checkout.helpers.BundleOrderHelper;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.postpaidBlueOrderParameters;

class MissingItemsRemovalPermissionsTest extends MissingItemsAbstractTest {

    @Autowired
    private OrderCreateHelper orderCreateHelper;
    @Autowired
    private BundleOrderHelper bundleOrderHelper;

    @Test
    void removalAllowedTest() {
        Parameters params = postpaidBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(params);

        checkAllItemsAllowed(order, true);
    }

    @Test
    void removalNotAllowedForItemsInBundleTest() {
        Order order = bundleOrderHelper.createTypicalOrderWithBundles();

        Set<Long> itemIdsWithBundle = order.getItems().stream()
                .filter(item -> item.getBundleId() != null)
                .map(OrderItem::getId)
                .collect(Collectors.toSet());
        assertFalse(itemIdsWithBundle.isEmpty());

        Set<Long> itemIdsWithoutBundle = order.getItems().stream()
                .filter(item -> item.getBundleId() == null)
                .map(OrderItem::getId)
                .collect(Collectors.toSet());
        assertFalse(itemIdsWithoutBundle.isEmpty());

        OrderItemsRemovalPermissionResponse removalPermission = getItemsRemovalPermission(order.getId());

        Set<Long> forbiddenItemIds = removalPermission.getItemRemovalPermissions().stream()
                .filter(itemPermission -> !itemPermission.isRemovalAllowed())
                .map(OrderItemRemovalPermission::getItemId)
                .collect(Collectors.toSet());
        assertTrue(forbiddenItemIds.containsAll(itemIdsWithBundle));
    }
}
