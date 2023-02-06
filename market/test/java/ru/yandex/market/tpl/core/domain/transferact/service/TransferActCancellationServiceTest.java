package ru.yandex.market.tpl.core.domain.transferact.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.common.transferact.client.api.TransferApi;
import ru.yandex.market.tpl.common.util.exception.TplException;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.transferact.TransferActStatus;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.user.UserProperties.TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED;

@RequiredArgsConstructor
class TransferActCancellationServiceTest extends TplAbstractTest {

    private final TransferActCancellationService transferActCancellationService;
    private final UserShiftRepository userShiftRepository;
    private final TestDataFactory testDataFactory;
    private final TestUserHelper userHelper;
    private final UserShiftTestHelper userShiftTestHelper;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final UserPropertyService userPropertyService;
    private final TransferApi transferApi;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    private User user;
    private Long userShiftId;
    private Long routePointId;
    private Long taskId;
    private final String transferActExternalId = "test-123";

    @BeforeEach
    void beforeEach() {
        transactionTemplate.execute(status -> {
            user = userHelper.findOrCreateUser(991L);
            userPropertyService.addPropertyToUser(user, TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, true);
            return null;
        });

        Shift shift = userHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), 18769);

        Long orderId1 = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build()
        ).getId();

        Long orderId2 = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build()
        ).getId();

        userShiftId = userShiftTestHelper.start(UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskOrderPickup(clock.instant()))
                .routePoint(userShiftCommandDataHelper.taskUnpaid("addr1", 12, orderId1))
                .routePoint(userShiftCommandDataHelper.taskUnpaid("addr2", 12, orderId2))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build()
        );
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                userShift.getShift().getSortingCenter(),
                SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED,
                false
        );

        var pickupTask = userShift.getCurrentRoutePoint().streamPickupTasks().findFirst().orElseThrow();
        routePointId = pickupTask.getRoutePoint().getId();
        taskId = pickupTask.getId();

        userShiftTestHelper.arriveAtRoutePoint(userShift);
        userShiftTestHelper.startOrderPickup(userShift, pickupTask);
        userShiftTestHelper.createTransferAct(userShift, pickupTask, List.of(orderId1), List.of(orderId2));
    }

    @Test
    void cancelTransferActs() {
        waitForTransferActSignature();
        when(transferApi.transferIdDeleteWithHttpInfo(any())).thenReturn(ResponseEntity.ok().build());

        transferActCancellationService.cancelTransferActs(user, userShiftId, routePointId, taskId);

        transactionTemplate.execute(status -> {
            UserShift userShift = userShiftRepository.getOne(userShiftId);
            OrderPickupTask orderPickupTask = userShiftTestHelper.getOrderPickupTask(userShift);

            assertThat(orderPickupTask.getStatus())
                    .isEqualTo(OrderPickupTaskStatus.IN_PROGRESS);

            orderPickupTask.getTransferActs().forEach(
                    it -> assertThat(it.getStatus()).isEqualTo(TransferActStatus.CANCELLED)
            );

            return null;
        });
    }

    @Test
    void cancelTransferActs_badRequest() {
        waitForTransferActSignature();
        when(transferApi.transferIdDeleteWithHttpInfo(any())).thenReturn(ResponseEntity.badRequest().build());

        assertThrows(
                TplIllegalStateException.class,
                () -> transferActCancellationService.cancelTransferActs(user, userShiftId, routePointId, taskId)
        );
    }

    @Test
    void cancelTransferActs_noTransferActExternalId() {
        when(transferApi.transferIdDeleteWithHttpInfo(any())).thenReturn(ResponseEntity.badRequest().build());

        assertThrows(
                TplIllegalStateException.class,
                () -> transferActCancellationService.cancelTransferActs(user, userShiftId, routePointId, taskId)
        );
    }

    @Test
    void cancelTransferActs_internalError() {
        waitForTransferActSignature();
        when(transferApi.transferIdDeleteWithHttpInfo(any()))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        TplException tplException = assertThrows(
                TplException.class,
                () -> transferActCancellationService.cancelTransferActs(user, userShiftId, routePointId, taskId)
        );

        assertThat(tplException.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private void waitForTransferActSignature() {
        transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            var pickupTask = userShift.getCurrentRoutePoint().streamPickupTasks().findFirst().orElseThrow();
            var transferActId = pickupTask.getTransferActs().stream().findAny().get().getId();
            userShiftTestHelper.waitForTransferActSignature(
                    userShift, pickupTask, transferActId, transferActExternalId
            );
            return null;
        });
    }

}
