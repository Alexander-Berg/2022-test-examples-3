package ru.yandex.market.tpl.core.domain.routing.async.yago;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.user.CourierVehicleType;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.yago.YandexGoOrderProperties;
import ru.yandex.market.tpl.core.domain.yago.YandexGoOrderSupportImpl;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleData;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CreateYandexGoSupportOrderTasksServiceTest {

    public static final Long COMPANY_ID = 100L;
    public static final Long SHIFT_ID = 200L;
    public static final String REQUEST_ID = "request-id";
    public static final Long USER_SHIFT_ID_1 = 301L;
    public static final Long USER_SHIFT_ID_2 = 302L;
    public static final Long ORDER_ID_1 = 401L;
    public static final Long ORDER_ID_11 = 4011L;
    public static final Long ORDER_ID_12 = 4012L;
    public static final Long ORDER_ID_2 = 402L;
    public static final Long ORDER_ID_21 = 4021L;
    public static final Long ORDER_ID_22 = 4022L;
    public static final Long ORDER_ID_3 = 403L;
    public static final Long ORDER_ID_31 = 4031L;
    public static final Long ORDER_ID_32 = 4032L;
    public static final LocalDate SHIFT_DATE = LocalDate.of(2012, 4, 24);
    public static final LocalTime USER_SHIFT_START_TIME_1 = LocalTime.of(10, 30);
    public static final LocalTime USER_SHIFT_FINISH_TIME_1 = LocalTime.of(12, 25);
    public static final LocalTime USER_SHIFT_START_TIME_2 = LocalTime.of(11, 30);
    public static final LocalTime USER_SHIFT_FINISH_TIME_2 = LocalTime.of(13, 25);
    public static final ZoneOffset USER_SHIFT_ZONE_ID_1 = ZoneOffset.MIN;
    public static final ZoneOffset USER_SHIFT_ZONE_ID_2 = ZoneOffset.MAX;
    public static final List<RoutePoint> EMPTY_ROUTE_POINTS = List.of();
    public static final List<OrderDeliveryTask> EMPTY_DELIVERY_TASKS = List.of();
    public static final List<Long> EMPTY_SHIFT_IDS = List.of();
    public static final Clock clock =
            Clock.fixed(OffsetDateTime.of(2021, 6, 6, 0, 0, 0, 0, ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    CreateYandexGoOrderTasksService service;
    YandexGoOrderProperties props;
    UserShiftRepository repository;
    UserPropertyService userPropertyService;
    CreateYandexGoOrderProducer producer;

    @BeforeEach
    void init() {
        repository = createStrictMock(UserShiftRepository.class);
        userPropertyService = createStrictMock(UserPropertyService.class);
        producer = mock(CreateYandexGoOrderProducer.class);
        props = createStrictMock(YandexGoOrderProperties.class);
        doReturn(Set.of(COMPANY_ID)).when(props).getCompanyIds();
        service = new CreateYandexGoOrderTasksService(repository, producer, props, new YandexGoOrderSupportImpl(props, userPropertyService));
    }

    private void mockRepository(List<Long> userShiftIds) {
        doReturn(userShiftIds).when(repository).getCompanyUserShiftIds(COMPANY_ID, SHIFT_ID);
    }

    @Test
    void shouldNotPublishTasks_IfNoShifts() {
        // given
        mockRepository(EMPTY_SHIFT_IDS);

        // when
        service.processPayload(new CreateYandexGoOrderTasksPayload(REQUEST_ID, SHIFT_ID));

        // then
        verifyNoMoreInteractions(producer);
    }

    @Test
    void shouldNotPublishTasks_IfNoRoutePoints() {
        // given
        mockRepository(List.of(USER_SHIFT_ID_1));
        Shift shift = mockShift();
        UserShift userShift =
                mockUserShift(
                        shift,
                        USER_SHIFT_ZONE_ID_1,
                        USER_SHIFT_START_TIME_1,
                        USER_SHIFT_FINISH_TIME_1,
                        EMPTY_ROUTE_POINTS);
        doReturn(userShift).when(repository).findByIdOrThrow(USER_SHIFT_ID_1);

        // when
        service.processPayload(new CreateYandexGoOrderTasksPayload(REQUEST_ID, SHIFT_ID));

        // then
        verifyNoMoreInteractions(producer);
    }

    @Test
    void shouldNotPublishTasks_IfTasksAbsent() {
        // given
        mockRepository(List.of(USER_SHIFT_ID_1));
        Shift shift = mockShift();
        RoutePoint routePoint = mockRoutePoint(EMPTY_DELIVERY_TASKS, null);
        UserShift userShift =
                mockUserShift(
                        shift,
                        USER_SHIFT_ZONE_ID_1,
                        USER_SHIFT_START_TIME_1,
                        USER_SHIFT_FINISH_TIME_1,
                        List.of(routePoint));
        doReturn(userShift).when(repository).findByIdOrThrow(USER_SHIFT_ID_1);

        // when
        service.processPayload(new CreateYandexGoOrderTasksPayload(REQUEST_ID, SHIFT_ID));

        // then
        verifyNoMoreInteractions(producer);
    }

    @Test
    void shouldPublishOneTask_IfOneDeliveryTaskExists() {
        mockRepository(List.of(USER_SHIFT_ID_1));
        Shift shift = mockShift();
        UserShift userShift =
                mockUserShift(
                        shift,
                        USER_SHIFT_ZONE_ID_1,
                        USER_SHIFT_START_TIME_1,
                        USER_SHIFT_FINISH_TIME_1,
                        List.of(mockRoutePoint(List.of(mockDeliveryTask(ORDER_ID_1, null)), null)));
        doReturn(userShift).when(repository).findByIdOrThrow(USER_SHIFT_ID_1);

        // when
        service.processPayload(new CreateYandexGoOrderTasksPayload(REQUEST_ID, SHIFT_ID));

        // then
        OffsetDateTime pickupTime = createPickupTime(shift, userShift);
        verify(producer).produce(ORDER_ID_1, null, USER_SHIFT_ID_1, 1, 0, pickupTime);
        verifyNoMoreInteractions(producer);
    }

    @Test
    void shouldPublishTwoTasks_IfMultiOrderDeliveryTasks() {
        // given
        mockRepository(List.of(USER_SHIFT_ID_1));
        Shift shift = mockShift();
        OrderDeliveryTask orderDeliveryTask1 = mockDeliveryTask(ORDER_ID_1, clock.instant());
        OrderDeliveryTask orderDeliveryTask2 = mockDeliveryTask(ORDER_ID_2, clock.instant().plusMillis(1));
        RoutePoint routePoint = mockRoutePoint(List.of(orderDeliveryTask1, orderDeliveryTask2), null);
        UserShift userShift =
                mockUserShift(
                        shift,
                        USER_SHIFT_ZONE_ID_1,
                        USER_SHIFT_START_TIME_1,
                        USER_SHIFT_FINISH_TIME_1,
                        List.of(routePoint));
        doReturn(userShift).when(repository).findByIdOrThrow(USER_SHIFT_ID_1);

        // when
        service.processPayload(new CreateYandexGoOrderTasksPayload(REQUEST_ID, SHIFT_ID));

        // then
        OffsetDateTime pickupTime = createPickupTime(shift, userShift);
        verify(producer).produce(ORDER_ID_1, null, USER_SHIFT_ID_1, 2, 0, pickupTime);
        verify(producer).produce(ORDER_ID_2, null, USER_SHIFT_ID_1, 2, 1, pickupTime);
        verifyNoMoreInteractions(producer);
    }

    @Test
    void shouldPublish2Tasks_IfThereAre2UserShiftWith1Task() {
        // given
        mockRepository(List.of(USER_SHIFT_ID_1, USER_SHIFT_ID_2));
        Shift shift = mockShift();

        OrderDeliveryTask orderDeliveryTask1 = mockDeliveryTask(ORDER_ID_1, null);
        RoutePoint routePoint1 = mockRoutePoint(List.of(orderDeliveryTask1), null);
        UserShift userShift1 =
                mockUserShift(
                        shift,
                        USER_SHIFT_ZONE_ID_1,
                        USER_SHIFT_START_TIME_1,
                        USER_SHIFT_FINISH_TIME_1,
                        List.of(routePoint1));
        doReturn(userShift1).when(repository).findByIdOrThrow(USER_SHIFT_ID_1);

        OrderDeliveryTask orderDeliveryTask2 = mockDeliveryTask(ORDER_ID_2, null);
        RoutePoint routePoint2 = mockRoutePoint(List.of(orderDeliveryTask2), null);
        UserShift userShift2 =
                mockUserShift(
                        shift,
                        USER_SHIFT_ZONE_ID_2,
                        USER_SHIFT_START_TIME_2,
                        USER_SHIFT_FINISH_TIME_2,
                        List.of(routePoint2));
        doReturn(userShift2).when(repository).findByIdOrThrow(USER_SHIFT_ID_2);

        // when
        service.processPayload(new CreateYandexGoOrderTasksPayload(REQUEST_ID, SHIFT_ID));

        // then
        OffsetDateTime pickupTime1 = createPickupTime(shift, userShift1);
        verify(producer).produce(ORDER_ID_1, null, USER_SHIFT_ID_1, 1, 0, pickupTime1);

        OffsetDateTime pickupTime2 = createPickupTime(shift, userShift2);
        verify(producer).produce(ORDER_ID_2, null, USER_SHIFT_ID_2, 1, 0, pickupTime2);

        verifyNoMoreInteractions(producer);
    }


    @Test
    void shouldPublish2Tasks_IfThereAre2UserShiftWith1And2Tasks() {
        // given
        mockRepository(List.of(USER_SHIFT_ID_1, USER_SHIFT_ID_2));
        Shift shift = mockShift();

        OrderDeliveryTask orderDeliveryTask1 = mockDeliveryTask(ORDER_ID_1, clock.instant().plusMillis(1));
        OrderDeliveryTask orderDeliveryTask11 = mockDeliveryTask(ORDER_ID_11, clock.instant().plusMillis(2));
        RoutePoint routePoint1 = mockRoutePoint(List.of(orderDeliveryTask1, orderDeliveryTask11), null);
        UserShift userShift1 =
                mockUserShift(
                        shift,
                        USER_SHIFT_ZONE_ID_1,
                        USER_SHIFT_START_TIME_1,
                        USER_SHIFT_FINISH_TIME_1,
                        List.of(routePoint1));
        doReturn(userShift1).when(repository).findByIdOrThrow(USER_SHIFT_ID_1);

        OrderDeliveryTask orderDeliveryTask2 = mockDeliveryTask(ORDER_ID_2, null);
        RoutePoint routePoint2 = mockRoutePoint(List.of(orderDeliveryTask2), null);
        UserShift userShift2 =
                mockUserShift(
                        shift,
                        USER_SHIFT_ZONE_ID_2,
                        USER_SHIFT_START_TIME_2,
                        USER_SHIFT_FINISH_TIME_2,
                        List.of(routePoint2));
        doReturn(userShift2).when(repository).findByIdOrThrow(USER_SHIFT_ID_2);

        // when
        service.processPayload(new CreateYandexGoOrderTasksPayload(REQUEST_ID, SHIFT_ID));

        // then
        OffsetDateTime pickupTime1 = createPickupTime(shift, userShift1);
        verify(producer).produce(ORDER_ID_1, null, USER_SHIFT_ID_1, 2, 0, pickupTime1);
        verify(producer).produce(ORDER_ID_11, null, USER_SHIFT_ID_1, 2, 1, pickupTime1);

        OffsetDateTime pickupTime2 = createPickupTime(shift, userShift2);
        verify(producer).produce(ORDER_ID_2, null, USER_SHIFT_ID_2, 1, 0, pickupTime2);

        verifyNoMoreInteractions(producer);
    }

    /**
     * Проверяет последовательность создаваемых заказов
     */
    @Test
    void shouldPublish2TasksInOrderByExpectedDeliveryTime() {
        // given
        mockRepository(List.of(USER_SHIFT_ID_1));
        Shift shift = mockShift();

        OrderDeliveryTask orderDeliveryTask1 = mockDeliveryTask(ORDER_ID_1, clock.instant().plusMillis(1));
        OrderDeliveryTask orderDeliveryTask11 = mockDeliveryTask(ORDER_ID_11, clock.instant().plusMillis(2));
        OrderDeliveryTask orderDeliveryTask12 = mockDeliveryTask(ORDER_ID_12, clock.instant().plusMillis(3));
        RoutePoint routePoint1 =
                mockRoutePoint(List.of(orderDeliveryTask1, orderDeliveryTask11, orderDeliveryTask12), clock.instant());


        OrderDeliveryTask orderDeliveryTask2 = mockDeliveryTask(ORDER_ID_2, clock.instant().plusMillis(5));
        OrderDeliveryTask orderDeliveryTask21 = mockDeliveryTask(ORDER_ID_21, clock.instant().plusMillis(6));
        OrderDeliveryTask orderDeliveryTask22 = mockDeliveryTask(ORDER_ID_22, clock.instant().plusMillis(7));
        RoutePoint routePoint2 =
                mockRoutePoint(List.of(orderDeliveryTask22, orderDeliveryTask2, orderDeliveryTask21),
                        clock.instant().plusMillis(4));

        OrderDeliveryTask orderDeliveryTask3 = mockDeliveryTask(ORDER_ID_3, clock.instant().plusMillis(9));
        OrderDeliveryTask orderDeliveryTask31 = mockDeliveryTask(ORDER_ID_31, clock.instant().plusMillis(10));
        OrderDeliveryTask orderDeliveryTask32 = mockDeliveryTask(ORDER_ID_32, clock.instant().plusMillis(10));
        RoutePoint routePoint3 =
                mockRoutePoint(List.of(orderDeliveryTask31, orderDeliveryTask32, orderDeliveryTask3),
                        clock.instant().plusMillis(8));

        UserShift userShift1 =
                mockUserShift(
                        shift,
                        USER_SHIFT_ZONE_ID_1,
                        USER_SHIFT_START_TIME_1,
                        USER_SHIFT_FINISH_TIME_1,
                        List.of(routePoint1, routePoint3, routePoint2));
        doReturn(userShift1).when(repository).findByIdOrThrow(USER_SHIFT_ID_1);

        // when
        service.processPayload(new CreateYandexGoOrderTasksPayload(REQUEST_ID, SHIFT_ID));

        // then
        InOrder inOrder = Mockito.inOrder(producer);

        OffsetDateTime pickupTime1 = createPickupTime(shift, userShift1);
        inOrder.verify(producer).produce(ORDER_ID_1, null, USER_SHIFT_ID_1, 9, 0, pickupTime1);
        inOrder.verify(producer).produce(ORDER_ID_11, null, USER_SHIFT_ID_1, 9, 1, pickupTime1);
        inOrder.verify(producer).produce(ORDER_ID_12, null, USER_SHIFT_ID_1, 9, 2, pickupTime1);
        inOrder.verify(producer).produce(ORDER_ID_2, null, USER_SHIFT_ID_1, 9, 3, pickupTime1);
        inOrder.verify(producer).produce(ORDER_ID_21, null, USER_SHIFT_ID_1, 9, 4, pickupTime1);
        inOrder.verify(producer).produce(ORDER_ID_22, null, USER_SHIFT_ID_1, 9, 5, pickupTime1);
        inOrder.verify(producer).produce(ORDER_ID_3, null, USER_SHIFT_ID_1, 9, 6, pickupTime1);
        inOrder.verify(producer).produce(ORDER_ID_31, null, USER_SHIFT_ID_1, 9, 7, pickupTime1);
        inOrder.verify(producer).produce(ORDER_ID_32, null, USER_SHIFT_ID_1, 9, 8, pickupTime1);

        verifyNoMoreInteractions(producer);
    }

    private OrderDeliveryTask mockDeliveryTask(Long orderId, Instant expectedDeliveryTime) {
        OrderDeliveryTask deliveryTask = createStrictMock(OrderDeliveryTask.class);
        doReturn(orderId).when(deliveryTask).getOrderId();
        if (expectedDeliveryTime != null) {
            doReturn(expectedDeliveryTime).when(deliveryTask).getExpectedDeliveryTime();
        }
        return deliveryTask;
    }

    private OffsetDateTime createPickupTime(Shift shift, UserShift userShift) {
        return OffsetDateTime.of(
                shift.getShiftDate(),
                userShift.getScheduleData().getShiftStart(),
                userShift.getZoneId());
    }

    private Shift mockShift() {
        Shift shift = createStrictMock(Shift.class);
        doReturn(SHIFT_DATE).when(shift).getShiftDate();
        return shift;
    }

    private UserShift mockUserShift(
            Shift shift,
            ZoneOffset userShiftZoneId,
            LocalTime userShiftStartTime,
            LocalTime userShiftFinishTime,
            List<RoutePoint> routePoints) {
        UserShift userShift = createStrictMock(UserShift.class);
        doReturn(userShiftZoneId).when(userShift).getZoneId();
        doReturn(StreamEx.of(routePoints)).when(userShift).streamRoutePoints();
        UserScheduleData userScheduleData =
                new UserScheduleData(
                        CourierVehicleType.CAR,
                        new RelativeTimeInterval(userShiftStartTime, userShiftFinishTime));
        doReturn(userScheduleData).when(userShift).getScheduleData();
        doReturn(shift).when(userShift).getShift();
        return userShift;
    }

    private RoutePoint mockRoutePoint(List<OrderDeliveryTask> deliveryTasks, Instant expectedDateTime) {
        RoutePoint routePoint = createStrictMock(RoutePoint.class);
        doReturn(StreamEx.of(deliveryTasks)).when(routePoint).streamOrderDeliveryTasks();
        if (expectedDateTime != null) {
            doReturn(expectedDateTime).when(routePoint).getExpectedDateTime();
        }
        return routePoint;
    }

    private <T> T createStrictMock(Class<T> classToMock) {
        return mock(
                classToMock,
                invocation -> {
                    throw new RuntimeException("Not Mocked");
                });
    }

}
