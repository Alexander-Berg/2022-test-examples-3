package ru.yandex.market.tpl.tms.service.order.address;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderDeliveryAddressClarificationDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.partner.OrderEventType;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Dimensions;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEvent;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.Address;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.address.AddressQueryService;
import ru.yandex.market.tpl.core.domain.order.address.AddressString;
import ru.yandex.market.tpl.core.domain.order.address.DeliveryAddress;
import ru.yandex.market.tpl.core.domain.region.actualization.TplRegionBorderGisDao;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.service.order.address.ClarifyOrderAddressPayload;
import ru.yandex.market.tpl.core.service.order.address.CourierAddressClarificationService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class CourierAddressClarificationServiceTest extends TplTmsAbstractTest {
    private static final Long BUYER_YANDEX_UID = 123L;
    private static final AddressGenerator.AddressGenerateParam ADDRESS_GENERATE_PARAM =
            AddressGenerator.AddressGenerateParam.builder()
                    .country("Россия")
                    .region("Москва")
                    .city("Москва")
                    .street("Пушкина")
                    .house("123")
                    .geoPoint(GeoPoint.ofLatLon(55.806786, 37.464592))
                    .build();

    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final CourierAddressClarificationService courierAddressClarificationService;
    private final OrderRepository orderRepository;
    private final Clock clock;
    private final ClarifyOrderAddressProcessingService clarifyOrderAddressProcessingService;
    private final OrderGenerateService orderGenerateService;
    private final OrderHistoryEventRepository orderHistoryEventRepository;
    private final TplRegionBorderGisDao tplRegionBorderGisDao;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final ClientReturnGenerator clientReturnGenerator;
    private final UserShiftCommandService commandService;
    private final ClientReturnRepository clientReturnRepository;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final UserPropertyService userPropertyService;
    private final TransactionTemplate transactionTemplate;

    private final AddressQueryService addressQueryService;

    private OrderDeliveryTask orderDeliveryTask;
    private User user;
    private UserShift userShift;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(1L);
        transactionTemplate.executeWithoutResult(ts -> userPropertyService.addPropertyToUser(user,
                UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true));
        userShift = userHelper.createEmptyShift(user, LocalDate.now(clock));
        orderDeliveryTask = testDataFactory.addDeliveryTaskAuto(
                user, userShift.getId(), OrderPaymentStatus.PAID, OrderPaymentType.PREPAID, 10,
                BUYER_YANDEX_UID, ADDRESS_GENERATE_PARAM);
    }

    @AfterEach
    void resetMocks() {
        Mockito.reset(addressQueryService);
    }

    @Test
    void courierAddressClarification() {
        var addressClarification = getClarificationDto();

        var order = orderRepository.findById(orderDeliveryTask.getOrderId()).orElseThrow();
        var oldAddress = order.getDelivery().getDeliveryAddress();
        var newAddress = getNewAddress(oldAddress);
        doReturn(Optional.of(newAddress))
                .when(addressQueryService)
                .queryByAddressString(eq(newAddress.getAddressString()));

        courierAddressClarificationService.clarifyAddress(user, orderDeliveryTask.getId(), addressClarification);

        var address = orderRepository.findByIdOrThrow(orderDeliveryTask.getOrderId())
                .getDelivery().getDeliveryAddress();
        assertThat(address.getEntrance()).isEqualTo(addressClarification.getEntrance());
        assertThat(address.getApartment()).isEqualTo(addressClarification.getApartment());
        assertThat(address.getEntryPhone()).isEqualTo(addressClarification.getEntryPhone());
        assertThat(address.getFloor()).isEqualTo(addressClarification.getFloor());
        assertThat(address.getClarified()).isTrue();

        dbQueueTestUtil.assertQueueHasSize(QueueType.ORDER_DELIVERY_ADDRESS_UPDATE_SEND_TO_SQS, 1);
    }

    @Test
    void courierAddressClarificationForClientReturn() {
        transactionTemplate.executeWithoutResult(ts -> userPropertyService.addPropertyToUser(user,
                UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true));
        when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.CLARIFY_ADDRESS_FOR_CLIENT_RETURN_ENABLED
        )).thenReturn(true);
        var addressClarification = getClarificationDto();

        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();

        var tod = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );
        courierAddressClarificationService.clarifyAddress(user, tod.getId(), addressClarification);

        var address = clientReturnRepository.findByIdOrThrow(clientReturn.getId()).getLogisticRequestPointFrom();
        assertThat(address.getEntrance()).isEqualTo(addressClarification.getEntrance());
        assertThat(address.getApartment()).isEqualTo(addressClarification.getApartment());
        assertThat(address.getEntryPhone()).isEqualTo(addressClarification.getEntryPhone());
        assertThat(address.getFloor()).isEqualTo(addressClarification.getFloor());
    }

    @Test
    void courierAddressClarificationForClientReturnAndOrder() {
        transactionTemplate.executeWithoutResult(ts -> userPropertyService.addPropertyToUser(user,
                UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true));
        when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.CLARIFY_ADDRESS_FOR_CLIENT_RETURN_ENABLED
        )).thenReturn(true);


        var geoPoint = GeoPointGenerator.generateLonLat();
        var cr = clientReturnGenerator.generateReturnFromClient();

        cr.getLogisticRequestPointFrom().setOriginalLatitude(geoPoint.getLatitude());
        cr.getLogisticRequestPointFrom().setOriginalLongitude(geoPoint.getLongitude());
        cr.getClient().getClientData().setPhone("phone1");
        clientReturnRepository.save(cr);

        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);

        var clientReturnDeliveryTask = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, cr.getId(), deliveryTime
                )
        );

         orderDeliveryTask = testDataFactory.addDeliveryTaskManual(user, userShift.getId(), routePointId,
                createOrderGenerateParam(geoPoint, "phone1", "10:00-14:00", 1));

        assertThat(orderDeliveryTask).isNotNull();
        assertThat(orderDeliveryTask.getMultiOrderId()).isEqualTo(
                String.format("m_%d_%d", orderDeliveryTask.getOrderId(), cr.getId())
        );
        var order = orderRepository.findByIdOrThrow(orderDeliveryTask.getOrderId());

        var oldAddress = order.getDelivery().getDeliveryAddress();
        var newAddress = getNewAddress(oldAddress);
        doReturn(Optional.of(newAddress))
                .when(addressQueryService)
                .queryByAddressString(any());


        transactionTemplate.executeWithoutResult(ts -> userPropertyService.addPropertyToUser(user,
                UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true));
        when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.CLARIFY_ADDRESS_FOR_CLIENT_RETURN_ENABLED
        )).thenReturn(true);
        var addressClarification = getClarificationDto();

        courierAddressClarificationService.clarifyAddress(user, clientReturnDeliveryTask.getId(), addressClarification);

        var crAddress = clientReturnRepository.findByIdOrThrow(cr.getId()).getLogisticRequestPointFrom();
        assertThat(crAddress.getEntrance()).isEqualTo(addressClarification.getEntrance());
        assertThat(crAddress.getApartment()).isEqualTo(addressClarification.getApartment());
        assertThat(crAddress.getEntryPhone()).isEqualTo(addressClarification.getEntryPhone());
        assertThat(crAddress.getFloor()).isEqualTo(addressClarification.getFloor());

        var orderAddress = orderRepository.findByIdOrThrow(orderDeliveryTask.getOrderId())
                .getDelivery().getDeliveryAddress();
        assertThat(orderAddress.getEntrance()).isEqualTo(addressClarification.getEntrance());
        assertThat(orderAddress.getApartment()).isEqualTo(addressClarification.getApartment());
        assertThat(orderAddress.getEntryPhone()).isEqualTo(addressClarification.getEntryPhone());
        assertThat(orderAddress.getFloor()).isEqualTo(addressClarification.getFloor());
        assertThat(orderAddress.getClarified()).isTrue();

        dbQueueTestUtil.assertQueueHasSize(QueueType.ORDER_DELIVERY_ADDRESS_UPDATE_SEND_TO_SQS, 1);
    }

    @Test
    void clarifyAddressAfterCourierAddressClarification() {
        var addressClarification = getClarificationDto();

        var order = orderRepository.findById(orderDeliveryTask.getOrderId()).orElseThrow();
        var oldAddress = order.getDelivery().getDeliveryAddress();
        var newAddress = getNewAddress(oldAddress);
        doReturn(Optional.of(newAddress))
                .when(addressQueryService)
                .queryByAddressString(eq(newAddress.getAddressString()));

        courierAddressClarificationService.clarifyAddress(user, orderDeliveryTask.getId(), addressClarification);

        var address = orderRepository.findById(orderDeliveryTask.getOrderId()).orElseThrow()
                .getDelivery().getDeliveryAddress();
        assertThat(address.getEntrance()).isEqualTo(addressClarification.getEntrance());
        assertThat(address.getApartment()).isEqualTo(addressClarification.getApartment());
        assertThat(address.getEntryPhone()).isEqualTo(addressClarification.getEntryPhone());
        assertThat(address.getFloor()).isEqualTo(addressClarification.getFloor());
        assertThat(address.getClarified()).isTrue();

        dbQueueTestUtil.assertQueueHasSize(QueueType.ORDER_DELIVERY_ADDRESS_UPDATE_SEND_TO_SQS, 1);

        var orderDeliveryTask = testDataFactory.addDeliveryTaskAuto(
                user, userShift.getId(), OrderPaymentStatus.PAID, OrderPaymentType.PREPAID, 10,
                BUYER_YANDEX_UID, ADDRESS_GENERATE_PARAM);
        order = orderRepository.findById(orderDeliveryTask.getOrderId()).orElseThrow();
        address = order.getDelivery().getDeliveryAddress();
        address.setFloor(null);
        address.setApartment(null);
        address.setEntrance(null);
        address.setEntryPhone(null);
        orderRepository.save(order);

        clarifyOrderAddressProcessingService.processPayload(
                new ClarifyOrderAddressPayload(
                        "requestId",
                        orderDeliveryTask.getOrderId()
                )
        );

        order = orderRepository.findByIdOrThrow(orderDeliveryTask.getOrderId());
        var clarifiedAddress = order.getDelivery().getDeliveryAddress();
        assertThat(clarifiedAddress.getEntrance()).isEqualTo(addressClarification.getEntrance());
        assertThat(clarifiedAddress.getApartment()).isEqualTo(addressClarification.getApartment());
        assertThat(clarifiedAddress.getEntryPhone()).isEqualTo(addressClarification.getEntryPhone());
        assertThat(clarifiedAddress.getFloor()).isEqualTo(addressClarification.getFloor());

        dbQueueTestUtil.assertQueueHasSize(QueueType.ORDER_DELIVERY_ADDRESS_UPDATE_SEND_TO_SQS, 1);
    }

    @Test
    void courierAddressClarificationAfterCall() {
        var addressClarification = getClarificationDto();

        var order = orderRepository.findById(orderDeliveryTask.getOrderId()).orElseThrow();
        var oldAddress = order.getDelivery().getDeliveryAddress();
        var newAddress = getNewAddress(oldAddress);
        doReturn(Optional.of(newAddress))
                .when(addressQueryService)
                .queryByAddressString(eq(newAddress.getAddressString()));


        var callTask = orderDeliveryTask.getCallToRecipientTask();
        courierAddressClarificationService.clarifyAddressAfterCall(user, callTask.getId(), addressClarification);

        var address = orderRepository.findByIdOrThrow(orderDeliveryTask.getOrderId())
                .getDelivery().getDeliveryAddress();

        assertThat(address.getEntrance()).isEqualTo(addressClarification.getEntrance());
        assertThat(address.getApartment()).isEqualTo(addressClarification.getApartment());
        assertThat(address.getEntryPhone()).isEqualTo(addressClarification.getEntryPhone());
        assertThat(address.getFloor()).isEqualTo(addressClarification.getFloor());
        assertThat(address.getClarified()).isTrue();

        dbQueueTestUtil.assertQueueHasSize(QueueType.ORDER_DELIVERY_ADDRESS_UPDATE_SEND_TO_SQS, 1);

        Mockito.reset(addressQueryService);
    }

    @Test
    void tryUpdateZeroCoordinates() {

        //given
        AddressGenerator.AddressGenerateParam.AddressGenerateParamBuilder equalsAddress =
                AddressGenerator.AddressGenerateParam.builder()
                        .city("Балашиха")
                        .street(null)
                        .house("18");

        double latitude = 55.806786;
        double longitude = 37.464592;

        var orderWithCoordinates = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .addressGenerateParam(equalsAddress.geoPoint(GeoPoint.ofLatLon(latitude, longitude)).build())
                        .buyerYandexUid(BUYER_YANDEX_UID)
                        .build()
        );
        orderWithCoordinates.getDelivery().getDeliveryAddress().setStreet(null);
        orderRepository.save(orderWithCoordinates);
        var orderWithoutCoordinates = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .addressGenerateParam(equalsAddress.geoPoint(GeoPoint.ofLatLon(0, 0)).build())
                        .buyerYandexUid(BUYER_YANDEX_UID)
                        .build()
        );
        orderWithoutCoordinates.getDelivery().getDeliveryAddress().setStreet(null);
        orderRepository.save(orderWithoutCoordinates);
        GeoPoint point = GeoPoint.ofLatLon(latitude, longitude);
        when(tplRegionBorderGisDao.findDsRegionIdsWithinMarginFromPoint(
                eq(orderWithoutCoordinates.getDeliveryServiceId()),
                any(), eq(point.getLongitude()), eq(point.getLatitude()),
                anyInt(), any())).thenReturn(List.of(1L));

        //when
        clarifyOrderAddressProcessingService.processPayload(
                new ClarifyOrderAddressPayload(
                        "requestId",
                        orderWithoutCoordinates.getId()
                )
        );


        //then
        Order updatedOrder = orderRepository.findByIdOrThrow(orderWithoutCoordinates.getId());

        assertThat(
                updatedOrder
                        .getDelivery()
                        .getDeliveryAddress()
                        .getGeoPoint()
                        .isZeroGeoPoint()
        ).isFalse();

        assertNotNull(updatedOrder.getIsAddressValid());
        assertNotNull(updatedOrder.getAddressValidatedAt());
        assertTrue(updatedOrder.getIsAddressValid());

        List<OrderHistoryEvent> orderHistoryEvents = orderHistoryEventRepository
                .findAllByOrderId(orderWithoutCoordinates.getId()).stream()
                .filter(orderHistoryEvent -> orderHistoryEvent.getType() == OrderEventType.COORDINATES_UPDATED)
                .collect(Collectors.toList());
        assertThat(orderHistoryEvents).hasSize(1);

        dbQueueTestUtil.assertQueueHasSize(QueueType.ORDER_DELIVERY_ADDRESS_UPDATE_SEND_TO_SQS, 1);
    }

    private OrderDeliveryAddressClarificationDto getClarificationDto() {
        return OrderDeliveryAddressClarificationDto.builder()
                .floor("8")
                .apartment("322")
                .entrance("42")
                .entryPhone("322K1580")
                .build();
    }

    private Address getNewAddress(DeliveryAddress oldAddress) {
        return new Address(
                AddressString.builder()
                        .dropDefaultCityFromAddressString(true)
                        .country("Россия")
                        .region("Москва")
                        .city("Москва")
                        .street("Пушкина")
                        .house("123")
                        .floor("8")
                        .apartment("322")
                        .entrance("42")
                        .entryPhone("322K1580")
                        .build(),
                GeoPoint.ofLatLon(oldAddress.getLatitude(), oldAddress.getLongitude()),
                oldAddress.getRegionId()
        );
    }

    private OrderGenerateService.OrderGenerateParam createOrderGenerateParam(GeoPoint geoPoint,
                                                                             String clientPhone,
                                                                             String timeInterval,
                                                                             int volumeInCubicMeters) {
        return OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .recipientPhone(clientPhone)
                .deliveryInterval(LocalTimeInterval.valueOf(timeInterval))
                .dimensions(new Dimensions(BigDecimal.ONE, 100, 100, volumeInCubicMeters * 100))
                .build();
    }
}
