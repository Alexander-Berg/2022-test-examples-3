package ru.yandex.market.tpl.core.service.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.market.tpl.api.model.tracking.DeliveryDto;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.ds.DsZoneOffsetCachingService;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.routing.MultiOrderMapper;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.tracking.TrackingRepository;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.service.crm.communication.AsyncCommunicationSender;
import ru.yandex.market.tpl.core.service.crm.communication.model.CourierPlatformCommunicationDto;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.util.TplUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.GET_MULTI_INTERVAL_BY_ALL_ORDERS;

@RequiredArgsConstructor
class NotificationAndWhereIsCourierIntervalTest extends TplAbstractTest {

    private static final long UID = 1L;

    private static final long DELIVERY_SERVICE_ID = 239L;
    private static final String INTERVAL_9_TO_18 = "09:00-18:00";
    private static final String INTERVAL_14_TO_22 = "14:00-22:00";
    private static final String INTERVAL_10_TO_12 = "10:00-12:00";
    private static final String EXT_ID_1 = "123456";
    private static final String EXT_ID_2 = "654321";

    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final TrackingService trackingService;
    private final TrackingRepository trackingRepository;
    private final DsZoneOffsetCachingService dsZoneOffsetCachingService;
    private final MultiOrderMapper multiOrderMapper;
    @SpyBean
    private AsyncCommunicationSender asyncCommunicationSender;

    private LocalDate now;
    private Order order1;
    private Order order2;
    private User user;
    private UserShift userShift;
    private final AsyncProducer mockedAsyncProducer;
    private Shift shift;
    private AddressGenerator.AddressGenerateParam addressGenerateParam;
    private GeoPoint geoPoint;

    @AfterEach
    void after() {
        Mockito.clearInvocations(mockedAsyncProducer);
    }

    @BeforeEach
    void setUp() {
        now = LocalDate.now();
        geoPoint = GeoPointGenerator.generateLonLat();
        shift = testUserHelper.findOrCreateOpenShift(now);
        user = testUserHelper.findOrCreateUser(UID);

        addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(geoPoint)
                .street("Колотушкина")
                .house("1")
                .build();
    }

    @Test
    void testNewTrackingInterval() {
        createJointIntervalOrders();
        //Подготовка переменных окружения
        configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                OrderGenerateService.DEFAULT_PHONE);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_STARTED_DELIVERY_EVENT, true);
        configurationServiceAdapter.insertValue(GET_MULTI_INTERVAL_BY_ALL_ORDERS, true);

        //начинаем шифт и отправляются ивенты на отправку сообщения
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        //ловим ивент на отправку в триггерную
        var eventCaptor = ArgumentCaptor.forClass(CourierPlatformCommunicationDto.class);
        verify(asyncCommunicationSender).send(eventCaptor.capture());
        var event = (CourierPlatformCommunicationDto.OrderStartedDeliveryEvent) eventCaptor.getValue();
        RelativeTimeInterval relativeTimeInterval = multiOrderMapper.getIntervalForOrders(List.of(order1, order2));
        var offset = dsZoneOffsetCachingService.getOffsetForDs(order1.getDeliveryServiceId());
        var extendedTimeInterval = TplUtils.DateTime.extendIntervalToLatestEnd(relativeTimeInterval, List.of(order1,
                order2), offset);
        assertThat(event.getDeliveryIntervalFrom()).isEqualTo(extendedTimeInterval.getStart());
        assertThat(event.getDeliveryIntervalTo()).isEqualTo(extendedTimeInterval.getEnd());

        var trackingDto =
                trackingService.getTrackingDto(trackingRepository.findTrackingIdByExternalOrderId(order1.getExternalOrderId()).orElseThrow());
        DeliveryDto deliveryDto1 = trackingDto.getDelivery();

        //для ссылки через первый заказ, получаем то же, что и в триггерной
        assertThat(LocalTime.ofInstant(deliveryDto1.getIntervalFrom(), offset)).isEqualTo(extendedTimeInterval.getStart());
        assertThat(LocalTime.ofInstant(deliveryDto1.getIntervalTo(), offset)).isEqualTo(extendedTimeInterval.getEnd());

        var trackingDto2 =
                trackingService.getTrackingDto(trackingRepository.findTrackingIdByExternalOrderId(order2.getExternalOrderId()).orElseThrow());

        DeliveryDto deliveryDto2 = trackingDto2.getDelivery();

        //Для ммылки на второй заказ результат из триггерной
        assertThat(LocalTime.ofInstant(deliveryDto2.getIntervalFrom(), offset)).isEqualTo(extendedTimeInterval.getStart());
        assertThat(LocalTime.ofInstant(deliveryDto2.getIntervalTo(), offset)).isEqualTo(extendedTimeInterval.getEnd());

        //При этом ссылки разные
        assertThat(trackingDto.getId()).isNotEqualTo(trackingDto2.getId());
    }

    @Test
    void disjointIntervalsTest() {
        createDisjointIntervalsOrders();

        //Подготовка переменных окружения
        configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                OrderGenerateService.DEFAULT_PHONE);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_STARTED_DELIVERY_EVENT, true);
        configurationServiceAdapter.insertValue(GET_MULTI_INTERVAL_BY_ALL_ORDERS, true);

        //начинаем шифт и отправляются ивенты на отправку сообщения
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        //ловим ивент на отправку в триггерную
        var eventCaptor = ArgumentCaptor.forClass(CourierPlatformCommunicationDto.class);
        verify(asyncCommunicationSender).send(eventCaptor.capture());
        var event = (CourierPlatformCommunicationDto.OrderStartedDeliveryEvent) eventCaptor.getValue();
        RelativeTimeInterval relativeTimeInterval = multiOrderMapper.getIntervalForOrders(List.of(order1, order2));
        var offset = dsZoneOffsetCachingService.getOffsetForDs(order1.getDeliveryServiceId());
        var extendedTimeInterval = TplUtils.DateTime.extendIntervalToLatestEnd(relativeTimeInterval, List.of(order1,
                order2), offset);
        assertThat(event.getDeliveryIntervalFrom()).isEqualTo(extendedTimeInterval.getStart());
        assertThat(event.getDeliveryIntervalTo()).isEqualTo(extendedTimeInterval.getEnd());

        var trackingDto =
                trackingService.getTrackingDto(trackingRepository.findTrackingIdByExternalOrderId(order1.getExternalOrderId()).orElseThrow());
        DeliveryDto deliveryDto1 = trackingDto.getDelivery();

        //для ссылки через первый заказ, получаем то же, что и в триггерной
        assertThat(LocalTime.ofInstant(deliveryDto1.getIntervalFrom(), offset)).isEqualTo(extendedTimeInterval.getStart());
        assertThat(LocalTime.ofInstant(deliveryDto1.getIntervalTo(), offset)).isEqualTo(extendedTimeInterval.getEnd());

        var trackingDto2 =
                trackingService.getTrackingDto(trackingRepository.findTrackingIdByExternalOrderId(order2.getExternalOrderId()).orElseThrow());

        DeliveryDto deliveryDto2 = trackingDto2.getDelivery();

        //Для ммылки на второй заказ результат из триггерной
        assertThat(LocalTime.ofInstant(deliveryDto2.getIntervalFrom(), offset)).isEqualTo(extendedTimeInterval.getStart());
        assertThat(LocalTime.ofInstant(deliveryDto2.getIntervalTo(), offset)).isEqualTo(extendedTimeInterval.getEnd());

        //При этом ссылки разные
        assertThat(trackingDto.getId()).isNotEqualTo(trackingDto2.getId());
    }

    private void createJointIntervalOrders() {
        order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .buyerYandexUid(1L)
                .externalOrderId(EXT_ID_1)
                .deliveryInterval(LocalTimeInterval.valueOf(INTERVAL_9_TO_18))
                .addressGenerateParam(addressGenerateParam)
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .build());
        order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .buyerYandexUid(1L)
                .externalOrderId(EXT_ID_2)
                .deliveryInterval(LocalTimeInterval.valueOf(INTERVAL_14_TO_22))
                .addressGenerateParam(addressGenerateParam)
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .build());

        user = testUserHelper.findOrCreateUser(UID);

        Instant deliveryTime = order1.getDelivery().getDeliveryIntervalFrom();
        RoutePointAddress myAddress = new RoutePointAddress("my_address", geoPoint);

        NewDeliveryRoutePointData delivery1 = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(deliveryTime)
                .expectedDeliveryTime(deliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order1, false, false)
                .build();

        NewDeliveryRoutePointData delivery2 = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(deliveryTime)
                .expectedDeliveryTime(deliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order2, false, false)
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift.getId())
                .routePoint(delivery1)
                .routePoint(delivery2)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        userShift = userShiftRepository.findByIdOrThrow(userShiftId);
    }

    private void createDisjointIntervalsOrders() {
        order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .buyerYandexUid(1L)
                .externalOrderId(EXT_ID_1)
                .deliveryInterval(LocalTimeInterval.valueOf(INTERVAL_10_TO_12))
                .addressGenerateParam(addressGenerateParam)
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .build());
        order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .buyerYandexUid(1L)
                .externalOrderId(EXT_ID_2)
                .deliveryInterval(LocalTimeInterval.valueOf(INTERVAL_14_TO_22))
                .addressGenerateParam(addressGenerateParam)
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .build());

        user = testUserHelper.findOrCreateUser(UID);

        Instant deliveryTime = order1.getDelivery().getDeliveryIntervalFrom();
        RoutePointAddress myAddress = new RoutePointAddress("my_address", geoPoint);

        NewDeliveryRoutePointData delivery1 = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(deliveryTime)
                .expectedDeliveryTime(deliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order1, false, false)
                .build();

        NewDeliveryRoutePointData delivery2 = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(deliveryTime)
                .expectedDeliveryTime(deliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order2, false, false)
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift.getId())
                .routePoint(delivery1)
                .routePoint(delivery2)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        userShift = userShiftRepository.findByIdOrThrow(userShiftId);
    }
}
