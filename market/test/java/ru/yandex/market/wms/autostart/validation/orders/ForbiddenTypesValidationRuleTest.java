package ru.yandex.market.wms.autostart.validation.orders;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.validation.ValidationResult;
import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.Order;

public class ForbiddenTypesValidationRuleTest extends BaseTest {

    private ForbiddenTypesValidationRule validationRule;

    @BeforeEach
    public void setup() {
        super.setup();
        validationRule = new ForbiddenTypesValidationRule();
    }

    @Test
    void validateOrderKeysListIsNull() {
        ValidationResult result = validationRule.validate(null);

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("ForbiddenTypesValidationRule");
    }

    @Test
    void validateOrderKeysListIsEmpty() {
        ValidationResult result = validationRule.validate(Collections.emptyList());

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("ForbiddenTypesValidationRule");
    }

    @Test
    void validateOrdersHavePermittedTypes() {
        List<Order> orders = List.of(
                Order.builder().type(OrderType.STANDARD.getCode()).build(),
                Order.builder().type(OrderType.OUTBOUND_FIT.getCode()).build(),
                Order.builder().type(OrderType.OUTBOUND_DEFECT.getCode()).build(),
                Order.builder().type(OrderType.OUTBOUND_EXPIRED.getCode()).build(),
                Order.builder().type(OrderType.OUTBOUND_SURPLUS.getCode()).build(),
                Order.builder().type(OrderType.OUTBOUND_AUCTION.getCode()).build(),
                Order.builder().type(OrderType.OUTBOUND_AUTO.getCode()).build(),
                Order.builder().type(OrderType.OUTBOUND_FIX_LOST_INVENTARIZATION.getCode()).build(),
                Order.builder().type(OrderType.OUTBOUND_OPER_LOST_INVENTARIZATION.getCode()).build(),
                Order.builder().type(OrderType.OUTBOUND_DEFECT_1P_SALE.getCode()).build(),
                Order.builder().type(OrderType.OUTBOUND_WH_2_WH.getCode()).build(),
                Order.builder().type(OrderType.OUTBOUND_WH_2_WH_DMG.getCode()).build(),
                Order.builder().type(OrderType.OUTBOUND_WH_2_WH_EXP.getCode()).build(),
                Order.builder().type(OrderType.MANUAL_UTILIZATION_OUTBOUND.getCode()).build(),
                Order.builder().type(OrderType.PLAN_UTILIZATION_OUTBOUND.getCode()).build()
        );

        ValidationResult result = validationRule.validate(orders);

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("ForbiddenTypesValidationRule");
    }

    @Test
    void validateErrorWhenOrdersHaveForbiddenType() {
        List<Order> orders = List.of(Order.builder().type(OrderType.LOAD_TESTING.getCode()).build());
        ValidationResult result = validationRule.validate(orders);

        assertions.assertThat(result.isOk()).isFalse();
        assertions.assertThat(result.getMessage()).isEqualTo("Заказы данного типа не поддерживаются в ручном запуске");
        assertions.assertThat(result.getRuleName()).isEqualTo("ForbiddenTypesValidationRule");
    }

    @Test
    void validateErrorWhenOrdersHaveMixedTypes() {
        List<Order> orders = List.of(
                Order.builder().type(OrderType.STANDARD.getCode()).build(),
                Order.builder().type(OrderType.OUTBOUND_FIT.getCode()).build(),
                Order.builder().type(OrderType.OUTBOUND_DEFECT.getCode()).build(),
                Order.builder().type(OrderType.OUTBOUND_EXPIRED.getCode()).build(),
                Order.builder().type(OrderType.OUTBOUND_SURPLUS.getCode()).build(),
                Order.builder().type(OrderType.LOAD_TESTING.getCode()).build()
        );
        ValidationResult result = validationRule.validate(orders);

        assertions.assertThat(result.isOk()).isFalse();
        assertions.assertThat(result.getMessage()).isEqualTo("Заказы данного типа не поддерживаются в ручном запуске");
        assertions.assertThat(result.getRuleName()).isEqualTo("ForbiddenTypesValidationRule");
    }
}
