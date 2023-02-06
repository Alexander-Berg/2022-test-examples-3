package ru.yandex.market.checkout.checkouter.order.item.removalrules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.color.SingleColorConfig;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemsRemovalPermissionResponse;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.ReasonForNotAbleRemoveFromOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemsRemovalPermissionResponse.MAX_PERCENT_ALLOWED;

class OrderColorItemsRemovalRuleTest extends AbstractRemovalRuleTest {

    @Mock
    private ColorConfig colorConfig;
    @Mock
    private SingleColorConfig singleColorConfig;
    private Order order;
    private OrderColorItemsRemovalRule colorRule;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        colorRule = new OrderColorItemsRemovalRule(colorConfig);
        Mockito.when(colorConfig.getFor(Mockito.any(Order.class))).thenReturn(singleColorConfig);
        order = new Order();
        order.setId(123L);
    }

    @Test
    @DisplayName("Возможность удаления товара из заказа исходя из цвета. Можно удалять")
    void successCase() {
        Mockito.when(singleColorConfig.isOrderItemRemovalEnabled()).thenReturn(true);

        OrderItemsRemovalPermissionResponse response = colorRule.apply(order);

        assertThat(response.getOrderId()).isEqualTo(order.getId());
        assertThat(response.isRemovalAllowed()).isTrue();
        assertThat(response.getMaxTotalPercentRemovable()).isEqualTo(MAX_PERCENT_ALLOWED);
        assertThat(response.getReasons()).isEmpty();
        assertThat(response.getItemRemovalPermissions()).isEmpty();
    }

    @Test
    @DisplayName("Возможность удаления товара из заказа исходя из цвета. Удалять нельзя")
    void negativeCase() {
        Mockito.when(singleColorConfig.isOrderItemRemovalEnabled()).thenReturn(false);

        OrderItemsRemovalPermissionResponse response = colorRule.apply(order);

        assertThat(response.getOrderId()).isEqualTo(order.getId());
        assertThat(response.isRemovalAllowed()).isFalse();
        assertThat(response.getMaxTotalPercentRemovable()).isEqualTo(MAX_PERCENT_ALLOWED);
        assertThat(response.getReasons()).containsOnly(ReasonForNotAbleRemoveFromOrder.NOT_ALLOWED_COLOR);
        assertThat(response.getItemRemovalPermissions()).isEmpty();
    }
}
