package ru.yandex.market.tpl.core.domain.usershift.transferact;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.transferact.TransferAct;
import ru.yandex.market.tpl.core.domain.transferact.TransferActDirection;
import ru.yandex.market.tpl.core.domain.transferact.TransferActStatus;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.RoutePointRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class UserShiftCommandServiceTransferActTest extends TplAbstractTest {

    private final TestDataFactory testDataFactory;
    private final TestUserHelper userHelper;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final UserShiftTestHelper userShiftTestHelper;
    private final RoutePointRepository routePointRepository;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final UserPropertyService userPropertyService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final DbQueueTestUtil dbQueueTestUtil;

    private User user;
    private Shift shift;
    private Long userShiftId;
    private Long pickupRoutePointId;
    private Long orderId1;
    private Long orderId2;

    @BeforeEach
    void beforeEach() {
        user = userHelper.findOrCreateUser(991L);
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        orderId1 = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build()
        ).getId();

        orderId2 = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build()
        ).getId();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskOrderPickup(clock.instant()))
                .routePoint(userShiftCommandDataHelper.taskUnpaid("addr1", 12, orderId1))
                .routePoint(userShiftCommandDataHelper.taskUnpaid("addr2", 12, orderId2))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShiftId = userShiftCommandService.createUserShift(createCommand);
        userShiftCommandService.switchActiveUserShift(user, userShiftId);
        userShiftCommandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShiftId));
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                userShift.getShift().getSortingCenter(),
                SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED,
                false
        );
        pickupRoutePointId = userShift.getCurrentRoutePoint().getId();

        clearAfterTest(user);
        clearAfterTest(shift);
    }

    @Test
    void pickupOrders_transferActEnabled() {
        transactionTemplate.execute(status -> {
            userPropertyService.addPropertyToUser(user, UserProperties.TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, true);
            return null;
        });

        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        userShiftTestHelper.arriveAtRoutePoint(userShift);
        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();

        userShiftTestHelper.startOrderPickup(userShift, pickupTask);

        userShiftCommandService.pickupOrders(user, new UserShiftCommand.FinishScan(
                userShiftId,
                pickupRoutePoint.getId(),
                pickupTask.getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(List.of(orderId1))
                        .skippedOrders(List.of(orderId2))
                        .build()
        ));

        transactionTemplate.execute(status -> {
            var pickupRoutePointResult = routePointRepository.findByIdOrThrow(pickupRoutePointId);
            var pickupTaskResult = pickupRoutePointResult.streamPickupTasks().findFirst().orElseThrow();

            assertThat(pickupTaskResult.getStatus()).isEqualTo(OrderPickupTaskStatus.TRANSFER_ACT_PROCESSING);
            assertThat(pickupTaskResult.getPickupOrderIds().size()).isEqualTo(1);
            assertThat(pickupTaskResult.getPickupOrderIds()).contains(orderId1);
            assertThat(pickupTaskResult.getSkippedOrderIds().size()).isEqualTo(1);
            assertThat(pickupTaskResult.getSkippedOrderIds()).contains(orderId2);

            assertThat(pickupTaskResult.getTransferActs().size()).isEqualTo(1);

            TransferAct transferAct = pickupTaskResult.getTransferActs().get(0);
            assertThat(transferAct.getStatus()).isEqualTo(TransferActStatus.CREATION);
            assertThat(transferAct.getDirection()).isEqualTo(TransferActDirection.RECEIVER);

            dbQueueTestUtil.assertQueueHasSize(QueueType.TRANSFER_ACT_CREATE, 1);

            return null;
        });
    }

    @Test
    void pickupOrders_transferActDisabled() {
        transactionTemplate.execute(status -> {
            userPropertyService.addPropertyToUser(user, UserProperties.TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, false);
            return null;
        });

        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        userShiftTestHelper.arriveAtRoutePoint(userShift);
        var pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();

        userShiftTestHelper.startOrderPickup(userShift, pickupTask);

        userShiftCommandService.pickupOrders(user, new UserShiftCommand.FinishScan(
                userShiftId,
                pickupRoutePoint.getId(),
                pickupTask.getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(List.of(orderId1))
                        .skippedOrders(List.of(orderId2))
                        .build()
        ));

        transactionTemplate.execute(status -> {
            var pickupRoutePointResult = routePointRepository.findByIdOrThrow(pickupRoutePointId);
            var pickupTaskResult = pickupRoutePointResult.streamPickupTasks().findFirst().orElseThrow();

            assertThat(pickupTaskResult.getStatus()).isEqualTo(OrderPickupTaskStatus.BOX_LOADING);
            dbQueueTestUtil.assertQueueHasSize(QueueType.TRANSFER_ACT_CREATE, 0);
            return null;
        });
    }

}
