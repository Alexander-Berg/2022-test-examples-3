package ru.yandex.market.pvz.core.test.factory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.logistic.api.model.delivery.CargoType;
import ru.yandex.market.pvz.client.model.order.DeliveryServiceType;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.delivery_service.DeliveryService;
import ru.yandex.market.pvz.core.domain.dispatch.model.DispatchType;
import ru.yandex.market.pvz.core.domain.order.OrderAdditionalInfoCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderParamsMapper;
import ru.yandex.market.pvz.core.domain.order.OrderPersonalCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderPersonalQueryService;
import ru.yandex.market.pvz.core.domain.order.OrderRepository;
import ru.yandex.market.pvz.core.domain.order.model.Dimensions;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderAdditionalInfo;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderItem;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentStatus;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.OrderType;
import ru.yandex.market.pvz.core.domain.order.model.OrderVerification;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.order.model.VatType;
import ru.yandex.market.pvz.core.domain.order.model.VendorArticle;
import ru.yandex.market.pvz.core.domain.order.model.cashbox.CashboxPaymentStatus;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderUpdateParams;
import ru.yandex.market.pvz.core.domain.order.model.personal.OrderPersonal;
import ru.yandex.market.pvz.core.domain.order.model.place.OrderPlace;
import ru.yandex.market.pvz.core.domain.order.model.place.OrderPlaceItem;
import ru.yandex.market.pvz.core.domain.order.model.sender.OrderSender;
import ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryFlow;
import ru.yandex.market.pvz.core.domain.order_delivery_result.params.CodeType;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.domain.shipment.ShipmentCommandService;
import ru.yandex.market.pvz.core.domain.shipment.ShipmentRepository;
import ru.yandex.market.pvz.core.domain.shipment.dispatch.DispatchCreateItemParams;
import ru.yandex.market.pvz.core.domain.shipment.dispatch.DispatchCreateParams;
import ru.yandex.market.pvz.core.domain.shipment.model.Shipment;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentCreateItemParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentCreateParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentStatus;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentType;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.configuration.ConfigurationProvider;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY;
import static ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY_BATCH;
import static ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType.GET_RECIPIENT_PHONE_TAIL_BATCH;
import static ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType.SET_EXPIRATION_DATE_BATCH;
import static ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType.TRY_GROUP_SIBLING_ORDERS;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryFlow.RETURN;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryScanType.SCAN;
import static ru.yandex.market.pvz.core.test.TestExternalConfiguration.DEFAULT_UID;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_DIMENSIONS;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_RECIPIENT_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_VERIFICATION_CODE;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.getDefaultPhoneTail;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.CreatePickupPointBuilder;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams;

@Slf4j
@Transactional
public class TestOrderFactory extends TestObjectFactory {

    public static final String UIT_1_1 = "ai2u34iu";
    public static final String UIT_2_1 = "bhfw73y7";
    public static final String UIT_2_2 = "cdofu873";

    public static final String CIS_1_1 = "CIS_1_1";
    public static final String CIS_2_1 = "CIS_2_1";
    public static final String CIS_2_2 = "CIS_2_2";

    public static final String EAN_1 = "5012345678900";

    public static final String BARCODE_1 = "FSN_RET_123456789";
    public static final String BARCODE_2 = "FSN_RET_547123612";
    public static final String BARCODE_3 = "HLP_MK_FSN_RET_1";
    public static final String BARCODE_4_1 = "PVZ_FBS_FSN_RET_1";
    public static final String BARCODE_4_2 = "PVZ_FBS_FSN_RET_2";


    @Autowired
    private TestPickupPointFactory testPickupPointFactory;

    @Autowired
    private TestOrderSenderFactory orderSenderFactory;

    @Autowired
    private DbQueueTestUtil dbQueueTestUtil;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderCommandService orderCommandService;

    @Autowired
    private OrderDeliveryResultCommandService orderDeliveryResultCommandService;

    @Autowired
    private OrderParamsMapper orderParamsMapper;

    @Autowired
    private OrderAdditionalInfoCommandService orderAdditionalInfoCommandService;

    @Autowired
    private OrderPersonalCommandService orderPersonalCommandService;

    @Autowired
    private OrderPersonalQueryService orderPersonalQueryService;

    @Autowired
    private ShipmentCommandService shipmentCommandService;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private TestDeliveryServiceFactory deliveryServiceFactory;

    @Autowired
    private ConfigurationProvider configurationProvider;

    public Order createOrder() {
        return createOrder(CreateOrderBuilder.builder().build());
    }

    public Order createOrder(CreateOrderBuilder builder) {
        if (builder.getPickupPoint() == null) {
            builder.setPickupPoint(testPickupPointFactory.createPickupPoint());
        }
        if (builder.getOrderSender() == null) {
            builder.setOrderSender(orderSenderFactory.createOrderSender());
        }
        var order = orderCommandService.create(buildOrder(
                builder.getParams(), builder.getPickupPoint(), builder.getOrderSender()));
        dbQueueTestUtil.executeAllQueueItems(CHANGE_PICKUP_POINT_CAPACITY);
        return order;
    }

    public void receiveByCourier(Order order, String courierId) {
        orderAdditionalInfoCommandService.updateCourierIdByExternalIds(List.of(order.getExternalId()), courierId);
    }

    public Order createReadyForReturnOrder(PickupPoint pickupPoint) {
        var order = createOrder(CreateOrderBuilder.builder().pickupPoint(pickupPoint).build());
        return readyForReturn(order.getId());
    }

    public Shipment createShipmentDispatch(
            PickupPointRequestData pickupPointRequestData, String externalId, DispatchType type
    ) {
        return shipmentCommandService.createShipment(
                pickupPointRequestData,
                new ShipmentCreateParams(
                        ShipmentType.DISPATCH, ShipmentStatus.FINISHED,
                        List.of(new ShipmentCreateItemParams(externalId, type))
                )
        );
    }

    public Shipment createShipment(PickupPointRequestData pickupPointRequestData, ShipmentType shipmentType,
                                   ShipmentCreateItemParams createItemParams, String transferId) {
        Shipment shipment = shipmentCommandService.createShipment(
                pickupPointRequestData,
                new ShipmentCreateParams(shipmentType, ShipmentStatus.FINISHED, List.of(createItemParams)));
        shipment = shipmentRepository.findByIdOrThrow(shipment.getId());
        shipment.setTransferId(transferId);
        return shipmentRepository.save(shipment);
    }

    public Order createSimpleFashionOrder() {
        return createSimpleFashionOrder(false);
    }

    public Order createSimpleFbsFashionOrder(boolean fbs) {
        DeliveryService deliveryService = deliveryServiceFactory.createDeliveryService();
        return createSimpleFashionOrder(
                false,
                testPickupPointFactory.createPickupPoint(CreatePickupPointBuilder.builder()
                        .params(PickupPointTestParams.builder()
                                .brandingType(PickupPointBrandingType.FULL)
                                .build())
                        .build()),
                deliveryService,
                fbs);
    }

    public Order createSimpleFashionOrder(boolean prepaid) {
        return createSimpleFashionOrder(
                prepaid,
                testPickupPointFactory.createPickupPoint(CreatePickupPointBuilder.builder()
                        .params(PickupPointTestParams.builder()
                                .brandingType(PickupPointBrandingType.FULL)
                                .build())
                        .build()));
    }

    public Order createSimpleFashionOrder(boolean isPrepaid, PickupPoint pickupPoint) {
        DeliveryService deliveryService = deliveryServiceFactory.createDeliveryService();
        return createSimpleFashionOrder(isPrepaid, pickupPoint, deliveryService);
    }

    public Order createSimpleFashionOrder(boolean isPrepaid, PickupPoint pickupPoint, DeliveryService deliveryService) {
        return createSimpleFashionOrder(isPrepaid, pickupPoint, deliveryService, false);
    }

    public Order createSimpleFashionOrder(boolean isPrepaid, PickupPoint pickupPoint, DeliveryService deliveryService,
                                          boolean fbs) {
        return createSimpleFashionOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(isPrepaid ? OrderPaymentType.PREPAID : OrderPaymentType.CARD)
                        .deliveryService(deliveryService)
                        .isDropShip(fbs)
                        .fbs(fbs)
                        .build())
                .build());
    }

    public Order createSimpleFashionOrder(CreateOrderBuilder builder) {
        builder.getParams().setPartialDeliveryAllowed(true);
        builder.getParams().setPartialDeliveryAvailable(true);

        if (builder.getParams().getDeliveryService() == null) {
            builder.getParams().setDeliveryService(deliveryServiceFactory.createDeliveryService());
        }

        boolean fbs = builder.getParams().getFbs();
        builder.getParams().setItems(
                List.of(
                        TestOrderFactory.OrderItemParams.builder()
                                .count(1)
                                .name("Футболка")
                                .photoUrl("//futbolka.ru/photo.jpg")
                                .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                .isService(false)
                                .uitValues(fbs ? null : List.of(UIT_1_1))
                                .cisValues(List.of(CIS_1_1))
                                .price(BigDecimal.valueOf(1000))
                                .supplierTaxpayerNumber("item_1_sup")
                                .color("red")
                                .size("xl")
                                .build(),

                        TestOrderFactory.OrderItemParams.builder()
                                .count(2)
                                .name("Штаны")
                                .photoUrl("//shtanyi.ru/photo.jpg")
                                .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                .isService(false)
                                .uitValues(fbs ? null : List.of(UIT_2_1, UIT_2_2))
                                .cisValues(List.of(CIS_2_1, CIS_2_2))
                                .price(BigDecimal.valueOf(2000))
                                .supplierTaxpayerNumber("item_2_sup")
                                .color("blue")
                                .size("xs")
                                .build()
                )
        );

        return createOrder(builder);
    }

    public Order createFashionOrder(String uit, String cis, List<String> eans) {
        PickupPoint pickupPoint = testPickupPointFactory.createPickupPoint(CreatePickupPointBuilder.builder()
                .params(PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .build())
                .build());
        DeliveryService deliveryService = deliveryServiceFactory.createDeliveryService();

        return createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.PREPAID)
                        .partialDeliveryAllowed(true)
                        .partialDeliveryAvailable(true)
                        .deliveryService(deliveryService)
                        .fbs(uit == null)
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(1)
                                        .name("Футболка")
                                        .photoUrl("//futbolka.ru/photo.jpg")
                                        .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                        .isService(false)
                                        .uitValues(uit == null ? null : List.of(uit))
                                        .cisValues(cis == null ? null : List.of(cis))
                                        .price(BigDecimal.valueOf(1000))
                                        .supplierTaxpayerNumber("item_1_sup")
                                        .build()
                        ))
                        .build())
                .build());
    }

    public Order createFashionOrder(String... cis) {
        if (cis.length == 0) {
            throw new IllegalArgumentException("Cis count must be more than 0!");
        }

        PickupPoint pickupPoint = testPickupPointFactory.createPickupPoint(CreatePickupPointBuilder.builder()
                .params(PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .build())
                .build());
        DeliveryService deliveryService = deliveryServiceFactory.createDeliveryService();

        return createOrder(CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(OrderParams.builder()
                        .paymentType(OrderPaymentType.PREPAID)
                        .partialDeliveryAllowed(true)
                        .partialDeliveryAvailable(true)
                        .deliveryService(deliveryService)
                        .fbs(true)
                        .items(List.of(
                                OrderItemParams.builder()
                                        .count(cis.length)
                                        .name("Футболка")
                                        .photoUrl("//futbolka.ru/photo.jpg")
                                        .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                        .isService(false)
                                        .cisValues(Arrays.asList(cis))
                                        .price(BigDecimal.valueOf(1000))
                                        .supplierTaxpayerNumber("item_1_sup")
                                        .build()
                        ))
                        .build())
                .build());
    }

    public Order createFashionWithPartialReturn(PickupPoint pickupPoint) {
        return createFashionWithPartialReturn(pickupPoint, List.of(BARCODE_1, BARCODE_2));
    }

    public Order createFashionWithPartialReturn(PickupPoint pickupPoint, List<String> barcodes) {
        Order fashionOrder = createSimpleFashionOrder(true, pickupPoint);
        fashionOrder = receiveOrder(fashionOrder.getId());
        var deliveryResult = orderDeliveryResultCommandService.startFitting(fashionOrder.getId());
        long iii1 = deliveryResult.getItems().get(0).getItemInstanceId();
        long iii2 = deliveryResult.getItems().get(1).getItemInstanceId();
        orderDeliveryResultCommandService.updateItemFlow(fashionOrder.getId(), UIT_2_1, iii1, RETURN, SCAN, null);
        orderDeliveryResultCommandService.updateItemFlow(fashionOrder.getId(), UIT_2_2, iii2, RETURN, SCAN, null);
        orderDeliveryResultCommandService.finishFitting(fashionOrder.getId());
        orderDeliveryResultCommandService.pay(fashionOrder.getId());
        for (String barcode : barcodes) {
            orderDeliveryResultCommandService.scanSafePackage(fashionOrder.getId(), barcode);
        }
        orderDeliveryResultCommandService.commitPackaging(fashionOrder.getId());
        return fashionOrder;
    }

    @Data
    @Builder
    public static class CreateOrderBuilder {

        @Builder.Default
        private OrderParams params = OrderParams.builder().build();

        private PickupPoint pickupPoint;

        private OrderSender orderSender;
    }

    public Order receiveOrder(long id) {
        var order = orderRepository.findByIdOrThrow(id);
        var pickupPoint = order.getPickupPoint();
        shipmentCommandService.createReceive(
                new PickupPointRequestData(pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(),
                        DEFAULT_UID, pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod()),
                List.of(order.getExternalId())
        );
        dbQueueTestUtil.executeAllQueueItems(CHANGE_PICKUP_POINT_CAPACITY_BATCH);
        dbQueueTestUtil.executeAllQueueItems(SET_EXPIRATION_DATE_BATCH);
        dbQueueTestUtil.executeAllQueueItems(TRY_GROUP_SIBLING_ORDERS);
        orderPersonalCommandService.updatePhoneTail(
                orderPersonalQueryService.getActive(order.getId()).get().getId(), getDefaultPhoneTail());
        // TODO: add kgt and enrich executions: need to mock checkouter response for it
        return orderRepository.findByIdOrThrow(id);
    }

    public Order verifyOrder(long id) {
        var order = orderRepository.findByIdOrThrow(id);
        return orderCommandService.verifyCode(id, order.getPickupPoint().getId(), DEFAULT_VERIFICATION_CODE);
    }

    public Order dispatchOrder(long id) {
        var order = orderRepository.findByIdOrThrow(id);
        var pickupPoint = order.getPickupPoint();
        shipmentCommandService.createDispatch(
                new PickupPointRequestData(pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(),
                        DEFAULT_UID, pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod()),
                DispatchCreateParams.builder()
                        .items(List.of(DispatchCreateItemParams.builder()
                                .id(order.getExternalId())
                                .type(DispatchType.EXPIRED)
                                .build()))
                        .build());
        return orderRepository.findByIdOrThrow(id);
    }

    public Order readyForReturn(long id) {
        Order order = orderRepository.findByIdOrThrow(id);
        order.setStatusOnly(PvzOrderStatus.READY_FOR_RETURN);
        return updateOrder(order);
    }

    public Order setStatusAndCheckpoint(long id, PvzOrderStatus status) {
        Order order = orderRepository.findByIdOrThrow(id);
        order.setStatusAndCheckpoint(status);
        return updateOrder(order);
    }

    public Order setStatusOnly(long id, PvzOrderStatus status) {
        Order order = orderRepository.findByIdOrThrow(id);
        order.setStatusOnly(status);
        return updateOrder(order);
    }

    public Order updateOrder(Order order) {
        return orderCommandService.updateHeavy(order);
    }

    public Order updateOrder(long id) {
        var order = orderRepository.findByIdOrThrow(id);
        return updateOrder(order);
    }

    public List<Order> updateOrder(List<Order> orders) {
        List<Order> savedOrders = orderRepository.saveAll(orders);
        orderCommandService.checkOrderStatusIsChanged(orders, true);
        return savedOrders;
    }

    public Order deliverOrder(long id, OrderDeliveryType deliveryType, OrderPaymentType paymentType) {
        Order order = orderRepository.findByIdOrThrow(id);
        orderCommandService.deliver(order, deliveryType, paymentType);
        return orderRepository.findByIdOrThrow(id);
    }

    public Order deliverOrder(
            CreateOrderBuilder builder,
            OrderDeliveryType deliveryType,
            OrderPaymentType paymentType
    ) {
        var order = createOrder(builder);
        order = receiveOrder(order.getId());
        return deliverOrder(order.getId(), deliveryType, paymentType);
    }

    public Order deliverOrderCompletely(long id, OrderDeliveryType deliveryType, OrderPaymentType paymentType) {
        Order orderToBeDelivered = deliverOrder(id, deliveryType, paymentType);
        orderCommandService.commitDeliver(orderToBeDelivered.getId());
        return orderRepository.findByIdOrThrow(id);
    }

    public Order forceDeliver(long id, OffsetDateTime deliveredAt) {
        Order order = orderRepository.findByIdOrThrow(id);
        order.setDeliveredAt(deliveredAt);
        order.setStatusAndCheckpoint(PvzOrderStatus.DELIVERED_TO_RECIPIENT);
        return orderRepository.save(order);
    }

    public Order forceDeliver(long id, LocalDate deliveryDate) {
        return forceDeliver(id, OffsetDateTime.of(deliveryDate, LocalTime.of(12, 0), ZoneOffset.UTC));
    }

    public Order forceReceive(long id, OffsetDateTime arrivedAt) {
        Order order = orderRepository.findByIdOrThrow(id);
        order.setArrivedAt(arrivedAt);
        order.setStatusAndCheckpoint(PvzOrderStatus.ARRIVED_TO_PICKUP_POINT);
        return orderRepository.save(order);
    }

    public Order forceReceive(long id, LocalDate arrivedDate) {
        return forceReceive(id, OffsetDateTime.of(arrivedDate, LocalTime.of(12, 0), ZoneOffset.UTC));
    }

    public Order partialDeliver(long orderId, List<String> uitsToReturn) {
        orderDeliveryResultCommandService.startFitting(orderId);

        for (String uit : uitsToReturn) {
            orderDeliveryResultCommandService.updateItemFlow(orderId, uit, ItemDeliveryFlow.RETURN);
        }

        orderDeliveryResultCommandService.finishFitting(orderId);
        orderDeliveryResultCommandService.pay(orderId);
        orderDeliveryResultCommandService.packageReturn(orderId, List.of("PARTIAL_RETURN_PVZ_" + orderId));

        Order order = orderRepository.findByIdOrThrow(orderId);
        orderCommandService.deliver(order, OrderDeliveryType.UNKNOWN, OrderPaymentType.CARD);
        return orderRepository.save(order);
    }

    public Order commitPartialDelivery(long id) {
        orderCommandService.commitPartialDeliver(id);
        return orderRepository.findByIdOrThrow(id);
    }

    public Order commitDelivery(long id) {
        orderCommandService.commitDeliver(id);
        return orderRepository.findByIdOrThrow(id);
    }

    public void cancelOrder(long id) {
        Order order = orderRepository.findByIdOrThrow(id);
        OrderUpdateParams orderUpdateParams = orderParamsMapper.mapUpdate(order, configurationProvider);
        orderCommandService.cancel(orderUpdateParams);
    }

    public Order extendStoragePeriod(long id) {
        Order order = orderRepository.findByIdOrThrow(id);
        return orderCommandService.extendStoragePeriod(order.getExternalId(), order.getIdOfPickupPoint(),
                order.getExpirationDate().plusDays(1));
    }

    // скопировал код из ManualOrderService::updateOrdersStatus
    public void updateStatus(Long id, PvzOrderStatus newStatus) {
        Order order = orderRepository.findByIdOrThrow(id);
        if (order.getExpirationDate() == null && newStatus != PvzOrderStatus.CREATED
                && newStatus != PvzOrderStatus.STORAGE_PERIOD_EXTENDED
        ) {
            order = receiveOrder(order.getId());
        }
        OrderUpdateParams orderUpdateParams = orderParamsMapper.mapUpdate(order, configurationProvider);
        if (PvzOrderStatus.UNCHANGEABLE_STATUSES.contains(orderUpdateParams.getStatus())) {
            throw new TplInvalidActionException("Заказ номер " + orderUpdateParams.getExternalId() + " находится в " +
                    "финальном статусе - " + orderUpdateParams.getStatus() + " и изменить его невозможно");
        }
        orderCommandService.setOrderStatusAndCheckpoint(orderUpdateParams, newStatus);
        if ((PvzOrderStatus.CAN_RESCHEDULE_EXPIRATION_STATUSES.contains(newStatus) ||
                PvzOrderStatus.STORAGE_PERIOD_EXTENDED == newStatus) &&
                orderUpdateParams.getExpirationDate() == null) {
            orderCommandService.setExpirationDate(orderUpdateParams, OffsetDateTime.now(clock));
        }
        if (orderUpdateParams.getStatus() == PvzOrderStatus.STORAGE_PERIOD_EXTENDED) {
            orderCommandService.extendStoragePeriod(orderUpdateParams, false);
        } else {
            orderCommandService.updateOrderStatus(orderUpdateParams, false);
        }
    }

    public void setOrderAcceptedByCourier(String externalId, boolean acceptedByCourier) {
        orderAdditionalInfoCommandService.updateAcceptedByCourier(externalId, acceptedByCourier);
    }

    public void cancelDelivery(long id) {
        Order order = orderRepository.findByIdOrThrow(id);
        orderCommandService.cancelDelivery(order);
    }

    private Order buildOrder(OrderParams params, PickupPoint pickupPoint, OrderSender orderSender) {
        if (params.getDeliveryDate() == null) {
            params.setDeliveryDate(LocalDate.ofInstant(
                    clock.instant().plus(1, ChronoUnit.DAYS), clock.getZone()));
        }
        List<OrderItem> items = params.getItems().stream().map(this::buildOrderItem).collect(Collectors.toList());
        List<OrderPlace> places = params.getPlaces().stream()
                .map(place -> buildOrderPlace(place, items.stream()
                        .map(this::buildOrderPlaceItem)
                        .collect(Collectors.toList())))
                .collect(Collectors.toList());

        OrderAdditionalInfo additionalInfo = buildOrderAdditionalInfo(params, items, places);
        List<OrderPersonal> personals = buildPersonal(params);
        var order = Order.builder()
                .externalId(params.getExternalId())
                .type(params.getType())
                .paymentType(params.getPaymentType())
                .paymentStatus(params.getPaymentStatus())
                .deliveryDate(params.getDeliveryDate())
                .shipmentDate(params.getShipmentDate())
                .expirationDate(params.getExpirationDate())
                .dimensions(params.getDimensions())
                .recipientName(params.getRecipientName())
                .recipientEmail(params.getRecipientEmail())
                .recipientPhone(params.getRecipientPhone())
                .recipientNotes(params.getRecipientNotes())
                .personals(personals)
                .pickupPoint(pickupPoint)
                .items(items)
                .isClickAndCollect(params.getIsClickAndCollect())
                .isDropShipBySeller(params.getIsDropShipBySeller())
                .isKgt(params.getIsKgt())
                .places(places)
                .sender(orderSender)
                .orderVerification(new OrderVerification(params.getVerificationCode()))
                .deliveryService(params.getDeliveryService())
                .buyerYandexUid(params.getBuyerYandexUid())
                .fake(false)
                .assessedCost(params.getAssessedCost())
                .partialDeliveryAllowed(params.getPartialDeliveryAllowed())
                .partialDeliveryAvailable(params.getPartialDeliveryAvailable())
                .cashboxPaymentStatus(params.getCashboxPaymentStatus())
                .build();
        additionalInfo.setOrder(order);
        order.setOrderAdditionalInfo(additionalInfo);
        return order;
    }

    private OrderAdditionalInfo buildOrderAdditionalInfo(OrderParams params, List<OrderItem> items,
                                                         List<OrderPlace> places) {
        return OrderAdditionalInfo.builder()
                .acceptedByCourier(false)
                .courierId(null)
                .totalPrice(StreamEx.of(items)
                        .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getCount())))
                        .reduce(BigDecimal::add)
                        .orElse(BigDecimal.ZERO)
                )
                .placesCount((short) places.size())
                .placeCodes(StreamEx.of(places).map(OrderPlace::getBarcode).toList())
                .storagePeriodExtended(false)
                .isAdult(StreamEx.of(items).anyMatch(OrderItem::isR18))
                .deliveryServiceType(params.getDeliveryServiceType())
                .isC2c(params.getIsC2c())
                .fbs(params.getIsDropShip())
                .build();
    }

    private OrderItem buildOrderItem(OrderItemParams params) {
        return OrderItem.builder()
                .name(params.getName())
                .article(params.getArticle())
                .count(params.getCount())
                .price(params.getPrice())
                .service(params.getIsService())
                .sumPrice(params.getPrice().multiply(BigDecimal.valueOf(params.getCount())))
                .vatType(params.getVatType())
                .vendorArticle(new VendorArticle(params.getVendorId(), params.getVendorArticle()))
                .cargoTypeCodes(params.getCargoTypeCodes())
                .cisValues(params.getCisValues())
                .uitValues(params.getUitValues())
                .cisFullValues(params.getCisFullValues())
                .supplierTaxpayerNumber(params.getSupplierTaxpayerNumber())
                .supplierName(params.getSupplierName())
                .supplierPhone(params.getSupplierPhone())
                .photoUrl(params.getPhotoUrl())
                .color(params.getColor())
                .size(params.getSize())
                .build();
    }

    private OrderPlace buildOrderPlace(OrderPlaceParams params, List<OrderPlaceItem> placeItems) {
        return OrderPlace.builder()
                .yandexId(params.getYandexId())
                .barcode(params.getBarcode())
                .dimensions(params.getDimensions())
                .items(placeItems)
                .build();
    }

    private OrderPlaceItem buildOrderPlaceItem(OrderItem orderItem) {
        return OrderPlaceItem.builder()
                .orderItem(orderItem)
                .count(orderItem.getCount())
                .build();
    }

    private List<OrderPersonal> buildPersonal(OrderParams params) {
        if (!params.getPersonal().isCreateWithPersonal()) {
            return null;
        }
        List<OrderPersonal> personals = new ArrayList<>();
        personals.add(
                OrderPersonal.builder()
                        .recipientFullNameId(params.getPersonal().getRecipientFullNameId())
                        .recipientPhoneId(params.getPersonal().getRecipientPhoneId())
                        .recipientEmailId(params.getPersonal().getRecipientEmailId())
                        .buyerYandexUid(params.getPersonal().getBuyerYandexUid())
                        .active(true)
                        .build()
        );
        return personals;
    }

    @Data
    @Builder
    public static class OrderParams {

        public static final OrderType DEFAULT_TYPE = OrderType.CLIENT;
        public static final DeliveryServiceType DEFAULT_DELIVERY_TYPE = DeliveryServiceType.MARKET_COURIER;
        public static final OrderPaymentType DEFAULT_PAYMENT_TYPE = OrderPaymentType.PREPAID;
        public static final OrderPaymentStatus DEFAULT_PAYMENT_STATUS = OrderPaymentStatus.PAID;
        public static final BigDecimal DEFAULT_WEIGHT = BigDecimal.ONE;
        public static final int DEFAULT_WIDTH = 5;
        public static final int DEFAULT_HEIGHT = 3;
        public static final int DEFAULT_LENGTH = 10;
        public static final Dimensions DEFAULT_DIMENSIONS = Dimensions.builder()
                .weight(DEFAULT_WEIGHT).width(DEFAULT_WIDTH).height(DEFAULT_HEIGHT).length(DEFAULT_LENGTH).build();
        public static final String DEFAULT_RECIPIENT_NAME = "Василий Пупкин";
        public static final String DEFAULT_RECIPIENT_EMAIL = "vasily@pupkin.com";
        public static final String DEFAULT_RECIPIENT_PHONE = "89992281488";
        public static final OrderPersonalParams DEFAULT_PERSONAL = OrderPersonalParams.builder().build();
        public static final String DEFAULT_RECIPIENT_NOTES = "Заказ приду забирать ночью";
        public static final long DEFAULT_BUYER_YANDEX_UID = 7760649815172968448L;
        public static final List<OrderItemParams> DEFAULT_ITEMS = List.of(
                OrderItemParams.builder().build(),
                OrderItemParams.builder().build()
        );
        public static final List<OrderItemParams> R18_ITEM = List.of(
                TestOrderFactory.OrderItemParams.builder()
                        .cargoTypeCodes(List.of(CargoType.R18.getCode()))
                        .build()
        );
        public static final List<OrderPlaceParams> DEFAULT_PLACES = StreamEx.of(
                OrderPlaceParams.builder().build(),
                OrderPlaceParams.builder().build(),
                OrderPlaceParams.builder().build()
        ).sortedBy(o -> o.barcode).toList();
        public static final String DEFAULT_VERIFICATION_CODE = "11111";

        public static final boolean DEFAULT_PARTIAL_DELIVERY_ALLOWED = false;
        public static final boolean DEFAULT_PARTIAL_DELIVERY_AVAILABLE = false;
        public static final CodeType DEFAULT_CODE_TYPE = CodeType.UIT;

        @Builder.Default
        private String externalId = randomString(6);

        @Builder.Default
        private OrderType type = DEFAULT_TYPE;

        @Builder.Default
        private DeliveryServiceType deliveryServiceType = DEFAULT_DELIVERY_TYPE;

        @Builder.Default
        private OrderPaymentType paymentType = DEFAULT_PAYMENT_TYPE;

        @Builder.Default
        private OrderPaymentStatus paymentStatus = DEFAULT_PAYMENT_STATUS;

        private LocalDate deliveryDate;

        private LocalDate shipmentDate;

        private LocalDate expirationDate;

        @Builder.Default
        private Dimensions dimensions = DEFAULT_DIMENSIONS;

        @Builder.Default
        private String recipientName = DEFAULT_RECIPIENT_NAME;

        @Builder.Default
        private String recipientEmail = DEFAULT_RECIPIENT_EMAIL;

        @Builder.Default
        private OrderPersonalParams personal = DEFAULT_PERSONAL;

        @Builder.Default
        private String recipientPhone = DEFAULT_RECIPIENT_PHONE;

        @Builder.Default
        private String recipientNotes = DEFAULT_RECIPIENT_NOTES;

        @Builder.Default
        private List<OrderItemParams> items = DEFAULT_ITEMS;

        @Builder.Default
        private List<OrderPlaceParams> places = DEFAULT_PLACES;

        @Builder.Default
        private String verificationCode = DEFAULT_VERIFICATION_CODE;

        @Deprecated
        @Builder.Default
        private Boolean isDropShip = false;

        @Builder.Default
        private Boolean fbs = false;

        @Builder.Default
        private Boolean isClickAndCollect = false;

        @Builder.Default
        private Boolean isDropShipBySeller = false;

        @Builder.Default
        private Boolean isKgt = null;

        @Builder.Default
        private Boolean yaDelivery = null;

        @Builder.Default
        private Boolean partialDeliveryAllowed = DEFAULT_PARTIAL_DELIVERY_ALLOWED;

        @Builder.Default
        private Boolean partialDeliveryAvailable = DEFAULT_PARTIAL_DELIVERY_AVAILABLE;

        @Builder.Default
        private long buyerYandexUid = DEFAULT_BUYER_YANDEX_UID;

        private DeliveryService deliveryService;

        @Builder.Default
        private BigDecimal assessedCost = new BigDecimal(RandomUtils.nextInt(100, 10000) + ".00");

        @Builder.Default
        private Boolean isC2c = false;

        private CashboxPaymentStatus cashboxPaymentStatus;

        @Builder.Default
        private CodeType codeType = DEFAULT_CODE_TYPE;

        @Builder.Default
        private String cis = null;

        @Builder.Default
        private List<String> eans = null;

        public static BigDecimal getDefaultTotalPrice() {
            return StreamEx.of(DEFAULT_ITEMS)
                    .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getCount())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public static String getDefaultPhoneTail() {
            return DEFAULT_RECIPIENT_PHONE.substring(DEFAULT_RECIPIENT_PHONE.length() - 4);
        }
    }

    @Data
    @Builder
    public static class OrderPersonalParams {

        public static final String DEFAULT_RECIPIENT_FULL_NAME_ID = "1122334455";
        public static final String DEFAULT_RECIPIENT_PHONE_ID = "666777888999";
        public static final String DEFAULT_RECIPIENT_EMAIL_ID = "1234512345";

        @Builder.Default
        private boolean createWithPersonal = true;

        @Builder.Default
        private String recipientFullNameId = DEFAULT_RECIPIENT_FULL_NAME_ID;

        @Builder.Default
        private String recipientPhoneId = DEFAULT_RECIPIENT_PHONE_ID;

        @Builder.Default
        private String recipientEmailId = DEFAULT_RECIPIENT_EMAIL_ID;

        @Builder.Default
        private long buyerYandexUid = OrderParams.DEFAULT_BUYER_YANDEX_UID;
    }

    @Data
    @Builder
    public static class OrderItemParams {

        public static final String DEFAULT_NAME = "Пиво";
        public static final Integer DEFAULT_COUNT = 10;
        public static final BigDecimal DEFAULT_PRICE = new BigDecimal("100.50");
        public static final Boolean DEFAULT_IS_SERVICE = false;
        public static final VatType DEFAULT_VAT_TYPE = VatType.VAT_18;
        public static final List<String> DEFAULT_CIS_VALUES = List.of("010290002149746021HcXBQNb3PUc7S");
        public static final String DEFAULT_SUPPLIER_TAXPAYER_NUMBER = "772203235975";
        public static final String DEFAULT_SUPPLIER_NAME = "ООО Яндекс.Бухло";
        public static final String DEFAULT_SUPPLIER_PHONE = "8 (800) 555-35-35";

        @Builder.Default
        private String name = DEFAULT_NAME;

        @Builder.Default
        private String article = randomString(8);

        @Builder.Default
        private Integer count = DEFAULT_COUNT;

        @Builder.Default
        private BigDecimal price = DEFAULT_PRICE;

        @Builder.Default
        private Boolean isService = DEFAULT_IS_SERVICE;

        @Builder.Default
        private VatType vatType = DEFAULT_VAT_TYPE;

        @Builder.Default
        private Long vendorId = RandomUtils.nextLong();

        @Builder.Default
        private String vendorArticle = UUID.randomUUID().toString();

        @Builder.Default
        private List<Integer> cargoTypeCodes = Collections.emptyList();

        @Builder.Default
        private List<String> cisValues = DEFAULT_CIS_VALUES;

        @Builder.Default
        private List<String> cisFullValues = List.of();

        @Builder.Default
        private List<String> uitValues = null;

        @Builder.Default
        private String supplierTaxpayerNumber = DEFAULT_SUPPLIER_TAXPAYER_NUMBER;

        @Builder.Default
        private String supplierName = DEFAULT_SUPPLIER_NAME;

        @Builder.Default
        private String supplierPhone = DEFAULT_SUPPLIER_PHONE;

        @Builder.Default
        private String photoUrl = null;

        @Builder.Default
        private String color = null;

        @Builder.Default
        private String size = null;
    }

    @Data
    @Builder
    public static class OrderPlaceParams {

        public static final BigDecimal DEFAULT_WEIGHT = BigDecimal.ONE;

        @Builder.Default
        private String yandexId = String.valueOf(RandomUtils.nextInt(0, Integer.MAX_VALUE));

        @Builder.Default
        private String barcode = String.valueOf(RandomUtils.nextInt(0, Integer.MAX_VALUE));

        @Builder.Default
        private Dimensions dimensions = DEFAULT_DIMENSIONS;
    }

    @Data
    @Builder
    public static class OrderVerificationParams {

        public static final BigDecimal DEFAULT_WEIGHT = BigDecimal.ONE;

        @Builder.Default
        private String verificationCode = String.valueOf(RandomUtils.nextInt(0, Integer.MAX_VALUE));

        @Builder.Default
        private String barcode = String.valueOf(RandomUtils.nextInt(0, Integer.MAX_VALUE));

        @Builder.Default
        private Dimensions dimensions = DEFAULT_DIMENSIONS;
    }
}
