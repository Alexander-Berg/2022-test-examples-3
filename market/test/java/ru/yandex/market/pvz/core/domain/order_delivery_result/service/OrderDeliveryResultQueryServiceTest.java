package ru.yandex.market.pvz.core.domain.order_delivery_result.service;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryFlow;
import ru.yandex.market.pvz.core.domain.order_delivery_result.PartialDeliveryStatus;
import ru.yandex.market.pvz.core.domain.order_delivery_result.params.CodeType;
import ru.yandex.market.pvz.core.domain.order_delivery_result.params.OrderDeliveryResultItemParams;
import ru.yandex.market.pvz.core.domain.order_delivery_result.params.OrderDeliveryResultParams;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.CIS_1_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.CIS_2_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.CIS_2_2;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.EAN_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_1_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_2_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_2_2;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderDeliveryResultQueryServiceTest {

    private static final String[] IGNORED_FIELDS = {
            "items",
            "canBePackaged",
            "isFullReturn",
            "isFullPurchase",
            "deliveryItemsToCheckCodeIds"
    };

    private final TestOrderFactory orderFactory;
    private final OrderDeliveryResultQueryService orderDeliveryResultQueryService;

    @Test
    void testGetNonExistentDeliveryResult() {
        Order order = orderFactory.createSimpleFashionOrder();

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());

        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.NOT_STARTED)
                .barcodes(List.of())
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(orderDeliveryResult);

        List<OrderDeliveryResultItemParams> items = orderDeliveryResult.getItems();
        items.forEach(item -> {
            item.setOrderItemId(null);
            item.setPriorityNumber(0);
        });

        assertThat(List.of(
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_1_1)
                        .cis(CIS_1_1)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .codeChecked(false)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_2_1)
                        .cis(CIS_2_1)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .codeChecked(false)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_2_2)
                        .cis(CIS_2_2)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .codeChecked(false)
                        .build()
        )).containsExactlyInAnyOrderElementsOf(items);
    }

    @Test
    void testGetItemWithUitHasUitCodeType() {
        Order order = orderFactory.createFashionOrder(UIT_1_1, CIS_1_1, List.of(EAN_1));
        OrderDeliveryResultParams expected = OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.NOT_STARTED)
                .barcodes(List.of())
                .build();

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(orderDeliveryResult)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(expected);

        List<CodeType> actualCodeTypes = orderDeliveryResult.getItems()
                .stream()
                .map(OrderDeliveryResultItemParams::getCodeType)
                .collect(Collectors.toList());
        assertThat(actualCodeTypes).isEqualTo(List.of(CodeType.UIT));
    }

    @Test
    void testGetItemWithUitHasCisCodeType() {
        Order order = orderFactory.createFashionOrder(null, CIS_1_1, List.of(EAN_1));
        OrderDeliveryResultParams expected = OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.NOT_STARTED)
                .barcodes(List.of())
                .build();

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(orderDeliveryResult)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(expected);

        List<CodeType> actualCodeTypes = orderDeliveryResult.getItems()
                .stream()
                .map(OrderDeliveryResultItemParams::getCodeType)
                .collect(Collectors.toList());
        assertThat(actualCodeTypes).isEqualTo(List.of(CodeType.CIS));
    }

    @Test
    @Disabled
        // TODO enable when EANs are read from order items
    void testGetItemWithUitHasEanCodeType() {
        Order order = orderFactory.createFashionOrder(null, null, List.of(EAN_1));
        OrderDeliveryResultParams expected = OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.NOT_STARTED)
                .barcodes(List.of())
                .build();

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(orderDeliveryResult)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(expected);

        List<CodeType> actualCodeTypes = orderDeliveryResult.getItems()
                .stream()
                .map(OrderDeliveryResultItemParams::getCodeType)
                .collect(Collectors.toList());
        assertThat(actualCodeTypes).isEqualTo(List.of(CodeType.EAN));
    }

    @Test
    void testGetItemWithUitHasNoneCodeType() {
        Order order = orderFactory.createFashionOrder(null, null, null);
        OrderDeliveryResultParams expected = OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.NOT_STARTED)
                .barcodes(List.of())
                .build();

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(orderDeliveryResult)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(expected);

        List<CodeType> actualCodeTypes = orderDeliveryResult.getItems()
                .stream()
                .map(OrderDeliveryResultItemParams::getCodeType)
                .collect(Collectors.toList());
        assertThat(actualCodeTypes).isEqualTo(List.of(CodeType.NONE));
    }

}
