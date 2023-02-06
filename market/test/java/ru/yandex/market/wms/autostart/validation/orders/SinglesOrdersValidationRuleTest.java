package ru.yandex.market.wms.autostart.validation.orders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.service.OrderFlowService;
import ru.yandex.market.wms.autostart.settings.ManualStarterSettings;
import ru.yandex.market.wms.autostart.validation.ValidationResult;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;
import ru.yandex.market.wms.common.spring.dao.entity.OrderFlowType;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDetailDao;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class SinglesOrdersValidationRuleTest extends BaseTest {

    private static final String ORDER_2 = "order-2";
    private static final String ORDER_3 = "order-3";
    private static final String ORDER_1 = "order-1";
    private OrderDetailDao orderDetailDao;
    private OrderFlowService orderFlowService;
    private SinglesOrdersValidationRule validationRule;
    private ManualStarterSettings manualStarterSettings;

    @BeforeEach
    public void setup() {
        super.setup();
        orderDetailDao = mock(OrderDetailDao.class);
        orderFlowService = mock(OrderFlowService.class);
        manualStarterSettings = mock(ManualStarterSettings.class);
        validationRule =
                new SinglesOrdersValidationRule(orderDetailDao, orderFlowService, manualStarterSettings);

        doReturn(
                OrderFlowType.builder()
                        .miniBatch(true)
                        .build()
        ).when(orderFlowService).getTypeById(1);
        doReturn(
                OrderFlowType.builder().nonSortable(true).build()
        ).when(orderFlowService).getTypeById(8);

        doReturn(true).when(manualStarterSettings).getNonSortSingleWavesEnabled();
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
    void validateOrdersWithoutAllSingles() {
        List<String> orderKeys = List.of("order-1", "order-2");
        List<Order> orders = List.of(
                Order.builder().orderKey("order-1").type("0").build(),
                Order.builder().orderKey("order-2").type("0").build()
        );
        doReturn(combine(
                nonSingle(ORDER_1),
                single(ORDER_2)
        )).when(orderDetailDao).findOrderDetailsByOrderKeys(orderKeys);

        ValidationResult result = validationRule.validate(orders);

        validate_ok(result);
    }

    @Test
    void validateOrderIsNotStandard() {
        List<Order> orders = List.of(Order.builder().orderKey("order-1").type("14").build());
        ValidationResult result = validationRule.validate(orders);

        validate_ok(result);
    }

    @Test
    void validateOrderWithOneOversizeSingle() {
        List<String> ordersKeys = List.of("order-1");
        List<Order> orders = List.of(Order.builder().orderKey("order-1").type("0").build());
        doReturn(singleOversize(ORDER_1))
                .when(orderDetailDao).findOrderDetailsByOrderKeys(ordersKeys);

        ValidationResult result = validationRule.validate(orders);

        validate_ok(result);
    }

    @Test
    void validateSinglesWithoutOversize() {
        List<String> orderKeys = List.of("order-1", "order-2");
        List<Order> orders = List.of(
                Order.builder().orderKey("order-1").type("0").build(),
                Order.builder().orderKey("order-2").type("0").build()
        );
        doReturn(combine(
                single(ORDER_1),
                single(ORDER_2)
        )).when(orderDetailDao).findOrderDetailsByOrderKeys(orderKeys);

        ValidationResult result = validationRule.validate(orders);

        validate_ok(result);
    }

    @Test
    void validateErrorWhenSinglesHaveOversize() {
        List<String> orderKeys = List.of("order-1", "order-2");
        List<Order> orders = List.of(
                Order.builder().orderKey("order-1").type("0").build(),
                Order.builder().orderKey("order-2").type("0").build()
        );
        doReturn(combine(
                single(ORDER_1),
                singleOversize(ORDER_2)
        )).when(orderDetailDao).findOrderDetailsByOrderKeys(orderKeys);

        ValidationResult result = validationRule.validate(orders);

        validate_error(result);
    }

    @Test
    void shouldNotAffectIfNoSingles() {
        List<String> orderKeys = List.of("order-1", "order-2");
        List<Order> orders = List.of(
                Order.builder().orderKey("order-1").type("0").build(),
                Order.builder().orderKey("order-2").type("0").build()
        );
        doReturn(combine(
                nonSingle(ORDER_1),
                nonSingleOversize(ORDER_2)
        )).when(orderDetailDao).findOrderDetailsByOrderKeys(orderKeys);

        ValidationResult result = validationRule.validate(orders);

        validate_ok(result);
    }

    @Test
    void shouldNotValidateOrderIfSingleDisabled() {
        doReturn(false).when(manualStarterSettings).getNonSortSingleWavesEnabled();

        List<String> orderKeys = List.of("order-1", "order-2");
        List<Order> orders = List.of(
                Order.builder().orderKey("order-1").build(),
                Order.builder().orderKey("order-2").build()
        );
        doReturn(combine(
                single(ORDER_1),
                singleOversize(ORDER_2)
        )).when(orderDetailDao).findOrderDetailsByOrderKeys(orderKeys);

        ValidationResult result = validationRule.validate(orders);

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("SinglesOrdersValidationRule");
    }

    @Test
    void errorOnSingleAndOversizeAndOthers() {
        List<String> orderKeys = List.of("order-1", "order-2", "order-3");
        List<Order> orders = List.of(
                Order.builder().orderKey("order-1").type("0").build(),
                Order.builder().orderKey("order-2").type("0").build(),
                Order.builder().orderKey("order-3").type("0").build()
        );
        doReturn(combine(
                single(ORDER_1),
                singleOversize(ORDER_2),
                nonSingleOversize(ORDER_3)
        )).when(orderDetailDao).findOrderDetailsByOrderKeys(orderKeys);

        ValidationResult result = validationRule.validate(orders);

        validate_error(result);
    }

    @Test
    void validateWithoutSingles() {
        List<String> orderKeys = List.of("order-2", "order-3");
        List<Order> orders = List.of(
                Order.builder().orderKey("order-2").type("0").build(),
                Order.builder().orderKey("order-3").type("0").build()
        );
        doReturn(combine(
                singleOversize(ORDER_1),
                nonSingleOversize(ORDER_2),
                nonSingle(ORDER_3)
        )).when(orderDetailDao).findOrderDetailsByOrderKeys(orderKeys);

        ValidationResult result = validationRule.validate(orders);

        validate_ok(result);
    }

    @Test
    void validateSingleAndNotSingle() {
        List<String> orderKeys = List.of("order-2", "order-3");
        List<Order> orders = List.of(
                Order.builder().orderKey("order-2").type("0").build(),
                Order.builder().orderKey("order-3").type("0").build()
        );
        doReturn(combine(
                nonSingle(ORDER_1),
                single(ORDER_2)
        )).when(orderDetailDao).findOrderDetailsByOrderKeys(orderKeys);

        ValidationResult result = validationRule.validate(orders);

        validate_ok(result);
    }

    @Test
    void validateOversizeSingleAndNonSingleWithOneDetail() {
        List<String> orderKeys = List.of("order-2", "order-3");
        List<Order> orders = List.of(
                Order.builder().orderKey("order-2").type("0").build(),
                Order.builder().orderKey("order-3").type("0").build()
        );
        doReturn(combine(
                singleOversize(ORDER_1),
                nonSingleWithSingleDetails(ORDER_2)
        )).when(orderDetailDao).findOrderDetailsByOrderKeys(orderKeys);

        ValidationResult result = validationRule.validate(orders);

        validate_ok(result);
    }

    private void validate_error(ValidationResult result) {
        assertions.assertThat(result.isOk()).isFalse();
        assertions.assertThat(result.getMessage()).isEqualTo("Нельзя смешивать синглы и кгт заказы в одной волне");
        assertions.assertThat(result.getRuleName()).isEqualTo("SinglesOrdersValidationRule");
    }

    private void validate_ok(ValidationResult result) {
        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("SinglesOrdersValidationRule");
    }

    private List<OrderDetail> combine(List<OrderDetail>... lists) {
        List<OrderDetail> combine = new ArrayList<>();
        for (List<OrderDetail> list : lists) {
            combine.addAll(list);
        }
        return combine;
    }

    private List<OrderDetail> single(String orderKey) {
        return List.of(OrderDetail.builder()
                .openQty(BigDecimal.ONE)
                .orderKey(orderKey)
                .orderFlowId(1)
                .build());
    }

    private List<OrderDetail> singleOversize(String orderKey) {
        return List.of(OrderDetail.builder()
                .openQty(BigDecimal.ONE)
                .orderKey(orderKey)
                .orderFlowId(8)
                .build());
    }

    private List<OrderDetail> nonSingleOversize(String orderKey) {
        return List.of(
                OrderDetail.builder()
                        .openQty(BigDecimal.ONE)
                        .orderKey(orderKey)
                        .orderFlowId(8)
                        .build(),
                OrderDetail.builder()
                        .openQty(BigDecimal.TEN)
                        .orderKey(orderKey)
                        .orderFlowId(1)
                        .build());
    }

    private List<OrderDetail> nonSingle(String orderKey) {
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

    private List<OrderDetail> nonSingleWithSingleDetails(String orderKey) {
        return List.of(
                OrderDetail.builder()
                        .openQty(BigDecimal.valueOf(2))
                        .orderKey(orderKey)
                        .orderFlowId(1)
                        .build());
    }
}
