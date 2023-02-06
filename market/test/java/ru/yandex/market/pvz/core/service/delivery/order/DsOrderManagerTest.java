package ru.yandex.market.pvz.core.service.delivery.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.api.model.delivery.request.CancelOrderRequest;
import ru.yandex.market.logistic.api.model.delivery.request.CreateOrderRequest;
import ru.yandex.market.logistic.api.model.delivery.request.GetOrderHistoryRequest;
import ru.yandex.market.logistic.api.model.delivery.request.GetOrdersStatusRequest;
import ru.yandex.market.logistic.api.model.delivery.request.UpdateRecipientRequest;
import ru.yandex.market.logistic.api.model.delivery.response.CreateOrderResponse;
import ru.yandex.market.logistic.api.model.delivery.response.GetOrdersStatusResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.OrderStatus;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.pvz.client.model.order.DeliveryServiceType;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.delivery_service.DeliveryService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.order.OrderRepository;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderItem;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentStatus;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.OrderType;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.order.model.personal.OrderPersonal;
import ru.yandex.market.pvz.core.domain.order.model.personal.OrderPersonalRepository;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.service.delivery.DsApiBaseTest;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestDeliveryServiceFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.CreatePickupPointBuilder;
import ru.yandex.market.tpl.common.ds.exception.DsApiException;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.configuration.ConfigurationProvider;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.pvz.client.model.order.DeliveryServiceType.YANDEX_DELIVERY;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.EXTERNAL_ID_NOT_UNIQUE_ENABLED;
import static ru.yandex.market.pvz.core.service.delivery.order.DsOrderManager.ON_DEMAND_ORDER_MUST_BE_PREPAID_MESSAGE;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class DsOrderManagerTest extends DsApiBaseTest {

    private static final String TOKEN = "1";
    private static final LocalDate DELIVERY_DATE = LocalDate.of(2020, 7, 16);
    private static final String DEFAULT_ORDER_ID = "22525525";
    private static final String DEFAULT_INN = "1234567890";

    private final TestDeliveryServiceFactory deliveryServiceFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;
    private final OrderDeliveryResultCommandService orderDeliveryResultCommandService;
    private final OrderRepository orderRepository;
    private final OrderPersonalRepository orderPersonalRepository;
    private final TransactionTemplate transactionTemplate;
    private final TestableClock clock;
    private final ConfigurationProvider configurationProvider;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    private DeliveryService deliveryService;
    private PickupPoint pickupPoint;
    private LegalPartner legalPartner;

    private final DsOrderManager dsOrderManager;


    @BeforeEach
    void setup() {
        deliveryService = deliveryServiceFactory.createDeliveryService(TestDeliveryServiceFactory.DeliveryServiceParams
                .builder()
                .token(TOKEN)
                .build());
        legalPartner = legalPartnerFactory.createLegalPartner(TestLegalPartnerFactory.LegalPartnerTestParamsBuilder
                .builder()
                .deliveryService(deliveryService)
                .build());
        pickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .build());
        clock.setFixed(Instant.now(), ZoneId.systemDefault());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testCreateOrder(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        CreateOrderResponse response = sendCreateOrder("create_order.xml");
        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();

        transactionTemplate.execute(ts -> {
            checkCreatedOrderFromDefaultXml(orderId, pickupPoint.getId());
            return  null;
        });
    }

    @Nullable
    private void checkCreatedOrderFromDefaultXml(String orderId, long pickupPointId) {
        Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));

        assertThat(order.getExternalId()).isEqualTo("22525525");
        assertThat(order.getPickupPoint().getId()).isEqualTo(pickupPointId);
        assertThat(order.getStatus()).isEqualTo(PvzOrderStatus.CREATED);
        assertThat(order.getDsApiCheckpoint()).isEqualTo(PvzOrderStatus.CREATED.getCode());
        assertThat(order.getDeliveryDate()).isEqualTo(DELIVERY_DATE.toString());

        assertThat(order.getType()).isEqualTo(OrderType.CLIENT);
        assertThat(order.getPaymentType()).isEqualTo(OrderPaymentType.CARD);
        assertThat(order.getPaymentStatus()).isEqualTo(OrderPaymentStatus.UNPAID);

        assertThat(order.getSender().getYandexId()).isEqualTo("431782");
        assertThat(order.getSender().getIncorporation()).isEqualTo("ООО Яндекс Маркет");
        assertThat(order.getSender().getPhone()).isEqualTo("+78002342712");

        assertThat(order.getRecipientName()).isEqualTo("Пупкин Валисий");
        assertThat(order.getRecipientEmail()).isEqualTo("vasya@pupkin.com");
        assertThat(order.getRecipientPhone()).isEqualTo("+79992281488");

        assertThat(order.getPersonals()).hasSize(1);
        assertThat(order.getActivePersonal().getRecipientFullNameId()).isEqualTo("8765487654");
        assertThat(order.getActivePersonal().getRecipientPhoneId()).isEqualTo("445566778899");
        assertThat(order.getActivePersonal().getRecipientEmailId()).isEqualTo("999888777666");
        assertThat(order.getActivePersonal().getBuyerYandexUid()).isEqualTo(867689494L);
        assertThat(order.getActivePersonal().getPhoneTail()).isNull();

        assertThat(order.getRecipientNotes()).isEqualTo("Заберу в понедельник 20.07");

        assertThat(order.getDimensions().getWeight().doubleValue()).isEqualTo(4.600);
        assertThat(order.getDimensions().getHeight()).isEqualTo(23);
        assertThat(order.getDimensions().getWidth()).isEqualTo(29);
        assertThat(order.getDimensions().getLength()).isEqualTo(44);

        assertThat(order.getItems()).hasSize(4);
        assertThat(order.getPlaces()).hasSize(2);
        assertThat(order.getDeliveryCost()).isZero();

        assertThat(order.getOrderVerification()).isNotNull();
        assertThat(order.getOrderVerification().getVerificationAttempts()).isEqualTo((short) 0);
        assertThat(order.getOrderVerification().getVerificationAccepted()).isFalse();
        assertThat(order.getOrderVerification().getVerificationCode()).isEqualTo("11111");

        assertThat(order.getAssessedCost()).isEqualByComparingTo(BigDecimal.valueOf(1360.00));
        assertThat(order.getPartialDeliveryAllowed()).isFalse();

        assertThat(order.getOrderAdditionalInfo()).isNotNull();

        // true for not production env
        assertThat(order.getOrderAdditionalInfo().isAcceptedByCourier(configurationProvider)).isTrue();

        assertThat(order.getOrderAdditionalInfo().getTotalPrice())
                .isEqualByComparingTo(BigDecimal.valueOf(1360.00));
        assertThat(order.getOrderAdditionalInfo().getPlacesCount()).isEqualTo((short) 2);
        assertThat(order.getOrderAdditionalInfo().getPlaceCodes()).containsExactlyInAnyOrder("22525525",
                "22525526");
        assertThat(order.getOrderAdditionalInfo().getStoragePeriodExtended()).isFalse();
        assertThat(order.getOrderAdditionalInfo().getIsAdult()).isFalse();
        assertThat(order.getOrderAdditionalInfo().isC2c()).isFalse();

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testCreateC2cOrder(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        CreateOrderResponse response = sendCreateOrder("create_order_c2c.xml");
        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();

        transactionTemplate.execute(ts -> {
            Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));
            assertThat(order.getOrderAdditionalInfo().isC2c()).isTrue();
            assertThat(order.getOrderAdditionalInfo().getDeliveryServiceType()).isEqualTo(YANDEX_DELIVERY);
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testCreateFashionOrder(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .params(TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .build())
                .build());

        CreateOrderResponse response = sendCreateOrder(
                "create_fashion_order.xml",
                DEFAULT_ORDER_ID,
                DEFAULT_INN,
                pickupPoint.getId()
        );

        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();

        transactionTemplate.execute(ts -> {
            Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));
            assertThat(order.getPartialDeliveryAllowed()).isTrue();
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testUpdateSender(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        sendCreateOrder("create_order.xml", "111", DEFAULT_INN);

        String newInn = "9876543210";
        CreateOrderResponse response = sendCreateOrder("create_order.xml", "222", newInn);

        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();

        transactionTemplate.execute(ts -> {
            Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));

            assertThat(order.getSender().getYandexId()).isEqualTo("431782");
            assertThat(order.getSender().getIncorporation()).isEqualTo("ООО Яндекс Маркет");
            assertThat(order.getSender().getPhone()).isEqualTo("+78002342712");

            return null;
        });
    }

    private CreateOrderResponse sendCreateOrder(String filename) {
        return sendCreateOrder(filename, DEFAULT_ORDER_ID, DEFAULT_INN);
    }

    private CreateOrderResponse sendCreateOrder(String filename, String orderId, String senderInn) {
        return sendCreateOrder(filename, orderId, senderInn, pickupPoint.getId());
    }

    private CreateOrderResponse sendCreateOrder(String filename, String orderId, String senderInn, Long pickupPointId) {
        CreateOrderRequest request = readRequest("/ds/order/" + filename, CreateOrderRequest.class, Map.of(
                "orderId", orderId,
                "pickup_point_code", pickupPointId,
                "delivery_date", DELIVERY_DATE.toString(),
                "inn", senderInn
        ));
        return dsOrderManager.createOrder(request.getOrder(), request.getRestrictedData(), deliveryService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testCreateOnDemandOrder(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        CreateOrderResponse response = sendCreateOrder("create_on_demand_order.xml");
        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();

        Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));
        assertThat(order.getType()).isEqualTo(OrderType.ON_DEMAND);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void unableToCreateOnDemandNotPrepaidOrder(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        assertThatThrownBy(() -> sendCreateOrder("create_on_demand_not_prepaid_order.xml"))
                .isExactlyInstanceOf(DsApiException.class)
                .hasMessageContaining(ON_DEMAND_ORDER_MUST_BE_PREPAID_MESSAGE);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void cancelNotReceivedOrder(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        sendCancelOrder(order.getExternalId(), order.getId());

        Order cancelledOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(cancelledOrder.getStatus()).isEqualTo(PvzOrderStatus.CANCELLED);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void cancelReceivedOrder(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        orderFactory.receiveOrder(order.getId());

        sendCancelOrder(order.getExternalId(), order.getId());

        Order cancelledOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(cancelledOrder.getStatus()).isEqualTo(PvzOrderStatus.READY_FOR_RETURN);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void notCancelDispatchedOrder(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        orderFactory.receiveOrder(order.getId());
        orderFactory.readyForReturn(order.getId());
        orderFactory.dispatchOrder(order.getId());

        assertThatThrownBy(() -> sendCancelOrder(order.getExternalId(), order.getId()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void notCancelTransmittedOrder(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        orderFactory.receiveOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.UNKNOWN, null);

        assertThatThrownBy(() -> sendCancelOrder(order.getExternalId(), order.getId()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void notCancelTransmittedOnDemandOrder(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .type(OrderType.ON_DEMAND)
                        .build())
                .build());

        orderFactory.receiveOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.UNKNOWN, null);

        assertThatThrownBy(() -> sendCancelOrder(order.getExternalId(), order.getId()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void notCancelOrderTwice(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        sendCancelOrder(order.getExternalId(), order.getId());
        sendCancelOrder(order.getExternalId(), order.getId());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testCancelFashionOrderBeforeStartedFitting(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        Order order = orderFactory.createSimpleFashionOrder(false, pickupPoint, deliveryService);

        sendCancelOrder(order.getExternalId(), order.getId());

        Order cancelledOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(cancelledOrder.getStatus()).isEqualTo(PvzOrderStatus.CANCELLED);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testThrowsExceptionWhenFittingStartedOrderCanceled(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        Order order = orderFactory.createSimpleFashionOrder(false, pickupPoint, deliveryService);
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());

        assertThatThrownBy(() -> sendCancelOrder(order.getExternalId(), order.getId()))
                .isExactlyInstanceOf(DsApiException.class)
                .hasMessageContaining("Order cannot be cancelled after fitting has been started");
    }

    private void sendCancelOrder(String externalId, long orderId) {
        CancelOrderRequest cancelRequest = readRequest("/ds/order/cancel_order.xml", CancelOrderRequest.class,
                Map.of("token", TOKEN, "externalId", externalId, "orderId", orderId));
        dsOrderManager.cancelOrder(cancelRequest.getOrderId(), deliveryService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getOrdersStatus(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        var createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("22525525")
                        .deliveryService(deliveryService)
                        .build())
                .build());
        long orderId = createdOrder.getId();

        GetOrdersStatusRequest getOrdersStatusRequest = readRequest("/ds/order/get_orders_status.xml",
                GetOrdersStatusRequest.class, Map.of("orderId", orderId));
        GetOrdersStatusResponse statuses = dsOrderManager.getOrdersStatus(
                getOrdersStatusRequest.getOrdersId(), deliveryService);
        assertThat(statuses.getOrderStatusHistories()).hasSize(2);

        Order order = orderRepository.findByIdOrThrow(orderId);
        List<OrderStatus> history = statuses.getOrderStatusHistories().get(0).getHistory();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatusCode()).isEqualTo(OrderStatusType.ORDER_CREATED_DS);
        assertThat(history.get(0).getSetDate())
                .isEqualTo(DateTime.fromLocalDateTime(DateTimeUtil.toLocalDateTime(clock.instant())));

        assertThat(statuses.getOrderStatusHistories().get(1).getHistory()
                .get(0).getStatusCode()).isEqualTo(OrderStatusType.ORDER_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getOrderHistory(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        var createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("22525525")
                        .build())
                .build());
        long orderId = createdOrder.getId();
        String externalId = createdOrder.getExternalId();

        assertThat(sendGetOrderHistory(orderId).stream().map(OrderStatus::getStatusCode))
                .containsExactly(OrderStatusType.ORDER_CREATED_DS);

        sendCancelOrder(externalId, orderId);

        assertThat(sendGetOrderHistory(orderId).stream().map(OrderStatus::getStatusCode))
                .containsExactly(
                        OrderStatusType.ORDER_CREATED_DS,
                        OrderStatusType.ORDER_CANCELLED_DS
                );
    }

    private List<OrderStatus> sendGetOrderHistory(long orderId) {
        GetOrderHistoryRequest getOrderHistoryRequest = readRequest("/ds/order/get_order_history.xml",
                GetOrderHistoryRequest.class, Map.of("orderId", orderId));

        return dsOrderManager.getOrderHistory(getOrderHistoryRequest.getOrderId())
                .getOrderStatusHistory().getHistory();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testUpdateRecipientData(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        var createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("22525525")
                        .build())
                .build());
        long orderId = createdOrder.getId();

        UpdateRecipientRequest request = readRequest(
                "/ds/order/update_recipient.xml",
                UpdateRecipientRequest.class,
                Map.of("pickup_point_code", pickupPoint.getId(),
                        "orderId", orderId));

        dsOrderManager.updateRecipentData(request, pickupPoint.getLegalPartner().getDeliveryService());

        Order order = orderRepository.findByIdOrThrow(orderId);
        assertThat(order.getRecipientName()).isEqualTo("Петров Петя");
        assertThat(order.getRecipientEmail()).isEqualTo("petya-petrov@yandex.ru");
        assertThat(order.getRecipientPhone()).isEqualTo("+79992281488");

        Optional<OrderPersonal> personal = orderPersonalRepository.findActiveByOrderId(order.getId());
        assertThat(personal).isPresent();
        assertThat(personal.get().getRecipientFullNameId()).isEqualTo("8765487653");
        assertThat(personal.get().getRecipientPhoneId()).isEqualTo("445566778898");
        assertThat(personal.get().getRecipientEmailId()).isEqualTo("999888777665");
        assertThat(personal.get().getBuyerYandexUid()).isEqualTo(867689493L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testUpdateRecipientDataWithoutPersonal(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        var createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("22525525")
                        .personal(TestOrderFactory.OrderPersonalParams.builder()
                                .createWithPersonal(false)
                                .build())
                        .build())
                .build());
        long orderId = createdOrder.getId();

        UpdateRecipientRequest request = readRequest(
                "/ds/order/update_recipient_without_personal.xml",
                UpdateRecipientRequest.class,
                Map.of("pickup_point_code", pickupPoint.getId(),
                        "orderId", orderId));

        dsOrderManager.updateRecipentData(request, pickupPoint.getLegalPartner().getDeliveryService());

        Order order = orderRepository.findByIdOrThrow(orderId);
        assertThat(order.getRecipientName()).isEqualTo("Петров Петя");
        assertThat(order.getRecipientEmail()).isEqualTo("petya-petrov@yandex.ru");
        assertThat(order.getRecipientPhone()).isEqualTo("+79992281488");

        Optional<OrderPersonal> personal = orderPersonalRepository.findActiveByOrderId(order.getId());
        assertThat(personal).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testDifferentPlaceBarcodes(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        CreateOrderResponse response = sendCreateOrder("create_order_with_different_place_barcodes.xml");
        String orderId = response.getOrderId().getPartnerId();

        transactionTemplate.execute(ts -> {
            Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));

            Map<String, String> yandexIdToBarcode = order.getPlaces()
                    .stream()
                    .collect(HashMap::new, (m, p) -> m.put(p.getYandexId(), p.getBarcode()), HashMap::putAll);

            assertThat(yandexIdToBarcode.get("31727557")).isEqualTo("22525525");
            assertThat(yandexIdToBarcode.get("134892")).isEqualTo("123412");
            assertThat(yandexIdToBarcode.get("88201")).isNull();
            assertThat(yandexIdToBarcode.get("49130")).isEqualTo("4832901");
            assertThat(yandexIdToBarcode.get("324119")).isEqualTo("43215134");

            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testPrepaidNoTax(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        CreateOrderResponse response = sendCreateOrder("create_order_with_prepaid_no_tax.xml");
        String orderId = response.getOrderId().getPartnerId();

        transactionTemplate.execute(ts -> {
            Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));
            assertThat(order.getPaymentType()).isEqualTo(OrderPaymentType.PREPAID);
            assertThat(order.getItems().stream().map(OrderItem::getVatType)).containsOnlyNulls();

            return null;
        });
    }

    @ParameterizedTest
    @MethodSource("testYandexDeliveryOrderParams")
    void testYandexDeliveryOrder(String externalId, boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        sendCreateOrder("create_order.xml", externalId, DEFAULT_INN);

        transactionTemplate.execute(ts -> {
            Order order = orderRepository.findByExternalIdAndPickupPointIdOrThrow(externalId, pickupPoint.getId());
            assertThat(order.getOrderAdditionalInfo().getDeliveryServiceType()).isEqualTo(YANDEX_DELIVERY);
            return null;
        });
    }

    private static Stream<Arguments> testYandexDeliveryOrderParams() {
        return Stream.of(
                Arguments.of("23423a-LO-709856", true),
                Arguments.of("23423a-LO-709856", false),
                Arguments.of("LO-1234", true),
                Arguments.of("LO-1234", false)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testCardNoTax(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        assertThatThrownBy(() -> sendCreateOrder("create_order_with_card_no_tax.xml"))
                .isExactlyInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("Tax not found for not prepared order!");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testInvalidVerificationCode(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        assertThatThrownBy(() -> sendCreateOrder("create_order_with_invalid_verification_code.xml"))
                .isExactlyInstanceOf(DsApiException.class)
                .hasMessageContaining("Verification code must consist of 5 digits. Got: 2222");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testCreateOrderWithDbsDelivery(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        CreateOrderResponse response = sendCreateOrder("create_order_with_dbs.xml");
        String orderId = response.getOrderId().getPartnerId();

        transactionTemplate.execute(ts -> {
            Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));
            assertThat(order.getOrderAdditionalInfo().getDeliveryServiceType()).isEqualTo(DeliveryServiceType.DBS);
            assertThat(order.getPlaces().get(0).getItems()).hasSize(2);
            assertThat(order.getPlaces().get(1).getItems()).hasSize(2);
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testCreateOrderWithYadoDelivery(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        String yadoOrderId = "123-LO-123";
        CreateOrderResponse response = sendCreateOrder("create_order.xml", yadoOrderId, DEFAULT_INN);
        String orderId = response.getOrderId().getPartnerId();

        transactionTemplate.execute(ts -> {
            Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));
            assertThat(order.getOrderAdditionalInfo().getDeliveryServiceType()).isEqualTo(YANDEX_DELIVERY);
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testCreateOrderWithMarketDelivery(boolean uniqueExternalId) {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);

        CreateOrderResponse response = sendCreateOrder("create_order.xml");
        String orderId = response.getOrderId().getPartnerId();

        transactionTemplate.execute(ts -> {
            Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));
            assertThat(order.getOrderAdditionalInfo().getDeliveryServiceType())
                    .isEqualTo(DeliveryServiceType.MARKET_COURIER);
            return null;
        });
    }

    @Test
    public void cancelOrderWhenTwoOrdersWithSameExternalId() {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, true);

        var firstOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        var secondPickupPoint = createSecondPickupPoint();

        var secondOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder().externalId(firstOrder.getExternalId()).build())
                .pickupPoint(secondPickupPoint)
                .build());

        sendCancelOrder(secondOrder.getExternalId(), secondOrder.getId());

        var notCanceledOrder = orderRepository.findByIdOrThrow(firstOrder.getId());
        var cancelledOrder = orderRepository.findByIdOrThrow(secondOrder.getId());

        assertThat(notCanceledOrder.getStatus()).isNotEqualTo(PvzOrderStatus.CANCELLED);
        assertThat(cancelledOrder.getStatus()).isEqualTo(PvzOrderStatus.CANCELLED);

    }

    @Test
    public void updateRecipientDataWhenTwoOrdersWithSameExternalId() {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, true);

        var firstOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("22525525")
                        .build())
                .build());

        var secondPickupPoint = createSecondPickupPoint();

        var secondOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(secondPickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("22525525")
                        .build())
                .build());

        long orderId = secondOrder.getId();
        var request = readRequest(
                "/ds/order/update_recipient.xml",
                UpdateRecipientRequest.class,
                Map.of("pickup_point_code", pickupPoint.getId(),
                        "orderId", orderId));

        dsOrderManager.updateRecipentData(request, pickupPoint.getLegalPartner().getDeliveryService());

        var updatedOrder = orderRepository.findByIdOrThrow(orderId);
        assertThat(updatedOrder.getRecipientName()).isEqualTo("Петров Петя");
        assertThat(updatedOrder.getRecipientEmail()).isEqualTo("petya-petrov@yandex.ru");
        assertThat(updatedOrder.getRecipientPhone()).isEqualTo("+79992281488");

        var personal = orderPersonalRepository.findActiveByOrderId(updatedOrder.getId());
        assertThat(personal).isPresent();
        assertThat(personal.get().getRecipientFullNameId()).isEqualTo("8765487653");
        assertThat(personal.get().getRecipientPhoneId()).isEqualTo("445566778898");
        assertThat(personal.get().getRecipientEmailId()).isEqualTo("999888777665");
        assertThat(personal.get().getBuyerYandexUid()).isEqualTo(867689493L);

        var notUpdatedOrder = orderRepository.findByIdOrThrow(firstOrder.getId());

        assertThat(notUpdatedOrder.getRecipientName()).isNotEqualTo("Петров Петя");
        assertThat(notUpdatedOrder.getRecipientEmail()).isNotEqualTo("petya-petrov@yandex.ru");
        assertThat(notUpdatedOrder.getRecipientPhone()).isNotEqualTo("+79992281488");
    }

    @Test
    public void createOrderThenAndThenCancelAndCreateInOtherPickupPoint() {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, true);

        String filename = "create_order.xml";

        var response = sendCreateOrder(filename);
        String orderId = response.getOrderId().getPartnerId();

        sendCancelOrder(response.getOrderId().getYandexId(), Long.parseLong(orderId));

        var secondPickupPoint = createSecondPickupPoint();
        var secondResponse = sendCreateOrder(filename, DEFAULT_ORDER_ID, DEFAULT_INN,
                secondPickupPoint.getId());

        String secondOrderId = secondResponse.getOrderId().getPartnerId();

        var canceledOrder = orderRepository.findByIdOrThrow(Long.parseLong(orderId));
        assertThat(secondOrderId).isNotNull().isNotEqualTo(orderId);
        assertThat(canceledOrder.getStatus()).isEqualTo(PvzOrderStatus.CANCELLED);
        transactionTemplate.execute(ts -> {
            checkCreatedOrderFromDefaultXml(secondOrderId, secondPickupPoint.getId());
            return  null;
        });

    }


    private PickupPoint createSecondPickupPoint() {
        return pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .params(TestPickupPointFactory.PickupPointTestParams.builder().name("second pvz").build())
                .legalPartner(legalPartner)
                .build());
    }

}
