package ru.yandex.market.pvz.internal.domain.order;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.order.OrderParamsMapper;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderItem;
import ru.yandex.market.pvz.core.domain.order.model.cashbox.CashboxPaymentStatus;
import ru.yandex.market.pvz.core.domain.order.model.cashbox.CashboxPaymentSubject;
import ru.yandex.market.pvz.core.domain.order.model.cashbox.OrderCashboxTransactionParams;
import ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryFlow;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.cashbox.CashboxOrderItemsDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.cashbox.CashboxReceiptRequestDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.cashbox.CashboxReceiptResponseDto;
import ru.yandex.market.tpl.common.web.go_zora.GoZoraClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.order.model.cashbox.CashboxPaymentStatus.AWAITING_CASHBOX_STATUS;
import static ru.yandex.market.pvz.core.domain.order.model.cashbox.CashboxPaymentStatus.ERROR;
import static ru.yandex.market.pvz.core.domain.order.model.cashbox.CashboxPaymentStatus.PROCESSED_THROUGH_UI;
import static ru.yandex.market.pvz.core.domain.order.model.cashbox.CashboxPaymentStatus.SUCCESS;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.CIS_1_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.CIS_2_2;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderItemParams.DEFAULT_VAT_TYPE;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_RECIPIENT_EMAIL;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_RECIPIENT_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_2_1;
import static ru.yandex.market.pvz.internal.controller.pi.order.mapper.OrderCashboxDtoMapper.CALLBACK_URL_TEMPLATE;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderCashboxServiceTest {

    private final TestBrandRegionFactory brandRegionFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory testOrderFactory;

    private final OrderCashboxService orderCashboxService;
    private final OrderDeliveryResultCommandService orderDeliveryResultCommandService;
    private final OrderDeliveryResultQueryService orderDeliveryResultQueryService;

    private final OrderParamsMapper orderParamsMapper;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private GoZoraClient goZoraClient;

    @ParameterizedTest
    @CsvSource({
            "true, 200, null, SUCCESS", "false, 200, null, SUCCESS",
            "true, 200, errorMessage, ERROR", "true, 500, null, ERROR"
    })
    void sendCashboxReceiptTest(
            boolean apiSupportAnswer, int zoraHttpStatus, String errorMessage, CashboxPaymentStatus status
    ) throws JsonProcessingException {
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .params(TestPickupPointFactory.PickupPointTestParams.builder()
                        .cashboxApiSupportAnswer(apiSupportAnswer)
                        .build())
                .build());
        var pickupPointId = pickupPoint.getId();
        var orders = createOrders(pickupPoint);
        var order = orders.get(0);

        if (zoraHttpStatus != HttpStatus.OK.value()) {
            when(goZoraClient.post(any(), any())).thenReturn(ResponseEntity.status(HttpStatus.valueOf(zoraHttpStatus)).build());
        } else {
            var response = CashboxReceiptResponseDto.builder()
                    .apiSupportAnswer(apiSupportAnswer)
                    .errorMessage(errorMessage)
                    .transactionId("1234")
                    .serviceLink("yandex.ru")
                    .build();

            when(goZoraClient.post(any(), any())).thenReturn(ResponseEntity.ok(MAPPER.writeValueAsString(response)));
        }
        var transactionParams = orderCashboxService.sendCashboxReceipt(
                pickupPointId, OrderDeliveryType.PAYMENT, order.getId());
        var expectedTransactionParams = OrderCashboxTransactionParams.builder()
                .transactionId(transactionParams.getTransactionId())
                .orderId(transactionParams.getOrderId())
                .deliveryType(OrderDeliveryType.PAYMENT)
                .callbackToken(transactionParams.getCallbackToken())
                .build();

        if (zoraHttpStatus == HttpStatus.OK.value()) {
            expectedTransactionParams.setPaymentStatus(apiSupportAnswer ? AWAITING_CASHBOX_STATUS :
                    PROCESSED_THROUGH_UI);
            expectedTransactionParams.setServiceLink("yandex.ru");
            expectedTransactionParams.setApiSupportAnswer(apiSupportAnswer);
        } else {
            expectedTransactionParams.setErrorMessage("Internal Server Error");
            expectedTransactionParams.setPaymentStatus(ERROR);
            expectedTransactionParams.setApiSupportAnswer(false);
        }

        assertThat(transactionParams).isNotNull();
        assertThat(transactionParams).isEqualToIgnoringNullFields(expectedTransactionParams);
    }

    @Test
    void createFashionRequest() {
        brandRegionFactory.createDefaults();
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .brandingType(PickupPointBrandingType.FULL)
                                .build())
                        .build());
        var fashionOrder = testOrderFactory.createSimpleFashionOrder(false, pickupPoint);
        testOrderFactory.receiveOrder(fashionOrder.getId());

        orderDeliveryResultCommandService.startFitting(fashionOrder.getId());
        orderDeliveryResultCommandService.updateItemFlow(fashionOrder.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);

        orderDeliveryResultCommandService.finishFitting(fashionOrder.getId());
        var actual = orderCashboxService.createRequest(orderParamsMapper.map(fashionOrder),
                fashionOrder.getPickupPoint().getPvzMarketId(), "lera-top", "123");

        var expected = CashboxReceiptRequestDto.builder()
                .orderId(fashionOrder.getExternalId())
                .deliveryDate(fashionOrder.getDeliveryDate())
                .recipientName(DEFAULT_RECIPIENT_NAME)
                .recipientEmail(DEFAULT_RECIPIENT_EMAIL)
                .totalSum(3000d)
                .itemCount(2)
                .callbackUrl(String.format(CALLBACK_URL_TEMPLATE, pickupPoint.getPvzMarketId()))
                .callbackToken("lera-top")
                .cashboxId("123")
                .orderItems(List.of(
                        CashboxOrderItemsDto.builder()
                                .name("Футболка")
                                .paymentSubject(CashboxPaymentSubject.ITEM)
                                .count(1)
                                .price(BigDecimal.valueOf(1000))
                                .sum(BigDecimal.valueOf(1000))
                                .vatType(DEFAULT_VAT_TYPE)
                                .cisValues(List.of(CIS_1_1))
                                .supplierInn("item_1_sup")
                                .supplierName(DEFAULT_SUPPLIER_NAME)
                                .supplierPhone(DEFAULT_SUPPLIER_PHONE)
                                .build(),
                        CashboxOrderItemsDto.builder()
                                .name("Штаны")
                                .paymentSubject(CashboxPaymentSubject.ITEM)
                                .count(1)
                                .price(BigDecimal.valueOf(2000))
                                .sum(BigDecimal.valueOf(2000))
                                .vatType(DEFAULT_VAT_TYPE)
                                .cisValues(List.of(CIS_2_2))
                                .supplierInn("item_2_sup")
                                .supplierName(DEFAULT_SUPPLIER_NAME)
                                .supplierPhone(DEFAULT_SUPPLIER_PHONE)
                                .build()))
                .build();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void createFashionRequestDeliverAllItems() {
        var fashionOrder = testOrderFactory.createSimpleFashionOrder();
        testOrderFactory.receiveOrder(fashionOrder.getId());

        var prevCount = fashionOrder.getItems()
                .stream().filter(item -> !item.isService()).mapToInt(OrderItem::getCount).sum();
        var prevSum = fashionOrder.getItems().stream()
                .filter(item -> !item.isService()).map(item -> item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();

        orderDeliveryResultCommandService.startFitting(fashionOrder.getId());
        orderDeliveryResultCommandService.finishFitting(fashionOrder.getId());

        var deliveryResult = orderDeliveryResultQueryService.get(fashionOrder.getId());

        var request = orderCashboxService.createRequest(orderParamsMapper.map(fashionOrder),
                fashionOrder.getPickupPoint().getPvzMarketId(), "lera-top", "123");
        var deliveredCount = deliveryResult.getItems().stream()
                .filter(item -> item.getFlow() == ItemDeliveryFlow.DELIVERY).count();

        assertThat(prevCount).isEqualTo(deliveredCount);
        assertThat(prevCount).isEqualTo(request.getItemCount());
        assertThat(prevSum).isEqualTo(request.getTotalSum());
        assertThat(request.getItemCount()).isEqualTo(deliveredCount);
    }

    @Test
    void testGetCashboxPaymentStatuses() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var pickupPointId = pickupPoint.getId();
        var orders = createOrders(pickupPoint);
        var order = orders.get(0);
        var cashboxStatuses = orderCashboxService.getCashboxStatuses(pickupPointId, List.of(order.getId()));

        assertThat(cashboxStatuses).hasSize(1);
        assertThat(cashboxStatuses.get(0)).hasFieldOrPropertyWithValue("orderId", order.getId());
        assertThat(cashboxStatuses.get(0)).hasFieldOrPropertyWithValue("cashboxPaymentStatus",
                order.getCashboxPaymentStatus());

        var allCashboxStatuses = orderCashboxService.getCashboxStatuses(pickupPointId,
                StreamEx.of(orders).map(Order::getId).toList());
        assertThat(allCashboxStatuses).hasSize(3);
        assertThat(allCashboxStatuses.get(2)).hasFieldOrPropertyWithValue("cashboxPaymentStatus",
                CashboxPaymentStatus.PENDING);
    }

    @Test
    void testGetAwaitingPaymentOrders() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var pickupPointId = pickupPoint.getId();
        var orders = createOrders(pickupPoint);
        var cashboxStatuses = orderCashboxService.getAwaitingPaymentStatus(pickupPointId);

        assertThat(cashboxStatuses).hasSize(2);
        assertThat(cashboxStatuses.get(0)).hasFieldOrPropertyWithValue("orderId", orders.get(1).getId());
        assertThat(cashboxStatuses.get(0)).hasFieldOrPropertyWithValue("cashboxPaymentStatus",
                CashboxPaymentStatus.AWAITING_CASHBOX_STATUS);

    }

    private List<Order> createOrders(PickupPoint pickupPoint) {
        var order = testOrderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder().pickupPoint(pickupPoint).params(
                        TestOrderFactory.OrderParams.builder().cashboxPaymentStatus(CashboxPaymentStatus.PENDING)
                                .build()).build());
        var order2 = testOrderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder().pickupPoint(pickupPoint).params(
                        TestOrderFactory.OrderParams.builder().cashboxPaymentStatus(CashboxPaymentStatus.AWAITING_CASHBOX_STATUS)
                                .build()).build());
        var order3 = testOrderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder().pickupPoint(pickupPoint).params(
                        TestOrderFactory.OrderParams.builder().cashboxPaymentStatus(SUCCESS)
                                .build()).build());
        return List.of(order, order2, order3);
    }

}
