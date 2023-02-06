package ru.yandex.market.wms.autostart.validation.orders;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.validation.ValidationResult;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.Order;

public class OrdersInSameBuildingValidationRuleTest extends BaseTest {

    private OrdersInSameBuildingValidationRule validationRule;

    @BeforeEach
    public void setup() {
        super.setup();
        validationRule = new OrdersInSameBuildingValidationRule();
    }

    @Test
    void validateOrderKeysListIsNull() {
        ValidationResult result = validationRule.validate(null);

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("OrdersInSameBuildingValidationRule");
    }

    @Test
    void validateOrderKeysListIsEmpty() {
        ValidationResult result = validationRule.validate(Collections.emptyList());

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("OrdersInSameBuildingValidationRule");
    }

    @Test
    void validateOrdersInSameBuilding() {
        List<Order> orders = List.of(
                Order.builder().building("0").build(),
                Order.builder().building("0").build(),
                Order.builder().building("0").build()
        );
        ValidationResult result = validationRule.validate(orders);

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("OrdersInSameBuildingValidationRule");
    }

    @Test
    void validateErrorWhenOrdersInDifferentBuildings() {
        List<Order> orders = List.of(
                Order.builder().building("0").build(),
                Order.builder().building("1").build(),
                Order.builder().building("1").build()
        );
        ValidationResult result = validationRule.validate(orders);

        assertions.assertThat(result.isOk()).isFalse();
        assertions.assertThat(result.getMessage()).isEqualTo("Выбраны заказы из разных зданий: 0,1");
        assertions.assertThat(result.getRuleName()).isEqualTo("OrdersInSameBuildingValidationRule");
    }

    @Test
    void allowOrdersWithoutBuildingToMixWithOthers() {
        List<Order> orders = List.of(
                Order.builder().building("0").build(),
                Order.builder().building("0").build(),
                Order.builder().building(null).build()
        );
        ValidationResult result = validationRule.validate(orders);

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("OrdersInSameBuildingValidationRule");
    }
}
