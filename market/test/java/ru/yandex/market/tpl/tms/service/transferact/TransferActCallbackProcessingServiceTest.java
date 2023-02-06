package ru.yandex.market.tpl.tms.service.transferact;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.common.transferact.client.model.TransferActCallbackDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferStatus;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.transferact.TransferAct;
import ru.yandex.market.tpl.core.domain.transferact.TransferActStatus;
import ru.yandex.market.tpl.core.domain.transferact.dbqueue.TransferActCallbackPayload;
import ru.yandex.market.tpl.core.domain.transferact.service.TransferActCancellationService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.RoutePointRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static ru.yandex.market.tpl.tms.service.usershift.distance.CalcUserShiftDistanceQueueProcessingServiceTest.REQUEST_ID;

@RequiredArgsConstructor
public class TransferActCallbackProcessingServiceTest extends TplTmsAbstractTest {

    private final TransferActCallbackProcessingService transferActCallbackProcessingService;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftRepository userShiftRepository;
    private final RoutePointRepository routePointRepository;
    private final UserPropertyService userPropertyService;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final UserShiftTestHelper userShiftTestHelper;
    private final OrderRepository orderRepository;
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final TestDataFactory testDataFactory;
    private final TransferActCancellationService transferActCancellationService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final String transferActExternalId = "test-123";
    private User user;
    private Long userShiftId;
    private Long pickupRoutePointId;
    private Long orderId1;
    private Long orderId2;

    static Stream<TransferStatus> unsupportedStatusesSource() {
        return StreamEx.of(
                TransferStatus.CANCELLED,
                TransferStatus.CREATED
        );
    }

    @BeforeEach
    void beforeEach() {
        transactionTemplate.execute(status -> {
            user = userHelper.findOrCreateUser(991L);
            userPropertyService.addPropertyToUser(user, UserProperties.TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, true);
            return null;
        });

        Shift shift = userHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), 18769);

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

        transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            sortingCenterPropertyService.upsertPropertyToSortingCenter(
                    userShift.getShift().getSortingCenter(),
                    SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED,
                    false
            );
            pickupRoutePointId = userShift.getCurrentRoutePoint().getId();
            var pickupTask = userShift.getCurrentRoutePoint().streamPickupTasks().findFirst().orElseThrow();

            userShiftTestHelper.arriveAtRoutePoint(userShift);
            userShiftTestHelper.startOrderPickup(userShift, pickupTask);
            userShiftTestHelper.createTransferAct(userShift, pickupTask, List.of(orderId1), List.of(orderId2));
            userShiftTestHelper.waitForTransferActSignature(
                    userShift,
                    pickupTask,
                    pickupTask.getTransferActs().stream().findAny().get().getId(),
                    transferActExternalId
            );
            return null;
        });
    }

    @Test
    void updateStatus() {
        transferActCallbackProcessingService.processPayload(new TransferActCallbackPayload(REQUEST_ID,
                getTransferActCallbackDto(transferActExternalId, TransferStatus.CLOSED)));

        transactionTemplate.execute(status -> {
            var pickupRoutePointResult = routePointRepository.findByIdOrThrow(pickupRoutePointId);
            var pickupTaskResult = pickupRoutePointResult.streamPickupTasks().findFirst().orElseThrow();
            assertThat(pickupTaskResult.getStatus()).isEqualTo(OrderPickupTaskStatus.BOX_LOADING);

            TransferAct transferAct = pickupTaskResult.getTransferActs().get(0);
            assertThat(transferAct.getStatus()).isEqualTo(TransferActStatus.SIGNED);

            assertThat(orderRepository.getOne(orderId1).getTransferActsIds()).contains(transferAct.getId());
            assertThat(orderRepository.getOne(orderId2).getTransferActsIds()).isEmpty();

            return null;
        });
    }

    @ParameterizedTest
    @MethodSource("unsupportedStatusesSource")
    void skipUnsupportedStatuses(TransferStatus transferStatus) {
        transferActCallbackProcessingService.processPayload(new TransferActCallbackPayload(REQUEST_ID,
                getTransferActCallbackDto(transferActExternalId, transferStatus)));

        transactionTemplate.execute(status -> {
            var pickupRoutePointResult = routePointRepository.findByIdOrThrow(pickupRoutePointId);
            var pickupTaskResult = pickupRoutePointResult.streamPickupTasks().findFirst().orElseThrow();

            TransferAct transferAct = pickupTaskResult.getTransferActs().get(0);
            assertThat(transferAct.getStatus()).isEqualTo(TransferActStatus.WAITING_FOR_SIGNATURE);

            assertThat(orderRepository.getOne(orderId1).getTransferActsIds()).isEmpty();
            assertThat(orderRepository.getOne(orderId2).getTransferActsIds()).isEmpty();

            return null;
        });
    }

    @Test
    void skipNotFound() {
        assertDoesNotThrow(() -> transferActCallbackProcessingService.processPayload(
                new TransferActCallbackPayload(
                        REQUEST_ID,
                        getTransferActCallbackDto(transferActExternalId + "-postfix", TransferStatus.CLOSED)
                )
        ));

        transactionTemplate.execute(status -> {
            var pickupRoutePointResult = routePointRepository.findByIdOrThrow(pickupRoutePointId);
            var pickupTaskResult = pickupRoutePointResult.streamPickupTasks().findFirst().orElseThrow();

            TransferAct transferAct = pickupTaskResult.getTransferActs().get(0);
            assertThat(transferAct.getStatus()).isEqualTo(TransferActStatus.WAITING_FOR_SIGNATURE);

            return null;
        });
    }

    @Test
    void skippingCancellationRequestIfAlreadySigned() {
        transferActCallbackProcessingService.processPayload(new TransferActCallbackPayload(REQUEST_ID,
                getTransferActCallbackDto(transferActExternalId, TransferStatus.CLOSED)));

        var pickupTaskId = transactionTemplate.execute(status -> {
            var pickupRoutePointResult = routePointRepository.findByIdOrThrow(pickupRoutePointId);
            var pickupTaskResult = pickupRoutePointResult.streamPickupTasks().findFirst().orElseThrow();
            assertThat(pickupTaskResult.getStatus()).isEqualTo(OrderPickupTaskStatus.BOX_LOADING);

            TransferAct transferAct = pickupTaskResult.getTransferActs().get(0);
            assertThat(transferAct.getStatus()).isEqualTo(TransferActStatus.SIGNED);

            return pickupTaskResult.getId();
        });

        assertDoesNotThrow(() ->
                transferActCancellationService.cancelTransferActs(user, userShiftId, pickupRoutePointId, pickupTaskId)
        );
    }

    @NotNull
    private TransferActCallbackDto getTransferActCallbackDto(
            String id,
            TransferStatus status
    ) {
        TransferActCallbackDto transferActCallback = new TransferActCallbackDto();
        TransferDto transfer = new TransferDto();
        transfer.setId(id);
        transfer.setStatus(status);
        transferActCallback.setTransfer(transfer);
        return transferActCallback;
    }

}
