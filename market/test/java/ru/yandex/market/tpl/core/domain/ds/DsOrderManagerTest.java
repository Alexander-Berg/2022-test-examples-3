package ru.yandex.market.tpl.core.domain.ds;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.request.CancelOrderRequest;
import ru.yandex.market.logistic.api.model.delivery.request.CreateOrderRequest;
import ru.yandex.market.logistic.api.model.delivery.request.GetOrderRequest;
import ru.yandex.market.logistic.api.model.delivery.request.GetOrdersDeliveryDateRequest;
import ru.yandex.market.logistic.api.model.delivery.request.GetOrdersStatusRequest;
import ru.yandex.market.logistic.api.model.delivery.request.UpdateOrderDeliveryDateRequest;
import ru.yandex.market.logistic.api.model.delivery.request.UpdateOrderRequest;
import ru.yandex.market.logistic.api.model.delivery.request.UpdateRecipientRequest;
import ru.yandex.market.logistic.api.model.delivery.response.CreateOrderResponse;
import ru.yandex.market.logistic.api.model.delivery.response.GetOrdersDeliveryDateResponse;
import ru.yandex.market.logistic.api.model.delivery.response.GetOrdersStatusResponse;
import ru.yandex.market.logistic.api.model.delivery.response.UpdateOrderResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.OrderStatus;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.VatType;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.locker.PickupPointType;
import ru.yandex.market.tpl.api.model.order.partner.OrderEventType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.user.UserRole;
import ru.yandex.market.tpl.common.ds.exception.DsApiException;
import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalRetrieveApi;
import ru.yandex.market.tpl.common.personal.client.model.CommonType;
import ru.yandex.market.tpl.common.personal.client.model.CommonTypeEnum;
import ru.yandex.market.tpl.common.personal.client.model.GpsCoord;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveRequestItem;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.tpl.common.personal.client.model.PersonalAddressKeys;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeRetrieveRequest;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeRetrieveResponse;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.TestUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.CargoType;
import ru.yandex.market.tpl.core.domain.order.Dimensions;
import ru.yandex.market.tpl.core.domain.order.DimensionsClass;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderDelivery;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEvent;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.OrderItem;
import ru.yandex.market.tpl.core.domain.order.OrderPlace;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceItem;
import ru.yandex.market.tpl.core.domain.order.OrderProperty;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.PersonalRecipient;
import ru.yandex.market.tpl.core.domain.order.Recipient;
import ru.yandex.market.tpl.core.domain.order.VendorArticle;
import ru.yandex.market.tpl.core.domain.order.address.AddressString;
import ru.yandex.market.tpl.core.domain.order.address.DeliveryAddress;
import ru.yandex.market.tpl.core.domain.order.property.TplOrderProperties;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.schedule.RoutingRequestWaveService;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserUtil;
import ru.yandex.market.tpl.core.domain.usershift.DeliverySubtask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftOrderQueryService;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.precise.PreciseGeoPointService;
import ru.yandex.market.tpl.core.domain.yago.YandexGoOrderProperties;
import ru.yandex.market.tpl.core.service.delivery.LogisticApiRequestProcessingConfiguration;
import ru.yandex.market.tpl.core.service.delivery.ds.DsRequestReader;
import ru.yandex.market.tpl.core.service.delivery.ds.request.CreateRegisterRequest;
import ru.yandex.market.tpl.core.service.delivery.ds.request.UpdateOrderDeliveryRequest;
import ru.yandex.market.tpl.core.service.order.OrderFeaturesResolver;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.MOSCOW_TIMEZONE_NAME;
import static ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval.valueOf;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.CREATE_ORDER_INN_VALIDATION_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.CREATE_ORDER_PHONE_NUMBER_VALIDATION_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DS_API_UPDATE_ADDRESS_BY_PERSONAL_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.NEW_DS_UPDATE_ORDER_ENABLED;
import static ru.yandex.market.tpl.core.domain.order.DimensionsClass.BULKY_CARGO;
import static ru.yandex.market.tpl.core.domain.order.DimensionsClass.REGULAR_CARGO;

/**
 * @author kukabara
 */
@RequiredArgsConstructor
@ContextConfiguration(classes = {
        LogisticApiRequestProcessingConfiguration.class
})
class DsOrderManagerTest extends TplAbstractTest {

    public static final String EXPECTED_WAREHOUSE_PHONE_NUMBER = "88000000000";

    private static final String YANDEX_ORDER_ID = "12981801";
    private static final String RECIPIENT_FIO = "Инокетий Иванович Смоктуновский";
    private static final String PERSONAL_RECIPIENT_FIO_ID = "1234";
    private static final String RECIPIENT_PHONE = "+71234567890";
    private static final String PERSONAL_RECIPIENT_PHONE_ID = "4321";
    private static final String RECIPIENT_EMAIL = "test@yandex-team.ru";
    private static final String PERSONAL_RECIPIENT_EMAIL_ID = "5678";
    private static final Recipient RECIPIENT = new Recipient(RECIPIENT_FIO, RECIPIENT_EMAIL, RECIPIENT_PHONE);
    private static final String PERSONAL_ADDRESS_ID = "1111";
    private static final String PERSONAL_GPS_ID = "2222";
    private static final PersonalRecipient PERSONAL_RECIPIENT =
            new PersonalRecipient(PERSONAL_RECIPIENT_FIO_ID, PERSONAL_RECIPIENT_EMAIL_ID, PERSONAL_RECIPIENT_PHONE_ID);
    private static final String RECIPIENT_NOTES = "Просьба позвонить за полчаса";
    private static final String WAREHOUSE_ID = "172";

    private final DsOrderManager dsOrderManager;
    private final PartnerRepository<DeliveryService> partnerRepository;
    private final OrderRepository orderRepository;
    private final OrderCommandService orderCommandService;
    private final DsRequestReader dsRequestReader;
    private final EntityManager entityManager;
    private final PickupPointRepository pickupPointRepository;
    private final TransactionTemplate transactionTemplate;
    private final OrderHistoryEventRepository orderHistoryEventRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TestUserHelper userHelper;
    private final UserShiftOrderQueryService userShiftOrderQueryService;
    private final ShiftRepository shiftRepository;
    private final PartnerRepository<DeliveryService> deliveryServiceRepository;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final OrderFeaturesResolver orderFeaturesResolver;
    private final DsPersonalAddressService dsPersonalAddressService;

    private final Clock clock;
    @MockBean
    private RoutingRequestWaveService mockedRoutingRequestWaveService;
    @MockBean
    private PreciseGeoPointService mockedPreciseGeoPointService;
    @MockBean
    private SortingCenterPropertyService sortingCenterPropertyService;
    @MockBean
    private DefaultPersonalRetrieveApi personalRetrieveApi;
    @SpyBean
    private YandexGoOrderProperties yandexGoOrderProperties;
    @SpyBean
    private SortingCenterService sortingCenterService;
    private DeliveryService partner;

    @BeforeEach
    void setUp() {
        ClockUtil.initFixed(clock, ClockUtil.defaultDateTime());
        partner = partnerRepository.findByIdOrThrow(DeliveryService.DEFAULT_DS_ID);
        Mockito.reset(configurationProviderAdapter);
    }

    /**
     * Даже пустой {@link AfterEach} нужен
     * для срабатывания {@link ru.yandex.market.tpl.core.test.TplAbstractTest#clearAfterTest(Object)}
     */
    @AfterEach
    void afterEach() {
    }

    @Test
    void createRegister() throws Exception {
        CreateRegisterRequest request = dsRequestReader.readRequest("/ds/create_register.xml",
                CreateRegisterRequest.class);
        var register = dsOrderManager.createRegister(request.getRegister(), partner);
        assertThat(register).isNotNull();
    }

    @Test
    void createOrder() throws Exception {
        CreateOrderResponse response = dsRequestReader.sendCreateOrder(partner);
        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();

        transactionTemplate.execute(tt -> {
            Order order = orderRepository.findById(Long.parseLong(orderId)).orElseThrow();
            assertThat(order.getExternalOrderId()).isEqualTo(YANDEX_ORDER_ID);
            assertThat(order.getDeliveryServiceId()).isEqualTo(partner.getId());
            assertThat(order.getItems()).hasSize(2);
            assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.CREATED);
            assertThat(order.isPickup()).isFalse();
            assertThat(order.getDeliveryPrice().compareTo(BigDecimal.valueOf(200))).isEqualTo(0);
            OrderItem orderItem = order.getItems().stream().filter(oi -> !oi.isService()).findFirst().orElseThrow();
            OrderItem deliveryOrderItem =
                    order.getItems().stream().filter(OrderItem::isService).findFirst().orElseThrow();
            assertThat(orderItem.getVatType()).isEqualTo(VatType.VAT_20);
            assertThat(orderItem.getSupplierInn()).isEqualTo("123456");
            assertThat(orderItem.getSupplierName()).isEqualTo("ООО Поставщик");
            assertThat(orderItem.getSupplierPhoneNumber()).isEqualTo("+71112223344");
            assertThat(deliveryOrderItem.getSupplierInn()).isEqualTo(OrderItem.YANDEX_INN);
            assertThat(deliveryOrderItem.getVatType()).isEqualTo(VatType.VAT_10);
            assertThat(order.getDelivery().getInterval().toLocalTimeInterval(DateTimeUtil.DEFAULT_ZONE_ID).toDashString())
                    .isEqualTo("10:00:00-18:00:00");
            assertThat(order.getDelivery().getDeliveryDate(DateTimeUtil.DEFAULT_ZONE_ID)).isEqualTo("2020-01-14");
            assertThat(order.getDelivery().getDeliveryAddress().getAddress())
                    .isEqualTo("Смоленский бульвар, д. 1, кв. 2");
            assertThat(order.getDelivery().getRecipientPhone()).isEqualTo("+71112223344");
            assertThat(order.getDelivery().getRecipientFio()).isEqualTo("Ivan Petrov");
            assertThat(order.getDelivery().getRecipientFioPersonalId()).isEqualTo(PERSONAL_RECIPIENT_FIO_ID);
            assertThat(order.getDelivery().getRecipientPhonePersonalId()).isEqualTo(PERSONAL_RECIPIENT_PHONE_ID);
            assertThat(order.getDelivery().getRecipientEmailPersonalId()).isEqualTo(PERSONAL_RECIPIENT_EMAIL_ID);
            assertThat(order.getDelivery().getDeliveryAddress().getAddressPersonalId()).isEqualTo(PERSONAL_ADDRESS_ID);
            assertThat(order.getDelivery().getDeliveryAddress().getGpsPersonalId()).isEqualTo(PERSONAL_GPS_ID);
            assertThat(order.getPaymentType()).isEqualTo(OrderPaymentType.PREPAID);
            assertThat(order.getDelivery().getRecipientNotes()).isEqualTo("Просьба позвонить за полчаса");
            assertThat(Objects.requireNonNull(order.getWarehouse()).getYandexId()).isEqualTo("10000010736");
            assertThat(Objects.requireNonNull(order.getWarehouseReturn()).getYandexId()).isEqualTo("10000010736");
            assertThat(Objects.requireNonNull(order.getSender()).getYandexId()).isEqualTo("431782");
            assertThat(order.getSender().getOgrn()).isEqualTo("1167746491395");
            assertThat(order.getTariff()).isEqualTo("Market_TPL_pok");
            checkOrderItemsInstancesCount(order);

            return null;
        });
    }

    @Test
    @DisplayName("Успешный перевод статуса заказа, все условия выполнены")
    void successCreateDeliveryBySellerOrder() throws IOException {
        User courier = userHelper.findOrCreateUser(2435328L);
        doReturn(true).when(sortingCenterService).usePvz(any());
        UserUtil.setRole(courier, UserRole.COURIER);
        DeliveryService partner = userHelper.createOrFindDbsDeliveryService();

        long orderId = sendCreateOrderResponse(partner);
        Order order = orderRepository.findByIdOrThrow(orderId);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
    }

    @Test
    @DisplayName("Не указаны идентификаторы служб доставки, статус не проставляется")
    void createDeliveryBySellerOrderWithoutServiceIdsParameter() throws IOException {
        long orderId = sendCreateOrderResponse();

        Order order = orderRepository.findByIdOrThrow(orderId);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.CREATED);

        checkForOrderDeliveryTask(order, true);

        checkForShiftCreated(order, true);
    }

    private Long sendCreateOrderResponse() throws IOException {
        return sendCreateOrderResponse(partner);
    }

    private Long sendCreateOrderResponse(DeliveryService partner) throws IOException {
        CreateOrderResponse response = dsRequestReader.sendCreateOrder("/ds/create_order.xml", null, partner);
        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();
        return Long.parseLong(orderId);
    }

    private void checkForOrderDeliveryTask(Order order, boolean noTasksExpected) {
        List<DeliverySubtask> subtasksStream = transactionTemplate.execute(
                tt -> userShiftOrderQueryService.findDeliverySubtasksByOrder(order).collect(Collectors.toList())
        );
        if (noTasksExpected) {
            assertThat(subtasksStream.size()).isEqualTo(0);
            return;
        }

        List<OrderDeliveryTask> tasks = subtasksStream.stream()
                .filter(deliveryTask -> deliveryTask instanceof OrderDeliveryTask)
                .map(deliveryTask -> (OrderDeliveryTask) deliveryTask)
                .collect(Collectors.toList());

        assertThat(tasks).hasSize(1);
        OrderDeliveryTask orderDeliveryTask = tasks.get(0);
        assertThat(orderDeliveryTask.getCallToRecipientTask()).isNotNull();
        assertThat(orderDeliveryTask.getExpectedDeliveryTime())
                .isEqualTo(order.getDelivery().getDeliveryIntervalFrom());
    }

    private void checkForShiftCreated(Order order, boolean noShiftCreated) {
        Long sortingCenterId = deliveryServiceRepository.findByIdOrThrow(order.getDeliveryServiceId())
                .getSortingCenter().getId();
        LocalDate deliveryDate = order.getDelivery().getDeliveryDate(ZoneId.systemDefault());

        Optional<Shift> optShift = shiftRepository.findByShiftDateAndSortingCenterId(deliveryDate, sortingCenterId);

        assertThat(optShift.isEmpty()).isEqualTo(noShiftCreated);
    }

    @Test
    void createOrderFromMarchroute() throws Exception {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds/create_order_from_marchroute.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID
        );
        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(request.getOrder(), null), partner);
        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();
        transactionTemplate.execute(tt -> {
            Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));
            assertThat(Objects.requireNonNull(order.getWarehouse()).getYandexId()).isEqualTo("145");
            assertThat(Objects.requireNonNull(order.getWarehouseReturn()).getYandexId()).isEqualTo("10000010736");
            return null;
        });
    }

    @Test
    void createOrderWithPickupPointInterval() throws Exception {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds/create_order_to_lavka.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);
        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(request.getOrder(), null), partner);
        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();
        transactionTemplate.execute(tt -> {
            Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));
            assertTrue(order.isPickup());
            LocalTimeInterval interval =
                    order.getDelivery().getInterval().toLocalTimeInterval(DateTimeUtil.DEFAULT_ZONE_ID);
            assertThat(interval).isEqualTo(LocalTimeInterval.valueOf("08:00-12:00"));
            checkOrderItemsInstancesCount(order);
            this.clearAfterTest(order.getPickupPoint());

            return null;
        });
    }

    @Test
    void createOrderWithExistingPickupPointInterval() throws Exception {
        PickupPoint pickupPoint = new PickupPoint();
        pickupPoint.setType(PickupPointType.LOCKER);
        pickupPoint.setPartnerSubType(PartnerSubType.LOCKER);
        pickupPoint.setLogisticPointId(100012345L);
        pickupPoint.setLastSyncAt(Instant.now());
        pickupPoint.setLastScheduleSyncAt(Instant.now());
        pickupPointRepository.save(pickupPoint);
        this.clearAfterTest(pickupPoint);

        CreateOrderRequest request = dsRequestReader.readRequest("/ds/create_order_to_locker.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);
        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(request.getOrder(), null), partner);
        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();
        Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));
        assertTrue(order.isPickup());
        LocalTimeInterval interval =
                order.getDelivery().getInterval().toLocalTimeInterval(DateTimeUtil.DEFAULT_ZONE_ID);
        assertThat(interval).isEqualTo(LocalTimeInterval.valueOf("06:00-15:00"));
    }

    @Test
    void createFashionOrderHappyPath() throws Exception {
        CreateOrderResponse response = dsRequestReader.sendCreateOrder("/ds/create_order_fashion.xml", "1", partner);

        transactionTemplate.execute(tt -> {
            var order = orderRepository.findByExternalOrderId(response.getOrderId().getYandexId())
                    .orElseThrow();
            assertThat(orderFeaturesResolver.isFashion(order)).isTrue();
            order.getItems().stream()
                    .filter(orderItem -> !orderItem.isService())
                    .forEach(orderItem -> assertThat(orderItem.isFashion()).isTrue());
            return null;
        });
    }

    @Test
    void createFashionOrderWhereSomeItemIsNotFashion() throws Exception {
        CreateOrderRequest request = dsRequestReader.readRequest(
                "/ds/create_order_fashion.xml",
                CreateOrderRequest.class,
                YANDEX_ORDER_ID
        );
        request.getOrder().getItems().stream()
                .findFirst()
                .ifPresent(item -> TestUtil.setPrivateFinalField(item, "cargoTypes", null));

        assertThatThrownBy(() -> dsOrderManager.createOrder(withExternalId(request.getOrder(), "1"), partner))
                .isInstanceOf(DsApiException.class)
                .hasMessageContaining("Fashion order must have all returnable items, but some items are not " +
                        "returnable");
    }

    @Test
    void createFashionOrderWhereSomeItemIsNotFashion_noServiceTrying() throws Exception {
        //given
        CreateOrderRequest request = dsRequestReader.readRequest(
                "/ds/create_order_fashion.xml",
                CreateOrderRequest.class,
                YANDEX_ORDER_ID
        );
        TestUtil.setPrivateFinalField(request.getOrder(), "services", List.of());

        //when
        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(request.getOrder(), "1"),
                partner);

        //then
        transactionTemplate.execute(tt -> {
            var order = orderRepository.findByExternalOrderId(response.getOrderId().getYandexId())
                    .orElseThrow();
            assertThat(orderFeaturesResolver.isFashion(order)).isFalse();
            return null;
        });
    }

    @Test
    void createOrderWithoutWarehousePhones() throws Exception {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds/create_order_without_warehouse_phones.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);
        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(request.getOrder(), null), partner);
        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();

        transactionTemplate.execute(tt -> {
            Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));
            assertThat(Objects.requireNonNull(order.getWarehouse()).getPhones()).isEmpty();
            assertThat(Objects.requireNonNull(order.getWarehouseReturn()).getPhones()).isEmpty();
            checkOrderItemsInstancesCount(order);
            return null;
        });
    }

    @Test
    void createOrderFromMarchrouteWithoutTaxes() throws Exception {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds/create_order_from_marchroute_without_taxes.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);
        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(request.getOrder(), null), partner);
        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();
        Order order = orderRepository.findById(Long.parseLong(orderId)).orElseThrow();
        assertThat(order.getItems().stream().filter(i -> i.getVatType() != null).count())
                .isEqualTo(0);
    }

    @Test
    void createOrderFromMarchrouteWithoutTaxesNotPrepaid() throws Exception {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds" +
                        "/create_order_from_marchroute_without_taxes_not_prepaid.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);
        assertThatThrownBy(() -> dsOrderManager.createOrder(withExternalId(request.getOrder(), null), partner))
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("Tax not found for not prepared order!");
    }

    @Test
    void createOrderFromMarchrouteWithoutTaxesOnDeliveryNotPrepaid() throws Exception {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds" +
                        "/create_order_from_marchroute_without_taxes_on_delivery_not_prepaid.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);
        assertThatThrownBy(() -> dsOrderManager.createOrder(withExternalId(request.getOrder(), null), partner))
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("Tax for delivery not found for not prepared order!");
    }

    @Test
    void createOrderFromMarchrouteWithoutTaxesOnDeliveryPrepaid() throws Exception {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds" +
                        "/create_order_from_marchroute_without_taxes_on_delivery_prepaid.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);
        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(request.getOrder(), null), partner);
        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();
        Order order = orderRepository.findById(Long.parseLong(orderId)).orElseThrow();
        assertThat(order.getItems().stream().filter(i -> i.getVatType() == null).collect(Collectors.toList()).get(0).isService())
                .isEqualTo(true);
    }

    @Test
    void createOrderTwice() throws Exception {
        CreateOrderResponse response = dsRequestReader.sendCreateOrder(partner);
        CreateOrderResponse response2 = dsRequestReader.sendCreateOrder(partner);
        assertThat(response2.getOrderId().getPartnerId())
                .isEqualTo(response.getOrderId().getPartnerId());
    }

    @Test
    void createTwoOrdersWithSameSenderAndWarehouse() throws Exception {
        CreateOrderResponse response1 = dsRequestReader.sendCreateOrder("/ds/create_order.xml", "1", partner);
        CreateOrderResponse response2 = dsRequestReader.sendCreateOrder("/ds/create_order.xml", "2", partner);

        assertThat(response1.getOrderId().getPartnerId()).isNotEqualTo(response2.getOrderId().getPartnerId());

        transactionTemplate.execute(tt -> {

            Order order1 = orderRepository.findByIdOrThrow(Long.parseLong(response1.getOrderId().getPartnerId()));
            Order order2 = orderRepository.findByIdOrThrow(Long.parseLong(response2.getOrderId().getPartnerId()));

            assertThat(order1.getWarehouse()).isEqualTo(order2.getWarehouse());
            assertThat(order1.getSender()).isEqualTo(order2.getSender());
            return null;
        });
    }

    @Test
    void shouldNotCreateOrderWithNullInn() {
        when(configurationProviderAdapter.isBooleanEnabled(CREATE_ORDER_INN_VALIDATION_ENABLED)).thenReturn(true);
        var ex = assertThrows(DsApiException.class,
                () -> dsRequestReader.sendCreateOrder("/ds/create_order_empty_inn.xml", "1", partner));
        assertThat(ex.getMessage()).isNotNull();
        assertThat(ex.getMessage()).contains("have invalid inn length");
    }

    @Test
    void shouldNotCreateOrderWithInvalidPhoneNumber() {
        when(configurationProviderAdapter.isBooleanEnabled(CREATE_ORDER_PHONE_NUMBER_VALIDATION_ENABLED)).thenReturn(true);
        var ex = assertThrows(DsApiException.class,
                () -> dsRequestReader.sendCreateOrder("/ds/create_order_invalid_phone.xml", "1", partner));
        assertThat(ex.getMessage()).isNotNull();
        assertThat(ex.getMessage()).contains("have invalid phone number length. Invalid phone number");
    }

    @Test
    void createOrderWithoutGeoCoords() throws IOException {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds/create_order.xml", CreateOrderRequest.class,
                YANDEX_ORDER_ID);
        TestUtil.setPrivateFinalField(request.getOrder().getLocationTo(), "lat", null);
        TestUtil.setPrivateFinalField(request.getOrder().getLocationTo(), "lng", null);

        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(
                request.getOrder(), "1"), partner);

        Order order = orderRepository.findByIdOrThrow(Long.parseLong(response.getOrderId().getPartnerId()));
        DeliveryAddress deliveryAddress = order.getDelivery().getDeliveryAddress();
        assertThat(deliveryAddress.getLatitude().doubleValue()).isEqualTo(0);
        assertThat(deliveryAddress.getLongitude().doubleValue()).isEqualTo(0);
    }

    @Test
    void createOrderWithDimensions() throws IOException {
        CreateOrderResponse response = dsRequestReader.sendCreateOrder(partner);
        Order order = orderRepository.findByIdOrThrow(Long.parseLong(response.getOrderId().getPartnerId()));
        assertThat(order.getDimensions()).isNotNull();
    }

    @Test
    void createOrderWithoutPlaces() throws IOException {
        String externalOrderId = "1";
        CreateOrderRequest request = dsRequestReader.readRequest("/ds/create_order_without_places.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);

        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(
                request.getOrder(), externalOrderId), partner);

        transactionTemplate.execute(tt -> {
            Order order = orderRepository.findByIdOrThrow(Long.parseLong(response.getOrderId().getPartnerId()));

            assertThat(order.getPlaces()).isNotNull();
            assertThat(order.getPlaces()).hasSize(1);

            return null;
        });
    }

    @Test
    void createOrderWithSinglePlace() throws IOException {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds/create_order.xml", CreateOrderRequest.class,
                YANDEX_ORDER_ID);

        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(
                request.getOrder(), "1"), partner);

        transactionTemplate.execute(tt -> {
            Order order =
                    orderRepository.findByIdOrThrow(Long.parseLong(response.getOrderId().getPartnerId()));

            assertThat(order.getPlaces()).isNotNull();
            assertThat(order.getPlaces()).hasSize(1);
            OrderPlace orderPlace = order.getPlaces().iterator().next();
            assertThat(orderPlace.getYandexId()).isEqualTo("15437991");
            assertThat(orderPlace.getBarcode()).isEqualTo(new OrderPlaceBarcode(WAREHOUSE_ID, "12981801"));
            assertThat(orderPlace.getDimensions()).isEqualTo(new Dimensions(
                    new BigDecimal("0.100"),
                    21,
                    5,
                    15)
            );

            // места заявлены - но нет привязок к item-ам заказа
            assertThat(orderPlace.getItems()).isEmpty();
            return null;
        });
    }

    @Test
    void createOrderWithSinglePlaceWithoutKorobyte() throws IOException {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds/create_order_with_place_without_korobyte.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);

        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(
                request.getOrder(), "1"), partner);

        transactionTemplate.execute(tt -> {

            Order order = orderRepository.findByIdOrThrow(Long.parseLong(response.getOrderId().getPartnerId()));
            assertThat(order.getPlaces()).hasSize(1);
            OrderPlace orderPlace = order.getPlaces().iterator().next();
            assertThat(orderPlace.getYandexId()).isEqualTo("15437991");
            assertThat(orderPlace.getBarcode()).isEqualTo(new OrderPlaceBarcode(WAREHOUSE_ID, "12981801"));
            assertThat(orderPlace.getDimensions()).isNull();
            return null;
        });
    }

    @Test
    void createOrderWithMultiplePlaces() throws IOException {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds/create_order_with_multiple_places.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);

        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(
                request.getOrder(), "1"), partner);

        transactionTemplate.execute(t -> {
            Order order = orderRepository.findByIdOrThrow(Long.parseLong(response.getOrderId().getPartnerId()));

            assertThat(order.getPlaces()).hasSize(2);
            assertThat(order.getPlaces())
                    .extracting(OrderPlace::getBarcode)
                    .containsExactlyInAnyOrder(
                            new OrderPlaceBarcode(WAREHOUSE_ID, "12981801-1"),
                            new OrderPlaceBarcode(WAREHOUSE_ID, "12981801-2"));
            return null;
        });
    }

    @Test
    void createOrderWithMultiplePlacesWithoutPartnerCodes() throws IOException {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds" +
                        "/create_order_with_multiple_places_without_partner_codes.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);

        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(
                request.getOrder(), "1"), partner);

        transactionTemplate.execute(t -> {
            Order order = orderRepository.findByIdOrThrow(Long.parseLong(response.getOrderId().getPartnerId()));

            assertThat(order.getPlaces()).hasSize(2);
            assertThat(order.getPlaces())
                    .extracting(OrderPlace::getBarcode)
                    .containsExactlyInAnyOrder(
                            new OrderPlaceBarcode(null, "12981801-1"),
                            new OrderPlaceBarcode(null, "12981801-2"));
            return null;
        });
    }

    @Test
    void createOrderReduceDuplicatedPlaces() throws IOException {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds/create_order_with_duplicated_places.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);

        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(
                request.getOrder(), "1"), partner);

        Order order =
                orderRepository.findByIdIn(List.of(Long.parseLong(response.getOrderId().getPartnerId()))).iterator().next();

        assertThat(order.getPlaces()).hasSize(1);
        assertThat(order.getPlaces())
                .extracting(OrderPlace::getBarcode)
                .containsExactlyInAnyOrder(
                        new OrderPlaceBarcode(WAREHOUSE_ID, "12981801-1"));
    }

    @Test
    void createOrderWithNoUnitId() throws IOException {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds/create_order_with_no_unit_id.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);

        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(
                request.getOrder(), "1"), partner);

        Order order = orderRepository.findByIdOrThrow(Long.parseLong(response.getOrderId().getPartnerId()));

        assertThat(order).isNotNull();
    }

    @Test
    void createOrderWithMultiplePlacesAndMultipleItems() throws IOException {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds/create_order_with_multiple_items.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);

        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(
                request.getOrder(), "1"), partner);

        transactionTemplate.execute(tt -> {
            Order order =
                    orderRepository.findByIdOrThrow(Long.parseLong(response.getOrderId().getPartnerId()));

            List<OrderItem> orderItems = order.getItems().stream()
                    .filter(i -> !i.isService())
                    .collect(Collectors.toList());
            assertThat(orderItems).hasSize(2);
            assertThat(orderItems)
                    .extracting(OrderItem::getVendorArticle)
                    .containsExactlyInAnyOrder(
                            new VendorArticle(484490L, "НС-0070604"),
                            new VendorArticle(484490L, "НС-0070605")
                    );

            assertThat(order.getPlaces()).hasSize(2);
            assertThat(order.getPlaces())
                    .extracting(OrderPlace::getBarcode)
                    .containsExactlyInAnyOrder(
                            new OrderPlaceBarcode(WAREHOUSE_ID, "12981801-1"),
                            new OrderPlaceBarcode(WAREHOUSE_ID, "12981801-2"));

            // в каждом грузовом месте - 1 item из заказа
            for (OrderPlace place : order.getPlaces()) {
                assertThat(place.getItems()).hasSize(1);
                OrderPlaceItem placeItem = place.getItems().iterator().next();
                assertThat(placeItem.getCount()).isEqualTo(1);
            }
            return null;
        });
    }

    @Test
    void updateDimensionsAndOrderPlacesOnUpdateOrder() throws IOException {
        String externalOrderId = "1";
        CreateOrderRequest request = dsRequestReader.readRequest(
                "/ds/create_order_without_places_with_multiple_items.xml", CreateOrderRequest.class, YANDEX_ORDER_ID);

        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(
                request.getOrder(), externalOrderId), partner);

        Order order =
                orderRepository.findByIdIn(List.of(Long.parseLong(response.getOrderId().getPartnerId()))).iterator().next();

        assertThat(order.getPlaces()).isNotNull();
        assertThat(order.getPlaces()).hasSize(1);

        when(configurationProviderAdapter.isBooleanEnabled(NEW_DS_UPDATE_ORDER_ENABLED)).thenReturn(true);

        // обновить заказ
        UpdateOrderRequest updateRequest = dsRequestReader.readRequest("/ds/update_order_multiple_places.xml",
                UpdateOrderRequest.class, YANDEX_ORDER_ID);
        UpdateOrderResponse updateResponse = dsOrderManager.updateOrder(withExternalId(
                updateRequest.getOrder(), externalOrderId), partner,
                dsPersonalAddressService.getNewDeliveryAddressFromPersonal(updateRequest),
                dsPersonalAddressService.getOldDeliveryAddressFromPersonal(updateRequest, partner));
        transactionTemplate.executeWithoutResult(tt -> {
            Order orderAfterUpdate =
                    orderRepository.findById(Long.parseLong(updateResponse.getOrderId().getPartnerId())).orElseThrow();

            assertThat(orderAfterUpdate.getPlaces()).hasSize(2);
            assertThat(orderAfterUpdate.getPlaces())
                    .extracting(OrderPlace::getBarcode)
                    .containsExactlyInAnyOrder(
                            new OrderPlaceBarcode(WAREHOUSE_ID, "12981801-1"),
                            new OrderPlaceBarcode(WAREHOUSE_ID, "12981801-2"));
            // в каждом грузовом месте - 1 item из заказа
            for (OrderPlace place : orderAfterUpdate.getPlaces()) {
                assertThat(place.getItems()).hasSize(1);
                OrderPlaceItem placeItem = place.getItems().iterator().next();
                assertThat(placeItem.getCount()).isEqualTo(1);
            }
            checkOrderItemsInstancesCount(orderAfterUpdate);
        });
    }

    @DisplayName("После обновления коробок в заказе, проставлаяется тип габарит заказа")
    @SneakyThrows
    @Test
    void updateDimensionsCargoAndOrderPlacesOnUpdateOrder() {
        String externalOrderId = "1";
        CreateOrderRequest request = dsRequestReader.readRequest(
                "/ds/create_order_without_places_with_multiple_items.xml", CreateOrderRequest.class, YANDEX_ORDER_ID);
        CreateOrderResponse response = dsOrderManager.createOrder(
                withExternalId(request.getOrder(), externalOrderId),
                partner
        );
        List<Long> orderIds = List.of(Long.parseLong(response.getOrderId().getPartnerId()));
        Order order = orderRepository.findByIdIn(orderIds)
                .iterator()
                .next();
        assertNotNull(order.getPlaces());
        assertThat(order.getPlaces()).hasSize(1);
        assertNotNull(order.getDimensions());
        assertNotNull(order.getDimensionsClass());
        assertEquals(order.getDimensionsClass(), DimensionsClass.fromDimensions(order.getDimensions()));

        when(configurationProviderAdapter.isBooleanEnabled(NEW_DS_UPDATE_ORDER_ENABLED)).thenReturn(true);

        // обновить заказ и проверить габариты
        UpdateOrderRequest updateRequest = dsRequestReader.readRequest("/ds/update_order_multiple_places.xml",
                UpdateOrderRequest.class, YANDEX_ORDER_ID);
        UpdateOrderResponse updateResponse = dsOrderManager.updateOrder(withExternalId(
                updateRequest.getOrder(), externalOrderId), partner,
                dsPersonalAddressService.getNewDeliveryAddressFromPersonal(updateRequest),
                dsPersonalAddressService.getOldDeliveryAddressFromPersonal(updateRequest, partner));
        transactionTemplate.executeWithoutResult(tt -> {
            Order orderAfterUpdate =
                    orderRepository.findById(Long.parseLong(updateResponse.getOrderId().getPartnerId())).orElseThrow();
            assertThat(orderAfterUpdate.getPlaces()).hasSize(2);
            assertEquals(orderAfterUpdate.getDimensionsClass(), REGULAR_CARGO);
        });
    }

    @DisplayName("После обновления коробок в заказе, поменялся грузотип заказа," +
            "сначала проставили грузотип по ВГХ заказа," +
            "после обновления грузотип стал характеризоваться коробками внутри заказа.")
    @SneakyThrows
    @Test
    void updateDimensionsBulkyCargoAndOrderPlacesOnUpdateOrder() {
        String externalOrderId = "1";
        CreateOrderRequest request = dsRequestReader.readRequest(
                "/ds/create_order_without_places_with_multiple_items.xml", CreateOrderRequest.class, YANDEX_ORDER_ID);
        CreateOrderResponse response = dsOrderManager.createOrder(
                withExternalId(request.getOrder(), externalOrderId),
                partner
        );
        List<Long> orderIds = List.of(Long.parseLong(response.getOrderId().getPartnerId()));
        Order order = orderRepository.findByIdIn(orderIds)
                .iterator()
                .next();
        assertNotNull(order.getPlaces());
        assertThat(order.getPlaces()).hasSize(1);
        assertNotNull(order.getDimensions());
        assertNotNull(order.getDimensionsClass());
        DimensionsClass dimensionsClassByOrder = DimensionsClass.fromDimensions(order.getDimensions());
        assertEquals(order.getDimensionsClass(), dimensionsClassByOrder);

        when(configurationProviderAdapter.isBooleanEnabled(NEW_DS_UPDATE_ORDER_ENABLED)).thenReturn(true);

        // обновить заказ и проверить габариты
        UpdateOrderRequest updateRequest = dsRequestReader.readRequest(
                "/ds/create_order_with_dimension_bulky_cargo.xml", UpdateOrderRequest.class, YANDEX_ORDER_ID);
        UpdateOrderResponse updateResponse = dsOrderManager.updateOrder(withExternalId(
                updateRequest.getOrder(), externalOrderId), partner,
                dsPersonalAddressService.getNewDeliveryAddressFromPersonal(updateRequest),
                dsPersonalAddressService.getOldDeliveryAddressFromPersonal(updateRequest, partner));
        transactionTemplate.executeWithoutResult(tt -> {
            Order orderAfterUpdate =
                    orderRepository.findById(Long.parseLong(updateResponse.getOrderId().getPartnerId())).orElseThrow();
            assertThat(orderAfterUpdate.getPlaces()).hasSize(2);
            assertEquals(orderAfterUpdate.getDimensionsClass(), BULKY_CARGO);
        });
    }

    @Test
    void getOrder() throws Exception {
        doReturn(EXPECTED_WAREHOUSE_PHONE_NUMBER).when(yandexGoOrderProperties).getWarehousePhoneNumber();
        dsRequestReader.sendCreateOrder(partner);
        GetOrderRequest request = dsRequestReader.readRequest("/ds/get_order.xml", GetOrderRequest.class,
                YANDEX_ORDER_ID);
        var order = dsOrderManager.getDsOrder(request.getOrderId(), partner);
        assertThat(order).isNotNull();
    }

    @Test
    void cancelOrder() throws Exception {
        CreateOrderResponse response = dsRequestReader.sendCreateOrder(partner);
        String orderId = response.getOrderId().getPartnerId();

        CancelOrderRequest cancelRequest = dsRequestReader.readRequest("/ds/cancel_order.xml",
                CancelOrderRequest.class, YANDEX_ORDER_ID, orderId);
        dsOrderManager.cancelOrder(cancelRequest.getOrderId(), partner);

        Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.CANCELLED);
    }

    @DisplayName("При обновлении заказа меняются адрес и даты доставки, а также комментарий получателя")
    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void updateAddressAndDeliveryDatesOnUpdateOrder(boolean withPersonal) {
        String externalOrderId = "1";
        CreateOrderRequest request = dsRequestReader.readRequest(
                "/ds/create_order_without_places_with_multiple_items.xml", CreateOrderRequest.class, YANDEX_ORDER_ID);

        dsOrderManager.createOrder(withExternalId(
                request.getOrder(), externalOrderId), partner);

        initDsUpdateOrderMocks();

        if (withPersonal) {
            when(configurationProviderAdapter.isBooleanEnabled(DS_API_UPDATE_ADDRESS_BY_PERSONAL_ENABLED))
                    .thenReturn(true);

            PersonalMultiTypeRetrieveRequest requestNewAddress = new PersonalMultiTypeRetrieveRequest().items(
                    List.of(new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.ADDRESS).id("123"),
                            new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.GPS_COORD).id("321"))
            );

            PersonalMultiTypeRetrieveRequest requestOldAddress = new PersonalMultiTypeRetrieveRequest().items(
                    List.of(new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.ADDRESS).id("456"),
                            new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.GPS_COORD).id("654"))
            );

            Map<String, String> newAddressMap = new HashMap<>();
            newAddressMap.put(PersonalAddressKeys.COUNTRY.getName(), "Россия");
            newAddressMap.put(PersonalAddressKeys.FEDERAL_DISTRICT.getName(), "Центральный федеральный округ");
            newAddressMap.put(PersonalAddressKeys.REGION.getName(), "Москва и Московская область");
            newAddressMap.put(PersonalAddressKeys.LOCALITY.getName(), "Москва");
            newAddressMap.put(PersonalAddressKeys.STREET.getName(), "Другой бульвар_pers");
            newAddressMap.put(PersonalAddressKeys.HOUSE.getName(), "1");
            newAddressMap.put(PersonalAddressKeys.HOUSING.getName(), "1");
            newAddressMap.put(PersonalAddressKeys.BUILDING.getName(), "3");
            newAddressMap.put(PersonalAddressKeys.ROOM.getName(), "2");
            newAddressMap.put(PersonalAddressKeys.ZIP_CODE.getName(), "123056");
            newAddressMap.put(PersonalAddressKeys.GEO_ID.getName(), "213");

            PersonalMultiTypeRetrieveResponse responseNewAddress = new PersonalMultiTypeRetrieveResponse().items(
                    List.of(
                            new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.ADDRESS).id("123")
                                    .value(new CommonType().address(newAddressMap)),
                            new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.GPS_COORD).id("321")
                                    .value(
                                            new CommonType().gpsCoord(
                                                    new GpsCoord().latitude(BigDecimal.valueOf(55.741794))
                                                            .longitude(BigDecimal.valueOf(37.582539))
                                            )
                                    )
                    )
            );

            PersonalMultiTypeRetrieveResponse responseOldAddress = new PersonalMultiTypeRetrieveResponse().items(
                    List.of(
                            new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.ADDRESS).id("456")
                                    .value(new CommonType().address(Map.of(
                                            PersonalAddressKeys.COUNTRY.getName(), "Россия",
                                            PersonalAddressKeys.FEDERAL_DISTRICT.getName(), "Центральный федеральный округ",
                                            PersonalAddressKeys.REGION.getName(), "Москва и Московская область",
                                            PersonalAddressKeys.LOCALITY.getName(), "Москва",
                                            PersonalAddressKeys.STREET.getName(), "Смоленский бульвар",
                                            PersonalAddressKeys.HOUSE.getName(), "1",
                                            PersonalAddressKeys.HOUSING.getName(), "1",
                                            PersonalAddressKeys.BUILDING.getName(), "3",
                                            PersonalAddressKeys.ROOM.getName(), "2",
                                            PersonalAddressKeys.ZIP_CODE.getName(), "123056"
                                    ))),
                            new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.GPS_COORD).id("654")
                                    .value(
                                            new CommonType().gpsCoord(
                                                    new GpsCoord().latitude(BigDecimal.valueOf(55.741794))
                                                            .longitude(BigDecimal.valueOf(37.582538))
                                            )
                                    )
                    )
            );

            when(personalRetrieveApi.v1MultiTypesRetrievePost(requestNewAddress)).thenReturn(responseNewAddress);
            when(personalRetrieveApi.v1MultiTypesRetrievePost(requestOldAddress)).thenReturn(responseOldAddress);
        }

        UpdateOrderRequest updateOrderRequest = dsRequestReader.readRequest(
                "/ds/update_order_address.xml",
                UpdateOrderRequest.class,
                externalOrderId
        );
        UpdateOrderResponse updateResponse = dsOrderManager.updateOrder(
                withExternalId(updateOrderRequest.getOrder(), externalOrderId),
                partner,
                dsPersonalAddressService.getNewDeliveryAddressFromPersonal(updateOrderRequest),
                dsPersonalAddressService.getOldDeliveryAddressFromPersonal(updateOrderRequest, partner)
        );
        transactionTemplate.executeWithoutResult(tt -> {
            Order updated = orderRepository.findById(Long.parseLong(updateResponse.getOrderId().getPartnerId()))
                    .orElseThrow();
            if (withPersonal) {
                assertThat(updated.getDelivery().getDeliveryAddress().getStreet()).isEqualTo("Другой бульвар_pers");
                assertThat(updated.getDelivery().getDeliveryAddress().getAddress())
                        .isEqualTo("г. Москва, ул. Другой бульвар_pers, д. 1, стр. 3, к. 1, кв. 2");
                assertThat(updated.getDelivery().getDeliveryAddress().getLatitude())
                        .isEqualTo(BigDecimal.valueOf(55.741794));
                assertThat(updated.getDelivery().getDeliveryAddress().getLongitude())
                        .isEqualTo(BigDecimal.valueOf(37.582539));
                assertThat(updated.getDelivery().getDeliveryAddress().getAddressPersonalId()).isEqualTo("123");
                assertThat(updated.getDelivery().getDeliveryAddress().getGpsPersonalId()).isEqualTo("321");
            }
            else {
                assertThat(updated.getDelivery().getDeliveryAddress().getLongitude())
                        .isEqualTo(BigDecimal.valueOf(37.582538));
                assertThat(updated.getDelivery().getDeliveryAddress().getStreet()).isEqualTo("Другой бульвар");
                assertThat(updated.getDelivery().getDeliveryAddress().getAddress())
                        .isEqualTo("Другой бульвар, д. 1, стр. 3, к. 1, кв. 2");
            }
            assertThat(updated.getDelivery().getDeliveryAddress())
                    .isEqualToIgnoringGivenFields(createAddress(), "street", "address", "longitude", "regionId",
                            "addressPersonalId", "gpsPersonalId");
            LocalDate newDate = LocalDate.of(2020, 1, 15);
            Interval interval = valueOf("09:00-18:00").toInterval(newDate, ZoneId.of(MOSCOW_TIMEZONE_NAME));
            assertThat(updated.getDelivery().getDeliveryDateAtDefaultTimeZone()).isEqualTo(newDate);
            assertThat(updated.getDelivery().getInterval()).isEqualTo(interval);
            assertThat(updated.getDelivery().getRecipientNotes()).isEqualTo("Просьба позвонить за час");
        });
    }

    @DisplayName("При обновлении типа доставки c курьера на пункт выдачи меняются тип, адрес и даты доставки, а " +
            "также добавляется пункт выдачи. Текущая задача доставки отменяется по причине переноса.")
    @SneakyThrows
    @Test
    void updateDeliveryTypeFromCourierToPickupOnUpdateOrder() {
        PickupPoint pickupPoint = createLockerPickupPoint();

        String externalOrderId = "1";
        CreateOrderRequest request = dsRequestReader.readRequest(
                "/ds/create_order_without_places_with_multiple_items.xml", CreateOrderRequest.class, YANDEX_ORDER_ID);
        CreateOrderResponse createResponse = dsOrderManager.createOrder(withExternalId(
                request.getOrder(), externalOrderId), partner);
        Order created = orderRepository.findById(Long.parseLong(createResponse.getOrderId().getPartnerId()))
                .orElseThrow();

        User user = testUserHelper.findOrCreateUser(1L);
        Shift shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        RoutePoint routePoint = testDataFactory.createEmptyRoutePoint(user, userShiftId, Instant.now(), Instant.now());

        testUserHelper.addDeliveryTaskToShift(user, routePoint.getUserShift(), created);

        initDsUpdateOrderMocks();

        UpdateOrderRequest updateOrderRequest = dsRequestReader.readRequest(
                "/ds/update_delivery_type_to_pickup.xml",
                UpdateOrderRequest.class,
                externalOrderId
        );
        UpdateOrderResponse updateResponse = dsOrderManager.updateOrder(
                withExternalId(updateOrderRequest.getOrder(), externalOrderId),
                partner,
                dsPersonalAddressService.getNewDeliveryAddressFromPersonal(updateOrderRequest),
                dsPersonalAddressService.getOldDeliveryAddressFromPersonal(updateOrderRequest, partner)
        );

        checkOrderUpdated(pickupPoint, updateResponse, OrderDeliveryTask.class);
    }

    @DisplayName("При обновлении типа доставки c постамата на пункт выдачи меняются тип, адрес и даты доставки, а " +
            "также меняется пункт выдачи. Текущая задача доставки отменяется по причине переноса.")
    @SneakyThrows
    @Test
    void updateDeliveryTypeFromLockerToPvzOnUpdateOrder() {
        PickupPoint pickupPoint = createPvzPickupPoint();

        String externalOrderId = "1";
        CreateOrderRequest request = dsRequestReader.readRequest(
                "/ds/create_order_without_places_with_multiple_items_to_pickup.xml",
                CreateOrderRequest.class,
                YANDEX_ORDER_ID
        );
        createLockerDeliveryOrderWithTask(externalOrderId, request);

        initDsUpdateOrderMocks();

        UpdateOrderRequest updateOrderRequest = dsRequestReader.readRequest(
                "/ds/update_delivery_type_to_pickup_pvz.xml",
                UpdateOrderRequest.class,
                externalOrderId
        );
        UpdateOrderResponse updateResponse = dsOrderManager.updateOrder(
                withExternalId(updateOrderRequest.getOrder(), externalOrderId),
                partner,
                dsPersonalAddressService.getNewDeliveryAddressFromPersonal(updateOrderRequest),
                dsPersonalAddressService.getOldDeliveryAddressFromPersonal(updateOrderRequest, partner)
        );

        checkOrderUpdated(pickupPoint, updateResponse, LockerSubtask.class);
    }

    private void initDsUpdateOrderMocks() {
        when(configurationProviderAdapter.isBooleanEnabled(NEW_DS_UPDATE_ORDER_ENABLED)).thenReturn(true);
        when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                eq(SortingCenterProperties.DELAYED_CHECKPOINT_ENABLED),
                anyLong()
        )).thenReturn(true);
        when(mockedPreciseGeoPointService.getByAddress(any(AddressString.class), any()))
                .thenReturn(Optional.of(GeoPoint.ofLatLon(55.741794, 37.582538)));
        when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                eq(SortingCenterProperties.IS_RESCHEDULING_LIMITED),
                anyLong()
        )).thenReturn(true);
    }

    private <SUBTASK extends DeliverySubtask> void checkOrderUpdated(
            PickupPoint pickupPoint,
            UpdateOrderResponse updateResponse,
            Class<SUBTASK> subtaskClass
    ) {
        transactionTemplate.executeWithoutResult(ts -> {
            Order updated = orderRepository.findById(Long.parseLong(updateResponse.getOrderId().getPartnerId()))
                    .orElseThrow();
            assertThat(updated.getDelivery().getDeliveryAddress().getStreet()).isEqualTo("Другой бульвар");
            LocalDate newDate = LocalDate.of(2020, 1, 15);
            Interval interval = valueOf("09:00-18:00").toInterval(newDate, ZoneId.of(MOSCOW_TIMEZONE_NAME));
            assertThat(updated.getDelivery().getDeliveryDateAtDefaultTimeZone()).isEqualTo(newDate);
            assertThat(updated.getDelivery().getInterval()).isEqualTo(interval);
            boolean isPickup = pickupPoint != null;
            assertThat(updated.isPickup()).isEqualTo(isPickup);
            assertThat(updated.getPickupPoint()).isEqualTo(pickupPoint);
            assertThat(updated.getPaymentType()).isEqualTo(isPickup
                    ? OrderPaymentType.PREPAID
                    : OrderPaymentType.CARD
            );
            assertThat(updated.getPaymentStatus()).isEqualTo(isPickup
                    ? OrderPaymentStatus.PAID
                    : OrderPaymentStatus.UNPAID
            );

            List<SUBTASK> subtasks =
                    StreamEx.of(userShiftOrderQueryService.findDeliverySubtasksByOrderIds(List.of(updated.getId())))
                            .select(subtaskClass)
                            .collect(Collectors.toList());
            assertThat(subtasks).hasSize(1);
            SUBTASK subtask = subtasks.get(0);
            assertThat(subtask.getFailReason()).isNotNull();
            assertThat(subtask.getFailReason().getType())
                    .isEqualTo(OrderDeliveryTaskFailReasonType.ORDER_TYPE_UPDATED);
        });
    }

    @DisplayName("При обновлении типа доставки c пункта выдачи на курьера меняются тип, адрес и даты доставки, а " +
            "также удаляется пункт выдачи. Текущая задача доставки отменяется по причине переноса.")
    @SneakyThrows
    @Test
    void updateDeliveryTypeFromPickupToCourierOnUpdateOrder() {
        createLockerPickupPoint();

        String externalOrderId = "1";
        CreateOrderRequest request = dsRequestReader.readRequest(
                "/ds/create_order_without_places_with_multiple_items_to_pickup.xml",
                CreateOrderRequest.class,
                YANDEX_ORDER_ID
        );

        createLockerDeliveryOrderWithTask(externalOrderId, request);

        initDsUpdateOrderMocks();

        UpdateOrderRequest updateOrderRequest = dsRequestReader.readRequest(
                "/ds/update_delivery_type_to_courier.xml",
                UpdateOrderRequest.class,
                externalOrderId
        );
        UpdateOrderResponse updateResponse = dsOrderManager.updateOrder(
                withExternalId(updateOrderRequest.getOrder(), externalOrderId),
                partner,
                dsPersonalAddressService.getNewDeliveryAddressFromPersonal(updateOrderRequest),
                dsPersonalAddressService.getOldDeliveryAddressFromPersonal(updateOrderRequest, partner)
        );

        checkOrderUpdated(null, updateResponse, LockerSubtask.class);
    }

    @DisplayName("Адрес заказа не изменяется при обновлении заказа без изменений любого из полей адреса")
    @SneakyThrows
    @Test
    void doNotUpdateAddressIfAddressFieldsDoNotChange() {
        String externalOrderId = "1";
        CreateOrderRequest request = dsRequestReader.readRequest(
                "/ds/create_order_without_places_with_multiple_items.xml", CreateOrderRequest.class, YANDEX_ORDER_ID);

        dsOrderManager.createOrder(withExternalId(
                request.getOrder(), externalOrderId), partner);

        initDsUpdateOrderMocks();

        UpdateOrderRequest updateOrderRequest = dsRequestReader.readRequest(
                "/ds/update_order_address_did_not_change.xml",
                UpdateOrderRequest.class,
                externalOrderId
        );
        UpdateOrderResponse updateResponse = dsOrderManager.updateOrder(
                withExternalId(updateOrderRequest.getOrder(), externalOrderId),
                partner,
                dsPersonalAddressService.getNewDeliveryAddressFromPersonal(updateOrderRequest),
                dsPersonalAddressService.getOldDeliveryAddressFromPersonal(updateOrderRequest, partner)
        );
        transactionTemplate.executeWithoutResult(tt -> {
            Order updated = orderRepository.findById(Long.parseLong(updateResponse.getOrderId().getPartnerId()))
                    .orElseThrow();
            assertThat(updated.getDelivery().getDeliveryAddress()).isEqualToIgnoringNullFields(createAddress());
            assertThat(updated.getDelivery().getRecipientNotes()).isEqualTo(RECIPIENT_NOTES);
        });
    }

    @DisplayName("Адрес заказа не изменяется при обновлении заказа без изменений улицы адреса после нормализации")
    @SneakyThrows
    @Test
    void doNotUpdateAddressIfAddressStreetDoNotChangeAfterNormalization() {
        String externalOrderId = "1";
        CreateOrderRequest request = dsRequestReader.readRequest(
                "/ds/create_order_without_places_with_multiple_items.xml", CreateOrderRequest.class, YANDEX_ORDER_ID);

        dsOrderManager.createOrder(withExternalId(
                request.getOrder(), externalOrderId), partner);

        initDsUpdateOrderMocks();

        UpdateOrderRequest updateOrderRequest = dsRequestReader.readRequest(
                "/ds/update_order_address_did_not_change_after_normalization.xml",
                UpdateOrderRequest.class,
                externalOrderId
        );
        UpdateOrderResponse updateResponse = dsOrderManager.updateOrder(
                withExternalId(updateOrderRequest.getOrder(), externalOrderId),
                partner,
                dsPersonalAddressService.getNewDeliveryAddressFromPersonal(updateOrderRequest),
                dsPersonalAddressService.getOldDeliveryAddressFromPersonal(updateOrderRequest, partner)
        );
        transactionTemplate.execute(tt -> {
            Order updated = orderRepository.findById(Long.parseLong(updateResponse.getOrderId().getPartnerId()))
                    .orElseThrow();
            assertThat(updated.getDelivery().getDeliveryAddress()).isEqualToIgnoringNullFields(createAddress());
            assertThat(updated.getDelivery().getRecipientNotes()).isEqualTo(RECIPIENT_NOTES);
            return null;
        });
    }

    @DisplayName("Комментарий в заказе стирается при передаче пустого в updateOrder")
    @SneakyThrows
    @Test
    void commentBecomesNullAfterUpdate() {
        String externalOrderId = "1";
        CreateOrderRequest request = dsRequestReader.readRequest(
                "/ds/create_order_without_places_with_multiple_items.xml", CreateOrderRequest.class, YANDEX_ORDER_ID);

        dsOrderManager.createOrder(withExternalId(
                request.getOrder(), externalOrderId), partner);

        initDsUpdateOrderMocks();

        UpdateOrderRequest updateOrderRequest = dsRequestReader.readRequest(
                "/ds/update_order_empty_comment.xml",
                UpdateOrderRequest.class,
                externalOrderId
        );
        UpdateOrderResponse updateResponse = dsOrderManager.updateOrder(
                withExternalId(updateOrderRequest.getOrder(), externalOrderId),
                partner,
                dsPersonalAddressService.getNewDeliveryAddressFromPersonal(updateOrderRequest),
                dsPersonalAddressService.getOldDeliveryAddressFromPersonal(updateOrderRequest, partner)
        );
        transactionTemplate.execute(tt -> {
            Order updated = orderRepository.findById(Long.parseLong(updateResponse.getOrderId().getPartnerId()))
                    .orElseThrow();
            assertThat(updated.getDelivery().getRecipientNotes()).isEqualTo(null);
            return null;
        });
    }

    @Test
    void getOrdersStatus() throws Exception {
        CreateOrderResponse response = dsRequestReader.sendCreateOrder(partner);
        String orderId = response.getOrderId().getPartnerId();

        GetOrdersStatusRequest getOrdersStatusRequest = dsRequestReader.readRequest("/ds/get_orders_status.xml",
                GetOrdersStatusRequest.class, YANDEX_ORDER_ID, orderId);
        GetOrdersStatusResponse statuses = dsOrderManager.getOrdersStatus(getOrdersStatusRequest.getOrdersId(),
                partner);
        assertThat(statuses.getOrderStatusHistories()).hasSize(2);

        Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));
        List<OrderStatus> history = statuses.getOrderStatusHistories().get(0).getHistory();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatusCode()).isEqualTo(OrderStatusType.ORDER_CREATED_DS);
        assertThat(history.get(0).getSetDate())
                .isEqualTo(DateTime.fromLocalDateTime(DateTimeUtil.toLocalDateTime(order.getOrderFlowStatusUpdatedAt())));

        assertThat(statuses.getOrderStatusHistories().get(1).getHistory()
                .get(0).getStatusCode()).isEqualTo(OrderStatusType.ORDER_NOT_FOUND);
    }

    @Test
    void getOrderHistory() throws Exception {
        CreateOrderResponse response = dsRequestReader.sendCreateOrder(partner);

        long orderId = Long.parseLong(response.getOrderId().getPartnerId());
        assertThat(dsRequestReader.sendGetOrderHistory(orderId, partner).stream().map(OrderStatus::getStatusCode))
                .containsExactly(OrderStatusType.ORDER_CREATED_DS);

        orderCommandService.updateFlowStatusFromSc(new OrderCommand.UpdateFlowStatus(orderId,
                OrderFlowStatus.SORTING_CENTER_CREATED));
        orderCommandService.updateFlowStatusFromSc(new OrderCommand.UpdateFlowStatus(orderId,
                OrderFlowStatus.SORTING_CENTER_ARRIVED));

        assertThat(dsRequestReader.sendGetOrderHistory(orderId, partner).stream().map(OrderStatus::getStatusCode))
                .containsExactly(
                        OrderStatusType.ORDER_CREATED_DS,
                        OrderStatusType.ORDER_CREATED_DS,
                        OrderStatusType.ORDER_ARRIVED_TO_DELIVERY_SERVICE_WAREHOUSE_DS
                );
    }

    @Test
    void doNotUpdateStatusAfter80Status() throws Exception {
        CreateOrderResponse response = dsRequestReader.sendCreateOrder(partner);

        long orderId = Long.parseLong(response.getOrderId().getPartnerId());

        orderCommandService.updateFlowStatusFromSc(new OrderCommand.UpdateFlowStatus(orderId,
                OrderFlowStatus.SORTING_CENTER_CREATED));
        orderCommandService.updateFlowStatusFromSc(new OrderCommand.UpdateFlowStatus(orderId,
                OrderFlowStatus.SORTING_CENTER_ARRIVED));
        orderCommandService.updateFlowStatusFromSc(new OrderCommand.UpdateFlowStatus(orderId,
                OrderFlowStatus.READY_FOR_RETURN));
        orderCommandService.updateFlowStatusFromSc(new OrderCommand.UpdateFlowStatus(orderId,
                OrderFlowStatus.RETURNED_ORDER_IS_DELIVERED_TO_SENDER));
        orderCommandService.updateFlowStatusFromSc(new OrderCommand.UpdateFlowStatus(orderId,
                OrderFlowStatus.READY_FOR_RETURN));
        orderCommandService.updateFlowStatusFromSc(new OrderCommand.UpdateFlowStatus(orderId,
                OrderFlowStatus.SORTING_CENTER_CREATED));
        assertThat(dsRequestReader.sendGetOrderHistory(orderId, partner).stream().map(OrderStatus::getStatusCode))
                .containsExactly(
                        OrderStatusType.ORDER_CREATED_DS,
                        OrderStatusType.ORDER_CREATED_DS,
                        OrderStatusType.ORDER_ARRIVED_TO_DELIVERY_SERVICE_WAREHOUSE_DS,
                        OrderStatusType.ORDER_IS_READY_FOR_RETURN,
                        OrderStatusType.RETURNED_ORDER_IS_DELIVERED_TO_SENDER
                );
    }

    @Test
    void getOrdersDeliveryDate() throws Exception {
        CreateOrderResponse response = dsRequestReader.sendCreateOrder(partner);
        long orderId = Long.parseLong(response.getOrderId().getPartnerId());

        GetOrdersDeliveryDateRequest request = dsRequestReader.readRequest("/ds/get_orders_delivery_date.xml",
                GetOrdersDeliveryDateRequest.class, YANDEX_ORDER_ID, orderId);
        GetOrdersDeliveryDateResponse ordersDeliveryDate =
                dsOrderManager.getOrdersDeliveryDate(request.getOrdersId(), partner);

        Order order = orderRepository.findByIdOrThrow(orderId);
        assertThat(ordersDeliveryDate.getOrderDeliveryDates()).hasSize(1);
        assertThat(ordersDeliveryDate.getOrderDeliveryDates().get(0).getDeliveryDate())
                .isEqualTo(DateTime.fromLocalDateTime(DateTimeUtil.toLocalDateTime(order.getDelivery().getDeliveryIntervalFrom())));
    }

    @Test
    void updateRecipient() throws IOException {
        CreateOrderResponse response = dsRequestReader.sendCreateOrder(partner);
        long orderId = Long.parseLong(response.getOrderId().getPartnerId());

        UpdateRecipientRequest request = dsRequestReader.readRequest("/ds/update_recipient.xml",
                UpdateRecipientRequest.class, YANDEX_ORDER_ID);
        dsOrderManager.updateRecipient(request.getOrderId(), request.getRecipient(), request.getPersonalRecipient(),
                partner);

        Order order = orderRepository.findByIdOrThrow(orderId);
        assertThat(order.getDelivery().getRecipient()).isEqualTo(RECIPIENT);
        assertThat(order.getDelivery().getRecipientNotes()).isEqualTo(RECIPIENT_NOTES);
        assertThat(order.getDelivery().getPersonalRecipient()).isEqualTo(PERSONAL_RECIPIENT);
        Page<OrderHistoryEvent> pageEvents = orderHistoryEventRepository.findByOrderId(orderId, Pageable.unpaged());
        assertThat(pageEvents).extracting(OrderHistoryEvent::getType).containsOnlyOnce(OrderEventType.RECIPIENT_DATA_CHANGED);
        assertThat(pageEvents).filteredOn(e -> OrderEventType.RECIPIENT_DATA_CHANGED.equals(e.getType()))
                .extracting(OrderHistoryEvent::getSource).containsOnly(Source.DELIVERY);
    }

    @Test
    void updateOrderDeliveryDate_success() throws IOException {
        //given
        CreateOrderResponse response = dsRequestReader.sendCreateOrder(partner);
        long orderId = Long.parseLong(response.getOrderId().getPartnerId());

        LocalDate date = LocalDate.now(clock);
        LocalTimeInterval timeInterval = valueOf("11:00-14:30");
        Interval interval = timeInterval.toInterval(date, DateTimeUtil.DEFAULT_ZONE_ID);
        UpdateOrderDeliveryDateRequest request = dsRequestReader.readRequest("/ds/update_order_delivery_date.xml",
                UpdateOrderDeliveryDateRequest.class, YANDEX_ORDER_ID, date.toString(), timeInterval.toString());

        //Волна маршрутизации еще не стартовала
        LocalDateTime firstWaveTime = LocalDateTime.now(clock).plusHours(1L);
        doReturn(Optional.of(firstWaveTime)).when(mockedRoutingRequestWaveService).getFirstRoutingWaveTime(anyLong(),
                any());

        when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                eq(SortingCenterProperties.IS_RESCHEDULING_LIMITED),
                anyLong()
        )).thenReturn(true);

        //when
        dsOrderManager.updateOrderDeliveryDate(request.getOrderDeliveryDate(), partner);

        //then
        Order order = orderRepository.findByIdOrThrow(orderId);
        assertThat(order.getDelivery().getInterval()).isEqualTo(interval);
    }

    @Test
    void updateOrderDeliveryDate_failure_whenRoutingStarts() throws IOException {
        //given
        CreateOrderResponse response = dsRequestReader.sendCreateOrder(partner);
        long orderId = Long.parseLong(response.getOrderId().getPartnerId());

        LocalDate date = orderRepository.findById(orderId)
                .map(Order::getDelivery)
                .map(OrderDelivery::getDeliveryIntervalTo)
                .map(DateTimeUtil::toLocalDate)
                .orElse(null);

        LocalTimeInterval timeInterval = valueOf("11:00-14:30");

        UpdateOrderDeliveryDateRequest request = dsRequestReader.readRequest("/ds/update_order_delivery_date.xml",
                UpdateOrderDeliveryDateRequest.class, YANDEX_ORDER_ID, date.toString(), timeInterval.toString());

        doReturn(true).when(configurationProviderAdapter)
                .isBooleanEnabled(ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED);

        //Волна маршрутизации уже стартовала
        LocalDateTime firstWaveTime = LocalDateTime.now(clock).minusHours(1L);
        doReturn(Optional.of(firstWaveTime)).when(mockedRoutingRequestWaveService).getFirstRoutingWaveTime(anyLong(),
                any());

        //when
        assertThrows(DsApiException.class,
                () -> dsOrderManager.updateOrderDeliveryDate(request.getOrderDeliveryDate(), partner));
    }

    @Test
    void updateOrderDelivery() throws IOException {
        dsRequestReader.sendCreateOrder(partner);
        UpdateOrderDeliveryRequest request = dsRequestReader.readRequest("/ds/update_order_delivery.xml",
                UpdateOrderDeliveryRequest.class, YANDEX_ORDER_ID);
        assertThatThrownBy(() -> dsOrderManager.updateOrderDelivery(request, partner))
                .isInstanceOf(DsApiException.class)
                .hasMessageContaining("Operation is not supported");
    }

    @Test
    void createOrderWithCargoTypes() throws Exception {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds/create_order_with_cargo_types.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);
        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(request.getOrder(), null), partner);
        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();
        Order order = orderRepository.findById(Long.parseLong(orderId)).orElseThrow();
        assertThat(order.getItems().stream()
                .filter(item -> item.getCargoTypeCodes() != null)
                .flatMap(item -> item.getCargoTypeCodes().stream()).filter(code -> code == 200)
                .count()).isEqualTo(1L);
        assertThat(order.getItems().stream()
                .filter(item -> item.getCargoTypeCodes() != null)
                .flatMap(item -> item.getCargoTypeCodes().stream()).filter(code -> code == 800)
                .count()).isEqualTo(1L);
        assertThat(order.getItems().stream()
                .filter(item -> CollectionUtils.isEmpty(item.getCargoTypeCodes()) && !item.isService())
                .count()).isEqualTo(1L);

        OrderItem orderItem = order.getItems().stream()
                .filter(item -> item.getCargoTypeCodes() != null && item.getCargoTypeCodes().contains(800))
                .findFirst().get();
        //У этого item-а нет неизвестных нам cargoType
        assertThat(orderItem.getCargoTypes().stream().filter(CargoType.UNDEFINED::equals).count()).isEqualTo(0);

        var newCodes = new ArrayList<>(orderItem.getCargoTypeCodes());
        newCodes.add(777);
        newCodes.add(888);
        //Вставляю cargoType, которого нет в нашем enum-е
        jdbcTemplate.update("UPDATE order_item SET cargo_type_codes = string_to_array(?, ',')::integer[]" +
                        " WHERE id = ?",
                newCodes.stream().map(e -> "" + e).collect(Collectors.joining(",")), orderItem.getId());

        entityManager.detach(order);
        order = orderRepository.findById(Long.parseLong(orderId)).orElseThrow();

        orderItem = order.getItems().stream()
                .filter(item -> item.getCargoTypeCodes() != null && item.getCargoTypeCodes().contains(800))
                .findFirst().get();
        //Неизвестные cargoType приходит как UNDEFINED
        assertThat(orderItem.getCargoTypes().stream().filter(CargoType.UNDEFINED::equals).count()).isEqualTo(2);
    }

    @DisplayName("Каждая коробка меньше крупногабаритного размера, определяем тип заказа - не КГТ")
    @Test
    void createOrderWithDimensionsRegularCargo() throws Exception {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds/create_order_with_dimension_regular_cargo.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);

        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(request.getOrder(), null), partner);

        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();
        Order order = orderRepository.findById(Long.parseLong(orderId)).orElseThrow();
        DimensionsClass dimensionsClass = order.getDimensionsClass();
        assertNotNull(dimensionsClass);
        assertEquals(dimensionsClass, REGULAR_CARGO);
    }

    @DisplayName("Одна из коробок больше крупногабаритного размера, определяем тип заказа - КГТ")
    @Test
    void createOrderWithDimensionsBulkyCargo() throws Exception {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds/create_order_with_dimension_bulky_cargo.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);

        CreateOrderResponse response = dsOrderManager.createOrder(withExternalId(request.getOrder(), null), partner);

        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();
        Order order = orderRepository.findById(Long.parseLong(orderId)).orElseThrow();
        DimensionsClass dimensionsClass = order.getDimensionsClass();
        assertNotNull(dimensionsClass);
        assertEquals(dimensionsClass, BULKY_CARGO);
    }

    @Test
    void createOrderWithNotExistedCargoTypes() throws Exception {
        assertThatThrownBy(() -> dsRequestReader.readRequest("/ds/create_order_with_not_existed_cargo_types.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID)).isInstanceOf(ValueInstantiationException.class);
    }

    @DisplayName("Создание заказа b2b клиенту с кодом доставки")
    @Test
    void createOrderToB2bCustomer() throws Exception {
        CreateOrderRequest request = dsRequestReader.readRequest("/ds/b2b/create_order_to_b2b_client.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID);
        ru.yandex.market.logistic.api.model.delivery.Order requestOrder = withExternalId(request.getOrder(), null);

        CreateOrderResponse response = dsOrderManager.createOrder(
                requestOrder,
                request.getRestrictedData(),
                partner
        );

        String orderId = response.getOrderId().getPartnerId();
        assertThat(orderId).isNotNull();
        transactionTemplate.execute(tt -> {
            Order order = orderRepository.findByIdOrThrow(Long.parseLong(orderId));
            assertFalse(order.isPickup());

            Map<String, OrderProperty> properties = order.getProperties();
            OrderProperty customerType = properties.get(TplOrderProperties.Names.CUSTOMER_TYPE.name());
            OrderProperty verificationCode =
                    properties.get(TplOrderProperties.Names.VERIFICATION_CODE_BEFORE_HANDING.name());
            assertThat(customerType.getValue()).isEqualTo(TplOrderProperties.B2B_CUSTOMER_TYPE.getDefaultValue());
            assertThat(verificationCode.getValue()).isEqualTo("11111"); //значение из /ds/create_order_to_b2b_client.xml
            return null;
        });
    }

    @DisplayName("Создание заказа b2b клиенту с кодом доставки, но не пришел код")
    @Test
    void createOrderToB2bCustomerWithoutVerificationCode() throws Exception {
        CreateOrderRequest request = dsRequestReader.readRequest(
                "/ds/b2b/create_order_to_b2b_client_without_code.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID
        );
        ru.yandex.market.logistic.api.model.delivery.Order requestOrder = withExternalId(request.getOrder(), null);

        assertThatThrownBy(
                () -> dsOrderManager.createOrder(requestOrder, request.getRestrictedData(), partner)
        ).isInstanceOf(DsApiException.class);
    }

    @DisplayName("Создание заказа b2b клиенту с кодом доставки и кодом, но он не олачен")
    @Test
    void createOrderToB2bCustomerNotPrepaid() throws Exception {
        CreateOrderRequest request = dsRequestReader.readRequest(
                "/ds/b2b/create_order_to_b2b_client_not_prepaid.xml",
                CreateOrderRequest.class, YANDEX_ORDER_ID
        );
        ru.yandex.market.logistic.api.model.delivery.Order requestOrder = withExternalId(request.getOrder(), null);

        assertThatThrownBy(
                () -> dsOrderManager.createOrder(requestOrder, request.getRestrictedData(), partner)
        ).isInstanceOf(DsApiException.class);
    }

    @SneakyThrows
    private ru.yandex.market.logistic.api.model.delivery.Order withExternalId(
            ru.yandex.market.logistic.api.model.delivery.Order order, String externalOrderId
    ) {
        if (externalOrderId != null) {
            TestUtil.setPrivateFinalField(order, "orderId",
                    new ResourceId(externalOrderId, null, null));
        }
        return order;
    }

    private void checkOrderItemsInstancesCount(Order order) {
        order.getItems().forEach(
                i -> {
                    if (i.isService()) {
                        assertThat(i.streamInstances().count()).isEqualTo(0);
                    } else {
                        assertThat(i.getCount()).isEqualTo(i.streamInstances().count());
                    }
                }
        );
    }

    private DeliveryAddress createAddress() {
        return DeliveryAddress.builder()
                .country("Россия")
                .federalDistrict("Центральный федеральный округ")
                .region("Москва и Московская область")
                .address("Смоленский бульвар, д. 1, стр. 3, к. 1, кв. 2")
                .city("Москва")
                .street("Смоленский бульвар")
                .house("1")
                .building("3")
                .housing("1")
                .apartment("2")
                .regionId(-1)
                .originalRegionId(213L)
                .latitude(BigDecimal.valueOf(55.741794))
                .longitude(BigDecimal.valueOf(37.582538))
                .zipCode("123056")
                .addressPersonalId("456")
                .gpsPersonalId("654")
                .build();
    }

    private void createLockerDeliveryOrderWithTask(String externalOrderId, CreateOrderRequest request) {
        transactionTemplate.executeWithoutResult(tt -> {
            CreateOrderResponse createResponse = dsOrderManager.createOrder(withExternalId(
                    request.getOrder(), externalOrderId), partner);
            Order created = orderRepository.findById(Long.parseLong(createResponse.getOrderId().getPartnerId()))
                    .orElseThrow();

            User user = testUserHelper.findOrCreateUser(1L);
            Shift shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
            long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
            RoutePoint routePoint = testDataFactory.createEmptyRoutePoint(user, userShiftId, Instant.now(),
                    Instant.now());
            testUserHelper.addLockerDeliveryTaskToShift(user, routePoint.getUserShift(), created);
        });
    }

    private PickupPoint createLockerPickupPoint() {
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 100012345L, 1L)
        );
        clearAfterTest(pickupPoint);
        return pickupPoint;
    }

    private PickupPoint createPvzPickupPoint() {
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1000100500L, 1L)
        );
        clearAfterTest(pickupPoint);
        return pickupPoint;
    }
}
