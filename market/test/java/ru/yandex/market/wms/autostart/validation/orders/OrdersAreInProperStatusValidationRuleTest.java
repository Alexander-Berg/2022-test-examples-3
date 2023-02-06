package ru.yandex.market.wms.autostart.validation.orders;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.settings.ManualStarterSettings;
import ru.yandex.market.wms.autostart.validation.ValidationResult;
import ru.yandex.market.wms.common.model.enums.OrderStatus;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.Order;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class OrdersAreInProperStatusValidationRuleTest extends BaseTest {

    private ManualStarterSettings manualStarterSettings;
    private OrdersAreInProperStatusValidationRule validationRule;

    @BeforeEach
    public void setup() {
        super.setup();
        manualStarterSettings = mock(ManualStarterSettings.class);
        validationRule = new OrdersAreInProperStatusValidationRule(manualStarterSettings);
    }

    @Test
    void validateOrderKeysListIsNull() {
        ValidationResult result = validationRule.validate(null);

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("OrdersAreInProperStatusValidationRule");
    }

    @Test
    void validateOrderKeysListIsEmpty() {
        ValidationResult result = validationRule.validate(Collections.emptyList());

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("OrdersAreInProperStatusValidationRule");
    }

    @Test
    void validateAllOrdersAreInProperStatus() {
        var orders = Arrays.asList(
                createOrder("order-1", OrderStatus.CREATED_EXTERNALLY),
                createOrder("order-2", OrderStatus.NOT_STARTED)
        );
        doReturn(false).when(manualStarterSettings).getFuckUpModeEnabled();

        ValidationResult result = validationRule.validate(orders);

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("OrdersAreInProperStatusValidationRule");
    }

    @Test
    void validateSomeOrdersAreNotInProperStatus() {
        var orders = Arrays.asList(
                createOrder("order-1", OrderStatus.CREATED_EXTERNALLY),
                createOrder("order-2", OrderStatus.ALLOCATED),
                createOrder("order-3", OrderStatus.NOT_STARTED),
                createOrder("order-4", OrderStatus.CREATED_INTERNALLY),
                createOrder("order-5", OrderStatus.RELEASED)
        );
        doReturn(false).when(manualStarterSettings).getFuckUpModeEnabled();

        ValidationResult result = validationRule.validate(orders);

        assertions.assertThat(result.isOk()).isFalse();
        assertions.assertThat(result.getMessage())
                .isEqualTo("Некоторые заказы находятся в неподходящем статусе: order-2 в 17, " +
                        "order-4 в 04, order-5 в 29");
        assertions.assertThat(result.getRuleName()).isEqualTo("OrdersAreInProperStatusValidationRule");
    }

    private static Order createOrder(String orderKey, OrderStatus status) {
        return Order.builder()
                .orderKey(orderKey)
                .status(status.getValue())
                .build();
    }
}
