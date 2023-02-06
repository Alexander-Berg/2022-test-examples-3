package ru.yandex.market.tpl.core.domain.yago;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryService;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrack;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackRequest;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceType;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryType;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.SurveyType;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.Car;
import ru.yandex.market.logistic.gateway.common.model.delivery.Courier;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderStatusType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Person;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.entities.restricted.CreateOrderRestrictedData;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskStatus;
import ru.yandex.market.tpl.api.model.tracking.TrackingCancelOrderDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingOrderCancelReason;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.ds.client.TplLgwClient;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.ds.YandexGoDsOrderLifecycle;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.routing.async.yago.CreateYandexGoOrderTasksProducer;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.tracking.TrackingRepository;
import ru.yandex.market.tpl.core.service.delivery.tracker.DeliveryTrackService;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus.NOT_DELIVERED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.UPDATE_YANDEX_GO_COURIER_ENABLED;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class YandexGoOrderLifecycleTest {

    /**
     * Идентификатор TPL в системе Delivery Tracker-а.
     */
    private static final long CONSUMER_ID = 4L;

    private static final Person EXTERNAL_PERSON = new Person("Василий", "Петров", null);
    private static final Courier EXTERNAL_COURIER =
            new Courier.CourierBuilder(List.of(EXTERNAL_PERSON))
                    .setCar(new Car("А001АА 01", "Лада ларгус"))
                    .setPhone(new Phone("89295472773", null))
                    .build();

    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper userShiftCommandHelper;

    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftRepository userShiftRepository;
    private final DeliveryTrackService deliveryTrackService;
    private final YandexGoOrderRepository yandexGoOrderRepository;
    private final OrderRepository orderRepository;
    private final Clock clock;
    private final CreateYandexGoOrderTasksProducer createYandexGoOrderTasksProducer;
    private final YandexGoDsOrderLifecycle yandexGoDsOrderLifecycle;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final ConfigurationService configurationService;
    private final TrackingService trackingService;
    private final TrackingRepository trackingRepository;
    private final OrderCommandService orderCommandService;

    @MockBean
    private YandexGoOrderProperties props;

    private final DeliveryClient lgwDeliveryClient;

    @MockBean
    private TrackerApiClient trackerApiClient;

    @MockBean
    private TplLgwClient tplLgwClient;

    private User user;

    private Long userShiftId;

    List<Order> orders;

    @BeforeEach
    void initTest() {
        configurationService.insertValue(UPDATE_YANDEX_GO_COURIER_ENABLED.name(), true);

        user = userHelper.findOrCreateUser(456321L);
        orders = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            OrderGenerateService.OrderGenerateParam orderParams =
                    OrderGenerateService.OrderGenerateParam.builder()
                            .deliveryDate(LocalDate.now(clock))
                            .flowStatus(OrderFlowStatus.SORTING_CENTER_CREATED)
                            .build();
            Order order = orderGenerateService.createOrder(orderParams);
            orders.add(order);
        }

        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        UserShift userShift = createUserShift(user, shift, Instant.now(clock), orders);
        userShiftCommandService.switchActiveUserShift(user, userShift.getId());
        this.userShiftId = userShift.getId();
        mockProps(getUserShift(this.userShiftId));
        mockLgwResponseUpdateCourier();
    }

    @ParameterizedTest
    @MethodSource("getTestParams")
    void testHappyPath(boolean cancellationEnabled) {
        setupTestcase(cancellationEnabled);
        prepareOrderInSc(0, 1, 2);
        shouldRunDbQueueTasks();
        shouldCreateYandexGoOrder_0();
        shouldYandexGoOrderBeConfirmed(0);
        shouldCreateCargoOrder_1();
        shouldCreateCargoOrder_2();
        shouldConfirmCargoOrder_1_2();
        shouldFoundUser();
        shouldGoingToSender();
        shouldArriveAtPickupPoint();
        shouldReceiveOrders();
        shouldDeliveringOrders();
        shouldOrderBeDelivered(0);
        shouldDeliveryOrder_1();
        shouldLastOrderBeDelivered(2);
        finishUserShift();
        shouldYandexGoOrdersComplete();
    }

    private void prepareOrderInSc(int... orderIndices) {
        for (int orderIndex : orderIndices) {
            Long orderId = getOrders().get(orderIndex).getId();
            orderCommandService.updateFlowStatusFromSc(
                    new OrderCommand.UpdateFlowStatus(orderId, OrderFlowStatus.SORTING_CENTER_ARRIVED));
            orderCommandService.updateFlowStatusFromSc(
                    new OrderCommand.UpdateFlowStatus(orderId, OrderFlowStatus.SORTING_CENTER_PREPARED));
        }
    }

    /**
     * Тестируется сценарий, когда события по двум заказам уже пришли в Курьерку,
     * а по третьему заказу события приходят позже
     *
     * @param cancellationEnabled
     */
    @ParameterizedTest
    @MethodSource("getTestParams")
    void testHappyPath_ifLateIncomingEventsOfAnOrder(boolean cancellationEnabled) {
        setupTestcase(cancellationEnabled);

        prepareOrderInSc(0, 1, 2);
        shouldRunDbQueueTasks();

        assertUsersBeSearching();

        shouldYandexGoOrderBeCreated(0);
        shouldYandexGoOrderBeCreated(1);
        shouldYandexGoOrderBeCreated(2);

        shouldYandexGoOrderBeConfirmed(0);
        shouldYandexGoOrderBeConfirmed(1);

        shouldUserBeFound(0);
        assertUserUpdatedInSc();
        shouldUserBeFound(1);

        shouldUserBeGoingToPickupPoint(0);
        shouldUserBeGoingToPickupPoint(1);

        shouldUserArriveAtPickupPoint(0);
        shouldUserArriveAtPickupPoint(1);

        shouldUserReceiveOrder(0);
        shouldUserReceiveOrder(1);

        shouldYandexGoOrderBeDelivering(0);
        shouldPickupFinished();
        shouldYandexGoOrderBeDelivering(1);

        shouldOrderBeDelivered(0);
        shouldOrderBeDelivered(1);

        shouldYandexGoOrderBeConfirmedOnTask(2); // late incoming events
        shouldUserBeFoundOnTask(2);
        shouldUserBeGoingToPickupPointOnTask(2);
        shouldUserArriveAtPickupPointOnTask(2);
        shouldUserReceiveOrderOnTask(2);
        shouldYandexGoOrderBeDelivering(2);
        shouldLastOrderBeDelivered(2);
        finishUserShift();
        shouldYandexGoOrdersComplete();
    }

    @ParameterizedTest
    @MethodSource("getTestParams")
    void testCancelOrder_whenCancelOrder_0_ByCustomerOnTrackingPage_ifOrderIsDelivering(boolean cancellationEnabled) {
        setupTestcase(cancellationEnabled);

        prepareOrderInSc(0, 1, 2);
        shouldRunDbQueueTasks();
        shouldCreateYandexGoOrder_0();
        shouldYandexGoOrderBeConfirmed(0);
        shouldCreateCargoOrder_1();
        shouldCreateCargoOrder_2();
        shouldConfirmCargoOrder_1_2();
        shouldFoundUser();
        shouldGoingToSender();
        shouldArriveAtPickupPoint();
        shouldReceiveOrders();
        shouldDeliveringOrders();

        cancelOrderByTpl(0);
        if (props.isCancellationSupported()) {
            cancelOrderByYandexGo(0);
        }

        deliverOrder(1);
        deliverOrder(2);

        assertShiftStatus(UserShiftStatus.ON_TASK);

        finishReturnTask(0);

        finishUserShift();

        shouldYandexGoOrdersComplete();
    }

    @ParameterizedTest
    @MethodSource("getTestParams")
    void testCancelOrder_whenCancelOrders_0_1_ByCustomerOnTrackingPage_ifOrderIsDelivering(boolean cancellationEnabled) {
        setupTestcase(cancellationEnabled);

        prepareOrderInSc(0, 1, 2);
        shouldRunDbQueueTasks();
        shouldCreateYandexGoOrder_0();
        shouldYandexGoOrderBeConfirmed(0);
        shouldCreateCargoOrder_1();
        shouldCreateCargoOrder_2();
        shouldConfirmCargoOrder_1_2();
        shouldFoundUser();
        shouldGoingToSender();
        shouldArriveAtPickupPoint();
        shouldReceiveOrders();
        shouldDeliveringOrders();
        cancelOrderByTpl(0);
        if (props.isCancellationSupported()) {
            cancelOrderByYandexGo(0);
        }
        cancelOrderByTpl(1);
        if (props.isCancellationSupported()) {
            cancelOrderByYandexGo(1);
        }
        deliverOrder(2);
        assertShiftStatus(UserShiftStatus.ON_TASK);
        finishReturnTask(0, 1);
        finishUserShift();
        shouldYandexGoOrdersComplete();
    }

    @ParameterizedTest
    @MethodSource("getTestParams")
    void testCancelOrder_whenCancelAllOrdersByCustomerOnTrackingPage_ifOrderIsDelivering(boolean cancellationEnabled) {
        setupTestcase(cancellationEnabled);

        prepareOrderInSc(0, 1, 2);
        shouldRunDbQueueTasks();
        shouldCreateYandexGoOrder_0();
        shouldYandexGoOrderBeConfirmed(0);
        shouldCreateCargoOrder_1();
        shouldCreateCargoOrder_2();
        shouldConfirmCargoOrder_1_2();
        shouldFoundUser();
        shouldGoingToSender();
        shouldArriveAtPickupPoint();
        shouldReceiveOrders();
        shouldDeliveringOrders();
        cancelOrderByTpl(0);
        cancelOrderByTpl(1);
        cancelOrderByTpl(2);
        if (props.isCancellationSupported()) {
            cancelOrderByYandexGo(0);
            cancelOrderByYandexGo(1);
            cancelOrderByYandexGo(2);
        }
        assertShiftStatus(UserShiftStatus.ON_TASK);
        finishReturnTask(0, 1, 2);
        finishUserShift();
        shouldYandexGoOrdersComplete();
    }

    @ParameterizedTest
    @MethodSource("getTestParams")
    void testCancelOrder_whenCancelOrder_0_ByYandexGo_ifOrderIsDelivering(boolean cancellationEnabled) {
        setupTestcase(cancellationEnabled);

        prepareOrderInSc(0, 1, 2);
        shouldRunDbQueueTasks();
        shouldCreateYandexGoOrder_0();
        shouldYandexGoOrderBeConfirmed(0);
        shouldCreateCargoOrder_1();
        shouldCreateCargoOrder_2();
        shouldConfirmCargoOrder_1_2();
        shouldFoundUser();
        shouldGoingToSender();
        shouldArriveAtPickupPoint();
        shouldReceiveOrders();
        shouldDeliveringOrders();

        cancelOrderByYandexGo(0);

        assertOrderNotDelivered(0);

        deliverOrder(1);
        deliverOrder(2);

        assertShiftStatus(UserShiftStatus.ON_TASK);

        finishReturnTask(0);

        finishUserShift();

        shouldYandexGoOrdersComplete();
    }

    private void assertOrderNotDelivered(int orderIndex) {
        assertThat(getOrders().get(orderIndex).getDeliveryStatus()).isEqualTo(NOT_DELIVERED);
    }

    @ParameterizedTest
    @MethodSource("getTestParams")
    void testCancelOrder_whenCancelOrder_0_1_ByYandexGo_ifOrderIsDelivering(boolean cancellationEnabled) {
        setupTestcase(cancellationEnabled);

        prepareOrderInSc(0, 1, 2);
        shouldRunDbQueueTasks();
        shouldCreateYandexGoOrder_0();
        shouldYandexGoOrderBeConfirmed(0);
        shouldCreateCargoOrder_1();
        shouldCreateCargoOrder_2();
        shouldConfirmCargoOrder_1_2();
        shouldFoundUser();
        shouldGoingToSender();
        shouldArriveAtPickupPoint();
        shouldReceiveOrders();
        shouldDeliveringOrders();

        cancelOrderByYandexGo(0);
        cancelOrderByYandexGo(1);

        deliverOrder(2);

        assertShiftStatus(UserShiftStatus.ON_TASK);

        finishReturnTask(0, 1);

        finishUserShift();

        shouldYandexGoOrdersComplete();
    }

    @ParameterizedTest
    @MethodSource("getTestParams")
    void testCancelOrder_whenCancelOrder_0_1_2_ByYandexGo_ifOrderIsDelivering(boolean cancellationEnabled) {
        setupTestcase(cancellationEnabled);

        prepareOrderInSc(0, 1, 2);
        shouldRunDbQueueTasks();
        shouldCreateYandexGoOrder_0();
        shouldYandexGoOrderBeConfirmed(0);
        shouldCreateCargoOrder_1();
        shouldCreateCargoOrder_2();
        shouldConfirmCargoOrder_1_2();
        shouldFoundUser();
        shouldGoingToSender();
        shouldArriveAtPickupPoint();
        shouldReceiveOrders();
        shouldDeliveringOrders();

        cancelOrderByYandexGo(0);
        cancelOrderByYandexGo(1);
        cancelOrderByYandexGo(2);

        assertShiftStatus(UserShiftStatus.ON_TASK);

        finishReturnTask(0, 1, 2);

        finishUserShift();

        shouldYandexGoOrdersComplete();
    }

    @ParameterizedTest
    @MethodSource("getTestParams")
    void testCancelOrder_whenCancelOrder_0_ByYandexGo_1_ByTpl_ifOrderIsDelivering(boolean cancellationEnabled) {
        setupTestcase(cancellationEnabled);

        prepareOrderInSc(0, 1, 2);
        shouldRunDbQueueTasks();
        shouldCreateYandexGoOrder_0();
        shouldYandexGoOrderBeConfirmed(0);
        shouldCreateCargoOrder_1();
        shouldCreateCargoOrder_2();
        shouldConfirmCargoOrder_1_2();
        shouldFoundUser();
        shouldGoingToSender();
        shouldArriveAtPickupPoint();
        shouldReceiveOrders();
        shouldDeliveringOrders();

        cancelOrderByYandexGo(0);

        cancelOrderByTpl(1);
        if (props.isCancellationSupported()) {
            cancelOrderByYandexGo(1);
        }

        deliverOrder(2);

        assertShiftStatus(UserShiftStatus.ON_TASK);

        finishReturnTask(0, 1);

        finishUserShift();

        shouldYandexGoOrdersComplete();
    }

    @ParameterizedTest
    @MethodSource("getTestParams")
    void testCancelOrder_whenOrderWasNotArrivedToSortingCenter(boolean cancellationEnabled) {
        setupTestcase(cancellationEnabled);

        prepareOrderInSc(0, 1);
        shouldRunDbQueueTasks();

        assertUsersBeSearching();

        shouldYandexGoOrderBeCreated(0);
        shouldYandexGoOrderBeCreated(1);
        shouldYandexGoOrderBeCreated(2);

        shouldYandexGoOrderBeConfirmed(0);
        shouldYandexGoOrderBeConfirmed(1);
        shouldYandexGoOrderBeConfirmed(2);

        shouldUserBeFound(0);
        assertUserUpdatedInSc();
        shouldUserBeFound(1);
        shouldUserBeFound(2);

        shouldUserBeGoingToPickupPoint(0);
        shouldUserBeGoingToPickupPoint(1);
        shouldUserBeGoingToPickupPoint(2);

        shouldUserArriveAtPickupPoint(0);
        shouldUserArriveAtPickupPoint(1);
        shouldUserArriveAtPickupPoint(2);

        shouldUserReceiveOrder(0);
        shouldUserReceiveOrder(1);
        shouldUserReceiveOrder(2);

        shouldYandexGoOrderBeDelivering(0);
        if (cancellationEnabled) {
            cancelOrderByYandexGo(2);
        }
        shouldPickupFinished();
        shouldYandexGoOrderBeDelivering(1);

        shouldOrderBeDelivered(0);
        shouldLastOrderBeDelivered(1);
        finishUserShift();
        shouldYandexGoOrdersComplete();
    }

    @Test
    void testRetryOrderCreation() {
        prepareOrderInSc(0, 1, 2);
        shouldRunDbQueueTasks();

        assertUsersBeSearching();

        shouldRetryToSendCreationRequest(0, false);
        shouldYandexGoOrderBeCreated(0);
        shouldRetryToSendCreationRequest(0, true);
        shouldYandexGoOrderBeCreated(1);
        shouldYandexGoOrderBeCreated(2);

        shouldYandexGoOrderBeConfirmed(0);
        shouldYandexGoOrderBeConfirmed(1);
        shouldYandexGoOrderBeConfirmed(2);

        shouldUserBeFound(0);
        assertUserUpdatedInSc();
        shouldUserBeFound(1);
        shouldUserBeFound(2);

        shouldUserBeGoingToPickupPoint(0);
        shouldUserBeGoingToPickupPoint(1);
        shouldUserBeGoingToPickupPoint(2);

        shouldUserArriveAtPickupPoint(0);
        shouldUserArriveAtPickupPoint(1);
        shouldUserArriveAtPickupPoint(2);

        shouldUserReceiveOrder(0);
        shouldUserReceiveOrder(1);
        shouldUserReceiveOrder(2);

        shouldYandexGoOrderBeDelivering(0);
        shouldPickupFinished();
        shouldYandexGoOrderBeDelivering(1);
        shouldYandexGoOrderBeDelivering(2);

        shouldOrderBeDelivered(0);
        shouldOrderBeDelivered(1);
        shouldLastOrderBeDelivered(2);
        finishUserShift();
        shouldYandexGoOrdersComplete();
    }

    @Test
    void testRetryCancelOrder() {
        doReturn(Duration.ZERO).when(props).getRetryDelay();
        setupTestcase(true);

        prepareOrderInSc(0, 1, 2);
        shouldRunDbQueueTasks();

        shouldYandexGoOrderBeCreated(0);
        shouldYandexGoOrderBeCreated(1);
        shouldYandexGoOrderBeCreated(2);

        shouldYandexGoOrderBeConfirmed(0);
        shouldYandexGoOrderBeConfirmed(1);
        shouldYandexGoOrderBeConfirmed(2);

        shouldUserBeFound(0);
        assertUserUpdatedInSc();
        shouldUserBeFound(1);
        shouldUserBeFound(2);

        shouldUserBeGoingToPickupPoint(0);
        shouldUserBeGoingToPickupPoint(1);
        shouldUserBeGoingToPickupPoint(2);

        shouldUserArriveAtPickupPoint(0);
        shouldUserArriveAtPickupPoint(1);
        shouldUserArriveAtPickupPoint(2);

        shouldUserReceiveOrder(0);
        shouldUserReceiveOrder(1);
        shouldUserReceiveOrder(2);

        shouldYandexGoOrderBeDelivering(0);
        shouldPickupFinished();
        shouldYandexGoOrderBeDelivering(1);
        shouldYandexGoOrderBeDelivering(2);

        cancelOrderByYandexGo(0);

        cancelOrderByTpl(1);
        shouldRetryToSendCancellationRequest(1);
        shouldRetryToSendCancellationRequest(1);
        cancelOrderByYandexGo(1);

        deliverOrder(2);

        finishReturnTask(0, 1);
        finishUserShift();
        shouldYandexGoOrdersComplete();
    }

    @Test
    void testOrderDeliveryChange_arriveToTheSameRoutePointTwice() {
        prepareOrderInSc(0, 1, 2);
        shouldRunDbQueueTasks();

        assertUsersBeSearching();

        shouldYandexGoOrderBeCreated(0);
        shouldYandexGoOrderBeCreated(1);
        shouldYandexGoOrderBeCreated(2);

        shouldYandexGoOrderBeConfirmed(0);
        shouldYandexGoOrderBeConfirmed(1);
        shouldYandexGoOrderBeConfirmed(2);

        shouldUserBeFound(0);
        assertUserUpdatedInSc();
        shouldUserBeFound(1);
        shouldUserBeFound(2);

        shouldUserBeGoingToPickupPoint(0);
        shouldUserBeGoingToPickupPoint(1);
        shouldUserBeGoingToPickupPoint(2);

        shouldUserArriveAtPickupPoint(0);
        shouldUserArriveAtPickupPoint(1);
        shouldUserArriveAtPickupPoint(2);

        shouldUserReceiveOrder(0);
        shouldUserReceiveOrder(1);
        shouldUserReceiveOrder(2);

        shouldYandexGoOrderBeDelivering(0);
        shouldPickupFinished();
        shouldYandexGoOrderBeDelivering(1);
        shouldYandexGoOrderBeDelivering(2);

        shouldOrderBeDelivered(0);

        // test case (change order's sequence)
        shouldOrderBeDelivered(2);
        shouldLastOrderBeDelivered(1);

        finishUserShift();
        shouldYandexGoOrdersComplete();
    }

    private void setupTestcase(boolean cancellationEnabled) {
        doReturn(cancellationEnabled).when(props).isCancellationSupported();
    }

    private static final boolean CANCELLATION_ENABLED = true;
    private static final boolean CANCELLATION_DISABLED = false;

    static List<Arguments> getTestParams() {
        return List.of(
                arguments(CANCELLATION_ENABLED),
                arguments(CANCELLATION_DISABLED)
        );
    }


    /**
     * От Яндекс.Го приходит статус, что заказ подготовлен к возврату
     */
    private void cancelOrderByYandexGo(int orderIndex) {
        testYandexOrderStatusChange(
                orderIndex,
                OrderDeliveryCheckpointStatus.RETURN_PREPARING,
                OrderStatusType.ORDER_IS_READY_FOR_RETURN,
                UserShiftStatus.ON_TASK);
    }

    private void mockProps(UserShift userShift) {
        long deliveryServiceId = getDeliveryServiceId();
        when(props.getCompanyIds()).thenReturn(Set.of(userShift.getUser().getCompany().getId()));
        when(props.getDeliveryServiceId()).thenReturn(Optional.of(deliveryServiceId));
        when(props.requireDeliveryServiceId()).thenReturn(deliveryServiceId);
        when(props.isYandexGoCompany(any())).thenReturn(true);
    }

    private void mockLgwResponseUpdateCourier() {
        when(tplLgwClient.getCourier(any(), any(), any())).thenReturn(EXTERNAL_COURIER);
    }

    private long getDeliveryServiceId() {
        return getOrders().get(0).getDeliveryServiceId();
    }

    private UserShift createUserShift(User user, Shift shift, Instant baseExpectedArrivalTime, List<Order> orders) {
        UserShiftCommand.Create.CreateBuilder builder =
                UserShiftCommand.Create.builder().userId(user.getId()).shiftId(shift.getId());
        Instant orderArrivalTime = baseExpectedArrivalTime;

        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        NewDeliveryRoutePointData routePoint =
                userShiftCommandHelper.taskPrepaid(
                        "Address_1", orders.get(0).getId(), orderArrivalTime, false, geoPoint);
        builder = builder.routePoint(routePoint);

        routePoint =
                userShiftCommandHelper.taskPrepaid(
                        "Address_1", orders.get(1).getId(), orderArrivalTime, false, geoPoint);
        builder = builder.routePoint(routePoint);

        orderArrivalTime = orderArrivalTime.plus(30, ChronoUnit.MINUTES);

        routePoint = userShiftCommandHelper.taskPrepaid("Address_2", orders.get(2).getId(), orderArrivalTime, false);
        builder = builder.routePoint(routePoint);

        builder.mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE);

        long userShiftId = userShiftCommandService.createUserShift(builder.build());
        return userShiftRepository.findById(userShiftId).orElseThrow();
    }

    private UserShift getUserShift(long userShiftId) {
        return userShiftRepository.findById(userShiftId).orElseThrow();
    }

    /**
     * Корректно завершает возврат товаров на СЦ
     */
    private void finishReturnTask(int... returnedOrderIndices) {
        UserShift userShift = getUserShift(userShiftId);
        RoutePoint routePoint = userShift.streamReturnRoutePoints().findFirst().get();

        userShiftCommandService.arriveAtRoutePoint(userShift.getUser(),
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShiftId,
                        routePoint.getId(),
                        new LocationDto(new BigDecimal("1"), new BigDecimal("2"))));


        List<Long> returnedOrderIds = new ArrayList<>();
        for (int returnedOrderIndex : returnedOrderIndices) {
            returnedOrderIds.add(getOrders().get(returnedOrderIndex).getId());
        }

        userShiftCommandService.finishReturnOrders(
                userShift.getUser(),
                new UserShiftCommand.FinishScan(
                        userShiftId,
                        routePoint.getId(),
                        routePoint.getOrderReturnTask().getId(),
                        ScanRequest.builder().successfullyScannedOrders(returnedOrderIds).build()));

        userShiftCommandService.finishReturnTask(
                userShift.getUser(),
                new UserShiftCommand.FinishReturnTask(
                        userShiftId,
                        routePoint.getId(),
                        routePoint.getOrderReturnTask().getId()));

        for (int returnedOrderIndex : returnedOrderIndices) {
            shouldReturnOrderToSc(returnedOrderIndex);
        }

        assertThat(userShift.streamReturnRoutePoints().findFirst().get().getOrderReturnTask().getStatus())
                .isEqualTo(OrderReturnTaskStatus.FINISHED);
        assertShiftStatus(UserShiftStatus.SHIFT_CLOSED);
    }

    void shouldReturnOrderToSc(int orderIndex) {
        // given
        Order order = getOrders().get(orderIndex);
        YandexGoOrder yandexGoOrder = getYandexGoOrders().get(orderIndex);
        DeliveryTrackCheckpoint checkpoint =
                createCheckpoint(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_ARRIVED);

        DeliveryTrack track =
                createDeliveryTrack(
                        getUserShift(userShiftId).getShift().getSortingCenter().getId(),
                        order,
                        yandexGoOrder,
                        List.of(checkpoint));

        // when
        deliveryTrackService.notifyTracks(List.of(track));

        // then
    }

    private void shouldYandexGoOrdersComplete() {
        assertOrderFlowStatus(0, YandexGoOrderFlowStatus.COMPLETED);
        assertOrderFlowStatus(1, YandexGoOrderFlowStatus.COMPLETED);
        assertOrderFlowStatus(2, YandexGoOrderFlowStatus.COMPLETED);
    }

    private void assertOrderFlowStatus(int orderIndex, YandexGoOrderFlowStatus expectedFlowStatus) {
        assertThat(getYandexGoOrders().get(orderIndex).getFlowStatus()).isEqualTo(expectedFlowStatus);
    }

    /**
     * После вызова отмены заказа со стороны трекинга, заказ должен перейти в статус CANCELLED и READY_FOR_RETURN, в
     * в Такси через LGW должен быть отправлен запрос на отмену заказа
     */
    @SneakyThrows
    void cancelOrderByTpl(int orderIndex) {
        Mockito.reset(lgwDeliveryClient);
        // given
        Order order = getOrders().get(orderIndex);
        String trackId = trackingRepository.findTrackingIdByExternalOrderId(order.getExternalOrderId()).get();

        // when
        trackingService.cancelOrder(
                trackId, new TrackingCancelOrderDto(TrackingOrderCancelReason.OTHER, "cancel-comment"), null);
        dbQueueTestUtil.executeAllQueueItems(QueueType.CANCEL_YANDEX_GO_ORDER);

        // then
        assertThat(order.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.CANCELLED);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);

        if (props.isCancellationSupported()) {
            YandexGoOrder yandexGoOrder = getYandexGoOrders().get(orderIndex);
            ArgumentCaptor<ResourceId> cancelledOrderResourceId = ArgumentCaptor.forClass(ResourceId.class);
            ArgumentCaptor<Partner> cancellingPartner = ArgumentCaptor.forClass(Partner.class);
            verify(lgwDeliveryClient).cancelOrder(cancelledOrderResourceId.capture(), cancellingPartner.capture());
            assertThat(cancelledOrderResourceId.getValue().getYandexId()).isEqualTo(order.getExternalOrderId());
            assertThat(cancelledOrderResourceId.getValue().getPartnerId())
                    .isEqualTo(yandexGoOrder.getExternalYandexGoOrderId());
        }
    }

    /**
     * Отработали задачи по созданию заказов в Яндекс.Го,
     * должны создаться заказы YandexGoOrder у нас в системе,
     * должны быть отправлены запросы на создание заказов в Яндекс.Го,
     * смена должна перейти в статус поиска курьеров
     */
    @SneakyThrows
    void shouldRunDbQueueTasks() {
        Mockito.reset(lgwDeliveryClient);

        // given
        UserShift userShift = userShiftRepository.findByIdOrThrow(userShiftId);

        // when
        createYandexGoOrderTasksProducer.produce(userShift.getShift().getId());
        dbQueueTestUtil.executeAllQueueItems(QueueType.CREATE_YANDEX_GO_ORDER_TASKS);
        dbQueueTestUtil.executeAllQueueItems(QueueType.CREATE_YANDEX_GO_ORDER);

        // then
        List<Long> actualYandexGoOrderOrderIds =
                getYandexGoOrders().stream()
                        .map(YandexGoOrder::getOrderId)
                        .collect(Collectors.toList());
        List<Long> expectedOrderIds = getOrders().stream().map(Order::getId).collect(Collectors.toList());
        assertThat(actualYandexGoOrderOrderIds).containsExactlyInAnyOrderElementsOf(expectedOrderIds);
        ArgumentCaptor<CreateOrderRestrictedData> createOrderRestrictedDataArgumentCaptor =
                ArgumentCaptor.forClass(CreateOrderRestrictedData.class);
        verify(lgwDeliveryClient, times(3))
                .createOrder(
                        any(),
                        any(),
                        createOrderRestrictedDataArgumentCaptor.capture(),
                        eq(new ClientRequestMeta(String.valueOf(userShiftId))));
        assertThat(createOrderRestrictedDataArgumentCaptor.getValue().getYandexGoData().getRouteId())
                .isEqualTo(String.valueOf(userShiftId));
        assertUsersBeSearching();
    }

    private void assertUsersBeSearching() {
        assertShiftStatus(UserShiftStatus.USER_SEARCH);
    }

    /**
     * От Яндекс.Го пришла нотификация createOrderSuccess, что заказ_0 успешно создан.
     * Ожидается, что заказ в Яндекс.Го начинает отслеживаться в Delivery Tracker
     */
    void shouldCreateYandexGoOrder_0() {
        shouldYandexGoOrderBeCreated(0);
    }

    /**
     * От Яндекс.Го пришла нотификация, что заказ_0 подтвержден.
     * Ожидается, что YandexGoOrder переходит в статус ORDER_CREATED
     */
    void shouldYandexGoOrderBeConfirmed(int orderIndex) {
        testYandexOrderStatusChange(
                orderIndex,
                OrderDeliveryCheckpointStatus.DELIVERY_LOADED,
                OrderStatusType.ORDER_CREATED,
                UserShiftStatus.USER_SEARCH);
    }

    void shouldYandexGoOrderBeConfirmedOnTask(int orderIndex) {
        testYandexOrderStatusChange(
                orderIndex,
                OrderDeliveryCheckpointStatus.DELIVERY_LOADED,
                OrderStatusType.ORDER_CREATED,
                UserShiftStatus.ON_TASK);
    }

    void shouldRetryToSendCreationRequest(int orderIndex, boolean orderAlreadyCreated) {
        // given
        String externalYandexGoOrderId = "external-yandexgo-order-id-" + orderIndex;
        YandexGoOrder yandexGoOrder = getYandexGoOrders().get(orderIndex);
        mockTrackerApiClient(yandexGoOrder, externalYandexGoOrderId);

        // when
        String externalOrderId = getOrders().get(orderIndex).getExternalOrderId();
        yandexGoDsOrderLifecycle.failCreateOrder(yandexGoOrder.getUserShiftId(), externalOrderId);

        // then
        if (orderAlreadyCreated) {
            assertThat(yandexGoOrder.getTrackId()).isEqualTo(externalYandexGoOrderId);
        } else {
            assertThat(yandexGoOrder.getTrackId()).isNull();
        }
    }


    @SneakyThrows
    void shouldRetryToSendCancellationRequest(int orderIndex) {
        Mockito.reset(lgwDeliveryClient);
        // given
        Order order = getOrders().get(orderIndex);
        YandexGoOrder yandexGoOrder = getYandexGoOrders().get(orderIndex);

        // when
        yandexGoDsOrderLifecycle.failCancelOrder(
                yandexGoOrder.getUserShiftId(), order.getExternalOrderId(), yandexGoOrder.getExternalYandexGoOrderId());
        dbQueueTestUtil.executeAllQueueItems(QueueType.CANCEL_YANDEX_GO_ORDER);

        // then
        assertThat(order.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.CANCELLED);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);

        if (props.isCancellationSupported()) {
            ArgumentCaptor<ResourceId> cancelledOrderResourceId = ArgumentCaptor.forClass(ResourceId.class);
            ArgumentCaptor<Partner> cancellingPartner = ArgumentCaptor.forClass(Partner.class);
            verify(lgwDeliveryClient).cancelOrder(cancelledOrderResourceId.capture(), cancellingPartner.capture());
            assertThat(cancelledOrderResourceId.getValue().getYandexId()).isEqualTo(order.getExternalOrderId());
            assertThat(cancelledOrderResourceId.getValue().getPartnerId())
                    .isEqualTo(yandexGoOrder.getExternalYandexGoOrderId());
        }

    }

    /**
     * От Яндекс.Го пришла нотификация createOrderSuccess, что заказ_1 успешно создан.
     * Ожидается, что заказ в Яндекс.Го начинает отслеживаться в Delivery Tracker
     */
    void shouldCreateCargoOrder_1() {
        shouldYandexGoOrderBeCreated(1);
    }

    /**
     * От Яндекс.Го пришла нотификация createOrderSuccess, что заказ_2 успешно создан.
     * Ожидается, что заказ в Яндекс.Го начинает отслеживаться в Delivery Tracker
     */
    void shouldCreateCargoOrder_2() {
        shouldYandexGoOrderBeCreated(2);
    }

    /**
     * От Яндекс.Го пришла нотификация, что заказ_1 и заказ_2 подтверждены.
     * Ожидается, что YandexGoOrders переходят в статус ORDER_CREATED
     */
    void shouldConfirmCargoOrder_1_2() {
        testYandexOrderStatusChange(
                1,
                OrderDeliveryCheckpointStatus.DELIVERY_LOADED,
                OrderStatusType.ORDER_CREATED,
                UserShiftStatus.USER_SEARCH);
        testYandexOrderStatusChange(
                2,
                OrderDeliveryCheckpointStatus.DELIVERY_LOADED,
                OrderStatusType.ORDER_CREATED,
                UserShiftStatus.USER_SEARCH);
    }

    /**
     * От Яндекс.Го пришла нотификация, что по заказам курьер найден.
     * Ожидается, что YandexGoOrder переходит в статус COURIER_FOUND, а смены - в статус
     * USER_FOUND.
     */
    void shouldFoundUser() {
        shouldUserBeFound(0);
        assertUserUpdatedInSc();
        shouldUserBeFound(1);
        shouldUserBeFound(2);
    }

    private void shouldUserBeFound(int orderIndex) {
        testYandexOrderStatusChange(
                orderIndex,
                OrderDeliveryCheckpointStatus.DELIVERY_COURIER_FOUND,
                OrderStatusType.COURIER_FOUND,
                UserShiftStatus.USER_FOUND);
    }

    private void shouldUserBeFoundOnTask(int orderIndex) {
        testYandexOrderStatusChange(
                orderIndex,
                OrderDeliveryCheckpointStatus.DELIVERY_COURIER_FOUND,
                OrderStatusType.COURIER_FOUND,
                UserShiftStatus.ON_TASK);
    }

    void shouldGoingToSender() {
        shouldUserBeGoingToPickupPoint(0);
        shouldUserBeGoingToPickupPoint(1);
        shouldUserBeGoingToPickupPoint(2);
    }

    /**
     * От Яндекс.Го пришла нотификация, что курьер едет за заказом
     * Ожидается, что YandexGoOrder переходит в статус COURIER_IN_TRANSIT_TO_SENDER
     */
    private void shouldUserBeGoingToPickupPoint(int orderIndex) {
        testYandexOrderStatusChange(
                orderIndex,
                OrderDeliveryCheckpointStatus.DELIVERY_COURIER_IN_TRANSIT_TO_SENDER,
                OrderStatusType.COURIER_IN_TRANSIT_TO_SENDER,
                UserShiftStatus.USER_FOUND);
    }

    private void shouldUserBeGoingToPickupPointOnTask(int orderIndex) {
        testYandexOrderStatusChange(
                orderIndex,
                OrderDeliveryCheckpointStatus.DELIVERY_COURIER_IN_TRANSIT_TO_SENDER,
                OrderStatusType.COURIER_IN_TRANSIT_TO_SENDER,
                UserShiftStatus.ON_TASK);
    }

    void shouldArriveAtPickupPoint() {
        shouldUserArriveAtPickupPoint(0);
        shouldUserArriveAtPickupPoint(1);
        shouldUserArriveAtPickupPoint(2);
    }

    /**
     * От Яндекс.Го пришла нотификация, что курьер приехал в точку получения
     * Ожидается, что YandexGoOrder переходит в статус COURIER_IN_TRANSIT_TO_SENDER
     */
    private void shouldUserArriveAtPickupPoint(int orderIndex) {
        testYandexOrderStatusChange(
                orderIndex,
                OrderDeliveryCheckpointStatus.DELIVERY_COURIER_ARRIVED_TO_SENDER,
                OrderStatusType.COURIER_ARRIVED_TO_SENDER,
                UserShiftStatus.USER_FOUND);
    }

    private void shouldUserArriveAtPickupPointOnTask(int orderIndex) {
        testYandexOrderStatusChange(
                orderIndex,
                OrderDeliveryCheckpointStatus.DELIVERY_COURIER_ARRIVED_TO_SENDER,
                OrderStatusType.COURIER_ARRIVED_TO_SENDER,
                UserShiftStatus.ON_TASK);
    }

    /**
     * От Яндекс.Го пришла нотификация, что курьер забрал заказ
     * Ожидается, что YandexGoOrder переходит в статус COURIER_RECEIVED_ORDER
     */
    void shouldReceiveOrders() {
        shouldUserReceiveOrder(0);
        shouldUserReceiveOrder(1);
        shouldUserReceiveOrder(2);
    }

    private void shouldUserReceiveOrder(int orderIndex) {
        testYandexOrderStatusChange(
                orderIndex,
                OrderDeliveryCheckpointStatus.DELIVERY_COURIER_RECEIVED,
                OrderStatusType.COURIER_RECEIVED_ORDER,
                UserShiftStatus.USER_FOUND);
    }

    private void shouldUserReceiveOrderOnTask(int orderIndex) {
        testYandexOrderStatusChange(
                orderIndex,
                OrderDeliveryCheckpointStatus.DELIVERY_COURIER_RECEIVED,
                OrderStatusType.COURIER_RECEIVED_ORDER,
                UserShiftStatus.ON_TASK);
        assertOrderStatus(orderIndex, OrderFlowStatus.TRANSPORTATION_RECIPIENT);
    }

    /**
     * От Яндекс.Го пришла нотификация, что курьер выехал к получателю
     * Ожидается, что YandexGoOrder переходит в статус COURIER_RECEIVED_ORDER
     * после получения статуса по последнему заказу задача забора заказов закрывается
     */
    void shouldDeliveringOrders() {
        shouldYandexGoOrderBeDelivering(0);
        shouldYandexGoOrderBeDelivering(1);
        shouldYandexGoOrderBeDelivering(2);

        shouldPickupFinished();

        assertAllOrdersAreBeingTransportedToRecipients();
    }

    private void assertAllOrdersAreBeingTransportedToRecipients() {
        assertOrderStatus(1, OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        assertOrderStatus(2, OrderFlowStatus.TRANSPORTATION_RECIPIENT);
    }

    private void shouldPickupFinished() {
        UserShift userShift = getUserShift(userShiftId);
        assertThat(userShift.streamPickupRoutePoints().findFirst().get().getStatus())
                .isEqualTo(RoutePointStatus.FINISHED);
    }

    private void shouldYandexGoOrderBeDelivering(int orderIndex) {
        testYandexOrderStatusChange(
                orderIndex,
                OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT,
                OrderStatusType.ORDER_IS_BEING_DELIVERED_TO_RECIPIENT,
                UserShiftStatus.ON_TASK);
        assertOrderDeliveryTaskStatus(orderIndex, OrderDeliveryTaskStatus.NOT_DELIVERED);
        assertOrderStatus(orderIndex, OrderFlowStatus.TRANSPORTATION_RECIPIENT);
    }

    /**
     * От Яндекс.Го пришла нотификация, что курьер доставил заказ_0
     * Ожидается, что YandexGoOrder переходит в статус DELIVERY_DELIVERED,
     * заказ и задача по его доставки отмечаются как выполненные
     */
    void shouldOrderBeDelivered(int orderIndex) {
        testYandexOrderStatusChange(
                orderIndex,
                OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT,
                OrderStatusType.ORDER_TRANSMITTED_TO_RECIPIENT,
                UserShiftStatus.ON_TASK);
        assertOrderDeliveryTaskStatus(orderIndex, OrderDeliveryTaskStatus.DELIVERED);
        assertOrderStatus(orderIndex, OrderFlowStatus.TRANSMITTED_TO_RECIPIENT);
    }


    /**
     * От Яндекс.Го пришла нотификация, что курьер доставил заказ
     * Ожидается, что YandexGoOrder переходит в статус DELIVERY_DELIVERED,
     * заказ и задача по его доставки отмечаются как выполненные
     */
    void deliverOrder(int orderIndex) {
        testYandexOrderStatusChange(
                orderIndex,
                OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT,
                OrderStatusType.ORDER_TRANSMITTED_TO_RECIPIENT,
                UserShiftStatus.ON_TASK);
        assertOrderDeliveryTaskStatus(orderIndex, OrderDeliveryTaskStatus.DELIVERED);
        assertOrderStatus(orderIndex, OrderFlowStatus.TRANSMITTED_TO_RECIPIENT);
    }

    /**
     * От Яндекс.Го пришла нотификация, что курьер выехал к получателю
     * Ожидается, что YandexGoOrder переходит в статус COURIER_RECEIVED_ORDER,
     * после получения статуса по последнему заказу задача забора заказов закрывается
     */
    void shouldDeliveryOrder_1() {
        testYandexOrderStatusChange(
                1,
                OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT,
                OrderStatusType.ORDER_TRANSMITTED_TO_RECIPIENT,
                UserShiftStatus.ON_TASK);
        assertOrderDeliveryTaskStatus(1, OrderDeliveryTaskStatus.DELIVERED);
        assertOrderStatus(1, OrderFlowStatus.TRANSMITTED_TO_RECIPIENT);
    }

    /**
     * От Яндекс.Го пришла нотификация, что курьер доставил заказ
     * Ожидается, что YandexGoOrder переходит в статус DELIVERY_DELIVERED,
     * заказ и задача по его доставки отмечаются как выполненные,
     * а смена должна закрыться
     *
     * @param orderIndex
     */
    void shouldLastOrderBeDelivered(int orderIndex) {
        testYandexOrderStatusChange(
                orderIndex,
                OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT,
                OrderStatusType.ORDER_TRANSMITTED_TO_RECIPIENT,
                UserShiftStatus.SHIFT_CLOSED);
        assertOrderDeliveryTaskStatus(orderIndex, OrderDeliveryTaskStatus.DELIVERED);
        assertOrderStatus(orderIndex, OrderFlowStatus.TRANSMITTED_TO_RECIPIENT);
        assertShiftStatus(UserShiftStatus.SHIFT_CLOSED);
    }

    private void assertShiftStatus(UserShiftStatus status) {
        assertThat(getUserShift(userShiftId).getStatus()).isEqualTo(status);
    }

    /**
     * После отработки завершения закрытых смен смена должна завершиться
     */
    void finishUserShift() {
        userShiftCommandService.finishUserShift(new UserShiftCommand.Finish(userShiftId));
        assertShiftStatus(UserShiftStatus.SHIFT_FINISHED);
    }

    private void assertOrderDeliveryTaskStatus(int index, OrderDeliveryTaskStatus status) {
        assertThat(getOrderDeliveryTasks(getUserShift(userShiftId)).get(index).getStatus()).isEqualTo(status);
    }

    private List<OrderDeliveryTask> getOrderDeliveryTasks(UserShift userShift) {
        return userShift.streamOrderDeliveryTasks()
                .sorted(Comparator.comparingLong(OrderDeliveryTask::getOrderId))
                .collect(Collectors.toList());
    }

    private void testYandexOrderStatusChange(
            int orderIndex,
            OrderDeliveryCheckpointStatus incomingYandexGoOrderStatus,
            OrderStatusType expectedYandexGoOrderStatus,
            UserShiftStatus expectedUserShiftStatus) {
        // given
        List<YandexGoOrder> yandexGoOrders = getYandexGoOrders();
        YandexGoOrder yandexGoOrder0 = yandexGoOrders.get(orderIndex);

        DeliveryTrackCheckpoint checkpoint = createCheckpoint(incomingYandexGoOrderStatus);
        Order order0 = getOrders().get(orderIndex);

        DeliveryTrack track =
                createDeliveryTrack(getDeliveryServiceId(), order0, yandexGoOrder0, List.of(checkpoint));

        // when
        deliveryTrackService.notifyTracks(List.of(track));

        // then
        assertThat(yandexGoOrder0.getStatus()).isEqualTo(expectedYandexGoOrderStatus);
        assertShiftStatus(expectedUserShiftStatus);
    }

    private void assertOrderStatus(int index, OrderFlowStatus status) {
        assertThat(getOrders().get(index).getOrderFlowStatus()).isEqualTo(status);
    }

    private void shouldYandexGoOrderBeCreated(int orderIndex) {
        // given
        String externalYandexGoOrderId = "external-yandexgo-order-id-" + orderIndex;
        YandexGoOrder yandexGoOrder = getYandexGoOrders().get(orderIndex);
        mockTrackerApiClient(yandexGoOrder, externalYandexGoOrderId);

        // when
        String externalOrderId = getOrders().get(orderIndex).getExternalOrderId();
        yandexGoDsOrderLifecycle.confirmCreateOrder(
                yandexGoOrder.getUserShiftId(),
                externalOrderId,
                externalYandexGoOrderId);

        // then
        assertThat(yandexGoOrder.getTrackId()).isEqualTo(externalYandexGoOrderId);
    }

    private void mockTrackerApiClient(YandexGoOrder yandexGoOrder, String externalYandexGoOrderId) {
        when(trackerApiClient.registerDeliveryTrack(
                eq(DeliveryTrackRequest.builder()
                        .trackCode(externalYandexGoOrderId)
                        .deliveryServiceId(getDeliveryServiceId())
                        .consumerId(CONSUMER_ID)
                        .entityId(yandexGoOrder.getExternalOrderId())
                        .deliveryType(DeliveryType.DELIVERY)
                        .isGlobalOrder(false)
                        .entityType(EntityType.ORDER)
                        .apiVersion(ApiVersion.DS)
                        .build()
                )
        ))
                .thenReturn(mock(DeliveryTrackMeta.class));
    }

    private List<Order> getOrders() {
        List<Order> orders = orderRepository.findAll();
        orders.sort(Comparator.comparingLong(Order::getId));
        return orders;
    }

    private List<YandexGoOrder> getYandexGoOrders() {
        List<YandexGoOrder> yandexGoOrders = new ArrayList<>(yandexGoOrderRepository.findAll());
        yandexGoOrders.sort(Comparator.comparingLong(YandexGoOrder::getOrderId));
        return yandexGoOrders;
    }

    private DeliveryTrack createDeliveryTrack(Long partnerId, Order order0, YandexGoOrder yandexGoOrder,
                                              List<DeliveryTrackCheckpoint> checkpoints) {
        return new DeliveryTrack(
                new DeliveryTrackMeta(
                        yandexGoOrder.getTrackId(),
                        new DeliveryService(partnerId, "sorting center", DeliveryServiceType.DELIVERY),
                        order0.getExternalOrderId(),
                        EntityType.ORDER, 0L)
                        .setId(10001L),
                checkpoints);
    }

    private DeliveryTrackCheckpoint createCheckpoint(OrderDeliveryCheckpointStatus status) {
        return new DeliveryTrackCheckpoint(101, new Date(), status, SurveyType.PUSH);
    }

    private void assertUserUpdatedInSc() {
        dbQueueTestUtil.assertQueueHasSize(QueueType.UPDATE_YANDEX_GO_COURIER, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.UPDATE_YANDEX_GO_COURIER);

        List<YandexGoOrder> yandexGoOrders =
                yandexGoOrderRepository.findAllByOrderIdIn(orders.stream().map(Order::getId).collect(Collectors.toList()));

        for (YandexGoOrder yandexGoOrder : yandexGoOrders) {
            YandexGoOrder.User yandexGoOrderUser = yandexGoOrder.getUser();

            assertThat(yandexGoOrderUser.getName()).isEqualTo(EXTERNAL_PERSON.getName());
            assertThat(yandexGoOrderUser.getSurname()).isEqualTo(EXTERNAL_PERSON.getSurname());
            assertThat(yandexGoOrderUser.getPhoneNumber()).isEqualTo(EXTERNAL_COURIER.getPhone().getPhoneNumber());
            assertThat(yandexGoOrderUser.getCarDescription()).isEqualTo(EXTERNAL_COURIER.getCar().getDescription());
            assertThat(yandexGoOrderUser.getCarNumber()).isEqualTo(EXTERNAL_COURIER.getCar().getNumber());
        }
        assertThat(user.getFirstName()).isEqualTo(EXTERNAL_PERSON.getName());
        assertThat(user.getLastName()).isEqualTo(EXTERNAL_PERSON.getSurname());
        assertThat(user.getPhone()).isEqualTo(EXTERNAL_COURIER.getPhone().getPhoneNumber());
        assertThat(user.getVehicleNumber()).isEqualTo(EXTERNAL_COURIER.getCar().getNumber());
    }
}
