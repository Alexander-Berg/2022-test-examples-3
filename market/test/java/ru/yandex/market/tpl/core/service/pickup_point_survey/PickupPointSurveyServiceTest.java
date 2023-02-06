package ru.yandex.market.tpl.core.service.pickup_point_survey;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.PickupPointSurveyType;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveySubtaskRepository;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTask;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTaskCommand;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTaskCommandService;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTaskRepository;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTaskSubtaskStatus;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RequiredArgsConstructor
public class PickupPointSurveyServiceTest extends TplAbstractTest {

    private static final Long UID = 1234L;

    private final PickupPointSurveyService pickupPointSurveyService;
    private final PickupPointSurveyTaskRepository surveyTaskRepository;
    private final PickupPointSurveySubtaskRepository surveySubtaskRepository;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final PickupPointSurveyGeneratorService pickupPointSurveyGeneratorService;
    private final PickupPointSurveyTaskCommandService taskCommandService;
    private final Clock clock;
    private final DbQueueTestUtil dbQueueTestUtil;

    private User user;
    private UserShift userShift;

    @BeforeEach
    public void init() {
        user = testUserHelper.findOrCreateUser(UID);
        userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
    }

    /**
     * Даже пустой {@link AfterEach} нужен
     * для срабатывания {@link ru.yandex.market.tpl.core.test.TplAbstractTest#clearAfterTest(Object)}
     */
    @AfterEach
    void afterEach() {
    }

    @Test
    public void surveyIsRequired() {
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L)
        );
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        var lockerDeliveryTask = testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        testUserHelper.arriveAtRoutePoint(userShift, lockerDeliveryTask.getRoutePoint().getId());
        var surveyTask = pickupPointSurveyGeneratorService.generateSurveyTask(pickupPoint, null);

        var taskDto = pickupPointSurveyService.assignSurveyAndGetSurveyTaskDto(user, lockerDeliveryTask.getId());

        assertThat(surveyTaskRepository.findByIdOrThrow(surveyTask.getId()).getStatus())
                .isEqualTo(PickupPointSurveyTaskSubtaskStatus.IN_PROGRESS);
        assertThat(taskDto.isSurveyRequired()).isTrue();
        assertThat(taskDto.getUrl()).isNotNull();
        var pvzId = lockerDeliveryTask.getRoutePoint().streamLockerDeliveryTasks()
                .findAny()
                .map(LockerDeliveryTask::getPickupPointId)
                .orElseThrow();
        var urlIds = getUrlIds(pickupPoint, surveyTask, pvzId);
        assertThat(taskDto.getUrl()).isEqualTo("test.com".concat(urlIds));

        clearAfterTest(pickupPoint);
    }

    @Test
    public void surveyIsRequiredAndAssigned() {
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L)
        );
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        var lockerDeliveryTask = testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        testUserHelper.arriveAtRoutePoint(userShift, lockerDeliveryTask.getRoutePoint().getId());
        var surveyTask = pickupPointSurveyGeneratorService.generateSurveyTask(pickupPoint, null);
        var taskDto =
                pickupPointSurveyService.assignSurveyAndGetSurveyTaskDto(user, lockerDeliveryTask.getId());
        var subtask =
                surveySubtaskRepository.findById(surveyTask.getSubtasks().get(0).getId()).orElseThrow();

        assertThat(surveyTaskRepository.findByIdOrThrow(surveyTask.getId()).getStatus())
                .isEqualTo(PickupPointSurveyTaskSubtaskStatus.IN_PROGRESS);
        assertThat(subtask.getUserShift().getId()).isEqualTo(userShift.getId());
        assertThat(subtask.getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.IN_PROGRESS);
        assertThat(taskDto.isSurveyRequired()).isTrue();
        assertThat(taskDto.getUrl()).isNotNull();
        var pvzId = lockerDeliveryTask.getRoutePoint().streamLockerDeliveryTasks()
                .findAny()
                .map(LockerDeliveryTask::getPickupPointId)
                .orElseThrow();
        var urlIds = getUrlIds(pickupPoint, surveyTask, pvzId);
        assertThat(taskDto.getUrl()).isEqualTo("test.com".concat(urlIds));

        clearAfterTest(pickupPoint);
    }

    @Test
    public void surveyIsNotRequiredBecauseOfStartDate() {
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L)
        );
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        var lockerDeliveryTask = testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        testUserHelper.arriveAtRoutePoint(userShift, lockerDeliveryTask.getRoutePoint().getId());
        var surveyTask = pickupPointSurveyGeneratorService.generateSurveyTask(pickupPoint, null);
        surveyTask.setStartedAt(null);
        surveyTaskRepository.save(surveyTask);

        var taskDto = pickupPointSurveyService.assignSurveyAndGetSurveyTaskDto(user, lockerDeliveryTask.getId());
        assertThat(taskDto.isSurveyRequired()).isFalse();
        assertThat(taskDto.getUrl()).isNull();

        clearAfterTest(pickupPoint);
    }

    @Test
    public void surveyIsSuccessfullyFinished() {
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L)
        );
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        var lockerDeliveryTask = testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        testUserHelper.arriveAtRoutePoint(userShift, lockerDeliveryTask.getRoutePoint().getId());
        var surveyTask = pickupPointSurveyGeneratorService.generateSurveyTask(pickupPoint, null);
        var taskDto =
                pickupPointSurveyService.assignSurveyAndGetSurveyTaskDto(user, lockerDeliveryTask.getId());
        var subtask =
                surveySubtaskRepository.findById(surveyTask.getSubtasks().get(0).getId()).orElseThrow();

        assertThat(surveyTaskRepository.findByIdOrThrow(surveyTask.getId()).getStatus())
                .isEqualTo(PickupPointSurveyTaskSubtaskStatus.IN_PROGRESS);
        assertThat(subtask.getUserShift().getId()).isEqualTo(userShift.getId());
        assertThat(subtask.getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.IN_PROGRESS);
        assertThat(taskDto.isSurveyRequired()).isTrue();
        assertThat(taskDto.getUrl()).isNotNull();
        var pvzId = lockerDeliveryTask.getRoutePoint().streamLockerDeliveryTasks()
                .findAny()
                .map(LockerDeliveryTask::getPickupPointId)
                .orElseThrow();
        var urlIds = getUrlIds(pickupPoint, surveyTask, pvzId);
        assertThat(taskDto.getUrl()).isEqualTo("test.com".concat(urlIds));
        pickupPointSurveyService.submitSurveyFormIsCompleted(user, lockerDeliveryTask.getId());

        subtask = surveySubtaskRepository.findById(surveyTask.getSubtasks().get(0).getId()).orElseThrow();
        assertThat(subtask.getUserShift().getId()).isEqualTo(userShift.getId());
        assertThat(subtask.getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.FINISHED);
        dbQueueTestUtil.assertQueueHasSize(QueueType.CHECK_AND_UPDATE_PICKUP_POINT_SURVEY_TASK_STATUS, 1);

        dbQueueTestUtil.executeSingleQueueItem(QueueType.CHECK_AND_UPDATE_PICKUP_POINT_SURVEY_TASK_STATUS);
        surveyTask = surveyTaskRepository.findByIdOrThrow(surveyTask.getId());
        assertThat(surveyTask.getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.FINISHED);

        taskDto =
                pickupPointSurveyService.assignSurveyAndGetSurveyTaskDto(user, lockerDeliveryTask.getId());
        assertThat(taskDto.isSurveyRequired()).isFalse();

        clearAfterTest(pickupPoint);
    }

    @Test
    public void subtaskIsFinishedAndTaskIsNot() {
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L)
        );
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        var lockerDeliveryTask = testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        testUserHelper.arriveAtRoutePoint(userShift, lockerDeliveryTask.getRoutePoint().getId());
        var pickupPointSurvey = pickupPointSurveyGeneratorService.generatePickupPointSurvey(List.of(
                "url1", "url2"), PickupPointSurveyType.PVZ_MARKET_BRANDED, true);
        var surveyTask = PickupPointSurveyTask.init(pickupPointSurvey, pickupPoint, Instant.now());
        surveyTaskRepository.save(surveyTask);
        var taskDto =
                pickupPointSurveyService.assignSurveyAndGetSurveyTaskDto(user, lockerDeliveryTask.getId());

        assertThat(surveyTaskRepository.findByIdOrThrow(surveyTask.getId())
                .getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.IN_PROGRESS);

        assertThat(taskDto.isSurveyRequired()).isTrue();
        assertThat(taskDto.getUrl()).isNotNull();
        pickupPointSurveyService.submitSurveyFormIsCompleted(user, lockerDeliveryTask.getId());

        dbQueueTestUtil.assertQueueHasSize(QueueType.CHECK_AND_UPDATE_PICKUP_POINT_SURVEY_TASK_STATUS, 1);
        dbQueueTestUtil.executeSingleQueueItem(QueueType.CHECK_AND_UPDATE_PICKUP_POINT_SURVEY_TASK_STATUS);
        surveyTask = surveyTaskRepository.findByIdOrThrow(surveyTask.getId());
        assertThat(surveyTask.getStatus()).isNotEqualTo(PickupPointSurveyTaskSubtaskStatus.FINISHED);

        clearAfterTest(pickupPoint);
    }

    @Test
    public void finishAlreadyFinishedTask() {
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L)
        );
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        var lockerDeliveryTask = testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        testUserHelper.arriveAtRoutePoint(userShift, lockerDeliveryTask.getRoutePoint().getId());
        var surveyTask = pickupPointSurveyGeneratorService.generateSurveyTask(pickupPoint, null);
        var taskDto =
                pickupPointSurveyService.assignSurveyAndGetSurveyTaskDto(user, lockerDeliveryTask.getId());
        var subtask =
                surveySubtaskRepository.findById(surveyTask.getSubtasks().get(0).getId()).orElseThrow();
        assertThat(surveyTaskRepository.findByIdOrThrow(surveyTask.getId()).getStatus())
                .isEqualTo(PickupPointSurveyTaskSubtaskStatus.IN_PROGRESS);
        assertThat(subtask.getUserShift().getId()).isEqualTo(userShift.getId());
        assertThat(subtask.getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.IN_PROGRESS);
        assertThat(taskDto.isSurveyRequired()).isTrue();
        assertThat(taskDto.getUrl()).isNotNull();
        var pvzId = lockerDeliveryTask.getRoutePoint().streamLockerDeliveryTasks()
                .findAny()
                .map(LockerDeliveryTask::getPickupPointId)
                .orElseThrow();
        var urlIds = getUrlIds(pickupPoint, surveyTask, pvzId);
        assertThat(taskDto.getUrl()).isEqualTo("test.com".concat(urlIds));
        pickupPointSurveyService.submitSurveyFormIsCompleted(user, lockerDeliveryTask.getId());

        subtask = surveySubtaskRepository.findById(surveyTask.getSubtasks().get(0).getId()).orElseThrow();
        assertThat(subtask.getUserShift().getId()).isEqualTo(userShift.getId());
        assertThat(subtask.getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.FINISHED);

        var exception = assertThrows(
                TplInvalidActionException.class,
                () -> pickupPointSurveyService.submitSurveyFormIsCompleted(user, lockerDeliveryTask.getId())
        );
        assertThat(
                "User " + user.getId() + " does not have active survey tasks for pickup point " + pickupPoint.getId()
        ).isEqualTo(exception.getMessage());

        clearAfterTest(pickupPoint);
    }

    @Test
    public void surveyIsNotRequired() {
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L)
        );
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        var lockerDeliveryTask = testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        testUserHelper.arriveAtRoutePoint(userShift, lockerDeliveryTask.getRoutePoint().getId());
        var task = pickupPointSurveyGeneratorService.generateSurveyTask(pickupPoint, userShift);
        taskCommandService.finishTask(new PickupPointSurveyTaskCommand.Finish(task.getId()));

        var taskDto = pickupPointSurveyService.assignSurveyAndGetSurveyTaskDto(user, lockerDeliveryTask.getId());
        assertThat(taskDto.isSurveyRequired()).isFalse();
        assertThat(taskDto.getUrl()).isNull();

        clearAfterTest(pickupPoint);
    }

    private String getUrlIds(PickupPoint pickupPoint, PickupPointSurveyTask surveyTask, Long pvzId) {
        return "?pvz_id=" + pvzId +
                "&user_id=" + user.getId() +
                "&lp_id=" + pickupPoint.getLogisticPointId() +
                "&survey_task_id=" + surveyTask.getId();
    }
}
