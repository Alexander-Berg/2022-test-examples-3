package ru.yandex.market.tpl.core.service.tracking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.AbstractComparableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import yandex.market.combinator.v0.CombinatorGrpc;
import yandex.market.combinator.v0.CombinatorOuterClass;
import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.CallRequirement;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.OrderTagsDto;
import ru.yandex.market.tpl.api.model.order.OrderType;
import ru.yandex.market.tpl.api.model.order.TransferType;
import ru.yandex.market.tpl.api.model.order.VatType;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.partner.OrderEventType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.task.LeavingAtReceptionStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.tracking.DeliveryConfirmationStatus;
import ru.yandex.market.tpl.api.model.tracking.DeliveryDto;
import ru.yandex.market.tpl.api.model.tracking.FlashMessageType;
import ru.yandex.market.tpl.api.model.tracking.TrackingCancelOrderDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingClarifyAddressDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus;
import ru.yandex.market.tpl.api.model.tracking.TrackingDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingIntervalDisplayMode;
import ru.yandex.market.tpl.api.model.tracking.TrackingRescheduleDto;
import ru.yandex.market.tpl.api.model.user.UserMode;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.personal.client.model.CommonType;
import ru.yandex.market.tpl.common.personal.client.model.CommonTypeEnum;
import ru.yandex.market.tpl.common.personal.client.model.GpsCoord;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeStoreResponseItem;
import ru.yandex.market.tpl.common.personal.client.tpl.PersonalExternalService;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.CargoType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEvent;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.OrderItem;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.Photo;
import ru.yandex.market.tpl.core.domain.order.PhotoRepository;
import ru.yandex.market.tpl.core.domain.order.address.Address;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.address.AddressQueryService;
import ru.yandex.market.tpl.core.domain.order.address.AddressString;
import ru.yandex.market.tpl.core.domain.order.address.DeliveryAddress;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.push.notification.PushNotificationRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.tvm.service.ServiceTicketRequest;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.location.Reception;
import ru.yandex.market.tpl.core.domain.usershift.location.ReceptionRepository;
import ru.yandex.market.tpl.core.domain.usershift.tracking.Tracking;
import ru.yandex.market.tpl.core.domain.usershift.tracking.TrackingRepository;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.service.order.address.PersonalServiceDataMapper;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.service.usershift.additionaldata.vehicle.AdditionalVehicleDataService;
import ru.yandex.market.tpl.core.service.usershift.additionaldata.vehicle.UpdateAdditionalVehicleDataDto;
import ru.yandex.market.tpl.core.service.vehicle.VehicleGenerateService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.shift.UserShiftStatus.SHIFT_CREATED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType.CLIENT_REQUEST;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType.DELIVERY_DELAY;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType.ORDER_TYPE_UPDATED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.CANNOT_PAY;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.CLIENT_REFUSED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.NO_CONTACT;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED;
import static ru.yandex.market.tpl.api.model.tracking.PreviouslyConfirmationDeliveryStatus.NO_CONFIRMATION_NEEDED;
import static ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus.CANCELLED_AFTER_NO_CONTACT;
import static ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus.DELIVERED;
import static ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus.DELIVERED_TO_RECEPTION;
import static ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus.DELIVERED_TO_THE_DOOR;
import static ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus.IN_PREPARATION;
import static ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus.IN_PROGRESS;
import static ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus.NOT_DELIVERED;
import static ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus.RESCHEDULED;
import static ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus.RESCHEDULED_AFTER_DELIVERY_METHOD_CHANGE;
import static ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus.RESCHEDULED_AFTER_NO_CONTACT;
import static ru.yandex.market.tpl.api.model.tracking.TrackingOrderCancelReason.FOUND_CHEAPER;
import static ru.yandex.market.tpl.api.model.user.UserMode.SOFT_MODE;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAt;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAtHour;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.tomorrowAtHour;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DELIVERY_CONFIRMATION_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DO_NOT_CALL_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.IS_FLASH_MESSAGE_AVAILABLE;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.NEAR_THE_DOOR_CONTAINS_CHECK_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SHOW_DELIVERY_METHOD_CHANGE_AS_RESCHEDULE_REASON_IN_TRACKING_SERVICE;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.TRACKING_DELIVERY_STATUS_UPDATED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.UPDATE_ADDRESS_PERSONAL_WHERE_COURIER_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED;
import static ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleInstanceType.PERSONAL;
import static ru.yandex.market.tpl.core.service.tracking.TrackingService.AVAILABLE_INTERVAL_TO_CLARIFY_ADDRESS;
import static ru.yandex.market.tpl.core.service.tracking.TrackingService.CONTACTLESS_DELIVERY_PREFIX;
import static ru.yandex.market.tpl.core.test.TestDataFactory.DELIVERY_SERVICE_ID;

/**
 * @author aostrikov
 */
@RequiredArgsConstructor
class TrackingServiceTest extends TplAbstractTest {

    private static final String NEAR_THE_DOOR_TEXT = "Оставить у двери";
    private static final String DO_NOT_CALL_TEXT = "Не звонить.";

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper dataHelper;
    private final UserShiftCommandDataHelper helper;

    private final TrackingRepository trackingRepository;
    private final PushNotificationRepository pushNotificationRepository;
    private final TrackingService trackingService;
    private final ReceptionRepository receptionRepository;
    private final OrderHistoryEventRepository orderHistoryEventRepository;

    private final OrderRepository orderRepository;
    private final UserShiftCommandService commandService;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftQueryService userShiftQueryService;
    private final UserShiftRepository repository;
    private final UserPropertyService userPropertyService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final PhotoRepository photoRepository;
    private final PickupPointRepository pickupPointRepository;
    private final VehicleGenerateService vehicleGenerateService;
    private final AdditionalVehicleDataService vehicleDataService;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final CombinatorGrpc.CombinatorBlockingStub combinatorBlockingStub;
    private final DbQueueTestUtil dbQueueTestUtil;

    private final TestDataFactory testDataFactory;

    @MockBean
    private AddressQueryService addressQueryService;

    @SpyBean
    private OrderCommandService orderCommandService;

    @MockBean
    private PersonalExternalService personalExternalService;

    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    private Order orderWithFixedAddress;

    private User user;
    private Integer newFloor;
    private Integer newEntrance;
    private Integer newApartment;
    private String newEntryPhone;
    private final ServiceTicketRequest serviceTicket = new ServiceTicketRequest();

    @BeforeEach
    void init() {
        ClockUtil.initFixed(getClock());
        when(configurationProviderAdapter.isBooleanEnabled(TRACKING_DELIVERY_STATUS_UPDATED)).thenReturn(true);
        when(configurationProviderAdapter.isBooleanEnabled(UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED))
                .thenReturn(true);
        user = userHelper.findOrCreateUser(999777L, LocalDate.now(getClock()));
        orderWithFixedAddress = newOrder();
        orderWithFixedAddress.setPaymentType(OrderPaymentType.CASH);
        orderWithFixedAddress.setPaymentStatus(OrderPaymentStatus.UNPAID);
        DeliveryAddress oldAddress = orderWithFixedAddress.getDelivery().getDeliveryAddress();
        newFloor = Integer.parseInt(oldAddress.getFloor()) + 1;
        newEntrance = Integer.parseInt(oldAddress.getEntrance()) + 1;
        newApartment = Integer.parseInt(oldAddress.getApartment()) + 1;
        newEntryPhone = oldAddress.getEntryPhone() + "1";
        var newAddress = new Address(
                AddressString.builder()
                        .country(oldAddress.getCountry())
                        .region(oldAddress.getRegion())
                        .federalDistrict(oldAddress.getFederalDistrict())
                        .subRegion(oldAddress.getSubRegion())
                        .city(oldAddress.getCity())
                        .settlement(oldAddress.getSettlement())
                        .street(oldAddress.getStreet())
                        .house(oldAddress.getHouse())
                        .building(oldAddress.getBuilding())
                        .housing(oldAddress.getHousing())
                        .floor(newFloor.toString())
                        .entrance(newEntrance.toString())
                        .apartment(newApartment.toString())
                        .entryPhone(newEntryPhone)
                        .zipCode(oldAddress.getZipCode())
                        .metro(oldAddress.getMetro())
                        .build(),
                GeoPoint.ofLatLon(oldAddress.getLatitude(), oldAddress.getLongitude()),
                oldAddress.getRegionId()
        );
        when(addressQueryService.queryByAddressString(any())).thenReturn(Optional.of(newAddress));
    }

    @Test
    @Transactional
    void showInPreparationOnCreatedShift() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        Order order = newOrder();
        order.getDelivery().setRecipientNotes(DO_NOT_CALL_TEXT);
        userHelper.createShiftWithDeliveryTask(user, SHIFT_CREATED,
                userHelper.findOrCreateOpenShift(order.getDelivery().getDeliveryDate(getClock().getZone())), order, null);

        TrackingDto tracking = trackingDto(order);
        assertDeliveryStatus(tracking).isEqualTo(IN_PREPARATION);
        assertThat(tracking.getDelivery().getCallRequirement()).isEqualTo(CallRequirement.DO_NOT_CALL);
    }

    @Test
    @Transactional
    void showTransportationRecipientBeforeCallAndInProgressOnOpenedShiftForUserWithCallEnabled() {
        userPropertyService.addPropertyToUser(user, UserProperties.CALL_TO_RECIPIENT_ENABLED, true);
        userPropertyService.addPropertyToUser(user, UserProperties.TRACKING_SHOW_DELIVERIES_LEFT, true);
        Order order = newOrder();
        UserShift userShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(userShift, true);
        TrackingDto tracking = trackingDto(order);
        //IN_PROGRESS -- так как времени меньше двух часов до заказа
        assertDeliveryStatus(tracking).isEqualTo(IN_PROGRESS);
        successAttemptCallOnCurrentRoutePoint(userShift);
        tracking = trackingDto(order);
        assertDeliveryStatus(tracking).isEqualTo(IN_PROGRESS);
        assertThat(tracking.getCourier().getDeliveriesLeft()).isEqualTo(0);
    }

    @Test
    void showInProgressForUserWithoutCallOnOpenedShift() {
        Order order = newOrder();
        UserShift userShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(userShift, true);
        TrackingDto tracking = trackingDto(order);
        assertDeliveryStatus(tracking).isEqualTo(IN_PROGRESS);
        assertThat(tracking.getCourier().getDeliveriesLeft()).isNull();
    }

    @Test
    @Transactional
    void shouldShowContactlessDeliveryNotice() {
        Order order = newOrder();
        order.getItems().clear();
        order.getItems().add(newOrderItem(order, 15_000));
        order.setPaymentType(OrderPaymentType.PREPAID);
        order.getDelivery().setRecipientNotes("И привезите заказ вовремя!");

        UserShift openedShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(openedShift);

        assertThat(trackingDto(order).getDelivery().isShowContactlessDeliveryNotice()).isTrue();
    }

    @Test
    void shouldPreparingBeforePickupFinish() {
        Order order = newOrder();
        order.setPaymentType(OrderPaymentType.PREPAID);
        order.getDelivery().setRecipientNotes("И привезите заказ вовремя!");

        UserShift openedShift = userHelper.createOpenedShift(user, order,
                order.getDelivery().getDeliveryDate(getClock().getZone()));

        assertDeliveryStatus(trackingDto(order)).isEqualTo(IN_PREPARATION);

        userHelper.finishPickupAtStartOfTheDay(openedShift);

        assertDeliveryStatus(trackingDto(order)).isEqualTo(IN_PROGRESS);
    }

    @Test
    @Transactional
    void shouldLeftOrderNearTheDoor() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);
        when(configurationProviderAdapter.isBooleanEnabled(NEAR_THE_DOOR_CONTAINS_CHECK_ENABLED)).thenReturn(true);

        Order order = newOrder();
        order.getItems().clear();
        order.getItems().add(newOrderItem(order, 15_000));
        order.setPaymentType(OrderPaymentType.PREPAID);
        order.getDelivery().setRecipientNotes(DO_NOT_CALL_TEXT + NEAR_THE_DOOR_TEXT);

        UserShift openedShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(openedShift);

        assertThat(trackingDto(order).getDelivery().getTransferType()).isEqualTo(TransferType.NEAR_THE_DOOR);
        assertThat(trackingDto(order).getDelivery().getCallRequirement()).isEqualTo(CallRequirement.DO_NOT_CALL);
    }

    @Test
    @Transactional
    void shouldHaveDoNotCall() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        Order order = newOrder();
        order.getItems().clear();
        order.getItems().add(newOrderItem(order, 15_000));
        order.setPaymentType(OrderPaymentType.PREPAID);
        order.getDelivery().setRecipientNotes(DO_NOT_CALL_TEXT);

        UserShift openedShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(openedShift);

        assertThat(trackingDto(order).getDelivery().getCallRequirement()).isEqualTo(CallRequirement.DO_NOT_CALL);
    }

    @Test
    void shouldNotHaveDoNotCall() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(false);

        Order order = newOrder();
        order.getItems().clear();
        order.getItems().add(newOrderItem(order, 15_000));
        order.setPaymentType(OrderPaymentType.PREPAID);
        order.getDelivery().setRecipientNotes(DO_NOT_CALL_TEXT);

        UserShift openedShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(openedShift);

        assertThat(trackingDto(order).getDelivery().getCallRequirement()).isEqualTo(null);
    }

    @Test
    @Transactional
    void shouldLeftOrderNearTheDoorAndDoNotCallWhenMultiOrder() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        UserShift userShift = userHelper.createEmptyShift(user, LocalDate.now(getClock()));

        configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                OrderGenerateService.DEFAULT_PHONE);
        configurationServiceAdapter.insertValue(ConfigurationProperties.SEND_SMS_WHEN_POSTPONE_ORDER_ENABLED, true);

        AddressGenerator.AddressGenerateParam addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(GeoPointGenerator.generateLonLat())
                .build();

        Order multiOrder1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("451234")
                .deliveryDate(LocalDate.now(getClock()))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .itemsPrice(BigDecimal.valueOf(5000.0))
                        .build()
                )
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .paymentType(OrderPaymentType.PREPAID)
                .recipientNotes(NEAR_THE_DOOR_TEXT)
                .build());

        Order multiOrder2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("4321231")
                .deliveryDate(LocalDate.now(getClock()))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .itemsPrice(BigDecimal.valueOf(3000.0))
                        .build()
                )
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .paymentType(OrderPaymentType.PREPAID)
                .recipientNotes("Консьержка." + DO_NOT_CALL_TEXT)
                .build());
        userShiftReassignManager.assign(userShift, multiOrder1);
        userShiftReassignManager.assign(userShift, multiOrder2);
        userHelper.checkinAndFinishPickup(userShift);
        assertThat(trackingDto(multiOrder1).getDelivery().getTransferType()).isEqualTo(TransferType.NEAR_THE_DOOR);
        assertThat(trackingDto(multiOrder2).getDelivery().getCallRequirement()).isEqualTo(CallRequirement.DO_NOT_CALL);
    }

    @Test
    void shouldDeliverOrderHandToHand() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        Order order = newOrder();
        order.getItems().clear();
        order.getItems().add(newOrderItem(order, 15_000));
        order.setPaymentType(OrderPaymentType.PREPAID);
        order.getDelivery().setRecipientNotes("Консьержка");

        UserShift openedShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(openedShift);

        DeliveryDto delivery = trackingDto(order).getDelivery();
        assertThat(delivery.getTransferType()).isEqualTo(TransferType.HAND_TO_HAND);
        assertThat(delivery.getCallRequirement()).isEqualTo(CallRequirement.CALL_REQUIRED);
    }

    @Test
    @Transactional
    void shouldDeliverOrderHandToHandWhenPaymentTypeNotPrepaidAndAskLeftNearTheDoor() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        Order order = newOrder();
        order.getItems().clear();
        order.getItems().add(newOrderItem(order, 15_000));
        order.setPaymentType(OrderPaymentType.CARD);
        order.getDelivery().setRecipientNotes(NEAR_THE_DOOR_TEXT + DO_NOT_CALL_TEXT);

        UserShift openedShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(openedShift);

        DeliveryDto delivery = trackingDto(order).getDelivery();
        assertThat(delivery.getTransferType()).isEqualTo(TransferType.HAND_TO_HAND);
        assertThat(delivery.getCallRequirement()).isEqualTo(CallRequirement.DO_NOT_CALL);
    }

    @Test
    void shouldDeliverOrderHandToHandWhenPaymentTypeNotPrepaidAndDoNotAskLeftNearTheDoor() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        Order order = newOrder();
        order.getItems().clear();
        order.getItems().add(newOrderItem(order, 15_000));
        order.setPaymentType(OrderPaymentType.CARD);
        order.getDelivery().setRecipientNotes("Консьержка");

        UserShift openedShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(openedShift);

        DeliveryDto delivery = trackingDto(order).getDelivery();
        assertThat(delivery.getTransferType()).isEqualTo(TransferType.HAND_TO_HAND);
        assertThat(delivery.getCallRequirement()).isEqualTo(CallRequirement.CALL_REQUIRED);
    }

    @Test
    void shouldDeliverOrderHandToHandWhenCostMaxThenLimit() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        Order order = newOrder();
        order.getItems().clear();
        order.getItems().add(newOrderItem(order,
                TrackingService.CONTACTLESS_DELIVERY_MAX_AMOUNT.intValue() + 5_000));
        order.setPaymentType(OrderPaymentType.CARD);
        order.getDelivery().setRecipientNotes(NEAR_THE_DOOR_TEXT);

        UserShift openedShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(openedShift);

        DeliveryDto delivery = trackingDto(order).getDelivery();
        assertThat(delivery.getTransferType()).isEqualTo(TransferType.HAND_TO_HAND);
        assertThat(delivery.getCallRequirement()).isEqualTo(CallRequirement.CALL_REQUIRED);
    }

    @Test
    void shouldDeliverOrderHandToHandWhenSeveralItemsAndOneCostMaxThenLimit() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        Order order = newOrder();
        order.getItems().clear();
        order.getItems().add(newOrderItem(order, 1_000));
        order.getItems().add(newOrderItem(order,
                TrackingService.CONTACTLESS_DELIVERY_MAX_AMOUNT.intValue() + 5_000));
        order.setPaymentType(OrderPaymentType.CARD);
        order.getDelivery().setRecipientNotes(NEAR_THE_DOOR_TEXT);

        UserShift openedShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(openedShift);

        DeliveryDto delivery = trackingDto(order).getDelivery();
        assertThat(delivery.getTransferType()).isEqualTo(TransferType.HAND_TO_HAND);
        assertThat(delivery.getCallRequirement()).isEqualTo(CallRequirement.CALL_REQUIRED);
    }

    @Test
    void shouldDeliverOrderHandToHandWhenR18() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        Order order = newOrder();
        order.getItems().clear();
        order.getItems().add(newR18OrderItem(order, 1_000));
        order.setPaymentType(OrderPaymentType.PREPAID);
        order.getDelivery().setRecipientNotes(NEAR_THE_DOOR_TEXT);

        UserShift openedShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(openedShift);

        DeliveryDto delivery = trackingDto(order).getDelivery();
        assertThat(delivery.getTransferType()).isEqualTo(TransferType.HAND_TO_HAND);
        assertThat(delivery.getCallRequirement()).isEqualTo(CallRequirement.CALL_REQUIRED);
    }

    @Test
    void shouldSkipContactlessDeliveryNoticeBecauseOfComment() {
        Order order = newOrder();
        order.getItems().clear();
        order.getItems().add(newOrderItem(order, 15_000));
        order.setPaymentType(OrderPaymentType.PREPAID);
        order.getDelivery().setRecipientNotes(CONTACTLESS_DELIVERY_PREFIX + " И привезите вовремя!");

        UserShift openedShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(openedShift);

        assertThat(trackingDto(order).getDelivery().isShowContactlessDeliveryNotice()).isFalse();
    }

    @Test
    @Transactional
    void shouldChangePaymentType() {
        Order order = newOrder();
        order.getItems().clear();
        order.getItems().add(newOrderItem(order, 33_000));
        order.setPaymentType(OrderPaymentType.CASH);
        order.setPaymentStatus(OrderPaymentStatus.UNPAID);

        UserShift openedShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(openedShift);

        assertThat(pushNotificationRepository.findAll()
                .stream().filter(e -> e.getBody().contains(order.getExternalOrderId()))
                .count()).isEqualTo(0);
        trackingService.changePaymentTypeToCard(tracking(order).getId(), serviceTicket);
        assertThat(order.getPaymentType()).isEqualTo(OrderPaymentType.CARD);
        assertThat(orderHistoryEventRepository.findAllByOrderId(order.getId()).stream()
                .filter(o -> o.getSource() == Source.CLIENT).findFirst().get().getContext())
                .isEqualTo("Изменение типа оплаты на CARD");
        assertThat(pushNotificationRepository.findAll()
                .stream()
                .filter(e -> e.getBody().contains(order.getExternalOrderId()))
                .count()
        ).isEqualTo(1);
    }

    @Test
    @Transactional
    void shouldShowMessageChangePaymentTypeIfOrderIsNotPrepaidAndPaymentTypeIsCashOrUnknown() {
        when(configurationProviderAdapter.isBooleanEnabled(IS_FLASH_MESSAGE_AVAILABLE)).thenReturn(true);

        Order orderWithPaymentTypeCash = newOrder();
        orderWithPaymentTypeCash.setPaymentType(OrderPaymentType.CASH);
        orderWithPaymentTypeCash.setPaymentStatus(OrderPaymentStatus.UNPAID);

        Order orderWithPaymentTypeCard = newOrder();
        orderWithPaymentTypeCard.setPaymentType(OrderPaymentType.CARD);
        orderWithPaymentTypeCard.setPaymentStatus(OrderPaymentStatus.UNPAID);

        Order orderWithPaymentTypeUnknown = newOrder();
        orderWithPaymentTypeUnknown.setPaymentType(OrderPaymentType.UNKNOWN);
        orderWithPaymentTypeUnknown.setPaymentStatus(OrderPaymentStatus.UNPAID);

        Order orderPaidWithPaymentTypeUnknown = newOrder();
        orderPaidWithPaymentTypeUnknown.setPaymentType(OrderPaymentType.CASH);
        orderPaidWithPaymentTypeUnknown.setPaymentStatus(OrderPaymentStatus.PAID);

        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(getClock()));

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(getClock().instant()))
                .routePoint(helper.taskPrepaid("addr1", 10, orderWithPaymentTypeCash.getId()))
                .routePoint(helper.taskPrepaid("addr2", 22, orderWithPaymentTypeCard.getId()))
                .routePoint(helper.taskPrepaid("addr3", 22, orderWithPaymentTypeUnknown.getId()))
                .routePoint(helper.taskPrepaid("addr4", 22, orderPaidWithPaymentTypeUnknown.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        UserShift userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        userHelper.checkin(userShift);
        userHelper.checkinAndFinishPickup(userShift);

        TrackingDto dto = trackingService.getTrackingDto(tracking(orderWithPaymentTypeCash).getId());
        assertThat(dto.getAvailableFlashMessages()).contains(FlashMessageType.CHANGE_PAY_TYPE);

        dto = trackingService.getTrackingDto(tracking(orderWithPaymentTypeCard).getId());
        assertThat(dto.getAvailableFlashMessages()).doesNotContain(FlashMessageType.CHANGE_PAY_TYPE);

        dto = trackingService.getTrackingDto(tracking(orderWithPaymentTypeUnknown).getId());
        assertThat(dto.getAvailableFlashMessages()).contains(FlashMessageType.CHANGE_PAY_TYPE);

        assertThatThrownBy(() -> trackingService.changePaymentTypeToCard(tracking(orderPaidWithPaymentTypeUnknown)
                .getId(), serviceTicket))
                .isInstanceOf(TplInvalidParameterException.class);

    }

    @Test
    @Transactional
    void shouldHaveUniquePhotos() {
        UserShift userShift = userHelper.createEmptyShift(user, LocalDate.now(getClock()));

        configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                OrderGenerateService.DEFAULT_PHONE);
        configurationServiceAdapter.insertValue(ConfigurationProperties.SEND_SMS_WHEN_POSTPONE_ORDER_ENABLED, true);

        AddressGenerator.AddressGenerateParam addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(GeoPointGenerator.generateLonLat())
                .build();

        Order multiOrder1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("451234")
                .deliveryDate(LocalDate.now(getClock()))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .itemsPrice(BigDecimal.valueOf(5000.0))
                        .build()
                )
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .paymentType(OrderPaymentType.PREPAID)
                .recipientNotes(NEAR_THE_DOOR_TEXT)
                .build());

        Order multiOrder2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("4321231")
                .deliveryDate(LocalDate.now(getClock()))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .itemsPrice(BigDecimal.valueOf(3000.0))
                        .build()
                )
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .paymentType(OrderPaymentType.PREPAID)
                .recipientNotes("Консьержка.")
                .build());

        userShiftReassignManager.assign(userShift, multiOrder1);
        userShiftReassignManager.assign(userShift, multiOrder2);
        userHelper.checkinAndFinishPickup(userShift);

        List<Photo> photoList = userShift.streamOrderDeliveryTasks()
                .map(task -> List.of(
                        Photo.builder().taskId(task.getId()).photoUrl("photo1").build(),
                        Photo.builder().taskId(task.getId()).photoUrl("photo2").build()
                ))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        photoRepository.saveAll(photoList);

        TrackingDto dto = trackingService.getTrackingDto(tracking(multiOrder1).getId(), true);
        assertThat(dto.getPhotos()).hasSize(2)
                .extracting("url").containsOnly("photo1", "photo2");
    }

    @Test
    @Transactional
    void shouldHaveFashionOrder() {
        UserShift userShift = userHelper.createEmptyShift(user, LocalDate.now(getClock()));

        configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                OrderGenerateService.DEFAULT_PHONE);
        configurationServiceAdapter.insertValue(ConfigurationProperties.SEND_SMS_WHEN_POSTPONE_ORDER_ENABLED, true);

        AddressGenerator.AddressGenerateParam addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(GeoPointGenerator.generateLonLat())
                .street("Колотушкина")
                .house("1")
                .build();

        var fashionOrder =
                orderGenerateService.createOrder(
                        OrderGenerateService.OrderGenerateParam.builder()
                                .externalOrderId("1321231")
                                .deliveryDate(LocalDate.now(getClock()))
                                .deliveryServiceId(239L)
                                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                                .addressGenerateParam(addressGenerateParam)
                                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                                .paymentType(OrderPaymentType.PREPAID)
                                .recipientNotes("Консьержка.")
                                .items(
                                        OrderGenerateService.OrderGenerateParam.Items.builder()
                                                .isFashion(true)
                                                .build()
                                )
                                .build());

        var nonFashionOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("4321231")
                .deliveryDate(LocalDate.now(getClock()))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .paymentType(OrderPaymentType.PREPAID)
                .recipientNotes("Консьержка.")
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .isFashion(false)
                        .itemsPrice(BigDecimal.valueOf(3000.0))
                        .build()
                )
                .build());

        userShiftReassignManager.assign(userShift, fashionOrder);
        userShiftReassignManager.assign(userShift, nonFashionOrder);
        userHelper.checkinAndFinishPickup(userShift);


        TrackingDto dto = trackingService.getTrackingDto(tracking(fashionOrder).getId(), false);
        assertThat(dto.getOrders()).hasSize(2);
        assertThat(dto.getCourier().getCourierVehicle()).isNull();
        var fashionOrders =
                dto.getOrders().stream()
                        .filter(orderDto -> orderDto.getId().equals(fashionOrder.getExternalOrderId()))
                        .collect(Collectors.toList());
        assertThat(fashionOrders).hasSize(1);
        assertThat(fashionOrders.get(0).getTags()).contains(OrderTagsDto.FASHION, OrderTagsDto.PARTIAL_RETURN);

        var nonFashionOrders =
                dto.getOrders().stream()
                        .filter(orderDto -> orderDto.getId().equals(nonFashionOrder.getExternalOrderId()))
                        .collect(Collectors.toList());
        assertThat(nonFashionOrders).hasSize(1);
        assertThat(nonFashionOrders.get(0).getTags()).isEmpty();

    }

    @ParameterizedTest
    @MethodSource("vehicleRegistrationNumbers")
    @Transactional
    void shouldHaveVehicleInfo(String registrationNumber, String registrationRegion, String expected) {
        UserShift userShift = userHelper.createEmptyShift(user, LocalDate.now(getClock()));

        var vehicle = vehicleGenerateService.generateVehicle();
        var vehicleColor = vehicleGenerateService.generateVehicleColor("White");
        var vehicleInstance =
                vehicleGenerateService.assignVehicleToUser(VehicleGenerateService.VehicleInstanceGenerateParam.builder()
                        .type(PERSONAL)
                        .infoUpdatedAt(Instant.now())
                        .users(List.of(user))
                        .color(vehicleColor)
                        .vehicle(vehicle)
                        .registrationNumber(registrationNumber)
                        .registrationNumberRegion(registrationRegion)
                        .build());

        var vehicleDataDto = UpdateAdditionalVehicleDataDto.builder()
                .userShiftId(userShift.getId())
                .vehicleDataDto(UpdateAdditionalVehicleDataDto.UpdateVehicleDataDto.builder()
                        .vehicleInstanceId(vehicleInstance.getId())
                        .vehicle(vehicle)
                        .registrationNumber(registrationNumber)
                        .registrationNumberRegion(registrationRegion)
                        .vehicleColor(vehicleColor)
                        .build())
                .build();
        vehicleDataService.updateAdditonalData(vehicleDataDto);

        AddressGenerator.AddressGenerateParam addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(GeoPointGenerator.generateLonLat())
                .build();

        var fashionOrder =
                orderGenerateService.createOrder(
                        OrderGenerateService.OrderGenerateParam.builder()
                                .externalOrderId("1321231")
                                .deliveryDate(LocalDate.now(getClock()))
                                .deliveryServiceId(239L)
                                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                                .addressGenerateParam(addressGenerateParam)
                                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                                .paymentType(OrderPaymentType.PREPAID)
                                .recipientNotes("Консьержка.")
                                .items(
                                        OrderGenerateService.OrderGenerateParam.Items.builder()
                                                .isFashion(true)
                                                .build()
                                )
                                .build());


        userShiftReassignManager.assign(userShift, fashionOrder);
        userHelper.checkinAndFinishPickup(userShift);


        TrackingDto dto = trackingService.getTrackingDto(tracking(fashionOrder).getId(), false);
        assertThat(dto.getCourier().getCourierVehicle()).isNotNull();
        assertThat(dto.getCourier().getCourierVehicle().getBrand()).isEqualTo(vehicle.getVehicleBrand().getName());
        assertThat(dto.getCourier().getCourierVehicle().getName()).isEqualTo(vehicle.getName());
        assertThat(dto.getCourier().getCourierVehicle().getColor()).isEqualTo(vehicleColor.getName());
        assertThat(dto.getCourier().getCourierVehicle().getRegistrationNumber()).isEqualTo(expected);
    }

    public static Stream<Arguments> vehicleRegistrationNumbers() {
        return Stream.of(
                Arguments.of(
                        "A000MP", "777", "A 000 MP 777"
                ),
                Arguments.of(
                        "0716PK02", "", "0716 PK 02"
                ),
                Arguments.of(
                        "BY8888", "AT-4", "BY 8888 AT-4"
                )
        );
    }


    @Test
    void shouldThrowExceptionDuringChangePaymentTypeInPrepaidOrder() {
        Order order = newOrder();
        order.getItems().clear();
        order.getItems().add(newOrderItem(order, 33_000));
        order.setPaymentType(OrderPaymentType.PREPAID);

        userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        assertThatThrownBy(() -> trackingService.changePaymentTypeToCard(tracking(order).getId(), serviceTicket))
                .isInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void shouldClarifyAddressOrder() {
        Order order = orderWithFixedAddress;
        String newComment = CONTACTLESS_DELIVERY_PREFIX;
        TrackingClarifyAddressDto dto = new TrackingClarifyAddressDto(newEntrance.toString(), newFloor.toString(),
                newApartment.toString(), newEntryPhone, newComment);
        UserShift openedShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(openedShift);
        var oldDeliveryAddress = order.getDelivery().getDeliveryAddress();
        trackingService.clarifyAddressOrder(tracking(order).getId(), dto, null);

        order = orderRepository.findById(order.getId()).get();
        Tracking tracking = tracking(order);
        DeliveryAddress address = order.getDelivery().getDeliveryAddress();
        assertThat(address.getCountry()).isEqualTo(oldDeliveryAddress.getCountry());
        assertThat(address.getRegion()).isEqualTo(oldDeliveryAddress.getRegion());
        assertThat(address.getCity()).isEqualTo(oldDeliveryAddress.getCity());
        assertThat(address.getEntrance()).isEqualTo(newEntrance.toString());
        assertThat(address.getFloor()).isEqualTo(newFloor.toString());
        assertThat(address.getEntryPhone()).isEqualTo(newEntryPhone);
        assertThat(address.getApartment()).isEqualTo(newApartment.toString());
        assertThat(order.getDelivery().getRecipientNotes()).isEqualTo(newComment);
        assertThat(tracking.getDeliveryConfirmationStatus()).isEqualTo(DeliveryConfirmationStatus.PENDING);
        Map<OrderEventType, String> eventByType =
                orderHistoryEventRepository.findAllByOrderId(order.getId()).stream()
                        .filter(o -> o.getSource() == Source.CLIENT)
                        .filter(o -> o.getContext() != null)
                        .collect(Collectors.toMap(OrderHistoryEvent::getType, OrderHistoryEvent::getContext));

        var captor = ArgumentCaptor.forClass(String.class);
        verify(orderCommandService).updateDeliveryAddress(
                any(),
                eq(Source.CLIENT),
                captor.capture()
        );
        assertNotNull(captor.getValue());
        assertEquals(trackingService.createMessage(oldDeliveryAddress, dto), captor.getValue());
        assertThat(eventByType).containsOnlyKeys(OrderEventType.ADDRESS_CHANGED, OrderEventType.CLIENT_MESSAGE);
        assertThat(eventByType.get(OrderEventType.CLIENT_MESSAGE))
                .isEqualTo("Клиент изменил адрес: Подъезд: " + newEntrance
                        + ". Код: " + newEntryPhone + ". Этаж: " + newFloor + ". Квартира: " + newApartment
                        + ". Комментарий: " + newComment);
        dbQueueTestUtil.assertQueueHasSize(QueueType.ORDER_DELIVERY_ADDRESS_UPDATE_SEND_TO_SQS, 1);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    void shouldClarifyAddressOrderUsingPersonal() {
        when(configurationProviderAdapter.isBooleanEnabled(UPDATE_ADDRESS_PERSONAL_WHERE_COURIER_ENABLED))
                .thenReturn(true);

        Order order = orderWithFixedAddress;
        String newComment = CONTACTLESS_DELIVERY_PREFIX;
        TrackingClarifyAddressDto dto = new TrackingClarifyAddressDto(newEntrance.toString(), newFloor.toString(),
                newApartment.toString(), newEntryPhone, newComment);
        UserShift openedShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(openedShift);
        var oldDeliveryAddress = order.getDelivery().getDeliveryAddress();
        var personalServiceRetrieveReturn = List.of(
                new MultiTypeRetrieveResponseItem()
                        .type(CommonTypeEnum.ADDRESS)
                        .value(new CommonType().address(PersonalServiceDataMapper.mapDeliveryAddressToAddressRequest(oldDeliveryAddress))),
                new MultiTypeRetrieveResponseItem()
                        .type(CommonTypeEnum.GPS_COORD)
                        .value(new CommonType().gpsCoord(new GpsCoord().longitude(oldDeliveryAddress.getLongitude()).latitude(oldDeliveryAddress.getLatitude())))
        );
        String newPersonalAddressId = "1234";
        String newPersonalGpsId = "0987";
        var personalServiceStoreReturn = List.of(
                new MultiTypeStoreResponseItem().id(newPersonalAddressId).value(new CommonType().address(Map.of())),
                new MultiTypeStoreResponseItem().id(newPersonalGpsId).value(new CommonType().gpsCoord(new GpsCoord()))
        );
        when(personalExternalService.getMultiTypePersonalByIds(List.of(
                Pair.of(oldDeliveryAddress.getAddressPersonalId(), CommonTypeEnum.ADDRESS),
                Pair.of(oldDeliveryAddress.getGpsPersonalId(), CommonTypeEnum.GPS_COORD)
        ))).thenReturn(personalServiceRetrieveReturn);
        when(personalExternalService.storeMultiTypePersonal(any(), any()))
                .thenReturn(personalServiceStoreReturn);

        trackingService.clarifyAddressOrder(tracking(order).getId(), dto, null);

        order = orderRepository.findById(order.getId()).get();
        Tracking tracking = tracking(order);
        DeliveryAddress address = order.getDelivery().getDeliveryAddress();
        assertThat(address.getCountry()).isEqualTo(oldDeliveryAddress.getCountry());
        assertThat(address.getRegion()).isEqualTo(oldDeliveryAddress.getRegion());
        assertThat(address.getCity()).isEqualTo(oldDeliveryAddress.getCity());
        assertThat(address.getEntrance()).isEqualTo(newEntrance.toString());
        assertThat(address.getFloor()).isEqualTo(newFloor.toString());
        assertThat(address.getEntryPhone()).isEqualTo(newEntryPhone);
        assertThat(address.getApartment()).isEqualTo(newApartment.toString());
        assertThat(order.getDelivery().getRecipientNotes()).isEqualTo(newComment);
        assertThat(tracking.getDeliveryConfirmationStatus()).isEqualTo(DeliveryConfirmationStatus.PENDING);
        Map<OrderEventType, String> eventByType =
                orderHistoryEventRepository.findAllByOrderId(order.getId()).stream()
                        .filter(o -> o.getSource() == Source.CLIENT)
                        .filter(o -> o.getContext() != null)
                        .collect(Collectors.toMap(OrderHistoryEvent::getType, OrderHistoryEvent::getContext));

        var captor = ArgumentCaptor.forClass(String.class);
        verify(orderCommandService).updateDeliveryAddress(
                any(),
                eq(Source.CLIENT),
                captor.capture()
        );
        verify(personalExternalService).getMultiTypePersonalByIds(any());
        verify(personalExternalService).storeMultiTypePersonal(any(), any());
        assertNotNull(captor.getValue());
        assertEquals(trackingService.createMessage(oldDeliveryAddress, dto), captor.getValue());
        assertThat(eventByType).containsOnlyKeys(OrderEventType.ADDRESS_CHANGED);
        dbQueueTestUtil.assertQueueHasSize(QueueType.ORDER_DELIVERY_ADDRESS_UPDATE_SEND_TO_SQS, 1);
    }

    @Test
    @Transactional
    void shouldPushIfChangePaymentTypeInNotActiveRoutePoint() {
        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(getClock()));
        Order order1 = newOrder();
        order1.setPaymentType(OrderPaymentType.CASH);
        order1.setPaymentStatus(OrderPaymentStatus.UNPAID);

        Order order2 = newOrder();
        order2.setPaymentType(OrderPaymentType.CASH);
        order2.setPaymentStatus(OrderPaymentStatus.UNPAID);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(getClock().instant()))
                .routePoint(helper.taskPrepaid("addr1", 10, order1.getId()))
                .routePoint(helper.taskPrepaid("addr2", 22, order2.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        UserShift userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        userHelper.checkin(userShift);
        userHelper.checkinAndFinishPickup(userShift);

        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        //Заказ не текущего routеPoint
        Long orderId = userShift.streamOrderDeliveryTasks().filter(e ->
                !e.getRoutePoint().getId().equals(currentRoutePoint.getId())).findFirst().get().getOrderId();
        Order order = orderRepository.findById(orderId).get();
        String externalOrderId = order.getExternalOrderId();

        trackingService.changePaymentTypeToCard(tracking(orderId).getId(), serviceTicket);
        assertThat(pushNotificationRepository.findAll()
                .stream().filter(e -> e.getBody().contains(externalOrderId))
                .count()).isEqualTo(0);
        TrackingClarifyAddressDto dto = new TrackingClarifyAddressDto(
                Integer.parseInt(order.getDelivery().getDeliveryAddress().getEntrance()) + 1 + "",
                newFloor.toString(),
                newApartment.toString(),
                newEntryPhone, "newComment");
        trackingService.clarifyAddressOrder(tracking(orderId).getId(), dto, null);

        assertThat(pushNotificationRepository.findAll()
                .stream().filter(e -> e.getBody().contains(externalOrderId))
                .count()).isEqualTo(0);
        //Заказ текущего routePoint
        orderId = orderId.equals(order1.getId()) ? order2.getId() : order1.getId();
        order = orderRepository.findById(orderId).get();
        String externalOrderId2 = order.getExternalOrderId();

        trackingService.changePaymentTypeToCard(tracking(orderId).getId(), serviceTicket);
        assertThat(pushNotificationRepository.findAll()
                .stream().filter(e -> e.getBody().contains(externalOrderId2))
                .count()).isEqualTo(1);
        assertThat(pushNotificationRepository.findAll().stream()
                .filter(e -> e.getBody().contains(externalOrderId2))
                .findFirst().get().getBody()).isEqualTo("Изменен тип оплаты по заказу " + externalOrderId2);
    }

    @Test
    @Transactional
    void shouldSkipContactlessDeliveryNoticeBecauseOfAmount() {
        Order order = newOrder();
        order.getItems().clear();
        OrderItem newOrderItem = newOrderItem(order, 33_000);
        order.getItems().add(newOrderItem);
        order.setPaymentType(OrderPaymentType.PREPAID);
        order.getDelivery().setRecipientNotes("И привезите заказ вовремя!");

        UserShift openedShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(openedShift);

        assertThat(trackingDto(order).getDelivery().isShowContactlessDeliveryNotice()).isFalse();
    }

    @Test
    @Transactional
    void shouldSkipContactlessDeliveryNoticeBecauseOfPaymentType() {
        Order order = newOrder();
        order.getItems().clear();
        order.getItems().add(newOrderItem(order, 15_000));
        order.setPaymentType(OrderPaymentType.CASH);
        order.getDelivery().setRecipientNotes("И привезите заказ вовремя!");

        UserShift openedShift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDay(openedShift);

        assertThat(trackingDto(order).getDelivery().isShowContactlessDeliveryNotice()).isFalse();
    }

    @Test
    void showCancelledWhenClientRefused() {
        Order order = newOrder();
        UserShift shift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));

        failDeliveryWithStatus(shift, tracking(order), CLIENT_REFUSED);

        assertDeliveryStatus(trackingDto(order)).isEqualTo(NOT_DELIVERED);
    }

    @Test
    @Transactional
    void showCancelledWhenCancelledByClient() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        Order order = newOrder();
        order.getDelivery().setRecipientNotes(DO_NOT_CALL_TEXT);
        userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));

        trackingService.cancelOrder(tracking(order).getId(),
                new TrackingCancelOrderDto(FOUND_CHEAPER, "Bringly 4 life"), serviceTicket);

        TrackingDto tracking = trackingDto(order);
        assertDeliveryStatus(tracking).isEqualTo(NOT_DELIVERED);
        assertThat(tracking.getDelivery().getCallRequirement()).isEqualTo(CallRequirement.DO_NOT_CALL);
    }

    @Test
    @Transactional
    void showRescheduledWhenRescheduledByClient() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        Order order = newOrder();
        order.getDelivery().setRecipientNotes(DO_NOT_CALL_TEXT);
        userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));

        trackingService.rescheduleOrder(tracking(order).getId(),
                new TrackingRescheduleDto(tomorrowAtHour(10, getClock()), tomorrowAtHour(14, getClock())), serviceTicket);

        TrackingDto tracking = trackingDto(order);
        assertDeliveryStatus(tracking).isEqualTo(RESCHEDULED);

        assertThat(tracking.getDelivery().getIntervalFrom()).isEqualTo(tomorrowAtHour(10, getClock()));
        assertThat(tracking.getDelivery().getIntervalTo()).isEqualTo(tomorrowAtHour(14, getClock()));
        assertThat(tracking.getPreviouslyConfirmationDelivery().getStatus()).isEqualTo(NO_CONFIRMATION_NEEDED);
        assertThat(tracking.getDelivery().getCallRequirement()).isEqualTo(CallRequirement.DO_NOT_CALL);
    }

    @Test
    void cantRescheduleForTodayByClient() {
        Order order = newOrder();
        userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));

        assertThatThrownBy(() -> trackingService.rescheduleOrder(tracking(order).getId(),
                new TrackingRescheduleDto(todayAtHour(14, getClock()), todayAtHour(18, getClock())), serviceTicket))
                .isInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void showCancelledWhenNoMoneyLeftToBuyDamnThing() {
        Order order = newOrder();
        UserShift shift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));

        failDeliveryWithStatus(shift, tracking(order), CANNOT_PAY);

        TrackingDto tracking = trackingDto(order);
        assertDeliveryStatus(tracking).isEqualTo(NOT_DELIVERED);
        assertThat(tracking.getPreviouslyConfirmationDelivery().getStatus()).isEqualTo(NO_CONFIRMATION_NEEDED);
    }

    @Test
    @Transactional
    void showCancelledWhenCancelledByOrderIsDamagedReason() {
        Order order = newOrder();
        UserShift shift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));

        failDeliveryWithStatus(shift, tracking(order), ORDER_IS_DAMAGED);

        assertDeliveryStatus(trackingDto(order)).isEqualTo(NOT_DELIVERED);
        assertThat(order.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.CANCELLED);
    }

    @Test
    void showInProgressWhenRescheduledToToday() {
        Order order = newOrder();
        UserShift shift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        userHelper.finishPickupAtStartOfTheDayWithoutFinishCallTasks(shift);

        rescheduleDelivery(shift, tracking(order), todayAtHour(18, getClock()), todayAtHour(22, getClock()), DELIVERY_DELAY);

        when(Instant.now(getClock()).plus(AVAILABLE_INTERVAL_TO_CLARIFY_ADDRESS, ChronoUnit.HOURS))
                .thenReturn(tracking(order).getOrderDeliveryTask().getExpectedDeliveryTime()
                        .plus(AVAILABLE_INTERVAL_TO_CLARIFY_ADDRESS - 1, ChronoUnit.HOURS));
        TrackingDto tracking = trackingDto(order);
        assertDeliveryStatus(tracking).isEqualTo(IN_PROGRESS);
        assertThat(tracking.getPreviouslyConfirmationDelivery().getStatus()).isEqualTo(NO_CONFIRMATION_NEEDED);
    }

    @Test
    void showRescheduledWhenRescheduledToTomorrow() {
        Order order = newOrder();
        UserShift shift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));

        rescheduleDelivery(shift, tracking(order), tomorrowAtHour(10, getClock()), tomorrowAtHour(14, getClock()),
                CLIENT_REQUEST);

        TrackingDto tracking = trackingDto(order);
        assertDeliveryStatus(trackingDto(order)).isEqualTo(RESCHEDULED);
        assertThat(tracking.getPreviouslyConfirmationDelivery().getStatus()).isEqualTo(NO_CONFIRMATION_NEEDED);
    }

    @Test
    void showRescheduledAfterDeliveryMethodChangeWhenRescheduledAndDeliveryTypeChangedToCourier() {
        when(configurationProviderAdapter
                .isBooleanEnabled(SHOW_DELIVERY_METHOD_CHANGE_AS_RESCHEDULE_REASON_IN_TRACKING_SERVICE)
        )
                .thenReturn(true);

        Order order = newOrder();
        order.setPaymentType(OrderPaymentType.CARD);
        order.setPaymentStatus(OrderPaymentStatus.UNPAID);
        UserShift shift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));

        changeDeliveryType(order, OrderType.CLIENT, null);
        rescheduleDelivery(shift, tracking(order), tomorrowAtHour(10, getClock()), tomorrowAtHour(14, getClock()),
                ORDER_TYPE_UPDATED);

        TrackingDto tracking = trackingDto(order);
        assertDeliveryStatus(trackingDto(order)).isEqualTo(RESCHEDULED_AFTER_DELIVERY_METHOD_CHANGE);
        assertThat(tracking.getPreviouslyConfirmationDelivery().getStatus()).isEqualTo(NO_CONFIRMATION_NEEDED);
    }

    @Test
    void showDeliveryMethodChangedWhenRescheduledAndDeliveryTypeChangedToPickupPoint() {
        when(configurationProviderAdapter
                .isBooleanEnabled(SHOW_DELIVERY_METHOD_CHANGE_AS_RESCHEDULE_REASON_IN_TRACKING_SERVICE)
        )
                .thenReturn(true);

        Order order = newOrder();
        order.setPaymentType(OrderPaymentType.PREPAID);
        order.setPaymentStatus(OrderPaymentStatus.PAID);
        UserShift shift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1L, 1L)
        );

        changeDeliveryType(order, OrderType.PVZ, pickupPoint);
        rescheduleDelivery(shift, tracking(order), tomorrowAtHour(10, getClock()), tomorrowAtHour(14, getClock()),
                ORDER_TYPE_UPDATED);

        TrackingDto tracking = trackingDto(order);
        assertDeliveryStatus(trackingDto(order)).isEqualTo(RESCHEDULED_AFTER_DELIVERY_METHOD_CHANGE);
        assertThat(tracking.getPreviouslyConfirmationDelivery().getStatus()).isEqualTo(NO_CONFIRMATION_NEEDED);
    }

    @Test
    void showRescheduledWhenRescheduledToTomorrowIntervalsFromCombinator() {
        Order order = newOrder();
        UserShift shift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));

        sortingCenterPropertyService.upsertPropertyToSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.GET_RESCHEDULE_DATE_FROM_COMBINATOR_ENABLED, true);

        LocalDate tomorrow = LocalDate.now(getClock()).plusDays(1);

        Mockito.when(combinatorBlockingStub.postponeDelivery(any()))
                .thenReturn(CombinatorOuterClass.PostponeDeliveryResponse.newBuilder()
                        .addAllOptions(List.of(
                                CombinatorOuterClass.DeliveryOption.newBuilder()
                                        .setDateTo(CombinatorOuterClass.Date.newBuilder()
                                                .setYear(tomorrow.getYear())
                                                .setMonth(tomorrow.getMonthValue())
                                                .setDay(tomorrow.getDayOfMonth())
                                                .build())
                                        .setInterval(CombinatorOuterClass.DeliveryInterval.newBuilder()
                                                .setFrom(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(10)
                                                        .setMinute(0)
                                                        .build())
                                                .setTo(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(14)
                                                        .setMinute(0)
                                                        .build())
                                                .build())
                                        .build()
                        ))
                        .build());

        trackingService.rescheduleOrder(tracking(order).getId(),
                new TrackingRescheduleDto(tomorrowAtHour(10, getClock()), tomorrowAtHour(14, getClock())), serviceTicket);

        TrackingDto trackingDto = trackingDto(order);
        assertDeliveryStatus(trackingDto).isEqualTo(RESCHEDULED);

        assertThat(trackingDto.getDelivery().getIntervalFrom()).isEqualTo(tomorrowAtHour(10, getClock()));
        assertThat(trackingDto.getDelivery().getIntervalTo()).isEqualTo(tomorrowAtHour(14, getClock()));
    }

    @Test
    void showNotRescheduledWhenRescheduledToTomorrowIntervalsFromCombinator() {
        Order order = newOrder();
        UserShift shift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));

        sortingCenterPropertyService.upsertPropertyToSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.GET_RESCHEDULE_DATE_FROM_COMBINATOR_ENABLED, true);

        LocalDate tomorrow = LocalDate.now(getClock()).plusDays(1);

        Mockito.when(combinatorBlockingStub.postponeDelivery(any()))
                .thenReturn(CombinatorOuterClass.PostponeDeliveryResponse.newBuilder()
                        .addAllOptions(List.of(
                                CombinatorOuterClass.DeliveryOption.newBuilder()
                                        .setDateTo(CombinatorOuterClass.Date.newBuilder()
                                                .setYear(tomorrow.getYear())
                                                .setMonth(tomorrow.getMonthValue())
                                                .setDay(tomorrow.getDayOfMonth())
                                                .build())
                                        .setInterval(CombinatorOuterClass.DeliveryInterval.newBuilder()
                                                .setFrom(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(10)
                                                        .setMinute(0)
                                                        .build())
                                                .setTo(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(20)
                                                        .setMinute(0)
                                                        .build())
                                                .build())
                                        .build()
                        ))
                        .build());

        assertThatThrownBy(() ->
                trackingService.rescheduleOrder(tracking(order).getId(),
                        new TrackingRescheduleDto(LocalDateTime.of(LocalDate.now(getClock()), LocalTime.of(10, 0))
                                .toInstant(ZoneOffset.ofHours(0)),
                                LocalDateTime.of(LocalDate.now(getClock()), LocalTime.of(14, 0))
                                        .toInstant(ZoneOffset.ofHours(0))), serviceTicket))
                .isInstanceOf(TplInvalidParameterException.class)
                .hasMessage("Interval is not available: 13:00 - 17:00");

    }

    @Test
    @Transactional
    void showRescheduleAfterNoContactWhenNoContactOneTimes() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        Order order = newOrder();
        order.getDelivery().setRecipientNotes(DO_NOT_CALL_TEXT);

        UserShift shift = userHelper.createOpenedShift(user, order,
                order.getDelivery().getDeliveryDate(getClock().getZone()));

        Tracking tracking = tracking(order);

        failDeliveryWithStatus(shift, tracking, NO_CONTACT);

        TrackingDto trackingDto = trackingDto(order);
        assertDeliveryStatus(trackingDto).isEqualTo(RESCHEDULED_AFTER_NO_CONTACT);
        Instant expectedDeliveryIntervalFrom = order.getDelivery().getDeliveryIntervalFrom().plus(1, ChronoUnit.DAYS);
        Instant expectedDeliveryIntervalTo = order.getDelivery().getDeliveryIntervalTo().plus(1, ChronoUnit.DAYS);

        assertThat(trackingDto.getDelivery().getIntervalFrom()).isEqualTo(expectedDeliveryIntervalFrom);
        assertThat(trackingDto.getDelivery().getIntervalTo()).isEqualTo(expectedDeliveryIntervalTo);
        assertThat(trackingDto.getPreviouslyConfirmationDelivery().getStatus()).isEqualTo(NO_CONFIRMATION_NEEDED);
        assertThat(trackingDto.getDelivery().getCallRequirement()).isEqualTo(CallRequirement.DO_NOT_CALL);
    }

    @Test
    @Transactional
    void showRescheduleWhenNoContactAndRescheduleByClient() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        Order order = newOrder();
        order.getDelivery().setRecipientNotes(DO_NOT_CALL_TEXT);
        UserShift shift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));

        Tracking tracking = tracking(order);

        failDeliveryWithStatus(shift, tracking, NO_CONTACT);

        // переносим заказ на другой день от имени пользователя
        trackingService.rescheduleOrder(tracking(order).getId(),
                new TrackingRescheduleDto(tomorrowAtHour(10, getClock()), tomorrowAtHour(14, getClock())), serviceTicket);

        TrackingDto trackingDto = trackingDto(order);
        assertDeliveryStatus(trackingDto).isEqualTo(RESCHEDULED);

        assertThat(trackingDto.getDelivery().getIntervalFrom()).isEqualTo(tomorrowAtHour(10, getClock()));
        assertThat(trackingDto.getDelivery().getIntervalTo()).isEqualTo(tomorrowAtHour(14, getClock()));
        assertThat(trackingDto.getDelivery().getCallRequirement()).isEqualTo(CallRequirement.DO_NOT_CALL);
    }

    @Test
    void showNotRescheduleWhenWrongTimeZone() {
        Order order = newOrder();
        UserShift shift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));

        Tracking tracking = tracking(order);

        failDeliveryWithStatus(shift, tracking, NO_CONTACT);
        // переносим заказ на другой день от имени пользователя, но с временной зоной, отличайщейся от Default
        assertThatThrownBy(() ->
                trackingService.rescheduleOrder(tracking(order).getId(),
                        new TrackingRescheduleDto(LocalDateTime.of(LocalDate.now(getClock()), LocalTime.of(10, 0))
                                .toInstant(ZoneOffset.ofHours(0)),
                                LocalDateTime.of(LocalDate.now(getClock()), LocalTime.of(14, 0))
                                        .toInstant(ZoneOffset.ofHours(0))), serviceTicket))

                .isInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void rescheduleWithNotDefaultTimeZone() {
        Order order = newOrder();
        var offset = 4;
        var zoneOffset = ZoneOffset.ofHours(offset);
        var sc = userHelper.sortingCenter(123L, offset);
        UserShift shift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()), sc.getId());

        Tracking tracking = tracking(order);

        failDeliveryWithStatus(shift, tracking, NO_CONTACT);
        var begin = LocalDateTime.of(LocalDate.now(getClock()).plusDays(1), LocalTime.of(10, 0))
                .toInstant(zoneOffset);
        var end = LocalDateTime.of(LocalDate.now(getClock()).plusDays(1), LocalTime.of(22, 0))
                .toInstant(zoneOffset);
        trackingService.rescheduleOrder(tracking(order).getId(),
                new TrackingRescheduleDto(begin, end), serviceTicket);

        TrackingDto trackingDto = trackingDto(order);

        assertThat(trackingDto.getDelivery().getIntervalFrom()).isEqualTo(tomorrowAtHour(10, getClock(), zoneOffset));
        assertThat(trackingDto.getDelivery().getIntervalTo()).isEqualTo(tomorrowAtHour(22, getClock(), zoneOffset));
    }

    @Test
    @Transactional
    void showCancelledAfterNoContactWhenNoContactThreeTimes() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        Order order = newOrder();
        order.getDelivery().setRecipientNotes(DO_NOT_CALL_TEXT);
        LocalDate deliveryDateFirst = order.getDelivery().getDeliveryDate(getClock().getZone());
        UserShift shift = userHelper.createOpenedShift(user, order, deliveryDateFirst);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.GET_RESCHEDULE_DATE_FROM_COMBINATOR_ENABLED, false);

        Instant deliveryInstantFirst = order.getDelivery().getDeliveryIntervalFrom();

        Tracking tracking = tracking(order);

        failDeliveryWithStatus(shift, tracking, NO_CONTACT);

        commandService.closeShift(new UserShiftCommand.Close(shift.getId()));
        userHelper.finishUserShift(shift);

        // выполнение таски с переносом
        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER_NO_CONTACT, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.RESCHEDULE_ORDER_NO_CONTACT);

        // и еще раз
        UserShift userShiftSecondAttempt = userHelper.createOpenedShift(user, order,
                LocalDate.ofInstant(deliveryInstantFirst.plus(1, ChronoUnit.DAYS), getClock().getZone()));
        failDeliveryWithStatus(userShiftSecondAttempt, tracking, NO_CONTACT);

        commandService.closeShift(new UserShiftCommand.Close(userShiftSecondAttempt.getId()));
        userHelper.finishUserShift(userShiftSecondAttempt);

        // выполнение таски с переносом
        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER_NO_CONTACT, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.RESCHEDULE_ORDER_NO_CONTACT);
        // и еще раз
        UserShift userShiftThirdAttempt = userHelper.createOpenedShift(user, order,
                LocalDate.ofInstant(deliveryInstantFirst.plus(2, ChronoUnit.DAYS), getClock().getZone()));
        failDeliveryWithStatus(userShiftThirdAttempt, tracking, NO_CONTACT);

        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER_NO_CONTACT, 0);

        TrackingDto trackingDto = trackingDto(order);
        assertDeliveryStatus(trackingDto).isEqualTo(CANCELLED_AFTER_NO_CONTACT);
        assertThat(trackingDto.getDelivery().getCallRequirement()).isEqualTo(CallRequirement.DO_NOT_CALL);
    }

    @Test
    void showDeliveredOnSuccessfulChequePrint() {
        Order order = newOrder();
        UserShift shift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));

        userHelper.finishPickupAtStartOfTheDay(shift, true);
        printChequeOnPrepaidOrder(order, shift);

        TrackingDto tracking = trackingDto(order);
        assertDeliveryStatus(tracking).isEqualTo(DELIVERED);
        assertThat(tracking.getPreviouslyConfirmationDelivery().getStatus()).isEqualTo(NO_CONFIRMATION_NEEDED);
        assertThat(tracking.getOrders()).hasSize(1);
        assertThat(tracking.getDelivery().getTransferType()).isEqualTo(TransferType.HAND_TO_HAND);
    }

    @Test
    @Transactional
    void showDeliveredOnSuccessfulChequePrintNotCurrentRoutePoint() {
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .paymentType(OrderPaymentType.CASH)
                .build());
        var order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.PAID)
                .paymentType(OrderPaymentType.PREPAID)
                .build());
        var order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.PAID)
                .paymentType(OrderPaymentType.PREPAID)
                .build());

        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(getClock()));

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskUnpaid("addr1", 12, order.getId()))
                .routePoint(helper.taskPrepaid("addr3", 14, order1.getId()))
                .routePoint(helper.taskPrepaid("addrPaid", 13, order2.getId()))
                .build();

        var userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        userHelper.checkin(userShift);

        userHelper.finishPickupAtStartOfTheDay(userShift, true);
        Order order4 = userShift.getCurrentRoutePoint().streamDeliveryTasks().flatMap(e -> e.getOrderIds().stream())
                .collect(Collectors.toSet()).contains(order.getId()) ? order1 : order;
        userPropertyService.addPropertyToUser(user, UserProperties.TRACKING_SHOW_PRECISE_INTERVAL, true);


        printChequeOnPrepaidOrderWithoutArriving(order4, userShift);

        TrackingDto tracking = trackingDto(order4);
        assertDeliveryStatus(tracking).isEqualTo(DELIVERED);
        assertThat(tracking.getOrders()).hasSize(1);
        assertThat(tracking.getDelivery().getTransferType()).isEqualTo(TransferType.HAND_TO_HAND);
    }

    @Test
    @Transactional
    void showGetDeliveryConfirmationOnSuccessfulChequePrint() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);
        when(configurationProviderAdapter.isBooleanEnabled(NEAR_THE_DOOR_CONTAINS_CHECK_ENABLED)).thenReturn(true);

        Order order = newOrder();
        order.getDelivery().setRecipientNotes(DO_NOT_CALL_TEXT + CONTACTLESS_DELIVERY_PREFIX + " И привезите вовремя!");
        when(configurationProviderAdapter.isBooleanEnabled(DELIVERY_CONFIRMATION_ENABLED)).thenReturn(true);
        UserShift shift = userHelper.createOpenedShift(user, order, LocalDate.now(getClock()));

        userHelper.finishPickupAtStartOfTheDay(shift, true);
        printChequeOnPrepaidOrder(order, shift);

        TrackingDto tracking = trackingDto(order);
        assertDeliveryStatus(tracking).isEqualTo(DELIVERED_TO_THE_DOOR);
        assertThat(tracking.getDelivery().getCallRequirement()).isEqualTo(CallRequirement.DO_NOT_CALL);

        trackingService.confirmContactlessDelivery(tracking.getId(), serviceTicket);
        assertDeliveryStatus(trackingDto(order)).isEqualTo(DELIVERED);
    }

    @Test
    void leavingAtReceptionStatusInitialization() {
        UserShift userShift = userHelper.createEmptyShift(user, LocalDate.now(getClock()));

        Reception reception = receptionRepository.findTopByOrderByIdAsc();

        Order orderToReception = newOrderWithGeoPoint(reception.getGeoPoint());
        Order notPrepaidOrderToReception = newOrderWithGeoPointAndPaymentType(
                reception.getGeoPoint(),
                OrderPaymentType.CASH
        );
        Order orderToSiberia = newOrderWithGeoPoint(GeoPoint.ofLatLon(
                BigDecimal.valueOf(101.136093),
                BigDecimal.valueOf(72.982509)
        ));

        userHelper.addDeliveryTaskToShift(user, userShift, orderToReception);
        userHelper.addDeliveryTaskToShift(user, userShift, notPrepaidOrderToReception);
        userHelper.addDeliveryTaskToShift(user, userShift, orderToSiberia);

        userHelper.openShift(user, userShift.getId());
        userHelper.finishPickupAtStartOfTheDay(userShift);

        TrackingDto trackingToReception = trackingDto(orderToReception);
        TrackingDto trackingNotPrepaidOrderToReception = trackingDto(notPrepaidOrderToReception);
        TrackingDto trackingToSiberia = trackingDto(orderToSiberia);

        assertThat(trackingToReception.getDelivery().getLeavingAtReceptionStatus())
                .isEqualTo(LeavingAtReceptionStatus.AVAILABLE);

        assertThat(trackingNotPrepaidOrderToReception.getDelivery().getLeavingAtReceptionStatus())
                .isEqualTo(LeavingAtReceptionStatus.UNAVAILABLE);

        assertThat(trackingToSiberia.getDelivery().getLeavingAtReceptionStatus())
                .isEqualTo(LeavingAtReceptionStatus.UNAVAILABLE);
    }

    @Test
    void leavingAtReceptionStatusConfirmation() {
        UserShift userShift = userHelper.createEmptyShift(user, LocalDate.now(getClock()));
        Reception reception = receptionRepository.findTopByOrderByIdAsc();

        Order orderToReception = newOrderWithGeoPoint(reception.getGeoPoint());

        userHelper.addDeliveryTaskToShift(user, userShift, orderToReception);

        userHelper.openShift(user, userShift.getId());
        userHelper.finishPickupAtStartOfTheDay(userShift);

        leaveOrderAtReception(orderToReception);

        TrackingDto trackingToReception = trackingDto(orderToReception);
        assertThat(trackingToReception.getDelivery().getLeavingAtReceptionStatus())
                .isEqualTo(LeavingAtReceptionStatus.CONFIRMED);

        printChequeOnPrepaidOrder(orderToReception, userShift);

        assertDeliveryStatus(trackingDto(orderToReception)).isEqualTo(DELIVERED_TO_RECEPTION);
    }

    @Test
    void leavingAtReceptionStatusValidation() {
        UserShift userShift = userHelper.createEmptyShift(user, LocalDate.now(getClock()));
        Reception reception = receptionRepository.findTopByOrderByIdAsc();

        Order orderToReception = newOrderWithGeoPoint(reception.getGeoPoint());
        Order orderToSiberia = newOrderWithGeoPoint(GeoPoint.ofLatLon(
                BigDecimal.valueOf(101.136093),
                BigDecimal.valueOf(72.982509)
        ));

        userHelper.addDeliveryTaskToShift(user, userShift, orderToReception);
        userHelper.addDeliveryTaskToShift(user, userShift, orderToSiberia);

        userHelper.openShift(user, userShift.getId());
        userHelper.finishPickupAtStartOfTheDay(userShift);

        assertThatThrownBy(() -> leaveOrderAtReception(orderToSiberia))
                .isInstanceOf(TplInvalidActionException.class);

        leaveOrderAtReception(orderToReception);
        assertThatThrownBy(() -> leaveOrderAtReception(orderToReception))
                .isInstanceOf(TplInvalidActionException.class);
    }

    static class TestDataProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(
                            LocalTimeInterval.valueOf("14:00-18:00"),
                            LocalTimeInterval.valueOf("14:00-18:00"),
                            LocalTime.of(17, 0),
                            11
                    ),
                    Arguments.of(
                            LocalTimeInterval.valueOf("14:00-18:00"),
                            LocalTimeInterval.valueOf("16:00-18:00"),
                            LocalTime.of(17, 0),
                            6
                    ),
                    Arguments.of(
                            LocalTimeInterval.valueOf("14:00-18:00"),
                            LocalTimeInterval.valueOf("16:40-17:40"),
                            LocalTime.of(17, 13),
                            2
                    ),
                    Arguments.of(
                            LocalTimeInterval.valueOf("09:00-22:00"),
                            LocalTimeInterval.valueOf("11:50-15:50"),
                            LocalTime.of(13, 50),
                            11
                    )
            );
        }
    }

    @ParameterizedTest
    @ArgumentsSource(TestDataProvider.class)
    @Transactional
    void showPreciseInterval(
            LocalTimeInterval clientInterval,
            LocalTimeInterval expectedInterval,
            LocalTime expectedDeliveryTime,
            int orderCount
    ) {
        // хотя по умолчанию включен soft_mode в котором не показывается уточненное время прибытия
        // тест удалять жалко
        userPropertyService.addPropertyToUser(user, UserProperties.TRACKING_SHOW_PRECISE_INTERVAL, true);

        UserShift userShift = userHelper.createEmptyShift(user, LocalDate.now(getClock()));

        OrderGenerateService.OrderGenerateParam.OrderGenerateParamBuilder builder =
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryInterval(clientInterval);

        LocalTime startTime = clientInterval.getStart();
        for (int i = 0; i < orderCount; i++) {
            Order order = orderGenerateService.createOrder(builder.build());
            NewDeliveryRoutePointData prepaidTask = helper.taskPrepaid("address",
                    order.getId(),
                    todayAt(startTime.plus(i * 15L, ChronoUnit.MINUTES), getClock()),
                    false);

            commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(
                    userShift.getId(),
                    prepaidTask,
                    SimpleStrategies.NO_MERGE
            ));
        }

        Order order = orderGenerateService.createOrder(builder.build());
        NewDeliveryRoutePointData prepaidTask = helper.taskPrepaid("address",
                order.getId(),
                todayAt(expectedDeliveryTime, getClock()),
                false);

        commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(
                userShift.getId(),
                prepaidTask,
                SimpleStrategies.NO_MERGE
        ));

        userHelper.openShift(user, userShift.getId());
        userHelper.finishPickupAtStartOfTheDay(userShift);

        TrackingDto tracking = trackingDto(order);
        DeliveryDto deliveryDeliveryDto = tracking.getDelivery();

        assertThat(deliveryDeliveryDto.getIntervalDisplayMode()).isNotNull()
                .isEqualTo(TrackingIntervalDisplayMode.FROM_TO);
        assertThat(deliveryDeliveryDto.getIntervalFrom()).isEqualTo(todayAt(expectedInterval.getStart(), getClock()));
        assertThat(deliveryDeliveryDto.getIntervalTo()).isEqualTo(todayAt(expectedInterval.getEnd(), getClock()));

        // пересекли левую границу уточненного интервала
        ClockUtil.initFixed(
                getClock(),
                LocalDateTime.ofInstant(
                        todayAt(expectedInterval.getStart().plus(10, ChronoUnit.MINUTES), getClock()),
                        getClock().getZone()));

        tracking = trackingDto(order);
        deliveryDeliveryDto = tracking.getDelivery();

        assertThat(deliveryDeliveryDto.getIntervalDisplayMode()).isEqualTo(TrackingIntervalDisplayMode.TO);
        assertThat(deliveryDeliveryDto.getIntervalFrom()).isEqualTo(todayAt(expectedInterval.getStart(), getClock()));
        assertThat(deliveryDeliveryDto.getIntervalTo()).isEqualTo(todayAt(expectedInterval.getEnd(), getClock()));
    }

    @Test
    @Transactional
    void showExpectedDeliveryTimeWhenCurrent() {
        userPropertyService.addPropertyToUser(user, UserProperties.TRACKING_SHOW_PRECISE_INTERVAL, true);

        UserShift userShift = userHelper.createEmptyShift(user, LocalDate.now(getClock()));

        OrderGenerateService.OrderGenerateParam.OrderGenerateParamBuilder builder =
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryInterval(LocalTimeInterval.valueOf("14:00-18:00"));

        Order order = orderGenerateService.createOrder(builder.build());
        NewDeliveryRoutePointData prepaidTask = helper.taskPrepaid("address",
                order.getId(),
                todayAt(LocalTime.of(17, 0), getClock()),
                false);

        commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(
                userShift.getId(),
                prepaidTask,
                SimpleStrategies.NO_MERGE
        ));

        userHelper.openShift(user, userShift.getId());
        userHelper.finishPickupAtStartOfTheDay(userShift);

        TrackingDto tracking = trackingDto(order);
        DeliveryDto deliveryDeliveryDto = tracking.getDelivery();

        assertThat(deliveryDeliveryDto.getIntervalDisplayMode()).isNotNull().isEqualTo(TrackingIntervalDisplayMode.EDT);
    }

    @Test
    @Transactional
    void doNotShowExpectedTimeSoftMode() {
        var userShift = userHelper.createEmptyShift(user, LocalDate.now(getClock()));
        var orderBuilder = OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("14:00-18:00"));
        var order = orderGenerateService.createOrder(orderBuilder.build());
        var prepaidTask = helper.taskPrepaid("address",
                order.getId(),
                todayAt(LocalTime.of(17, 0), getClock()),
                false);

        commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(
                userShift.getId(),
                prepaidTask,
                SimpleStrategies.NO_MERGE
        ));

        userHelper.openShift(user, userShift.getId());
        userHelper.finishPickupAtStartOfTheDay(userShift);

        userPropertyService.addPropertyToUser(user, UserProperties.TRACKING_SHOW_PRECISE_INTERVAL, true);
        userPropertyService.addPropertyToUser(user, UserProperties.TRACKING_SHOW_DELIVERIES_LEFT, true);

        var tracking = trackingDto(order);
        assertNotEquals(order.getDelivery().getDeliveryIntervalFrom(), tracking.getDelivery().getIntervalFrom());
        assertNotEquals(order.getDelivery().getDeliveryIntervalTo(), tracking.getDelivery().getIntervalTo());
        assertEquals(TrackingIntervalDisplayMode.EDT, tracking.getDelivery().getIntervalDisplayMode());
        assertEquals(0, tracking.getCourier().getDeliveriesLeft());
        assertThat(tracking.getDelivery().isPreciseInterval()).isTrue();

        userPropertyService.addPropertyToUser(user, UserProperties.TRACKING_SHOW_PRECISE_INTERVAL, false);
        userHelper.clearUserPropertiesCache();

        tracking = trackingDto(order);
        assertEquals(order.getDelivery().getDeliveryIntervalFrom(), tracking.getDelivery().getIntervalFrom());
        assertEquals(order.getDelivery().getDeliveryIntervalTo(), tracking.getDelivery().getIntervalTo());
        assertEquals(TrackingIntervalDisplayMode.FROM_TO, tracking.getDelivery().getIntervalDisplayMode());
        assertEquals(0, tracking.getCourier().getDeliveriesLeft());
        assertThat(tracking.getCourier().getIsSoftMode()).isTrue();
        assertThat(tracking.getDelivery().isPreciseInterval()).isFalse();
    }

    @Test
    @Transactional
    void showDeliveriesLeft() {
        var userShift = userHelper.createEmptyShift(user, LocalDate.now(getClock()));
        var orderBuilder = OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("14:00-18:00"));
        var order1 = orderGenerateService.createOrder(orderBuilder.build());
        var order2 = orderGenerateService.createOrder(orderBuilder.build());
        var order3 = orderGenerateService.createOrder(orderBuilder.build());
        var prepaidTask1 = helper.taskPrepaid("address", order1.getId(),
                todayAt(LocalTime.of(15, 0), getClock()), false);
        var prepaidTask2 = helper.taskPrepaid("address", order2.getId(),
                todayAt(LocalTime.of(16, 0), getClock()), false);
        var prepaidTask3 = helper.taskPrepaid("address", order3.getId(),
                todayAt(LocalTime.of(17, 0), getClock()), false);

        commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(userShift.getId(), prepaidTask1,
                SimpleStrategies.NO_MERGE));
        commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(userShift.getId(), prepaidTask2,
                SimpleStrategies.NO_MERGE));
        commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(userShift.getId(), prepaidTask3,
                SimpleStrategies.NO_MERGE));

        userHelper.openShift(user, userShift.getId());
        userHelper.finishPickupAtStartOfTheDay(userShift);

        userPropertyService.addPropertyToUser(user, UserProperties.TRACKING_SHOW_DELIVERIES_LEFT, true);
        var tracking = trackingDto(order3);
        assertThat(tracking.getCourier().getDeliveriesLeft()).isEqualTo(2);

        userPropertyService.addPropertyToUser(user, UserProperties.TRACKING_SHOW_DELIVERIES_LEFT, false);
        userHelper.clearUserPropertiesCache();
        tracking = trackingDto(order3);
        assertThat(tracking.getCourier().getDeliveriesLeft()).isNull();
    }

    @Test
    @Transactional
    void showDeliveryDisclaimer() {
        userPropertyService.addPropertyToUser(user, UserProperties.TRACKING_SHOW_PRECISE_INTERVAL, true);

        var userShift = userHelper.createEmptyShift(user, LocalDate.now(getClock()));
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder().build());
        var prepaidTask = helper.taskPrepaid("address",
                order.getId(),
                todayAt(LocalTime.of(17, 0), getClock()),
                false);

        commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(
                userShift.getId(),
                prepaidTask,
                SimpleStrategies.NO_MERGE
        ));

        userHelper.openShift(user, userShift.getId());
        userHelper.finishPickupAtStartOfTheDay(userShift);

        var tracking = trackingDto(order);
        assertThat(tracking.getDelivery().getDisclaimer()).isNull();

        doReturn(Optional.of("test disclaimer")).when(configurationProviderAdapter)
                .getValue(ConfigurationProperties.TRACKING_DELIVERY_DISCLAIMER);
        tracking = trackingDto(order);
        assertThat(tracking.getDelivery().getDisclaimer()).isNull();

        userPropertyService.addPropertyToUser(user, UserProperties.TRACKING_SHOW_DELIVERY_DISCLAIMER, true);
        userHelper.clearUserPropertiesCache();
        tracking = trackingDto(order);
        assertThat(tracking.getDelivery().getDisclaimer()).isEqualTo("test disclaimer");

        doReturn(Optional.empty()).when(configurationProviderAdapter)
                .getValue(ConfigurationProperties.TRACKING_DELIVERY_DISCLAIMER);
        tracking = trackingDto(order);
        assertThat(tracking.getDelivery().getDisclaimer()).isNull();
    }

    private void printChequeOnPrepaidOrder(Order order, UserShift shift) {
        Tracking tracking = tracking(order);

        commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                shift.getId(),
                tracking.getOrderDeliveryTask().getRoutePoint().getId(),
                new LocationDto(BigDecimal.ONE, BigDecimal.ONE, "9384h-uire", shift.getId())
        ));

        commandService.printCheque(user, new UserShiftCommand.PrintOrReturnCheque(
                shift.getId(),
                tracking.getOrderDeliveryTask().getRoutePoint().getId(),
                tracking.getOrderDeliveryTask().getId(),
                dataHelper.getChequeDto(OrderPaymentType.PREPAID),
                Instant.now(getClock()),
                false, null,
                Optional.empty()
        ));
    }

    private void printChequeOnPrepaidOrderWithoutArriving(Order order, UserShift shift) {
        Tracking tracking = tracking(order);

        commandService.printCheque(user, new UserShiftCommand.PrintOrReturnCheque(
                shift.getId(),
                tracking.getOrderDeliveryTask().getRoutePoint().getId(),
                tracking.getOrderDeliveryTask().getId(),
                dataHelper.getChequeDto(OrderPaymentType.PREPAID),
                Instant.now(getClock()),
                SOFT_MODE.equals(UserMode.valueOf(
                        userPropertyService.findPropertyForUser(UserProperties.USER_MODE, user))),
                null,
                Optional.empty()
        ));
    }

    private void rescheduleDelivery(UserShift shift, Tracking tracking, Instant intervalFrom, Instant intervalTo,
                                    OrderDeliveryRescheduleReasonType type) {
        commandService.rescheduleDeliveryTask(user, new UserShiftCommand.RescheduleOrderDeliveryTask(
                shift.getId(),
                tracking.getOrderDeliveryTask().getRoutePoint().getId(),
                tracking.getOrderDeliveryTask().getId(),
                DeliveryReschedule.fromCourier(user, intervalFrom, intervalTo, type),
                Instant.now(getClock()),
                shift.getZoneId()
        ));
    }

    private void changeDeliveryType(Order order, OrderType orderType, PickupPoint pickupPoint) {
        orderCommandService.updateOrderType(
                new OrderCommand.UpdateOrderType(
                        order.getId(),
                        orderType,
                        pickupPoint,
                        order.getPaymentType(),
                        order.getPaymentStatus()
                ),
                Source.DELIVERY
        );
    }

    private void failDeliveryWithStatus(UserShift shift, Tracking tracking, OrderDeliveryTaskFailReasonType type) {
        failDeliveryWithStatus(shift, tracking, type, null);
    }

    private void failDeliveryWithStatus(UserShift shift, Tracking tracking,
                                        OrderDeliveryTaskFailReasonType type, String comment) {
        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                shift.getId(),
                tracking.getOrderDeliveryTask().getRoutePoint().getId(),
                tracking.getOrderDeliveryTask().getId(),
                new OrderDeliveryFailReason(type, comment)
        ));
    }

    private void leaveOrderAtReception(Order order) {
        Tracking tracking = tracking(order);

        trackingService.leaveOrderAtReception(tracking.getId(), serviceTicket);
    }

    private Order newOrder() {
        return orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(LocalDate.now(getClock()).plusDays(11))
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .build());
    }

    private Order newOrderWithGeoPoint(GeoPoint geoPoint) {
        return orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .build()
        );
    }

    private Order newOrderWithGeoPointAndPaymentType(GeoPoint geoPoint, OrderPaymentType paymentType) {
        return orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .paymentType(paymentType)
                        .build()
        );
    }

    private AbstractComparableAssert<?, TrackingDeliveryStatus> assertDeliveryStatus(TrackingDto tracking) {
        return assertThat(tracking.getDelivery().getStatus());
    }

    private TrackingDto trackingDto(Order order) {
        return trackingService.getTrackingDto(tracking(order).getId());
    }

    private Tracking tracking(Order order) {
        return trackingRepository.findByOrderId(order.getId()).orElseThrow();
    }

    private Tracking tracking(Long orderId) {
        return trackingRepository.findByOrderId(orderId).orElseThrow();
    }

    private OrderItem newOrderItem(Order order, int price) {
        return new OrderItem(order, "Test", false, 1, BigDecimal.valueOf(price),
                BigDecimal.valueOf(price), VatType.VAT_20, "24FD¬43", null, null,
                null, null, null);
    }

    private OrderItem newR18OrderItem(Order order, int price) {
        return new OrderItem(order, "Test", false, 1, BigDecimal.valueOf(price),
                BigDecimal.valueOf(price), VatType.VAT_20, "24FD¬43", null, null,
                null, null, List.of(CargoType.R18.getCode()));
    }

    private void successAttemptCallOnCurrentRoutePoint(UserShift userShift) {
        RoutePointDto routePointInfo =
                userShiftQueryService.getRoutePointInfo(user, userShift.getCurrentRoutePoint().getId());

        routePointInfo.getCallTasks()
                .forEach(callTaskDto -> commandService.successAttemptCall(
                        user,
                        new UserShiftCommand.AttemptCallToRecipient(
                                userShift.getId(),
                                routePointInfo.getId(),
                                callTaskDto.getId(),
                                "")));
    }

}
