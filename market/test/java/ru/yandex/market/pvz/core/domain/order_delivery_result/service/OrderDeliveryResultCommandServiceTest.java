package ru.yandex.market.pvz.core.domain.order_delivery_result.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.order.DeliveryServiceType;
import ru.yandex.market.pvz.core.config.PvzCoreInternalConfiguration;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.dispatch.model.DispatchType;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryFlow;
import ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryScanType;
import ru.yandex.market.pvz.core.domain.order_delivery_result.PartialDeliveryStatus;
import ru.yandex.market.pvz.core.domain.order_delivery_result.UnableToScanReason;
import ru.yandex.market.pvz.core.domain.order_delivery_result.barcode.BarcodeType;
import ru.yandex.market.pvz.core.domain.order_delivery_result.model.history.OrderDeliveryHistoryResultRepository;
import ru.yandex.market.pvz.core.domain.order_delivery_result.model.history.OrderDeliveryResultHistory;
import ru.yandex.market.pvz.core.domain.order_delivery_result.params.CodeType;
import ru.yandex.market.pvz.core.domain.order_delivery_result.params.OrderDeliveryResultItemParams;
import ru.yandex.market.pvz.core.domain.order_delivery_result.params.OrderDeliveryResultParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnType;
import ru.yandex.market.pvz.core.domain.shipment.ShipmentCommandService;
import ru.yandex.market.pvz.core.domain.shipment.dispatch.Dispatch;
import ru.yandex.market.pvz.core.domain.shipment.dispatch.DispatchRepository;
import ru.yandex.market.pvz.core.domain.shipment.model.Shipment;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentCreateItemParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentCreateParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentStatus;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentType;
import ru.yandex.market.pvz.core.domain.shipment.returns_and_expired.ReturnsAndExpired;
import ru.yandex.market.pvz.core.domain.shipment.returns_and_expired.ReturnsAndExpiredRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.tpl.common.lrm.client.api.ReturnsApi;
import ru.yandex.market.tpl.common.lrm.client.model.SearchReturn;
import ru.yandex.market.tpl.common.lrm.client.model.SearchReturnsRequest;
import ru.yandex.market.tpl.common.lrm.client.model.SearchReturnsResponse;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.CHECK_SAFE_PACKAGE_IN_LRM_ENABLED;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.GENERATE_SAFE_PACKAGE_BARCODE_FOR_FBY_ENABLED;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.SCAN_CIS_FOR_DELIVERY_ENABLED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.CREATED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.READY_FOR_RETURN;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.TRANSMITTED_TO_RECIPIENT;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryScanType.MANUAL;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryScanType.SCAN;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.PartialDeliveryStatus.DELIVERY_CHECK;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.PartialDeliveryStatus.FITTING_DONE;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.barcode.BarcodeType.FBS;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.barcode.BarcodeType.FBY;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultBarcodeChecker.EXCEEDED_MAXIMUM_NUMBER_BARCODES_MSG;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultBarcodeChecker.NON_UNIQUE_SAFE_PACKAGE_OTHER_ORDER_MSG;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultBarcodeChecker.NON_UNIQUE_SAFE_PACKAGE_THIS_ORDER_MSG;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService.PASS_PAY_STAGE_FULL_RETURN_ERROR_MSG;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService.UNABLE_TO_START_FITTING_MSG;
import static ru.yandex.market.pvz.core.domain.returns.model.ReturnType.WITH_DISADVANTAGES;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.CIS_1_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.CIS_2_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.CIS_2_2;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_1_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_2_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_2_2;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderDeliveryResultCommandServiceTest {

    private static final ReturnType RETURN_REASON = ReturnType.UNSUITABLE;
    private static final String RETURN_COMMENT = "не хочу брать этот хлам";

    private static final List<String> BARCODES = List.of("12312414791", "12414134136");

    private static final long LRM_RETURN_ID = 42;

    private static final String[] IGNORED_FIELDS = {
            "id",
            "items",
            "canBePackaged",
            "isFullReturn",
            "isFullPurchase",
            "deliveryItemsToCheckCodeIds"
    };

    private static final String[] IGNORED_FIELDS_ITEMS_ID_ONLY = {
            "id",
            "items",
            "deliveryItemsToCheckCodeIds"
    };

    private final TestOrderFactory orderFactory;
    private final OrderDeliveryResultQueryService orderDeliveryResultQueryService;
    private final OrderDeliveryResultBarcodeQueryService orderDeliveryResultBarcodeQueryService;
    private final OrderDeliveryResultCommandService orderDeliveryResultCommandService;
    private final ReturnsAndExpiredRepository returnsAndExpiredRepository;
    private final ShipmentCommandService shipmentCommandService;
    private final DispatchRepository dispatchRepository;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;
    private final OrderDeliveryHistoryResultRepository orderDeliveryHistoryResultRepository;
    private final TestableClock testableClock;


    @MockBean
    private ReturnsApi returnsApi;

    @BeforeEach
    void setUpMocks() {
        configurationGlobalCommandService.setValue(CHECK_SAFE_PACKAGE_IN_LRM_ENABLED, true);
        when(returnsApi.searchReturns(any())).thenReturn(new SearchReturnsResponse());
    }

    @Test
    void unableToStartFittingNotReceivedOrder() {
        Order order = orderFactory.createSimpleFashionOrder();

        assertThatThrownBy(() -> orderDeliveryResultCommandService.startFitting(order.getId()))
                .isExactlyInstanceOf(TplInvalidActionException.class)
                .hasMessage(String.format(UNABLE_TO_START_FITTING_MSG, CREATED.getDescription()));
    }

    @Test
    void unableToStartFittingDeliveredOrder() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());
        orderFactory.verifyOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.BARCODE, OrderPaymentType.PREPAID);

        assertThatThrownBy(() -> orderDeliveryResultCommandService.startFitting(order.getId()))
                .isExactlyInstanceOf(TplInvalidActionException.class)
                .hasMessage(String.format(UNABLE_TO_START_FITTING_MSG, TRANSMITTED_TO_RECIPIENT.getDescription()));
    }

    @Test
    void unableToStartFittingCancelledOrder() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());
        orderFactory.cancelOrder(order.getId());
        assertThatThrownBy(() -> orderDeliveryResultCommandService.startFitting(order.getId()))
                .isExactlyInstanceOf(TplInvalidActionException.class)
                .hasMessage(String.format(UNABLE_TO_START_FITTING_MSG, READY_FOR_RETURN.getDescription()));
    }

    @Test
    void testUpdateItemFlow() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.CREATED)
                .barcodes(List.of())
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS_ITEMS_ID_ONLY)
                .isEqualTo(orderDeliveryResult);

        List<OrderDeliveryResultItemParams> items = orderDeliveryResult.getItems();
        items.forEach(item -> {
            item.setOrderItemId(null);
            item.setPriorityNumber(0);
            item.setItemInstanceId(0);
        });

        assertThat(List.of(
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_1_1)
                        .cis(CIS_1_1)
                        .codeType(CodeType.UIT)
                        .scanType(SCAN)
                        .flow(ItemDeliveryFlow.RETURN)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_2_1)
                        .cis(CIS_2_1)
                        .codeType(CodeType.UIT)
                        .scanType(SCAN)
                        .flow(ItemDeliveryFlow.RETURN)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_2_2)
                        .cis(CIS_2_2)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build()
        )).containsExactlyInAnyOrderElementsOf(items);
    }

    @Test
    void testUpdateItemFlowByInstanceId() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());
        orderDeliveryResultCommandService.startFitting(order.getId());

        OrderDeliveryResultParams deliveryResult = orderDeliveryResultQueryService.get(order.getId());
        long itemInstanceId1 = deliveryResult.getItems().get(0).getItemInstanceId();
        long itemInstanceId2 = deliveryResult.getItems().get(2).getItemInstanceId();

        orderDeliveryResultCommandService.updateItemFlow(order.getId(), null, itemInstanceId1,
                ItemDeliveryFlow.RETURN, ItemDeliveryScanType.SCAN, null);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), null, itemInstanceId2,
                ItemDeliveryFlow.RETURN, ItemDeliveryScanType.MANUAL, null);

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.CREATED)
                .barcodes(List.of())
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS_ITEMS_ID_ONLY)
                .isEqualTo(orderDeliveryResult);

        List<OrderDeliveryResultItemParams> items = orderDeliveryResult.getItems();
        items.forEach(item -> {
            item.setOrderItemId(null);
            item.setPriorityNumber(0);
            item.setItemInstanceId(0);
        });

        assertThat(items).containsExactlyInAnyOrderElementsOf(List.of(
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_1_1)
                        .cis(CIS_1_1)
                        .codeType(CodeType.UIT)
                        .scanType(ItemDeliveryScanType.SCAN)
                        .flow(ItemDeliveryFlow.RETURN)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_2_1)
                        .cis(CIS_2_1)
                        .codeType(CodeType.UIT)
                        .scanType(ItemDeliveryScanType.MANUAL)
                        .flow(ItemDeliveryFlow.RETURN)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_2_2)
                        .cis(CIS_2_2)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build()
        ));
    }

    @Test
    void testRevertFlow() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.DELIVERY);

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.CREATED)
                .barcodes(List.of())
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(orderDeliveryResult);

        List<OrderDeliveryResultItemParams> items = orderDeliveryResult.getItems();
        items.forEach(item -> {
            item.setOrderItemId(null);
            item.setPriorityNumber(0);
            item.setItemInstanceId(0);
        });

        assertThat(List.of(
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_1_1)
                        .cis(CIS_1_1)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_2_1)
                        .cis(CIS_2_1)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_2_2)
                        .cis(CIS_2_2)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build()
        )).containsExactlyInAnyOrderElementsOf(items);
    }

    @Test
    void testUpdateItemComment() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemReturnComment(
                order.getId(), UIT_2_1, null, RETURN_REASON, RETURN_COMMENT);

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.CREATED)
                .barcodes(List.of())
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS_ITEMS_ID_ONLY)
                .isEqualTo(orderDeliveryResult);

        List<OrderDeliveryResultItemParams> items = orderDeliveryResult.getItems();
        items.forEach(item -> {
            item.setOrderItemId(null);
            item.setPriorityNumber(0);
            item.setItemInstanceId(0);
        });

        assertThat(List.of(
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_1_1)
                        .cis(CIS_1_1)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_2_1)
                        .cis(CIS_2_1)
                        .codeType(CodeType.UIT)
                        .scanType(SCAN)
                        .flow(ItemDeliveryFlow.RETURN)
                        .returnReason(RETURN_REASON)
                        .comment(RETURN_COMMENT)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_2_2)
                        .cis(CIS_2_2)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build()
        )).containsExactlyInAnyOrderElementsOf(items);
    }

    @Test
    void testUpdateItemAndThenUpdateItemReasonWithoutComment() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemReturnComment(
                order.getId(), UIT_2_1, null, RETURN_REASON, RETURN_COMMENT);

        orderDeliveryResultCommandService.updateItemReturnComment(
                order.getId(), UIT_2_1, null, WITH_DISADVANTAGES, null);

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        List<OrderDeliveryResultItemParams> items = orderDeliveryResult.getItems();
        items.forEach(item -> {
            item.setOrderItemId(null);
            item.setPriorityNumber(0);
            item.setItemInstanceId(0);
        });

        assertThat(List.of(
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_1_1)
                        .cis(CIS_1_1)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_2_1)
                        .cis(CIS_2_1)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.RETURN)
                        .scanType(SCAN)
                        .returnReason(WITH_DISADVANTAGES)
                        .comment(RETURN_COMMENT)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_2_2)
                        .cis(CIS_2_2)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build()
        )).containsExactlyInAnyOrderElementsOf(items);
    }

    @Test
    void testUpdateItemCommentViaItemInstanceId() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());
        orderDeliveryResultCommandService.startFitting(order.getId());

        List<OrderDeliveryResultItemParams> items = orderDeliveryResultQueryService.get(order.getId()).getItems();
        OrderDeliveryResultItemParams orderItem = items.get(1);

        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_2, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemReturnComment(order.getId(), null, orderItem.getItemInstanceId(),
                RETURN_REASON, RETURN_COMMENT);

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.CREATED)
                .barcodes(List.of())
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS_ITEMS_ID_ONLY)
                .isEqualTo(orderDeliveryResult);

        List<OrderDeliveryResultItemParams> actualItems = orderDeliveryResult.getItems();
        actualItems.forEach(item -> {
            item.setOrderItemId(null);
            item.setPriorityNumber(0);
        });

        assertThat(actualItems).containsExactlyInAnyOrderElementsOf(List.of(
                OrderDeliveryResultItemParams.builder()
                        .itemInstanceId(items.get(0).getItemInstanceId())
                        .uit(UIT_1_1)
                        .cis(CIS_1_1)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .itemInstanceId(orderItem.getItemInstanceId())
                        .uit(UIT_2_2)
                        .cis(CIS_2_2)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.RETURN)
                        .scanType(SCAN)
                        .returnReason(RETURN_REASON)
                        .comment(RETURN_COMMENT)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .itemInstanceId(items.get(2).getItemInstanceId())
                        .uit(UIT_2_1)
                        .cis(CIS_2_1)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build()
        ));
    }

    @Test
    void testUpdateItemCommentViaItemInstanceIdFbs() {
        Order order = orderFactory.createSimpleFbsFashionOrder(true);
        orderFactory.receiveOrder(order.getId());
        orderDeliveryResultCommandService.startFitting(order.getId());

        List<OrderDeliveryResultItemParams> items = orderDeliveryResultQueryService.get(order.getId()).getItems();
        OrderDeliveryResultItemParams orderItem = items.get(1);

        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_2, orderItem.getItemInstanceId(),
                ItemDeliveryFlow.RETURN, SCAN, null);
        orderDeliveryResultCommandService.updateItemReturnComment(order.getId(), null, orderItem.getItemInstanceId(),
                RETURN_REASON, RETURN_COMMENT);

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.CREATED)
                .barcodes(List.of())
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS_ITEMS_ID_ONLY)
                .isEqualTo(orderDeliveryResult);

        List<OrderDeliveryResultItemParams> actualItems = orderDeliveryResult.getItems();
        actualItems.forEach(item -> {
            item.setOrderItemId(null);
            item.setPriorityNumber(0);
        });

        assertThat(actualItems).containsExactlyInAnyOrderElementsOf(List.of(
                OrderDeliveryResultItemParams.builder()
                        .itemInstanceId(items.get(0).getItemInstanceId())
                        .cis(CIS_1_1)
                        .codeType(CodeType.CIS)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .itemInstanceId(orderItem.getItemInstanceId())
                        .cis(CIS_2_2)
                        .codeType(CodeType.CIS)
                        .flow(ItemDeliveryFlow.RETURN)
                        .scanType(SCAN)
                        .returnReason(RETURN_REASON)
                        .comment(RETURN_COMMENT)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .itemInstanceId(items.get(2).getItemInstanceId())
                        .cis(CIS_2_1)
                        .codeType(CodeType.CIS)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build()
        ));
    }

    @Test
    void testPay() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.PAYED)
                .barcodes(List.of())
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(orderDeliveryResult);
    }

    @Test
    void testPayWithoutReturnedItems() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.PAYED)
                .barcodes(List.of())
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(orderDeliveryResult);
    }

    @Test
    void testCannotEditAfterPayment() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        assertThatThrownBy(() ->
                orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN));
    }

    @Test
    void testCannotPayFullyReturned() {
        Order order = orderFactory.createSimpleFashionOrder(true);
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_2, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        assertThatThrownBy(() -> orderDeliveryResultCommandService.pay(order.getId()));
    }

    @Test
    void testPackageNonPrepaid() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderDeliveryResultCommandService.packageReturn(order.getId(), BARCODES);

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.PACKAGED)
                .barcodes(BARCODES)
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS_ITEMS_ID_ONLY)
                .isEqualTo(orderDeliveryResult);

        List<ReturnsAndExpired> returnsAndExpireds =
                returnsAndExpiredRepository.findAllByPickupPointId(order.getPickupPoint().getId());
        assertThat(returnsAndExpireds).hasSize(1);

        ReturnsAndExpired returnsAndExpired = returnsAndExpireds.get(0);
        assertThat(returnsAndExpired).isEqualToComparingFieldByField(ReturnsAndExpired.builder()
                .idWithType(returnsAndExpired.getIdWithType())
                .name(returnsAndExpired.getName())  // todo remove comparison with itself
                .email(returnsAndExpired.getEmail())
                .phone(returnsAndExpired.getPhone())
                .deliveryServiceType(DeliveryServiceType.MARKET_COURIER)
                .pickupPointId(order.getPickupPoint().getId())
                .barcode(BARCODES.get(0) + "\n" + BARCODES.get(1))
                .placesCount(2)
                .senderName(returnsAndExpired.getSenderName())
                .senderId(returnsAndExpired.getSenderId())
                .placeBarcodes(BARCODES)
                .build()
        );
    }

    @Test
    void tryToScanSafePackageWithExistentBarcode() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderDeliveryResultCommandService.scanSafePackage(order.getId(), BARCODES.get(0));

        assertThatThrownBy(() -> orderDeliveryResultCommandService.scanSafePackage(order.getId(), BARCODES.get(0)))
                .isExactlyInstanceOf(TplIllegalStateException.class)
                .hasMessage(NON_UNIQUE_SAFE_PACKAGE_THIS_ORDER_MSG);
    }

    @Test
    void tryToScanSafePackageWithDeletedBarcode() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderDeliveryResultCommandService.scanSafePackage(order.getId(), BARCODES.get(0));
        var packageId = orderDeliveryResultBarcodeQueryService.getPackages(List.of(BARCODES.get(0))).get(0).getId();
        orderDeliveryResultCommandService.deleteSafePackage(order.getId(), packageId);

        orderDeliveryResultCommandService.scanSafePackage(order.getId(), BARCODES.get(0));
    }

    @Test
    void tryToScanTwoSafePackageWithOneReturnItem() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderDeliveryResultCommandService.scanSafePackage(order.getId(), BARCODES.get(0));

        assertThatThrownBy(() -> orderDeliveryResultCommandService.scanSafePackage(order.getId(), BARCODES.get(1)))
                .isExactlyInstanceOf(TplIllegalStateException.class)
                .hasMessage(EXCEEDED_MAXIMUM_NUMBER_BARCODES_MSG);
    }

    @Test
    void tryToScanSafePackageWithExistentBarcodeInLrm() {
        var request = new SearchReturnsRequest();
        request.setBoxExternalIds(List.of(BARCODES.get(0)));
        var response = new SearchReturnsResponse();
        response.setReturns(List.of(new SearchReturn()));
        when(returnsApi.searchReturns(request)).thenReturn(response);

        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());

        assertThatThrownBy(() -> orderDeliveryResultCommandService.scanSafePackage(order.getId(), BARCODES.get(0)))
                .isExactlyInstanceOf(TplIllegalStateException.class)
                .hasMessage(NON_UNIQUE_SAFE_PACKAGE_OTHER_ORDER_MSG);
    }

    @Test
    void testPackageNonPrepaidSafeBarcodes() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderDeliveryResultCommandService.scanSafePackage(order.getId(), BARCODES.get(0));
        orderDeliveryResultCommandService.scanSafePackage(order.getId(), BARCODES.get(1));
        orderDeliveryResultCommandService.commitPackaging(order.getId());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.PACKAGED)
                .barcodes(BARCODES)
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS_ITEMS_ID_ONLY)
                .isEqualTo(orderDeliveryResult);

        List<ReturnsAndExpired> returnsAndExpireds =
                returnsAndExpiredRepository.findAllByPickupPointId(order.getPickupPoint().getId());
        assertThat(returnsAndExpireds).hasSize(1);

        ReturnsAndExpired returnsAndExpired = returnsAndExpireds.get(0);
        assertThat(returnsAndExpired).isEqualToComparingFieldByField(ReturnsAndExpired.builder()
                .idWithType(returnsAndExpired.getIdWithType())
                .name(returnsAndExpired.getName())
                .email(returnsAndExpired.getEmail())
                .phone(returnsAndExpired.getPhone())
                .deliveryServiceType(DeliveryServiceType.MARKET_COURIER)
                .pickupPointId(order.getPickupPoint().getId())
                .barcode(BARCODES.get(0) + "\n" + BARCODES.get(1))
                .placesCount(2)
                .senderId(returnsAndExpired.getSenderId())
                .senderName(returnsAndExpired.getSenderName())
                .placeBarcodes(BARCODES)
                .build()
        );
    }

    @Test
    void testPackagePrepaid() {
        Order order = orderFactory.createSimpleFashionOrder(true);
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderDeliveryResultCommandService.packageReturn(order.getId(), BARCODES);

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.PACKAGED)
                .barcodes(BARCODES)
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS_ITEMS_ID_ONLY)
                .isEqualTo(orderDeliveryResult);
    }

    @Test
    void testPackagePrepaidSafeBarcodes() {
        Order order = orderFactory.createSimpleFashionOrder(true);
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderDeliveryResultCommandService.scanSafePackage(order.getId(), BARCODES.get(0));
        orderDeliveryResultCommandService.scanSafePackage(order.getId(), BARCODES.get(1));
        orderDeliveryResultCommandService.commitPackaging(order.getId());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.PACKAGED)
                .barcodes(BARCODES)
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS_ITEMS_ID_ONLY)
                .isEqualTo(orderDeliveryResult);
    }

    @Test
    void testPackageNonPrepaidWithoutReturns() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderDeliveryResultCommandService.packageReturn(order.getId(), List.of());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.FULLY_PURCHASED)
                .barcodes(List.of())
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(orderDeliveryResult);

        assertThat(returnsAndExpiredRepository.findAllByPickupPointId(order.getPickupPoint().getId())).isEmpty();
    }

    @Test
    void testPackageNonPrepaidWithoutReturnsSafeBarcodes() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderDeliveryResultCommandService.commitPackaging(order.getId());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.FULLY_PURCHASED)
                .barcodes(List.of())
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(orderDeliveryResult);

        assertThat(returnsAndExpiredRepository.findAllByPickupPointId(order.getPickupPoint().getId())).isEmpty();
    }

    @Test
    void testPackageNonPrepaidReturningAllItems() {
        Order order = orderFactory.createSimpleFashionOrder();
        order = orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_2, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.packageReturn(order.getId(), List.of());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.PACKAGED)
                .barcodes(List.of())
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(orderDeliveryResult);
    }

    @Test
    void testPackageNonPrepaidReturningAllItemsSafeBarcodes() {
        Order order = orderFactory.createSimpleFashionOrder();
        order = orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_2, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.scanSafePackage(order.getId(), BARCODES.get(0));
        orderDeliveryResultCommandService.scanSafePackage(order.getId(), BARCODES.get(1));
        orderDeliveryResultCommandService.commitPackaging(order.getId());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.PACKAGED)
                .barcodes(BARCODES)
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(orderDeliveryResult);
    }

    @Test
    void testPackagePrepaidWithoutReturns() {
        Order order = orderFactory.createSimpleFashionOrder(true);
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderDeliveryResultCommandService.packageReturn(order.getId(), List.of());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.FULLY_PURCHASED)
                .barcodes(List.of())
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(orderDeliveryResult);
    }

    @Test
    void testPackagePrepaidWithoutReturnsSafeBarcodes() {
        Order order = orderFactory.createSimpleFashionOrder(true);
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderDeliveryResultCommandService.commitPackaging(order.getId());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.FULLY_PURCHASED)
                .barcodes(List.of())
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(orderDeliveryResult);
    }

    @Test
    void testPackagePrepaidWithoutReturnsButWithBarcodes() {
        Order order = orderFactory.createSimpleFashionOrder(true);
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.finishFitting(order.getId());

        assertThatThrownBy(() -> orderDeliveryResultCommandService.packageReturn(order.getId(), BARCODES));
    }

    @Test
    void testPackagePrepaidWithoutReturnsButWithBarcodesSafePackage() {
        Order order = orderFactory.createSimpleFashionOrder(true);
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.finishFitting(order.getId());

        assertThatThrownBy(() -> orderDeliveryResultCommandService.scanSafePackage(order.getId(), BARCODES.get(0)));
    }

    @Test
    void testPackageNonPrepaidWithReturnsButWithoutBarcodesSafePackage() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());

        assertThatThrownBy(() -> orderDeliveryResultCommandService.commitPackaging(order.getId()));
    }

    @Test
    void testCannotEditOrPayAfterPackaging() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderDeliveryResultCommandService.packageReturn(order.getId(), List.of());

        assertThatThrownBy(() -> orderDeliveryResultCommandService.pay(order.getId()));
        assertThatThrownBy(() ->
                orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN));
    }

    @Test
    void testCannotEditOrPayAfterPackagingSafePackage() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderDeliveryResultCommandService.commitPackaging(order.getId());

        assertThatThrownBy(() -> orderDeliveryResultCommandService.pay(order.getId()));
        assertThatThrownBy(() ->
                orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN));
    }

    @Test
    void testDeletePackageSafePackage() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());
        long pickupPointId = order.getPickupPoint().getId();

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderDeliveryResultCommandService.scanSafePackage(order.getId(), BARCODES.get(0));
        var packageId = orderDeliveryResultBarcodeQueryService.getPackages(List.of(BARCODES.get(0))).get(0).getId();
        orderDeliveryResultCommandService.deleteSafePackage(order.getId(), packageId);
        orderDeliveryResultCommandService.scanSafePackage(order.getId(), BARCODES.get(1));
        orderDeliveryResultCommandService.commitPackaging(order.getId());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.PACKAGED)
                .barcodes(List.of(BARCODES.get(1)))
                .build())
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS_ITEMS_ID_ONLY)
                .isEqualTo(orderDeliveryResult);

        List<ReturnsAndExpired> returnsAndExpireds =
                returnsAndExpiredRepository.findAllByPickupPointId(pickupPointId);
        assertThat(returnsAndExpireds).hasSize(1);

        ReturnsAndExpired returnsAndExpired = returnsAndExpireds.get(0);
        assertThat(returnsAndExpired).isEqualToComparingFieldByField(ReturnsAndExpired.builder()
                .idWithType(returnsAndExpired.getIdWithType())
                .name(returnsAndExpired.getName())
                .email(returnsAndExpired.getEmail())
                .deliveryServiceType(DeliveryServiceType.MARKET_COURIER)
                .phone(returnsAndExpired.getPhone())
                .pickupPointId(pickupPointId)
                .barcode(BARCODES.get(1))
                .placesCount(1)
                .senderName(returnsAndExpired.getSenderName())
                .senderId(returnsAndExpired.getSenderId())
                .placeBarcodes(List.of(BARCODES.get(1)))
                .build()
        );

        PickupPointRequestData pickupPointAuthInfo = new PickupPointRequestData(
                pickupPointId,
                order.getPickupPoint().getPvzMarketId(),
                order.getPickupPoint().getName(),
                1L,
                order.getPickupPoint().getTimeOffset(),
                order.getPickupPoint().getStoragePeriod()
        );
        Shipment shipment = shipmentCommandService.createShipment(pickupPointAuthInfo, new ShipmentCreateParams(
                ShipmentType.DISPATCH, ShipmentStatus.FINISHED,
                List.of(
                        getSafePackageShipmentItem(order.getExternalId())
                )));
        List<Dispatch> dispatches =
                dispatchRepository.findAllByPickupPointIdAndShipmentId(pickupPointId, shipment.getId());
        assertThat(dispatches.get(0).getBarcode()).isEqualTo(BARCODES.get(1));
    }

    private ShipmentCreateItemParams getSafePackageShipmentItem(String orderId) {
        ShipmentCreateItemParams shipmentCreateItemDto = new ShipmentCreateItemParams(orderId);
        shipmentCreateItemDto.setType(DispatchType.SAFE_PACKAGE);
        return shipmentCreateItemDto;
    }

    @Test
    void testSetLrmReturnId() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderDeliveryResultCommandService.bindLrmReturnId(order.getId(), LRM_RETURN_ID);

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(orderDeliveryResult.getLrmReturnId()).isEqualTo(LRM_RETURN_ID);
    }

    @Test
    void testReturnToFitting() {
        Order order = orderFactory.createSimpleFashionOrder(false);
        long id = order.getId();
        orderFactory.receiveOrder(id);

        orderDeliveryResultCommandService.startFitting(id);
        orderDeliveryResultCommandService.finishFitting(id);
        orderDeliveryResultCommandService.returnToFitting(id);

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(id);
        OrderDeliveryResultParams expected = OrderDeliveryResultParams.builder()
                .orderId(id)
                .status(PartialDeliveryStatus.CREATED)
                .barcodes(List.of())
                .build();

        assertThat(orderDeliveryResult)
                .usingRecursiveComparison()
                .ignoringFields("items", "isFullPurchase", "id", "deliveryItemsToCheckCodeIds")
                .isEqualTo(expected);
    }

    @Test
    void testCannotReturnToFittingFromCreated() {
        Order order = orderFactory.createSimpleFashionOrder(false);
        long id = order.getId();
        orderFactory.receiveOrder(id);

        orderDeliveryResultCommandService.startFitting(id);
        assertThatThrownBy(() -> orderDeliveryResultCommandService.returnToFitting(id));
    }

    @Test
    void testCannotReturnToFittingFromPaid() {
        Order order = orderFactory.createSimpleFashionOrder(false);
        long id = order.getId();
        orderFactory.receiveOrder(id);

        orderDeliveryResultCommandService.startFitting(id);
        orderDeliveryResultCommandService.finishFitting(id);
        orderDeliveryResultCommandService.pay(id);

        assertThatThrownBy(() -> orderDeliveryResultCommandService.returnToFitting(id));
    }

    @Test
    void passPayStageForFullReturn() {
        Order order = orderFactory.createSimpleFashionOrder(false);
        long id = order.getId();
        orderFactory.receiveOrder(id);

        var orderDeliveryResult = orderDeliveryResultCommandService.startFitting(id);
        orderDeliveryResultCommandService.updateItemFlow(id, UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(id, UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(id, UIT_2_2, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(id);
        var actual = orderDeliveryResultCommandService.passPayStageForFullReturn(id);

        var expected = OrderDeliveryResultParams.builder()
                .id(orderDeliveryResult.getId())
                .orderId(order.getId())
                .status(PartialDeliveryStatus.PAYED)
                .barcodes(List.of())
                .isFullReturn(true)
                .isFullPurchase(false)
                .canBePackaged(true)
                .build();

        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("items", "deliveryItemsToCheckCodeIds")
                .isEqualTo(expected);
    }

    @Test
    void tryToPassPayStageForFullReturnWithWrongStatus() {
        Order order = orderFactory.createSimpleFashionOrder(false);
        long id = order.getId();
        orderFactory.receiveOrder(id);

        orderDeliveryResultCommandService.startFitting(id);
        orderDeliveryResultCommandService.updateItemFlow(id, UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(id, UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(id, UIT_2_2, ItemDeliveryFlow.RETURN);
        assertThatThrownBy(() -> orderDeliveryResultCommandService.passPayStageForFullReturn(id))
                .isExactlyInstanceOf(TplInvalidActionException.class)
                .hasMessage(String.format(PASS_PAY_STAGE_FULL_RETURN_ERROR_MSG, PartialDeliveryStatus.CREATED, true));
    }

    @Test
    void tryToPassPayStageForFullReturnWithNotAllItemsRefused() {
        Order order = orderFactory.createSimpleFashionOrder(false);
        long id = order.getId();
        orderFactory.receiveOrder(id);

        orderDeliveryResultCommandService.startFitting(id);
        orderDeliveryResultCommandService.updateItemFlow(id, UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(id, UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(id);
        assertThatThrownBy(() -> orderDeliveryResultCommandService.passPayStageForFullReturn(id))
                .isExactlyInstanceOf(TplInvalidActionException.class)
                .hasMessage(String.format(
                        PASS_PAY_STAGE_FULL_RETURN_ERROR_MSG, FITTING_DONE, false));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testGenerateBarcode(boolean isFbs) {
        configurationGlobalCommandService.setValue(GENERATE_SAFE_PACKAGE_BARCODE_FOR_FBY_ENABLED, true);
        Order order = orderFactory.createSimpleFbsFashionOrder(isFbs);
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());

        var deliveryResult = orderDeliveryResultQueryService.get(order.getId());
        long itemInstanceId1 = deliveryResult.getItems().get(0).getItemInstanceId();
        long itemInstanceId2 = deliveryResult.getItems().get(2).getItemInstanceId();
        orderDeliveryResultCommandService.updateItemFlow(
                order.getId(), UIT_1_1, itemInstanceId1, ItemDeliveryFlow.RETURN, SCAN, null);
        orderDeliveryResultCommandService.updateItemFlow(
                order.getId(), UIT_2_1, itemInstanceId2, ItemDeliveryFlow.RETURN, SCAN, null);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());

        OrderDeliveryResultParams deliveryResultWithBarcode =
                orderDeliveryResultCommandService.generateAndScanBarcode(order.getId(), isFbs ? FBS : FBY);
        assertThat(deliveryResultWithBarcode.getBarcodes())
                .hasSize(1)
                .allMatch(s -> s.startsWith(isFbs ? FBS.getPrefix() : FBY.getPrefix()));

        OrderDeliveryResultParams actualDeliveryResult =
                orderDeliveryResultCommandService.commitPackaging(order.getId());

        OrderDeliveryResultParams expectedDeliveryResult = OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.PACKAGED)
                .build();

        assertThat(actualDeliveryResult)
                .usingRecursiveComparison()
                .ignoringFields("items", "id", "canBePackaged", "barcodes", "deliveryItemsToCheckCodeIds")
                .isEqualTo(expectedDeliveryResult);
        assertThat(actualDeliveryResult.getBarcodes())
                .hasSize(1)
                .allMatch(s -> s.startsWith(isFbs ? FBS.getPrefix() : FBY.getPrefix()));
    }

    @Test
    void tryToGenerateBarcodeForFbyOrderWithDisabledToggle() {
        configurationGlobalCommandService.setValue(GENERATE_SAFE_PACKAGE_BARCODE_FOR_FBY_ENABLED, false);
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());

        var deliveryResult = orderDeliveryResultQueryService.get(order.getId());
        long itemInstanceId1 = deliveryResult.getItems().get(0).getItemInstanceId();
        long itemInstanceId2 = deliveryResult.getItems().get(2).getItemInstanceId();
        orderDeliveryResultCommandService.updateItemFlow(
                order.getId(), UIT_1_1, itemInstanceId1, ItemDeliveryFlow.RETURN, SCAN, null);
        orderDeliveryResultCommandService.updateItemFlow(
                order.getId(), UIT_2_1, itemInstanceId2, ItemDeliveryFlow.RETURN, SCAN, null);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());

        assertThatThrownBy(() -> orderDeliveryResultCommandService.generateAndScanBarcode(order.getId(), FBY))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    @ParameterizedTest
    @EnumSource(BarcodeType.class)
    void testGenerateBarcode(BarcodeType barcodeType) {
        String barcode = orderDeliveryResultCommandService.generateBarcode(barcodeType);
        assertThat(barcode)
                .startsWith(barcodeType.getPrefix());
    }

    @Test
    void testHasItemsToScanForDelivery() {
        Order order = orderFactory.createFashionOrder(null, CIS_1_1, null);
        orderFactory.receiveOrder(order.getId());
        orderDeliveryResultCommandService.startFitting(order.getId());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(orderDeliveryResult.getDeliveryItemsToCheckCodeIds())
                .containsExactly(orderDeliveryResult.getItems().get(0).getItemInstanceId());
    }


    @Test
    void testNoItemsToScanForDeliveryIfNoCis() {
        Order order = orderFactory.createFashionOrder(null, null, null);
        orderFactory.receiveOrder(order.getId());
        orderDeliveryResultCommandService.startFitting(order.getId());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(orderDeliveryResult.getDeliveryItemsToCheckCodeIds()).isEmpty();
    }

    @Test
    void testNoItemsToScanForDeliveryIfFlowIsReturn() {
        Order order = orderFactory.createFashionOrder(null, null, null);
        orderFactory.receiveOrder(order.getId());
        orderDeliveryResultCommandService.startFitting(order.getId());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        orderDeliveryResult = orderDeliveryResultCommandService.updateItemFlow(
                order.getId(), null,
                orderDeliveryResult.getItems().get(0).getItemInstanceId(),
                ItemDeliveryFlow.RETURN, SCAN, null);

        assertThat(orderDeliveryResult.getDeliveryItemsToCheckCodeIds()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"false,FITTING_DONE", "true,DELIVERY_CHECK"})
    void testFinishFittingWithEnabledOrDisabledDeliveryCisScanFlag(boolean flagValue, PartialDeliveryStatus status) {
        configurationGlobalCommandService.setValue(SCAN_CIS_FOR_DELIVERY_ENABLED, flagValue);

        Order order = orderFactory.createFashionOrder(CIS_1_1, CIS_2_1, CIS_2_2);
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.finishFitting(order.getId());

        assertThat(deliveryResult.getStatus()).isEqualTo(status);
    }

    @Test
    void testFinishFittingWithEnabledDeliveryCisScanFlagAndNoRequiredToScanItems() {
        configurationGlobalCommandService.setValue(SCAN_CIS_FOR_DELIVERY_ENABLED, true);

        Order order = orderFactory.createFashionOrder(CIS_1_1, null);
        orderFactory.receiveOrder(order.getId());

        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.startFitting(order.getId());
        Long itemIdWithCis = deliveryResult.getItems().stream()
                .filter(item -> item.getCis() != null)
                .map(OrderDeliveryResultItemParams::getItemInstanceId)
                .findFirst().orElseThrow();

        orderDeliveryResultCommandService.updateItemFlow(
                order.getId(), null, itemIdWithCis, ItemDeliveryFlow.RETURN, SCAN, null);

        deliveryResult = orderDeliveryResultCommandService.finishFitting(order.getId());

        assertThat(deliveryResult.getStatus()).isEqualTo(FITTING_DONE);
    }

    @Test
    void testCheckCodeSuccessfully() {
        configurationGlobalCommandService.setValue(SCAN_CIS_FOR_DELIVERY_ENABLED, true);

        Order order = orderFactory.createFashionOrder(CIS_1_1, CIS_2_1);
        orderFactory.receiveOrder(order.getId());
        orderDeliveryResultCommandService.startFitting(order.getId());
        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.finishFitting(order.getId());

        List<Long> itemIds = deliveryResult.getItems().stream()
                .map(OrderDeliveryResultItemParams::getItemInstanceId)
                .collect(Collectors.toList());

        orderDeliveryResultCommandService.checkDeliveryItemCode(order.getId(), itemIds.get(0), SCAN, null);
        orderDeliveryResultCommandService.checkDeliveryItemCode(order.getId(), itemIds.get(1), SCAN, null);

        deliveryResult = orderDeliveryResultCommandService.finishFitting(order.getId());
        assertThat(deliveryResult.getStatus()).isEqualTo(FITTING_DONE);
    }

    @Test
    void testCheckCodeFailAllItems() {
        configurationGlobalCommandService.setValue(SCAN_CIS_FOR_DELIVERY_ENABLED, true);

        Order order = orderFactory.createFashionOrder(CIS_1_1, CIS_2_1);
        orderFactory.receiveOrder(order.getId());
        orderDeliveryResultCommandService.startFitting(order.getId());
        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.finishFitting(order.getId());

        List<Long> itemIds = deliveryResult.getItems().stream()
                .map(OrderDeliveryResultItemParams::getItemInstanceId)
                .collect(Collectors.toList());

        orderDeliveryResultCommandService.checkDeliveryItemCode(order.getId(), itemIds.get(0), MANUAL,
                UnableToScanReason.CIS_ABSENT);
        deliveryResult = orderDeliveryResultCommandService.checkDeliveryItemCode(order.getId(), itemIds.get(1), MANUAL,
                UnableToScanReason.CIS_ABSENT);

        assertThat(deliveryResult.getStatus()).isEqualTo(PartialDeliveryStatus.CREATED);
    }

    @Test
    void testCheckCodeFailsInInvalidStatus() {
        configurationGlobalCommandService.setValue(SCAN_CIS_FOR_DELIVERY_ENABLED, true);

        Order order = orderFactory.createFashionOrder(CIS_1_1, CIS_2_1);
        orderFactory.receiveOrder(order.getId());

        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.startFitting(order.getId());
        List<Long> itemIds = deliveryResult.getItems().stream()
                .map(OrderDeliveryResultItemParams::getItemInstanceId)
                .collect(Collectors.toList());

        assertThat(deliveryResult.getStatus()).isNotEqualTo(DELIVERY_CHECK);
        assertThatThrownBy(() -> orderDeliveryResultCommandService.checkDeliveryItemCode(
                order.getId(), itemIds.get(0), SCAN, null
        )).isExactlyInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("Unable to check item code in status");
    }

    @Test
    void testCheckCodeFailsForItemsNotRequiredToScan() {
        configurationGlobalCommandService.setValue(SCAN_CIS_FOR_DELIVERY_ENABLED, true);

        Order order = orderFactory.createFashionOrder(CIS_1_1, null, CIS_2_2);
        orderFactory.receiveOrder(order.getId());
        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.startFitting(order.getId());

        Long itemIdWithCis = deliveryResult.getItems().stream()
                .filter(item -> CIS_1_1.equals(item.getCis()))
                .map(OrderDeliveryResultItemParams::getItemInstanceId)
                .findFirst().orElseThrow();

        Long itemIdWithoutCis = deliveryResult.getItems().stream()
                .filter(item -> item.getCis() == null)
                .map(OrderDeliveryResultItemParams::getItemInstanceId)
                .findFirst().orElseThrow();

        orderDeliveryResultCommandService.updateItemFlow(
                order.getId(), null, itemIdWithCis, ItemDeliveryFlow.RETURN, SCAN, null);

        deliveryResult = orderDeliveryResultCommandService.finishFitting(order.getId());
        assertThat(deliveryResult.getStatus()).isEqualTo(DELIVERY_CHECK);

        // error because item is for return
        assertThatThrownBy(() -> orderDeliveryResultCommandService.checkDeliveryItemCode(
                order.getId(), itemIdWithCis, SCAN, null
        )).isExactlyInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("Code check not required for item");

        // error because no cis
        assertThatThrownBy(() -> orderDeliveryResultCommandService.checkDeliveryItemCode(
                order.getId(), itemIdWithoutCis, SCAN, null
        )).isExactlyInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("Code check not required for item");
    }

    @Test
    void testUpdateStatusLog() {
        testableClock.clearFixed();
        Order order = orderFactory.createSimpleFashionOrder();
        Long orderId = order.getId();
        orderFactory.receiveOrder(orderId);

        var emptyLogs = orderDeliveryHistoryResultRepository.findAll();
        assertThat(emptyLogs).hasSize(0);

        orderDeliveryResultCommandService.startFitting(orderId);
        assertThat(getLastLoggedStatus()).isEqualTo(PartialDeliveryStatus.CREATED);

        orderDeliveryResultCommandService.finishFitting(orderId);
        assertThat(getLastLoggedStatus()).isEqualTo(FITTING_DONE);

        orderDeliveryResultCommandService.returnToFitting(orderId);
        assertThat(getLastLoggedStatus()).isEqualTo(PartialDeliveryStatus.CREATED);

        orderDeliveryResultCommandService.finishFitting(orderId);
        assertThat(getLastLoggedStatus()).isEqualTo(FITTING_DONE);

        orderDeliveryResultCommandService.pay(orderId);
        assertThat(getLastLoggedStatus()).isEqualTo(PartialDeliveryStatus.PAYED);

        orderDeliveryResultCommandService.packageReturn(order.getId(), List.of());
        assertThat(getLastLoggedStatus()).isEqualTo(PartialDeliveryStatus.FULLY_PURCHASED);

        var finalLogs = orderDeliveryHistoryResultRepository.findAll();
        assertThat(finalLogs).hasSize(6);
        assertThat(finalLogs).allMatch(p -> p.getUid().equals(String.valueOf(PvzCoreInternalConfiguration.DEFAULT_LOCAL_UID)));
    }

    private PartialDeliveryStatus getLastLoggedStatus() {
        return orderDeliveryHistoryResultRepository.findAll().stream()
                .max(Comparator.comparing(OrderDeliveryResultHistory::getUpdatedAt))
                .get().getStatus();
    }

    @Test
    void testUpdateStatusLogOnlyOnChange() {
        testableClock.clearFixed();
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        var emptyLogs = orderDeliveryHistoryResultRepository.findAll();
        assertThat(emptyLogs).hasSize(0);

        try {
            orderDeliveryResultCommandService.pay(order.getId());
        } catch (Exception e) {
            //simulate illegal request for test that should not be logged
        }

        var finalLogs = orderDeliveryHistoryResultRepository.findAll();
        assertThat(finalLogs).hasSize(0);
    }
}
