package ru.yandex.market.checkout.checkouter.order.itemsremoval;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.checkout.checkouter.order.itemsremoval.ReasonForNotAbleRemoveItem.NOT_ALLOWED_BY_ORDER;
import static ru.yandex.market.checkout.checkouter.order.itemsremoval.ReasonForNotAbleRemoveItem.NOT_ALLOWED_PROMO;

class OrderItemRemovalPermissionTest {

    @Test
    @DisplayName("Нельзя объединять разрешения с разными идентификаторами")
    void exceptionWithDifferentIds() {
        OrderItemRemovalPermission permission1 = OrderItemRemovalPermission.initAllowed(1L);
        OrderItemRemovalPermission permission2 = OrderItemRemovalPermission.initAllowed(2L);

        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> permission1.merge(permission2));
        assertThat(exception.getMessage()).contains("Only permissions with equals item's ids can merge");
    }

    @Test
    @DisplayName("Объединение положительного разрешения с положительным")
    void mergeAllowedPlusAllowed() {
        OrderItemRemovalPermission permission1 = OrderItemRemovalPermission.initAllowed(1L);
        OrderItemRemovalPermission permission2 = OrderItemRemovalPermission.initAllowed(1L);

        permission1.merge(permission2);

        assertThat(permission1.getItemId()).isEqualTo(1L);
        assertThat(permission1.isRemovalAllowed()).isTrue();
        assertThat(permission1.getReasons()).isEmpty();
    }

    @Test
    @DisplayName("Объединение положительного разрешения с отрицательным")
    void mergeAllowedPlusDisabled() {
        OrderItemRemovalPermission permission1 = OrderItemRemovalPermission.initAllowed(1L);
        OrderItemRemovalPermission permission2 = OrderItemRemovalPermission.initDisabled(1L, NOT_ALLOWED_PROMO);

        permission1.merge(permission2);

        assertThat(permission1.getItemId()).isEqualTo(1L);
        assertThat(permission1.isRemovalAllowed()).isFalse();
        assertThat(permission1.getReasons()).containsOnly(NOT_ALLOWED_PROMO);
    }

    @Test
    @DisplayName("Объединение отрицательного разрешения с отрицательным")
    void mergeDisabledPlusDisabled() {
        OrderItemRemovalPermission permission1 = OrderItemRemovalPermission.initDisabled(1L, NOT_ALLOWED_BY_ORDER);
        OrderItemRemovalPermission permission2 = OrderItemRemovalPermission.initDisabled(1L, NOT_ALLOWED_PROMO);

        permission1.merge(permission2);

        assertThat(permission1.getItemId()).isEqualTo(1L);
        assertThat(permission1.isRemovalAllowed()).isFalse();
        assertThat(permission1.getReasons()).containsOnly(NOT_ALLOWED_BY_ORDER, NOT_ALLOWED_PROMO);
    }
}
