package ru.yandex.market.delivery.mdbapp.testutils;

import java.math.BigDecimal;
import java.util.Collections;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemRemovalPermission;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemsRemovalPermissionResponse;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

public final class CheckouterTestUtils {

    private CheckouterTestUtils() {
        throw new UnsupportedOperationException();
    }

    public static void mockGetOrderItemsRemovalPermissions(CheckouterAPI checkouterAPI) {
        doReturn(createOrderItemsRemovalPermissionResponse())
            .when(checkouterAPI).getOrderItemsRemovalPermissions(eq(60734220L));
    }

    @Nonnull
    private static OrderItemsRemovalPermissionResponse createOrderItemsRemovalPermissionResponse() {
        return new OrderItemsRemovalPermissionResponse(
            60734220L,
            BigDecimal.valueOf(10),
            true,
            Collections.singletonList(new OrderItemRemovalPermission(7406006L, true))
        );
    }
}
