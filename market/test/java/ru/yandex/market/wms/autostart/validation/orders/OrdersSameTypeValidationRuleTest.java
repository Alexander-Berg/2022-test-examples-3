package ru.yandex.market.wms.autostart.validation.orders;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.validation.ValidationResult;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.Order;

public class OrdersSameTypeValidationRuleTest extends BaseTest {

    private OrdersSameTypeValidationRule validationRule;

    @BeforeEach
    public void setup() {
        super.setup();
        validationRule = new OrdersSameTypeValidationRule();
    }

    @Test
    void validateOrderKeysListIsNull() {
        ValidationResult result = validationRule.validate(null);

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("OrdersSameTypeValidationRule");
    }

    @Test
    void validateOrderKeysListIsEmpty() {
        ValidationResult result = validationRule.validate(Collections.emptyList());

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("OrdersSameTypeValidationRule");
    }

    @Test
    void validateOrdersHaveSameTypes() {
        List<Order> orders = List.of(
                Order.builder().type("0").build(),
                Order.builder().type("0").build(),
                Order.builder().type("0").build()
        );
        ValidationResult result = validationRule.validate(orders);

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("OrdersSameTypeValidationRule");
    }

    @Test
    void validateErrorWhenOrdersHaveDifferentTypes() {
        List<Order> orders = List.of(
                Order.builder().type("0").build(),
                Order.builder().type("19").build(),
                Order.builder().type("13").build()
        );
        ValidationResult result = validationRule.validate(orders);

        assertions.assertThat(result.isOk()).isFalse();
        assertions.assertThat(result.getMessage()).isEqualTo("Заказы разных типов недопустимы");
        assertions.assertThat(result.getRuleName()).isEqualTo("OrdersSameTypeValidationRule");
    }

    @Test
    void validateErrorWhenOrdersHaveDifferentTypesAndDoNotContainStandardType() {
        List<Order> orders = List.of(
                Order.builder().type("19").build(),
                Order.builder().type("13").build()
        );
        ValidationResult result = validationRule.validate(orders);

        assertions.assertThat(result.isOk()).isFalse();
        assertions.assertThat(result.getMessage()).isEqualTo("Заказы разных типов недопустимы");
        assertions.assertThat(result.getRuleName()).isEqualTo("OrdersSameTypeValidationRule");
    }
}
