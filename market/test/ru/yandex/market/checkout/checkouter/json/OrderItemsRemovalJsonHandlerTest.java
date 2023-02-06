package ru.yandex.market.checkout.checkouter.json;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemRemovalPermission;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemsRemovalPermissionResponse;

import static ru.yandex.market.checkout.checkouter.json.Names.OrderItemsRemoval.DISABLED_REASONS;
import static ru.yandex.market.checkout.checkouter.json.Names.OrderItemsRemoval.ITEM_REMOVAL_PERMISSIONS;
import static ru.yandex.market.checkout.checkouter.json.Names.OrderItemsRemoval.MAX_TOTAL_PERCENT_REMOVABLE;
import static ru.yandex.market.checkout.checkouter.json.Names.OrderItemsRemoval.REMOVAL_ALLOWED;
import static ru.yandex.market.checkout.checkouter.order.itemsremoval.ReasonForNotAbleRemoveFromOrder.NOT_ALLOWED_COLOR;
import static ru.yandex.market.checkout.checkouter.order.itemsremoval.ReasonForNotAbleRemoveItem.NOT_ALLOWED_BY_ORDER;

public class OrderItemsRemovalJsonHandlerTest extends AbstractJsonHandlerTestBase {

    private static final long ORDER_ID = 123;
    private static final long ITEM_ID = 1234;
    private static final String PERCENT = "21.123456789";

    @Test
    public void orderItemsRemovalPermissionResponseSerializeTest() throws Exception {
        OrderItemsRemovalPermissionResponse response = OrderItemsRemovalPermissionResponse.Builder
                .initDisable(ORDER_ID, NOT_ALLOWED_COLOR)
                .setMaxTotalPercentRemovable(new BigDecimal(PERCENT))
                .addItemPermission(OrderItemRemovalPermission.initAllowed(ITEM_ID))
                .build();

        String json = write(response);

        checkJson(json, "$." + Names.OrderItemsRemoval.ORDER_ID, ORDER_ID);
        checkJson(json, "$." + MAX_TOTAL_PERCENT_REMOVABLE, PERCENT);
        checkJson(json, "$." + REMOVAL_ALLOWED, false);
        checkJson(json, "$." + DISABLED_REASONS + "[0]", NOT_ALLOWED_COLOR.name());
        checkJson(json, "$." + ITEM_REMOVAL_PERMISSIONS + "[0]." + Names.OrderItemsRemoval.ITEM_ID, ITEM_ID);
        checkJson(json, "$." + ITEM_REMOVAL_PERMISSIONS + "[0]." + REMOVAL_ALLOWED, false);
        checkJson(json, "$." + ITEM_REMOVAL_PERMISSIONS + "[0]." + DISABLED_REASONS + "[0]",
                NOT_ALLOWED_BY_ORDER.name());
    }
}
