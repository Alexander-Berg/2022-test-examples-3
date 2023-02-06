package ru.yandex.market.checkout.checkouter.order.item.removalrules;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemsRemovalPermissionResponse;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTotalItemsRemovalRuleTest extends AbstractRemovalRuleTest {

    private OrderTotalItemsRemovalRule totalItemsRemovalRule;
    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(123L);
    }

    @Test
    @DisplayName("Ошибка валидации. Максимально разрешенный процент меньше нуля")
    void exceptionWhenMaxPercentLessZero() {
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new OrderTotalItemsRemovalRule(new BigDecimal("-1")));
        assertThat(exception.getMessage()).contains("Percent value is not valid");
    }

    @Test
    @DisplayName("Ошибка валидации. Максимально разрешенный процент равен 100")
    void exceptionWhenMaxPercentEqual100() {
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new OrderTotalItemsRemovalRule(new BigDecimal("100")));
        assertThat(exception.getMessage()).contains("Percent value is not valid");
    }

    @Test
    @DisplayName("Ошибка валидации. Максимально разрешенный процент больше 100")
    void exceptionWhenMaxPercentGreater100() {
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new OrderTotalItemsRemovalRule(new BigDecimal("101")));
        assertThat(exception.getMessage()).contains("Percent value is not valid");
    }

    @Test
    @DisplayName("Проставление максимального процента на который можно удалить товаров из заказа")
    void successCase() {
        totalItemsRemovalRule = new OrderTotalItemsRemovalRule(new BigDecimal(70));

        OrderItemsRemovalPermissionResponse response = totalItemsRemovalRule.apply(order);

        assertThat(response.getOrderId()).isEqualTo(order.getId());
        assertThat(response.isRemovalAllowed()).isTrue();
        assertThat(response.getMaxTotalPercentRemovable()).isEqualTo(new BigDecimal(70));
        assertThat(response.getReasons()).isEmpty();
        assertThat(response.getItemRemovalPermissions()).isEmpty();
    }
}
