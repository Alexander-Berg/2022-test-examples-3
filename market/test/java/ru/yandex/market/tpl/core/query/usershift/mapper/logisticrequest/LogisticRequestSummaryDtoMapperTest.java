package ru.yandex.market.tpl.core.query.usershift.mapper.logisticrequest;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.logisticrequest.LogisticRequestType;
import ru.yandex.market.tpl.api.model.order.OrderSummaryDto;
import ru.yandex.market.tpl.api.model.order.OrderType;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.TaskFailReasonDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequest;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestGenerateService;
import ru.yandex.market.tpl.core.domain.specialrequest.lockerinventory.LockerInventoryFailReasonType;
import ru.yandex.market.tpl.core.domain.usershift.FlowTaskManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.task.persistence.FlowTaskEntity;
import ru.yandex.market.tpl.core.task.persistence.FlowTaskRepository;
import ru.yandex.market.tpl.core.task.projection.TaskStatus;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sekulebyakin
 */
@RequiredArgsConstructor
public class LogisticRequestSummaryDtoMapperTest extends TplAbstractTest {

    private final LogisticRequestSummaryDtoMapper mapper;
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandService userShiftCommandService;
    private final SpecialRequestGenerateService specialRequestGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final UserShiftCommandDataHelper commandDataHelper;
    private final TransactionTemplate transactionTemplate;
    private final LogisticRequestRepository logisticRequestRepository;
    private final FlowTaskRepository flowTaskRepository;
    private final FlowTaskManager flowTaskManager;

    private final long userUid = 12345L;

    @Test
    void mappingSummaryDtoTest() {

        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 12L, 1L));
        var partnerSubType = pickupPoint.getPartnerSubType();

        var specialRequestId = specialRequestGenerateService.createSpecialRequest(
                SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                        .pickupPointId(pickupPoint.getId())
                        .build())
                .getId();
        var taskId = prepareTask(specialRequestId);

        transactionTemplate.executeWithoutResult(s -> {
            var task = flowTaskRepository.findByIdOrThrow(taskId);
            var specialRequest = logisticRequestRepository.findByIdOrThrow(specialRequestId);
            var dto = mapper.toOrderSummaryDto(specialRequest, task, 5);

            checkSummaryDto(dto, specialRequest, task, partnerSubType, 5);
        });
    }

    @Test
    void mappingSummaryDtoCancelledTaskTest() {
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 12L, 1L));
        var partnerSubType = pickupPoint.getPartnerSubType();
        var specialRequestId = specialRequestGenerateService.createSpecialRequest(
                SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                        .pickupPointId(pickupPoint.getId())
                        .build())
                .getId();
        var taskId = prepareTask(specialRequestId);
        var user = userHelper.findOrCreateUser(userUid);
        transactionTemplate.executeWithoutResult(s -> {
            var routePoint = flowTaskRepository.findByIdOrThrow(taskId).getRoutePoint();
            userHelper.arriveAtRoutePoint(routePoint);
        });

        flowTaskManager.failTask(taskId, new TaskFailReasonDto(
                LockerInventoryFailReasonType.LOCKER_NOT_WORKING.name(), "t", null), user);

        transactionTemplate.executeWithoutResult(s -> {
            var task = flowTaskRepository.findByIdOrThrow(taskId);
            var specialRequest = logisticRequestRepository.findByIdOrThrow(specialRequestId);
            var dto = mapper.toOrderSummaryDto(specialRequest, task, 5);

            checkSummaryDto(dto, specialRequest, task, partnerSubType, 5);
        });
    }

    private void checkSummaryDto(OrderSummaryDto dto, LogisticRequest logisticRequest, FlowTaskEntity task,
                                 PartnerSubType partnerSubType, Integer intervalId) {
        assertThat(dto.getExternalOrderId()).isEqualTo(logisticRequest.getExternalId());
        assertThat(dto.getDeliveryAddress()).isEqualTo(logisticRequest.getPointTo().getAddress());
        assertThat(dto.getTaskId()).isEqualTo(task.getId());
        assertThat(dto.getSubtaskId()).isEqualTo(task.getId());
        assertThat(dto.getMultiOrderId()).isEqualTo(String.valueOf(task.getId()));
        assertThat(dto.getRoutePointId()).isEqualTo(task.getRoutePoint().getId());
        assertThat(dto.getExpectedDeliveryTime()).isEqualTo(task.getRoutePoint().getExpectedDateTime());
        assertThat(dto.getOrdinalNumber()).isEqualTo(task.getOrdinalNumber());

        if (partnerSubType == null) {
            assertThat(dto.getOrderType()).isEqualTo(OrderType.CLIENT);
        } else {
            assertThat(dto.getOrderType()).isEqualTo(OrderType.map(partnerSubType));
        }

        if (task.getFailReason() != null) {
            assertThat(dto.getFailReason().getReason()).isEqualTo(task.getFailReason().getType());
            assertThat(dto.getFailReason().getComment()).isEqualTo(task.getFailReason().getComment());
            assertThat(dto.getFailReason().getSource()).isEqualTo(task.getFailReason().getSource());
        }

        if (task.getStatus() == TaskStatus.CANCELLED) {
            assertThat(dto.getTaskStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        } else if (task.getStatus() == TaskStatus.FINISHED) {
            assertThat(dto.getTaskStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERED);
        } else {
            assertThat(dto.getTaskStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        }

        if (logisticRequest.getType() == LogisticRequestType.SPECIAL_REQUEST) {
            checkSpecialRequestSummaryDto(dto, logisticRequest, task);
        }
    }

    private void checkSpecialRequestSummaryDto(OrderSummaryDto dto, LogisticRequest logisticRequest, FlowTaskEntity task) {
        assertThat(dto.getPlaceCount()).isEqualTo(0);
        if (task.getStatus() == TaskStatus.CANCELLED && task.getFailReasonType().isCourierReason()) {
            assertThat(dto.getActions()).isNotNull();
            assertThat(dto.getActions()).hasSize(1);
            assertThat(dto.getActions().get(0).getType()).isEqualTo(LockerDeliveryTaskDto.ActionType.REOPEN);
        } else {
            assertThat(dto.getActions()).isEmpty();
        }
    }

    private Long prepareTask(long logisticRequestId) {
        return transactionTemplate.execute(s -> {
            var user = userHelper.findOrCreateUser(userUid);
            var userShift = userHelper.createEmptyShift(user, LocalDate.now());
            var logisticRequest = logisticRequestRepository.findByIdOrThrow(logisticRequestId);

            userShiftCommandService.addFlowTask(user, new UserShiftCommand.AddFlowTask(userShift.getId(),
                    logisticRequest.resolveTaskFlow(), commandDataHelper.logisticRequest(12, logisticRequest)));

            userShiftCommandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
            userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));

            var task = userShift.streamFlowTasks().findFirst().orElseThrow();
            return task.getId();
        });
    }

}
