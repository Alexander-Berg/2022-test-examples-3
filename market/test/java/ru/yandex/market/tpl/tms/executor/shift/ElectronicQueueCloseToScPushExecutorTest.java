package ru.yandex.market.tpl.tms.executor.shift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePointRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.UserLocation;
import ru.yandex.market.tpl.core.domain.usershift.location.UserLocationRepository;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.dbqueue.model.QueueType.PUSH_NOTIFICATION_SEND;

@RequiredArgsConstructor
class ElectronicQueueCloseToScPushExecutorTest extends TplTmsAbstractTest {

    private final Clock clock;
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandDataHelper helper;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final UserLocationRepository userLocationRepository;
    private final RoutePointRepository routePointRepository;
    private final ElectronicQueueCloseToScPushExecutor electronicQueueCloseToScPushExecutor;
    private final TransactionTemplate transactionTemplate;

    private User user;
    private Shift shift;
    private Long userShiftId;
    private Long pickupRoutePointId;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(352361L);
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskUnpaid("addr1", 12, order.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShiftId = commandService.createUserShift(createCommand);
        commandService.switchActiveUserShift(user, userShiftId);
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                userShift.getShift().getSortingCenter(),
                SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED,
                true
        );
        pickupRoutePointId = userShift.getCurrentRoutePoint().getId();
    }

    @Test
    void sendPush_closeToSc() {
        UserShift shift = userShiftRepository.findByIdOrThrow(userShiftId);
        createLocation(shift.getSortingCenter().getLatitude(), shift.getSortingCenter().getLongitude());

        electronicQueueCloseToScPushExecutor.doRealJob(null);

        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupTask.isPushSent()).isTrue();
        dbQueueTestUtil.assertQueueHasSize(PUSH_NOTIFICATION_SEND, 1);
    }

    @Test
    void sendPush_closeToScCallTwice() {
        UserShift shift = userShiftRepository.findByIdOrThrow(userShiftId);
        createLocation(shift.getSortingCenter().getLatitude(), shift.getSortingCenter().getLongitude());

        electronicQueueCloseToScPushExecutor.doRealJob(null);
        electronicQueueCloseToScPushExecutor.doRealJob(null);

        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupTask.isPushSent()).isTrue();
        dbQueueTestUtil.assertQueueHasSize(PUSH_NOTIFICATION_SEND, 1);
    }

    @Test
    void sendPush_farFromSc() {
        UserShift shift = userShiftRepository.findByIdOrThrow(userShiftId);
        createLocation(BigDecimal.ONE, BigDecimal.ONE);

        electronicQueueCloseToScPushExecutor.doRealJob(null);

        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupTask.isPushSent()).isFalse();
        dbQueueTestUtil.assertQueueHasSize(PUSH_NOTIFICATION_SEND, 0);
    }

    @Test
    void sendPush_noLocation() {
        UserShift shift = userShiftRepository.findByIdOrThrow(userShiftId);

        electronicQueueCloseToScPushExecutor.doRealJob(null);

        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupTask.isPushSent()).isFalse();
        dbQueueTestUtil.assertQueueHasSize(PUSH_NOTIFICATION_SEND, 0);
    }

    @Test
    void sendPush_finishedUserShift() {
        createLocation(shift.getSortingCenter().getLatitude(), shift.getSortingCenter().getLongitude());
        transactionTemplate.execute(status -> {
            UserShift userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            userShift.streamOrderDeliveryTasks().forEach(OrderDeliveryTask::forceFinish);
            return null;
        });
        commandService.closeShift(new UserShiftCommand.Close(userShiftId));

        electronicQueueCloseToScPushExecutor.doRealJob(null);

        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        assertThat(pickupTask.isPushSent()).isFalse();
        dbQueueTestUtil.assertQueueHasSize(PUSH_NOTIFICATION_SEND, 0);
    }

    private void createLocation(BigDecimal latitude, BigDecimal longitude) {
        var userLocation = new UserLocation();
        userLocation.setUserShiftId(userShiftId);
        userLocation.setLatitude(latitude);
        userLocation.setLongitude(longitude);
        userLocation.setUserId(user.getId());
        userLocationRepository.save(userLocation);
    }

}
