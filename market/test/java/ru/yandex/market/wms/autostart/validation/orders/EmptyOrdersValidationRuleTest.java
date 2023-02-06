package ru.yandex.market.wms.autostart.validation.orders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.validation.ValidationResult;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDetailDao;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class EmptyOrdersValidationRuleTest extends BaseTest {

    private static final String ORDER_1 = "order-1";
    private static final String ORDER_2 = "order-2";
    private OrderDetailDao orderDetailDao;
    private EmptyOrdersValidationRule validationRule;

    @BeforeEach
    public void setup() {
        super.setup();
        orderDetailDao = mock(OrderDetailDao.class);

        validationRule = new EmptyOrdersValidationRule(orderDetailDao);

    }

    private void validate_ok(ValidationResult result) {
        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("EmptyOrdersValidationRule");
    }

    private void validate_error(ValidationResult result, String orders) {
        assertions.assertThat(result.isOk()).isFalse();
        assertions.assertThat(result.getMessage())
                .isEqualTo(String.format("Нельзя запускать заказы нулевым количеством товара: %s", orders));
        assertions.assertThat(result.getRuleName()).isEqualTo("EmptyOrdersValidationRule");
    }

    private List<OrderDetail> combine(List<OrderDetail>... lists) {
        List<OrderDetail> combine = new ArrayList<>();
        for (List<OrderDetail> list : lists) {
            combine.addAll(list);
        }
        return combine;
    }

    private List<OrderDetail> notEmptyOrder(String orderKey) {
        return List.of(
                OrderDetail.builder()
                        .openQty(BigDecimal.ONE)
                        .orderKey(orderKey)
                        .orderFlowId(1)
                        .build(),
                OrderDetail.builder()
                        .openQty(BigDecimal.TEN)
                        .orderKey(orderKey)
                        .orderFlowId(1)
                        .build());
    }

    private List<OrderDetail> emptyOrder(String orderKey) {
        return List.of(
                OrderDetail.builder()
                        .openQty(BigDecimal.ZERO)
                        .orderKey(orderKey)
                        .orderFlowId(1)
                        .build());
    }

    @Test
    void validateOrderKeysListIsNull() {
        ValidationResult result = validationRule.validate(null);

        validate_ok(result);
        verify(orderDetailDao, never()).findOrderDetailsByOrderKeys(anyList());
    }

    @Test
    void validateOrderKeysListIsEmpty() {
        ValidationResult result = validationRule.validate(Collections.emptyList());

        validate_ok(result);
        verify(orderDetailDao, never()).findOrderDetailsByOrderKeys(anyList());
    }

    @Test
    void validateNotEmptyOrders() {
        List<String> orderKeys = List.of(ORDER_1, ORDER_2);
        List<Order> orders = List.of(
                Order.builder().orderKey(ORDER_1).type("0").build(),
                Order.builder().orderKey(ORDER_2).type("0").build()
        );
        doReturn(combine(
                notEmptyOrder(ORDER_1), notEmptyOrder(ORDER_2)
        )).when(orderDetailDao).findOrderDetailsByOrderKeys(orderKeys);

        ValidationResult result = validationRule.validate(orders);

        validate_ok(result);
    }

    @Test
    void validateOneEmptyOrder() {
        List<String> orderKeys = List.of(ORDER_1, ORDER_2);
        List<Order> orders = List.of(
                Order.builder().orderKey(ORDER_1).type("0").build(),
                Order.builder().orderKey(ORDER_2).type("0").build()
        );
        doReturn(combine(
                notEmptyOrder(ORDER_1), emptyOrder(ORDER_2)
        )).when(orderDetailDao).findOrderDetailsByOrderKeys(orderKeys);

        ValidationResult result = validationRule.validate(orders);

        validate_error(result, ORDER_2);
    }

    @Test
    void validateEmptyOrders() {
        List<String> orderKeys = List.of(ORDER_1, ORDER_2);
        List<Order> orders = List.of(
                Order.builder().orderKey(ORDER_1).type("0").build(),
                Order.builder().orderKey(ORDER_2).type("0").build()
        );
        doReturn(combine(
                emptyOrder(ORDER_1), emptyOrder(ORDER_2)
        )).when(orderDetailDao).findOrderDetailsByOrderKeys(orderKeys);

        ValidationResult result = validationRule.validate(orders);

        validate_error(result, String.join(",", orderKeys));
    }

}
