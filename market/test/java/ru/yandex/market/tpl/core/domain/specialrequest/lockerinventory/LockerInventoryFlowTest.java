package ru.yandex.market.tpl.core.domain.specialrequest.lockerinventory;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.FlowTaskDto;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.TaskFailReasonDto;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.actions.processor.SupportChatPayload;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequest;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestCommand;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestGenerateService;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestRepository;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestStatus;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.FlowTaskManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.task.flow.EmptyPayload;
import ru.yandex.market.tpl.core.task.flow.TaskActionRunResult;
import ru.yandex.market.tpl.core.task.persistence.LogisticRequestLinkStatus;
import ru.yandex.market.tpl.core.task.projection.TaskAction;
import ru.yandex.market.tpl.core.task.projection.TaskActionStatus;
import ru.yandex.market.tpl.core.task.projection.TaskActionType;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;
import ru.yandex.market.tpl.core.task.projection.TaskStatus;
import ru.yandex.market.tpl.core.task.service.LogisticRequestLinkService;
import ru.yandex.market.tpl.core.task.service.TaskRepository;
import ru.yandex.market.tpl.core.task.service.TaskService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sekulebyakin
 */
@RequiredArgsConstructor
public class LockerInventoryFlowTest extends TplAbstractTest {

    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final TaskRepository taskRepository;
    private final FlowTaskManager flowTaskManager;
    private final SpecialRequestGenerateService specialRequestGenerateService;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftRepository userShiftRepository;
    private final SpecialRequestRepository specialRequestRepository;
    private final UserShiftCommandService userShiftCommandService;
    private final LogisticRequestCommandService logisticRequestCommandService;
    private final ConfigurationServiceAdapter configuration;
    private final LogisticRequestLinkService logisticRequestLinkService;
    private final TransactionTemplate transactionTemplate;
    private final TaskService taskService;

    private User user;
    private UserShift userShift;
    private SpecialRequest specialRequest;
    private Long taskId;
    private Long actionId;
    private PickupPoint pickupPoint;

    @Value("${tpl.tracker.ticket.url:https://st.yandex-team.ru/}")
    private String trackerUrl;

    @Value("${external.lms.admin.pp.url:https://lms-admin.market.yandex-team.ru/lms/logistics-point/}")
    private String lmsPickupPointUrl;

    @BeforeEach
    void setup() {
        transactionTemplate.executeWithoutResult(s -> {
            user = testUserHelper.findOrCreateUser(123L);
            userShift = testUserHelper.createEmptyShift(user, LocalDate.now());
            pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L);
            specialRequest = specialRequestGenerateService.createSpecialRequest(
                    SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                            .pickupPointId(pickupPoint.getId())
                            .build());
            var task = testDataFactory.addFlowTask(userShift.getId(), TaskFlowType.LOCKER_INVENTORY,
                    List.of(specialRequest));

            var specialRequestId = specialRequest.getId();
            taskId = task.getId();

            var order = orderGenerateService.createOrder(
                    OrderGenerateService.OrderGenerateParam.builder()
                            .pickupPoint(pickupPoint)
                            .build());
            testDataFactory.addLockerDeliveryTask(userShift.getId(), order);

            var actions = taskRepository.getTaskActions(task.getId());
            assertThat(actions).hasSize(1);
            assertThat(actions.get(0).getType()).isEqualTo(TaskActionType.SUPPORT_CHAT);
            assertThat(actions.get(0).getStatus()).isEqualTo(TaskActionStatus.CREATED);
            assertThat(specialRequest.getStatus()).isEqualTo(SpecialRequestStatus.CREATED);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.NOT_STARTED);
            actionId = actions.get(0).getId();

            userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
            testUserHelper.finishPickupAtStartOfTheDay(userShift);
            testUserHelper.arriveAtRoutePoint(userShift, task.getRoutePoint().getId());

            logisticRequestCommandService.execute(new SpecialRequestCommand.Start(specialRequestId));
            assertThat(specialRequestRepository.findByIdOrThrow(specialRequestId).getStatus())
                    .isEqualTo(SpecialRequestStatus.IN_PROGRESS);

            var routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
            assertThat(routePoint.streamTasks().count()).isEqualTo(2);
        });
    }

    @Test
    void lockerInventoryFullTest() {
        var actions = taskService.getActiveActions(taskId, user);
        assertThat(actions).hasSize(1);
        var supportChatAction = actions.get(0);
        assertThat(supportChatAction.getType()).isEqualTo(TaskActionType.SUPPORT_CHAT);
        assertThat(supportChatAction.getData()).isInstanceOf(SupportChatPayload.class);
        var payload = (SupportChatPayload) supportChatAction.getData();
        assertThat(supportChatAction.getOrdinal()).isEqualTo(1);
        assertThat(payload.getInitialMessage()).isEqualTo("Внешний ID постамата: <a " +
                "href='" + lmsPickupPointUrl + "/" + pickupPoint.getLogisticPointId() + "'>" +
                pickupPoint.getCode() + "</a>" +
                "<br><br>Адрес постамата: <a href='" + lmsPickupPointUrl + "/" +
                pickupPoint.getLogisticPointId() + "'>" + pickupPoint.getAddress() + "</a>" +
                "<br><br>Инвентаризация: <a href='" + trackerUrl
                + specialRequest.getRequestSource() + "'>" + specialRequest.getRequestSource() + "</a>");

        var result = flowTaskManager.executeTaskAction(taskId, TaskActionType.SUPPORT_CHAT, actionId,
                new EmptyPayload(), user, Source.COURIER);

        assertThat(result.getResult()).isEqualTo(TaskActionRunResult.FLOW_FINISHED);
        assertThat(result.getTask().getId()).isEqualTo(taskId);
        assertThat(result.getNextActions()).isEmpty();

        specialRequest = specialRequestRepository.findByIdOrThrow(specialRequest.getId());
        var task = taskRepository.getTask(taskId);
        var action = findFirstAction();

        assertThat(specialRequest.getStatus()).isEqualTo(SpecialRequestStatus.FINISHED);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.FINISHED);
        assertThat(action.getStatus()).isEqualTo(TaskActionStatus.FINISHED);
    }

    @Test
    void lockerInventoryCancelTaskTest() {
        LocalDate originalIntervalFrom = specialRequest.getIntervalFrom().toLocalDate();
        flowTaskManager.failTask(
                taskId,
                new TaskFailReasonDto(LockerInventoryFailReasonType.LOCKER_NOT_WORKING.name(), null, null),
                user
        );
        specialRequest = specialRequestRepository.findByIdOrThrow(specialRequest.getId());
        var task = taskRepository.getTask(taskId);
        var action = findFirstAction();
        LocalDate newIntervalFrom = specialRequest.getIntervalFrom().toLocalDate();

        assertThat(newIntervalFrom).isEqualTo(originalIntervalFrom.plusDays(1));
        assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(action.getStatus()).isEqualTo(TaskActionStatus.CANCELLED);

        logisticRequestLinkService.findLinksForTask(taskId)
                .forEach(link -> assertThat(link.getStatus()).isEqualTo(LogisticRequestLinkStatus.CANCELLED));

    }

    @Test
    void lockerInventoryReopenTaskFlagTest() {
        // Проверка флага
        configuration.mergeValue(ConfigurationProperties.SPECIAL_REQUEST_TASK_REOPEN_ENABLED, false);
        assertThat(taskService.isTaskCanBeReopened(taskId, user)).isFalse();

        configuration.mergeValue(ConfigurationProperties.SPECIAL_REQUEST_TASK_REOPEN_ENABLED, true);
        assertThat(taskService.isTaskCanBeReopened(taskId, user)).isFalse(); // таска не завершена, переоткрыть нельзя

        // Завершаем флоу, теперь можно переоткрыть
        flowTaskManager.executeTaskAction(taskId, TaskActionType.SUPPORT_CHAT,
                actionId, new EmptyPayload(), user, Source.COURIER);
        assertThat(taskService.isTaskCanBeReopened(taskId, user)).isTrue();
    }

    @Test
    void lockerInventoryReopenFinishedTaskTest() {
        // Завершаем флоу
        var executionResult = flowTaskManager.executeTaskAction(taskId, TaskActionType.SUPPORT_CHAT,
                actionId, new EmptyPayload(), user, Source.COURIER);
        assertThat(executionResult.getResult()).isEqualTo(TaskActionRunResult.FLOW_FINISHED);

        specialRequest = specialRequestRepository.findByIdOrThrow(specialRequest.getId());
        assertThat(specialRequest.getStatus()).isEqualTo(SpecialRequestStatus.FINISHED);

        var notFinished = taskRepository.getTaskActions(taskId).stream()
                .filter(a -> a.getStatus() != TaskActionStatus.FINISHED)
                .count();
        assertThat(notFinished).isEqualTo(0);

        verifyReopen();
    }

    @Test
    void lockerInventoryReopenCancelledTaskTest() {
        // Отменяем таску
        flowTaskManager.failTask(
                taskId,
                new TaskFailReasonDto(LockerInventoryFailReasonType.LOCKER_NOT_WORKING.name(), "t", null),
                user
        );
        var task = taskRepository.getTask(taskId);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);

        var action = findFirstAction();
        assertThat(action.getStatus()).isEqualTo(TaskActionStatus.CANCELLED);

        specialRequest = specialRequestRepository.findByIdOrThrow(specialRequest.getId());
        assertThat(specialRequest.getStatus()).isEqualTo(SpecialRequestStatus.IN_PROGRESS);

        logisticRequestLinkService.findLinksForTask(taskId)
                .forEach(link -> assertThat(link.getStatus()).isEqualTo(LogisticRequestLinkStatus.CANCELLED));

        verifyReopen();
    }

    private void verifyReopen() {
        configuration.mergeValue(ConfigurationProperties.SPECIAL_REQUEST_TASK_REOPEN_ENABLED, true);

        // Переоткрываем флоу
        var reopenedTask = (FlowTaskDto) flowTaskManager.reopenTask(taskId, user, Source.COURIER);
        assertThat(reopenedTask.getFlowActions()).hasSize(1);

        // Проверяем что переоткрылись таска и экшен
        specialRequest = specialRequestRepository.findByIdOrThrow(specialRequest.getId());
        assertThat(specialRequest.getStatus()).isEqualTo(SpecialRequestStatus.IN_PROGRESS);

        var task = taskRepository.getTask(taskId);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.NOT_STARTED);

        var action = findFirstAction();
        assertThat(action.getStatus()).isEqualTo(TaskActionStatus.CREATED);
        assertThat(task.getCurrentActionId()).isEqualTo(actionId);

        logisticRequestLinkService.findLinksForTask(taskId)
                .forEach(link -> assertThat(link.getStatus()).isEqualTo(LogisticRequestLinkStatus.ACTIVE));
    }

    private TaskAction findFirstAction() {
        return taskRepository.getTaskActions(taskId).stream()
                .filter(a -> Objects.equals(a.getId(), actionId))
                .findFirst()
                .orElseThrow();
    }
}
