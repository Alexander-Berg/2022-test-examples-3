package ru.yandex.market.pvz.internal.controller.pi.shipment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.dispatch.model.DispatchType;
import ru.yandex.market.pvz.core.domain.order.OrderParamsMapper;
import ru.yandex.market.pvz.core.domain.order.OrderPlaceParams;
import ru.yandex.market.pvz.core.domain.order.model.Dimensions;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderParams;
import ru.yandex.market.pvz.core.domain.order.model.place.OrderPlace;
import ru.yandex.market.pvz.core.domain.order.model.sender.OrderSender;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnRequestParams;
import ru.yandex.market.pvz.core.domain.shipment.ShipmentCommandService;
import ru.yandex.market.pvz.core.domain.shipment.ShipmentRepository;
import ru.yandex.market.pvz.core.domain.shipment.model.Shipment;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentCreateItemParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentCreateParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentStatus;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentType;
import ru.yandex.market.pvz.core.domain.shipment.receive.Receive;
import ru.yandex.market.pvz.core.domain.shipment.receive.ReceiveRepository;
import ru.yandex.market.pvz.core.domain.transfer_act.TransferActService;
import ru.yandex.market.pvz.core.domain.transfer_act.TransferSignatureCredentials;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderSenderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestReturnRequestFactory;
import ru.yandex.market.pvz.core.test.factory.TestShipmentsFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderSenderDto;
import ru.yandex.market.pvz.internal.controller.pi.shipment.dispatch.DiscrepancyDto;
import ru.yandex.market.pvz.internal.controller.pi.shipment.dispatch.DispatchCreateDto;
import ru.yandex.market.pvz.internal.controller.pi.shipment.dispatch.DispatchCreateItemDto;
import ru.yandex.market.pvz.internal.controller.pi.shipment.dispatch.DispatchDto;
import ru.yandex.market.pvz.internal.controller.pi.shipment.dispatch.DispatchDtoRecipient;
import ru.yandex.market.pvz.internal.controller.pi.shipment.dispatch.DispatchItemDto;
import ru.yandex.market.pvz.internal.controller.pi.shipment.dispatch.PendingDispatchDto;
import ru.yandex.market.pvz.internal.controller.pi.shipment.dto.ShipmentActDto;
import ru.yandex.market.pvz.internal.controller.pi.shipment.receive.PendingReceiveDto;
import ru.yandex.market.pvz.internal.controller.pi.shipment.receive.ReceiveCreateItemDto;
import ru.yandex.market.pvz.internal.controller.pi.shipment.receive.ReceiveCreateTransferDto;
import ru.yandex.market.tpl.common.transferact.client.api.TransferApi;
import ru.yandex.market.tpl.common.transferact.client.model.ItemQualifierDto;
import ru.yandex.market.tpl.common.transferact.client.model.PendingTransferDto;
import ru.yandex.market.tpl.common.transferact.client.model.RegistryItemTypeDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferStatus;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.dispatch.model.DispatchType.EXPIRED;
import static ru.yandex.market.pvz.core.domain.dispatch.model.DispatchType.RETURN;
import static ru.yandex.market.pvz.core.domain.dispatch.model.DispatchType.SAFE_PACKAGE;
import static ru.yandex.market.pvz.core.domain.yandex.YandexMigrationManager.YANDEX_MARKET_ORGANIZATION;
import static ru.yandex.market.pvz.core.test.factory.TestDeliveryServiceFactory.DeliveryServiceParams.DEFAULT_SORTING_CENTER_ADDRESS;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.BARCODE_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.BARCODE_2;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.BARCODE_3;
import static ru.yandex.market.tpl.common.util.CommonUtil.nvl;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ShipmentReportServiceTest {

    private static final String COURIER_ID = "courier vasya's ID";
    private static final long LMS_ID = 123;
    private static final long OPERATOR_UID = 9876;
    private static final String TRANSFER_ID = "transfer id";
    private static final String TRANSFER_ID_WITH_DISCREPANCIES = "transfer id 2";

    private final ShipmentReportService shipmentReportService;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentCommandService shipmentCommandService;
    private final ReceiveRepository receiveRepository;

    private final TestReturnRequestFactory returnRequestFactory;
    private final TestShipmentsFactory shipmentsFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderSenderFactory orderSenderFactory;
    private final TestOrderFactory orderFactory;
    private final TestableClock clock;
    private final OrderParamsMapper orderParamsMapper;

    @SpyBean
    private TransferActService transferActService;

    @MockBean
    private TransferApi transferApi;

    @Captor
    private ArgumentCaptor<List<OrderParams>> orderParamsCaptor;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.now(), clock.getZone());
    }

    @Test
    void createReceiveTransferAndCancelReceiveTransfer() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .lmsId(LMS_ID)
                                .build()).build());
        long pickupPointId = pickupPoint.getId();
        Order order1 = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());
        Order order2 = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());
        when(transferApi.transferPut(any()))
                .thenReturn(new TransferDto().id(TRANSFER_ID).status(TransferStatus.CREATED));
        var pickupPointData = new PickupPointRequestData(pickupPointId, pickupPoint.getPvzMarketId(),
                pickupPoint.getName(), OPERATOR_UID, pickupPoint.getTimeOffset(),
                pickupPoint.getStoragePeriod());

        PendingReceiveDto pendingReceive = shipmentReportService.createReceiveTransfer(pickupPointData,
                ReceiveCreateTransferDto.builder().items(List.of(new ReceiveCreateItemDto(order1.getExternalId()),
                                new ReceiveCreateItemDto(order2.getExternalId())))
                        .courierId(COURIER_ID)
                        .build());

        assertThat(pendingReceive.getStatus()).isEqualTo(ShipmentStatus.PENDING);
        verify(transferActService).createTransfer(eq(nvl(pickupPoint.getLmsId(), 0L)),
                orderParamsCaptor.capture(), eq(COURIER_ID), any(TransferSignatureCredentials.class));//TODO FIX

        List<Entry<String, String>> actualOrdersWithPlaces =
                orderParamsToEntries(StreamEx.of(orderParamsCaptor.getValue()));
        List<Entry<String, String>> expectedOrdersWithPlaces =
                orderParamsToEntries(StreamEx.of(order2, order1).map(orderParamsMapper::map));

        assertThat(actualOrdersWithPlaces).isEqualTo(expectedOrdersWithPlaces);

        List<Receive> actualReceives =
                receiveRepository.findByPickupPointIdAndStatus(pickupPointId, ShipmentStatus.PENDING);
        List<Long> actualReceivedOrderIds = actualReceives.stream()
                .map(Receive::getOrderId)
                .collect(Collectors.toList());
        assertThat(actualReceivedOrderIds).containsExactlyInAnyOrder(order1.getId(), order2.getId());

        long shipmentId = pendingReceive.getShipment().getId();
        shipmentReportService.cancelReceiveTransfer(pickupPointData, shipmentId);

        verify(transferActService).cancelTransfer(actualReceives.get(0).getTransferId());
        List<Receive> receives = receiveRepository.findByPickupPointIdAndStatus(pickupPointId, ShipmentStatus.PENDING);
        assertThat(receives).isEmpty();
    }

    private List<Entry<String, String>> orderParamsToEntries(StreamEx<OrderParams> ordersToPlaces) {
        return ordersToPlaces.mapToEntry(OrderParams::getPlaces)
                .flatMapValues(List::stream)
                .mapKeys(OrderParams::getExternalId)
                .mapValues(OrderPlaceParams::getBarcode)
                .sorted(Entry.<String, String>comparingByKey().thenComparing(Entry.comparingByValue()))
                .toList();
    }

    @Test
    void returnPendingDispatchIfNoOpenSessions() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .lmsId(LMS_ID).build())
                        .build());
        long pickupPointId = pickupPoint.getId();
        var pickupPointData = new PickupPointRequestData(pickupPointId, pickupPoint.getPvzMarketId(),
                pickupPoint.getName(), OPERATOR_UID, pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());
        ReturnRequestParams returnRequest = returnRequestFactory.createReceivedReturn(pickupPoint);
        when(transferApi.pendingTransfersGet(any())).thenReturn(List.of());

        PendingDispatchDto pendingDispatch = shipmentReportService.trySignDispatchTransfer(pickupPointData,
                new DispatchCreateDto(List.of(DispatchCreateItemDto.builder()
                        .id(returnRequest.getReturnId())
                        .type(RETURN)
                        .build())));

        assertThat(pendingDispatch).isEqualTo(PendingDispatchDto.pending());
    }

    @Test
    void returnFinishedDispatchIfNoDiscrepancies() {
        Instant createdAt = Instant.parse("2007-12-03T10:15:30.00Z");
        clock.setFixed(createdAt, ZoneId.of("UTC+3"));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .lmsId(LMS_ID).build())
                        .build());
        long pickupPointId = pickupPoint.getId();
        var pickupPointData = new PickupPointRequestData(pickupPointId, pickupPoint.getPvzMarketId(),
                pickupPoint.getName(), OPERATOR_UID, pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());

        Order expiredOrder = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder().pickupPoint(pickupPoint).build());
        orderFactory.setStatusAndCheckpoint(expiredOrder.getId(), PvzOrderStatus.STORAGE_PERIOD_EXPIRED);
        ReturnRequestParams returnOrder = returnRequestFactory.createReceivedReturn(pickupPoint);
        Order fashionOrder = orderFactory.createFashionWithPartialReturn(pickupPoint);
        Order fashionOrder2 = orderFactory.createFashionWithPartialReturn(pickupPoint, List.of(BARCODE_3));

        when(transferApi.pendingTransfersGet(String.valueOf(LMS_ID)))
                .thenReturn(List.of(
                        new PendingTransferDto().id(TRANSFER_ID_WITH_DISCREPANCIES),
                        new PendingTransferDto().id(TRANSFER_ID)));

        when(transferApi.transferIdGet(TRANSFER_ID_WITH_DISCREPANCIES))
                .thenReturn(ofOrders(List.of(
                        Pair.of(RETURN, returnOrder.getBarcode()),
                        Pair.of(SAFE_PACKAGE, BARCODE_3)))
                        .id(TRANSFER_ID_WITH_DISCREPANCIES));
        List<Pair<DispatchType, String>> correctOrders = StreamEx.of(expiredOrder.getPlaces())
                .map(place -> Pair.of(EXPIRED, place.getBarcode()))
                .append(Pair.of(RETURN, returnOrder.getBarcode()),
                        Pair.of(SAFE_PACKAGE, BARCODE_1),
                        Pair.of(SAFE_PACKAGE, BARCODE_2))
                .toList();
        when(transferApi.transferIdGet(TRANSFER_ID))
                .thenReturn(ofOrders(correctOrders).id(TRANSFER_ID));

        PendingDispatchDto dispatch = shipmentReportService.trySignDispatchTransfer(pickupPointData,
                new DispatchCreateDto(List.of(
                        DispatchCreateItemDto.builder()
                                .type(RETURN)
                                .id(returnOrder.getReturnId())
                                .build(),
                        DispatchCreateItemDto.builder()
                                .type(SAFE_PACKAGE)
                                .id(fashionOrder.getExternalId())
                                .build(),
                        DispatchCreateItemDto.builder()
                                .type(EXPIRED)
                                .id(expiredOrder.getExternalId())
                                .build())));
        Shipment shipment = shipmentRepository.findByIdOrThrow(dispatch.getShipment().getId());

        PendingDispatchDto expectedDispatch = PendingDispatchDto.finished(DispatchDto.builder()
                .shipmentDate(LocalDate.now(clock))
                .ordersPrice(new BigDecimal("2656.00"))
                .ordersCount(6)
                .type(ShipmentType.DISPATCH)
                .responsibleUserId(OPERATOR_UID)
                .items(List.of(DispatchItemDto.builder()
                                .type(RETURN)
                                .placesCount(1)
                                .placeBarcodes(List.of(returnOrder.getBarcode()))
                                .build(),
                        DispatchItemDto.builder()
                                .type(SAFE_PACKAGE)
                                .placesCount(2)
                                .placeBarcodes(List.of(BARCODE_1, BARCODE_2))
                                .build(),
                        DispatchItemDto.builder()
                                .type(EXPIRED)
                                .placesCount(expiredOrder.getPlaces().size())
                                .placeBarcodes(StreamEx.of(expiredOrder.getPlaces())
                                        .map(OrderPlace::getBarcode).toList())
                                .build())).build());
        assertThat(dispatch)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .ignoringCollectionOrder()
                .ignoringFields("id", "shipment.id", "shipment.items.id", "shipment.items.barcode",
                        "shipment.ordersPrice", "shipment.items.labels")
                .ignoringFieldsOfTypes(Instant.class, DispatchDtoRecipient.class, OrderSenderDto.class)
                .isEqualTo(expectedDispatch);
        assertThat(shipment.getTransferId()).isEqualTo(TRANSFER_ID);
    }

    @Test
    void returnDiscrepancyDispatchIfFoundDiscrepancy() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .lmsId(LMS_ID).build())
                        .build());
        long pickupPointId = pickupPoint.getId();
        var pickupPointData = new PickupPointRequestData(pickupPointId, pickupPoint.getPvzMarketId(),
                pickupPoint.getName(), OPERATOR_UID, pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());

        Order expiredOrder = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder().pickupPoint(pickupPoint).build());
        orderFactory.setStatusAndCheckpoint(expiredOrder.getId(), PvzOrderStatus.STORAGE_PERIOD_EXPIRED);
        ReturnRequestParams returnOrder = returnRequestFactory.createReceivedReturn(pickupPoint);
        ReturnRequestParams returnOrder2 = returnRequestFactory.createReceivedReturn(pickupPoint);
        Order fashionOrder = orderFactory.createFashionWithPartialReturn(pickupPoint);

        DispatchCreateDto createDto = new DispatchCreateDto(List.of(
                DispatchCreateItemDto.builder()
                        .type(RETURN)
                        .id(returnOrder.getReturnId())
                        .build(),
                DispatchCreateItemDto.builder()
                        .type(SAFE_PACKAGE)
                        .id(fashionOrder.getExternalId())
                        .build(),
                DispatchCreateItemDto.builder()
                        .type(EXPIRED)
                        .id(expiredOrder.getExternalId())
                        .build()));

        when(transferApi.pendingTransfersGet(String.valueOf(LMS_ID)))
                .thenReturn(List.of(new PendingTransferDto().id(TRANSFER_ID)));

        List<Pair<DispatchType, String>> ordersWithDeficiencies = List.of(
                Pair.of(RETURN, returnOrder.getBarcode()),
                Pair.of(SAFE_PACKAGE, BARCODE_1),
                Pair.of(SAFE_PACKAGE, BARCODE_2));
        List<Pair<DispatchType, String>> ordersWithExcesses = StreamEx.of(expiredOrder.getPlaces())
                .map(place -> Pair.of(EXPIRED, place.getBarcode()))
                .append(Pair.of(RETURN, returnOrder.getBarcode()),
                        Pair.of(RETURN, returnOrder2.getBarcode()),
                        Pair.of(SAFE_PACKAGE, BARCODE_1),
                        Pair.of(SAFE_PACKAGE, BARCODE_2))
                .toList();
        List<Pair<DispatchType, String>> ordersWithBoth = ListUtils.union(List.of(
                Pair.of(RETURN, returnOrder2.getBarcode())), ordersWithDeficiencies);

        testDiscrepancy(pickupPointData, ordersWithDeficiencies, createDto, PendingDispatchDto.discrepancy(
                DiscrepancyDto.builder()
                        .deficiencies(Set.of())
                        .excesses(Set.of(expiredOrder.getExternalId())).build()));

        testDiscrepancy(pickupPointData, ordersWithExcesses, createDto, PendingDispatchDto.discrepancy(
                DiscrepancyDto.builder()
                        .deficiencies(Set.of(returnOrder2.getReturnId()))
                        .excesses(Set.of()).build()));

        testDiscrepancy(pickupPointData, ordersWithBoth, createDto, PendingDispatchDto.discrepancy(
                DiscrepancyDto.builder()
                        .deficiencies(Set.of(returnOrder2.getReturnId()))
                        .excesses(Set.of(expiredOrder.getExternalId())).build()));
    }

    @Test
    void returnTransferWithLeastDiscrepancy() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .lmsId(LMS_ID).build())
                        .build());
        long pickupPointId = pickupPoint.getId();
        var pickupPointData = new PickupPointRequestData(pickupPointId, pickupPoint.getPvzMarketId(),
                pickupPoint.getName(), OPERATOR_UID, pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());

        Order expiredOrder = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder().pickupPoint(pickupPoint).build());
        orderFactory.setStatusAndCheckpoint(expiredOrder.getId(), PvzOrderStatus.STORAGE_PERIOD_EXPIRED);
        Order expiredOrder2 = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .params(TestOrderFactory.OrderParams.builder()
                                .places(List.of(TestOrderFactory.OrderPlaceParams.builder().build()))
                                .build()).build());
        orderFactory.setStatusAndCheckpoint(expiredOrder2.getId(), PvzOrderStatus.STORAGE_PERIOD_EXPIRED);
        ReturnRequestParams returnOrder = returnRequestFactory.createReceivedReturn(pickupPoint);
        ReturnRequestParams returnOrder2 = returnRequestFactory.createReceivedReturn(pickupPoint);
        Order fashionOrder = orderFactory.createFashionWithPartialReturn(pickupPoint);
        Order fashionOrder2 = orderFactory.createFashionWithPartialReturn(pickupPoint, List.of(BARCODE_3));

        DispatchCreateDto createDto = new DispatchCreateDto(List.of(
                DispatchCreateItemDto.builder()
                        .type(RETURN)
                        .id(returnOrder.getReturnId())
                        .build(),
                DispatchCreateItemDto.builder()
                        .type(SAFE_PACKAGE)
                        .id(fashionOrder.getExternalId())
                        .build(),
                DispatchCreateItemDto.builder()
                        .type(EXPIRED)
                        .id(expiredOrder.getExternalId())
                        .build()));

        when(transferApi.pendingTransfersGet(String.valueOf(LMS_ID)))
                .thenReturn(List.of(new PendingTransferDto().id(TRANSFER_ID),
                        new PendingTransferDto().id(TRANSFER_ID + 1),
                        new PendingTransferDto().id(TRANSFER_ID + 2)));

        List<Pair<DispatchType, String>> ordersWith3Differences = StreamEx.of(expiredOrder2.getPlaces())
                .map(place -> Pair.of(EXPIRED, place.getBarcode()))
                .append(Pair.of(RETURN, returnOrder2.getBarcode()), Pair.of(SAFE_PACKAGE, BARCODE_3))
                .toList();
        when(transferApi.transferIdGet(TRANSFER_ID))
                .thenReturn(ofOrders(ordersWith3Differences).id(TRANSFER_ID));

        List<Pair<DispatchType, String>> ordersWith1Differences = StreamEx.of(expiredOrder2.getPlaces())
                .map(place -> Pair.of(EXPIRED, place.getBarcode()))
                .append(Pair.of(RETURN, returnOrder.getBarcode()),
                        Pair.of(SAFE_PACKAGE, BARCODE_1), Pair.of(SAFE_PACKAGE, BARCODE_2))
                .toList();
        when(transferApi.transferIdGet(TRANSFER_ID + 1))
                .thenReturn(ofOrders(ordersWith1Differences).id(TRANSFER_ID + 1));

        List<Pair<DispatchType, String>> ordersWith2Differences = StreamEx.of(expiredOrder2.getPlaces())
                .map(place -> Pair.of(EXPIRED, place.getBarcode()))
                .append(Pair.of(RETURN, returnOrder.getBarcode()), Pair.of(SAFE_PACKAGE, BARCODE_3))
                .toList();
        when(transferApi.transferIdGet(TRANSFER_ID + 2))
                .thenReturn(ofOrders(ordersWith2Differences).id(TRANSFER_ID + 2));

        PendingDispatchDto dispatch = shipmentReportService.trySignDispatchTransfer(pickupPointData, createDto);
        assertThat(dispatch)
                .isEqualTo(PendingDispatchDto.discrepancy(DiscrepancyDto.builder()
                        .deficiencies(Set.of(expiredOrder2.getExternalId()))
                        .excesses(Set.of(expiredOrder.getExternalId()))
                        .build()));
    }

    private void testDiscrepancy(PickupPointRequestData pickupPointData, List<Pair<DispatchType, String>> orders,
                                 DispatchCreateDto createDto, PendingDispatchDto expectedDispatch) {
        when(transferApi.transferIdGet(TRANSFER_ID)).thenReturn(ofOrders(orders).id(TRANSFER_ID));
        PendingDispatchDto dispatch = shipmentReportService.trySignDispatchTransfer(pickupPointData, createDto);
        assertThat(dispatch).isEqualTo(expectedDispatch);
    }

    @Test
    void returnDiscrepancyIfCourierScannedPlacesOfAnotherOrder() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .lmsId(LMS_ID).build())
                        .build());
        long pickupPointId = pickupPoint.getId();
        var pickupPointData = new PickupPointRequestData(pickupPointId, pickupPoint.getPvzMarketId(),
                pickupPoint.getName(), OPERATOR_UID, pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());

        ReturnRequestParams returnOrder = returnRequestFactory.createReceivedReturn(pickupPoint);
        ReturnRequestParams returnOrder2 = returnRequestFactory.createReceivedReturn(pickupPoint);

        DispatchCreateDto createDto = new DispatchCreateDto(List.of(
                DispatchCreateItemDto.builder()
                        .type(RETURN)
                        .id(returnOrder.getReturnId())
                        .build()));
        when(transferApi.pendingTransfersGet(String.valueOf(LMS_ID)))
                .thenReturn(List.of(new PendingTransferDto().id(TRANSFER_ID)));

        List<Pair<DispatchType, String>> ordersWithUnexpected = List.of(
                Pair.of(RETURN, returnOrder.getBarcode()),
                Pair.of(RETURN, returnOrder2.getBarcode()));

        testDiscrepancy(pickupPointData, ordersWithUnexpected, createDto, PendingDispatchDto.discrepancy(
                DiscrepancyDto.builder()
                        .deficiencies(Set.of(returnOrder2.getReturnId()))
                        .excesses(Set.of()).build()));
    }

    @Test
    void throwExceptionIfCourierScannedInvalidPlaces() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .lmsId(LMS_ID).build())
                        .build());
        long pickupPointId = pickupPoint.getId();
        var pickupPointData = new PickupPointRequestData(pickupPointId, pickupPoint.getPvzMarketId(),
                pickupPoint.getName(), OPERATOR_UID, pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());

        ReturnRequestParams returnOrder = returnRequestFactory.createReceivedReturn(pickupPoint);

        DispatchCreateDto createDto = new DispatchCreateDto(List.of(
                DispatchCreateItemDto.builder()
                        .type(RETURN)
                        .id(returnOrder.getReturnId())
                        .build()));
        when(transferApi.pendingTransfersGet(String.valueOf(LMS_ID)))
                .thenReturn(List.of(new PendingTransferDto().id(TRANSFER_ID)));

        List<Pair<DispatchType, String>> orders = List.of(Pair.of(RETURN, "EAN_8-800-555-35-35"));
        when(transferApi.transferIdGet(TRANSFER_ID)).thenReturn(ofOrders(orders).id(TRANSFER_ID));

        assertThatThrownBy(() -> shipmentReportService.trySignDispatchTransfer(pickupPointData, createDto))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Deprecated(forRemoval = true, since = "https://st.yandex-team.ru/MARKETTPLPVZ-2447")
    private TransferDto ofOrders(List<Pair<DispatchType, String>> orders) {
        List<ItemQualifierDto> items = StreamEx.of(orders)
                .map(place -> place.getFirst() == EXPIRED ?
                        new ItemQualifierDto()
                                .type(RegistryItemTypeDto.PLACE)
                                .placeId(place.getSecond()) :
                        new ItemQualifierDto()
                                .type(RegistryItemTypeDto.LOT)
                                .externalId(place.getSecond()))
                .toList();
        return new TransferDto().receivedItems(items);
    }

    @Test
    @Disabled
    void testGetReceiveSummary() {
        var dataPack = createTestDataPack(ShipmentType.RECEIVE, PvzOrderStatus.CREATED);

        var summary = shipmentReportService.getReceiveSummary(dataPack.getPickupPointAuthInfo(), LocalDate.now(clock));

        assertThat(summary.getTotalOrdersCount()).isEqualTo(4);
        assertThat(summary.getAcceptedOrdersCount()).isEqualTo(1);
        assertThat(summary.getLeftOrdersCount()).isEqualTo(3);

        assertThat(summary.getTotalPrice()).isEqualTo(
                StreamEx.of(dataPack.getPresentOrPastOrders())
                        .map(Order::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        assertThat(summary.getAcceptedPrice()).isEqualTo(
                dataPack.getFinishedShipmentOrder().getTotalPrice()
        );
        assertThat(summary.getLeftPrice()).isEqualTo(
                dataPack.getNoShipmentTodayOrder().getTotalPrice()
                        .add(dataPack.getDraftShipmentOrder().getTotalPrice())
                        .add(dataPack.getNoShipmentYesterdayOrder().getTotalPrice()));
    }

    @Test
    @Disabled
    void testGetReceiveSummaryForPreviousDate() {
        var dataPack = createTestDataPack(ShipmentType.RECEIVE, PvzOrderStatus.CREATED);

        var summary = shipmentReportService.getReceiveSummary(
                dataPack.getPickupPointAuthInfo(), LocalDate.now(clock).minusDays(1));

        assertThat(summary.getTotalOrdersCount()).isEqualTo(0);
        assertThat(summary.getAcceptedOrdersCount()).isEqualTo(0);
        assertThat(summary.getLeftOrdersCount()).isEqualTo(0);

        assertThat(summary.getTotalPrice().longValue()).isEqualTo(0);
        assertThat(summary.getAcceptedPrice().longValue()).isEqualTo(0);
        assertThat(summary.getLeftPrice().longValue()).isEqualTo(0);
    }

    @Test
    void tryToReceiveSummaryForFutureDate() {
        var dataPack = createTestDataPack(ShipmentType.RECEIVE, PvzOrderStatus.CREATED);

        assertThatThrownBy(() ->
                shipmentReportService.getReceiveSummary(
                        dataPack.getPickupPointAuthInfo(), LocalDate.now(clock).plusDays(1)))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    @Disabled
    void testGetDispatchSummary() {
        var dataPack = createTestDataPack(ShipmentType.DISPATCH, PvzOrderStatus.READY_FOR_RETURN);

        var summary = shipmentReportService.getDispatchSummary(dataPack.getPickupPointAuthInfo(), LocalDate.now(clock));

        assertThat(summary.getTotalOrdersCount()).isEqualTo(5);
        assertThat(summary.getDispatchedOrdersCount()).isEqualTo(1);
        assertThat(summary.getLeftOrdersCount()).isEqualTo(4);
    }

    @Test
    @Disabled
    void testGetDispatchReturnsAndExpiredSummary() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint).build());
        var order2 = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint).build());
        var returnRequest = returnRequestFactory.createReceivedReturn(pickupPoint);
        var returnRequest3 = returnRequestFactory.createReceivedReturn(pickupPoint);
        var orderReadyToReturn = orderFactory.readyForReturn(order.getId());
        orderFactory.readyForReturn(order2.getId());

        var shipment = new ShipmentCreateParams(ShipmentType.DISPATCH, ShipmentStatus.FINISHED, List.of(
                new ShipmentCreateItemParams(returnRequest.getReturnId(), RETURN),
                new ShipmentCreateItemParams(returnRequest3.getReturnId(), RETURN),
                new ShipmentCreateItemParams(orderReadyToReturn.getExternalId(), EXPIRED)
        ));
        var pickupPointData = new PickupPointRequestData(pickupPoint.getId(), pickupPoint.getPvzMarketId(), "PVZ",
                123L, pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());
        shipmentCommandService.createShipment(pickupPointData, shipment);

        var summary = shipmentReportService.getDispatchSummary(pickupPointData, LocalDate.now(clock));

        assertThat(summary.getTotalOrdersCount()).isEqualTo(5);
        assertThat(summary.getDispatchedOrdersCount()).isEqualTo(3);
        assertThat(summary.getLeftOrdersCount()).isEqualTo(2);
    }

    @Test
    @Disabled
    void testGetDispatchSummaryForPreviousDate() {
        var dataPack = createTestDataPack(ShipmentType.DISPATCH, PvzOrderStatus.READY_FOR_RETURN);

        var summary = shipmentReportService.getDispatchSummary(
                dataPack.getPickupPointAuthInfo(), LocalDate.now(clock).minusDays(1));

        assertThat(summary.getTotalOrdersCount()).isEqualTo(0);
        assertThat(summary.getDispatchedOrdersCount()).isEqualTo(0);
        assertThat(summary.getLeftOrdersCount()).isEqualTo(0);
    }

    @Test
    void tryToGetDispatchSummaryForFutureDate() {
        var dataPack = createTestDataPack(ShipmentType.DISPATCH, PvzOrderStatus.READY_FOR_RETURN);

        assertThatThrownBy(() ->
                shipmentReportService.getDispatchSummary(
                        dataPack.getPickupPointAuthInfo(), LocalDate.now(clock).plusDays(2)))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void testGetReceiveAct() {
        var dataPack = createTestDataPack(ShipmentType.RECEIVE, PvzOrderStatus.CREATED);
        var act = shipmentReportService.getReceiveAct(dataPack.getPickupPointAuthInfo(), LocalDate.now());
        assertActIsConsistentWithDataPack(act, dataPack);
        assertThat(act.getDeliveryAddress()).isEqualTo(dataPack.getPickupPoint().getLocation().getAddress());
    }

    @Test
    void testGetDispatchAct() {
        var dataPack = createTestDataPack(ShipmentType.DISPATCH, PvzOrderStatus.READY_FOR_RETURN);
        var act = shipmentReportService.getDispatchAct(dataPack.getPickupPointAuthInfo(), LocalDate.now());
        assertActIsConsistentWithDataPack(act, dataPack);
        assertThat(act.getDeliveryAddress()).isEqualTo(DEFAULT_SORTING_CENTER_ADDRESS);
    }

    private void assertActIsConsistentWithDataPack(ShipmentActDto act, TestDataPack dataPack) {
        ShipmentActDto expectedAct = ShipmentActDto.builder()
                .number(String.valueOf(dataPack.getFinishedShipment().getId()))
                .executor(dataPack.getPickupPoint().getLegalPartner().getOrganization().getFullName())
                .sellerName(YANDEX_MARKET_ORGANIZATION)
                .sender(dataPack.getPickupPoint().getLegalPartner().getOrganization().getFullName())
                .date(LocalDate.now())
                .totalItems(dataPack.getFinishedShipmentOrder().getPlaces().size())
                .totalSum(dataPack.getFinishedShipmentOrder().getAssessedCost())
                .shipments(List.of(ShipmentActDto.Shipment.builder()
                        .id(dataPack.getFinishedShipmentOrder().getExternalId())
                        .items(dataPack.getFinishedShipmentOrder().getPlaces().size())
                        .totalSum(dataPack.getFinishedShipmentOrder().getAssessedCost())
                        .build()))
                .build();
        assertThat(act).isEqualToIgnoringGivenFields(expectedAct, "totalSum", "shipments", "deliveryAddress");
        assertThat(act.getTotalSum()).isEqualByComparingTo(expectedAct.getTotalSum());
        assertThat(act.getShipments().get(0))
                .isEqualToIgnoringGivenFields(expectedAct.getShipments().get(0), "totalSum");
        assertThat(act.getShipments().get(0).getTotalSum())
                .isEqualByComparingTo(expectedAct.getShipments().get(0).getTotalSum());
    }

    private TestDataPack createTestDataPack(ShipmentType shipmentType, PvzOrderStatus orderStatus) {
        TestDataPack dataPack = new TestDataPack();

        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        OrderSender orderSender = orderSenderFactory.createOrderSender();
        dataPack.setNoShipmentTodayOrder(orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .orderSender(orderSender)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("test-no-shipment-today")
                        .deliveryDate(LocalDate.now(clock))
                        .places(List.of(
                                TestOrderFactory.OrderPlaceParams.builder()
                                        .barcode("123456")
                                        .dimensions(Dimensions.builder()
                                                .length(10)
                                                .width(5)
                                                .height(3)
                                                .weight(BigDecimal.ONE)
                                                .build())
                                        .build(),
                                TestOrderFactory.OrderPlaceParams.builder()
                                        .barcode("123457")
                                        .dimensions(Dimensions.builder()
                                                .length(10)
                                                .width(5)
                                                .height(3)
                                                .weight(BigDecimal.ONE)
                                                .build())
                                        .build(),
                                TestOrderFactory.OrderPlaceParams.builder()
                                        .barcode("123458")
                                        .dimensions(Dimensions.builder()
                                                .length(10)
                                                .width(5)
                                                .height(3)
                                                .weight(BigDecimal.ONE)
                                                .build())
                                        .build()
                        ))
                        .build())
                .build()));
        dataPack.setNoShipmentYesterdayOrder(orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .orderSender(orderSender)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("test-no-shipment-yesterday")
                        .deliveryDate(LocalDate.now(clock).minusDays(1))
                        .build())
                .build()));
        dataPack.setNoShipmentNextDayOrder(orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .orderSender(orderSender)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("test-no-shipment-next-day")
                        .deliveryDate(LocalDate.now(clock).plusDays(1))
                        .build())
                .build()));
        dataPack.setDraftShipmentOrder(orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .orderSender(orderSender)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("test-draft-shipment-today")
                        .deliveryDate(LocalDate.now(clock))
                        .build())
                .build()));
        dataPack.setFinishedShipmentOrder(orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .orderSender(orderSender)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("test-finished-shipment-today")
                        .deliveryDate(LocalDate.now(clock))
                        .build())
                .build()));

        dataPack.setPickupPoint(dataPack.getNoShipmentTodayOrder().getPickupPoint());

        if (orderStatus != PvzOrderStatus.CREATED) {
            for (var order : dataPack.getAllOrders()) {
                orderFactory.setStatusAndCheckpoint(order.getId(), orderStatus);
            }
        }

        dataPack.setFinishedShipment(shipmentCommandService.createShipment(dataPack.getPickupPointAuthInfo(),
                new ShipmentCreateParams(shipmentType, ShipmentStatus.FINISHED,
                        List.of(new ShipmentCreateItemParams(dataPack.getFinishedShipmentOrder().getExternalId()))
                )));

        return dataPack;
    }

    @Data
    private static class TestDataPack {
        private Order noShipmentTodayOrder;
        private Order noShipmentYesterdayOrder;
        private Order noShipmentNextDayOrder;
        private Order draftShipmentOrder;
        private Order finishedShipmentOrder;

        private Shipment finishedShipment;

        private PickupPointRequestData pickupPointAuthInfo;
        private PickupPoint pickupPoint;

        public void setPickupPoint(PickupPoint pickupPoint) {
            this.pickupPoint = pickupPoint;
            this.pickupPointAuthInfo = new PickupPointRequestData(
                    pickupPoint.getId(),
                    pickupPoint.getPvzMarketId(),
                    pickupPoint.getName(),
                    1L,
                    pickupPoint.getTimeOffset(),
                    pickupPoint.getStoragePeriod()
            );

            for (Order order : getAllOrders()) {
                order.setPickupPoint(pickupPoint);
            }
        }

        private List<Order> getAllOrders() {
            return List.of(noShipmentTodayOrder, noShipmentYesterdayOrder,
                    noShipmentNextDayOrder, draftShipmentOrder, finishedShipmentOrder);
        }

        public List<Order> getPresentOrPastOrders() {
            return List.of(noShipmentTodayOrder, noShipmentYesterdayOrder, draftShipmentOrder, finishedShipmentOrder);
        }
    }

    @Test
    void loadDispatchesWithNoExpiredOrders() {
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        var dispatches = shipmentReportService.loadReturnsAndExpired(order.getPickupPoint().getId(), null);
        assertThat(dispatches).isEmpty();
    }

    @Test
    void loadEmptyDispatchesForPickupPoint() {
        var returnRequest = returnRequestFactory.createReturnRequest();
        var dispatches = shipmentReportService.loadReturnsAndExpired(returnRequest.getPickupPointId() + 1, null);

        assertThat(dispatches).isEmpty();
    }
}
