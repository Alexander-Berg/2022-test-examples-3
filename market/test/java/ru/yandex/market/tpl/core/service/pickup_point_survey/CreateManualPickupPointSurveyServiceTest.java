package ru.yandex.market.tpl.core.service.pickup_point_survey;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.PickupPointSurvey;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.PickupPointSurveyType;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTask;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTaskCommand;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTaskCommandService;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTaskRepository;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTaskSubtaskStatus;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.DeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.service.pickup_point_survey.ticket.PickupPointSurveyTicketService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CreateManualPickupPointSurveyServiceTest extends TplAbstractTest {

    private static final Long UID1 = 1234L;
    private static final Long UID2 = 4321L;
    private static final String SURVEY_SERVICE_NO_VALID_PICKUP_POINTS =
            "Среди перечисленных логистических точек нет брендированных ПВЗ.";
    private static final String MANUALLY_CLOSED_SURVEY_TASK = "Ручное закрытие.";

    private final PickupPointSurveyService pickupPointSurveyService;
    private final PickupPointSurveyTaskRepository surveyTaskRepository;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final PickupPointSurveyGeneratorService pickupPointSurveyGeneratorService;
    private final Clock clock;
    private final PickupPointSurveyTicketService pickupPointSurveyTicketService;
    private final PickupPointSurveyTaskCommandService taskCommandService;


    private final static List<String> urls = List.of(
            "url1.com"
    );
    private User user1;
    private User user2;
    private PickupPoint pickupPoint1;
    private PickupPoint pickupPoint2;
    private DeliveryTask deliveryTask1;
    private DeliveryTask deliveryTask2;
    private PickupPointSurveyTask surveyTask1;
    private PickupPointSurveyTask surveyTask2;


    @BeforeEach
    void init() {
        user1 = testUserHelper.findOrCreateUser(UID1);
        user2 = testUserHelper.findOrCreateUser(UID2);
        UserShift userShift1 = testUserHelper.createEmptyShift(user1, LocalDate.now(clock));
        UserShift userShift2 = testUserHelper.createEmptyShift(user2, LocalDate.now(clock));
        pickupPoint1 = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1L, 1, true)
        );
        pickupPoint2 = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 2L, 2L, true)
        );
        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint1)
                .build());
        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint2)
                .build());
        deliveryTask1 = testUserHelper.addDeliveryTaskToShift(user1, userShift1, order1);
        deliveryTask2 = testUserHelper.addDeliveryTaskToShift(user2, userShift2, order2);

        testUserHelper.openShift(user1, userShift1.getId());
        testUserHelper.openShift(user2, userShift2.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift1);
        testUserHelper.finishPickupAtStartOfTheDay(userShift2);
        testUserHelper.arriveAtRoutePoint(userShift1, deliveryTask1.getRoutePoint().getId());
        testUserHelper.arriveAtRoutePoint(userShift2, deliveryTask2.getRoutePoint().getId());

        PickupPointSurvey survey = pickupPointSurveyGeneratorService.generatePickupPointSurvey(urls,
                PickupPointSurveyType.PVZ_MARKET_BRANDED, true);

        surveyTask1 = pickupPointSurveyGeneratorService.generateSurveyTaskForOneSurvey(survey, pickupPoint1,
                userShift1);
        surveyTask2 = pickupPointSurveyGeneratorService.generateSurveyTaskForOneSurvey(survey, pickupPoint2,
                userShift2);
    }

    /**
     * Даже пустой {@link AfterEach} нужен
     * для срабатывания {@link ru.yandex.market.tpl.core.test.TplAbstractTest#clearAfterTest(Object)}
     */
    @AfterEach
    void afterEach() {
    }

    @Test
    void forceCreateNewSurveys_WhenPickupPointHasSurveyNotFinished() {
        pickupPointSurveyService.assignSurveyAndGetSurveyTaskDto(user1, deliveryTask1.getId());
        pickupPointSurveyService.assignSurveyAndGetSurveyTaskDto(user2, deliveryTask2.getId());

        pickupPointSurveyTicketService.addCommentForStTicket(surveyTask1, MANUALLY_CLOSED_SURVEY_TASK);
        taskCommandService.finishTask(new PickupPointSurveyTaskCommand.Finish(surveyTask1.getId()));

        assertThat(surveyTaskRepository.findByIdOrThrow(surveyTask1.getId()).getStatus())
                .isEqualTo(PickupPointSurveyTaskSubtaskStatus.FINISHED);
        assertThat(surveyTaskRepository.findByIdOrThrow(surveyTask2.getId()).getStatus())
                .isEqualTo(PickupPointSurveyTaskSubtaskStatus.CREATED);

        var failedPickupPointSurveys = pickupPointSurveyService.createNewSurveysOnLogisticPoints(
                List.of(pickupPoint1.getLogisticPointId(), pickupPoint2.getLogisticPointId()),
                PickupPointSurveyType.PVZ_MARKET_BRANDED,
                true
        );

        assertThat(failedPickupPointSurveys.isEmpty()).isTrue();
        assertThat(surveyTaskRepository.findByIdOrThrow(surveyTask1.getId()).getStatus())
                .isEqualTo(PickupPointSurveyTaskSubtaskStatus.FINISHED);
        assertThat(surveyTaskRepository.findByIdOrThrow(surveyTask2.getId()).getStatus())
                .isEqualTo(PickupPointSurveyTaskSubtaskStatus.FINISHED);

        var newSurveyTask1 = surveyTaskRepository.findActiveSurveyTask(pickupPoint1.getId());
        var newSurveyTask2 = surveyTaskRepository.findActiveSurveyTask(pickupPoint2.getId());

        assertThat(newSurveyTask1.isPresent()).isTrue();
        assertThat(newSurveyTask1.get().getId()).isNotEqualTo(surveyTask1.getId());
        assertThat(newSurveyTask1.get().getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.CREATED);
        assertThat(newSurveyTask2.isPresent()).isTrue();
        assertThat(newSurveyTask2.get().getId()).isNotEqualTo(surveyTask2.getId());
    }

    @Test
    void failToCreateNewSurvey_WhenNotFinishedNotForce() {
        pickupPointSurveyService.assignSurveyAndGetSurveyTaskDto(user1, deliveryTask1.getId());
        pickupPointSurveyService.assignSurveyAndGetSurveyTaskDto(user2, deliveryTask2.getId());

        pickupPointSurveyTicketService.addCommentForStTicket(surveyTask1, MANUALLY_CLOSED_SURVEY_TASK);
        taskCommandService.finishTask(new PickupPointSurveyTaskCommand.Finish(surveyTask1.getId()));

        assertThat(surveyTaskRepository.findByIdOrThrow(surveyTask1.getId()).getStatus())
                .isEqualTo(PickupPointSurveyTaskSubtaskStatus.FINISHED);
        assertThat(surveyTaskRepository.findByIdOrThrow(surveyTask2.getId()).getStatus())
                .isEqualTo(PickupPointSurveyTaskSubtaskStatus.CREATED);

        var failedPickupPointSurveys = pickupPointSurveyService.createNewSurveysOnLogisticPoints(
                List.of(pickupPoint1.getLogisticPointId(), pickupPoint2.getLogisticPointId()),
                PickupPointSurveyType.PVZ_MARKET_BRANDED,
                false
        );

        assertThat(failedPickupPointSurveys).hasSize(1);
        assertThat(failedPickupPointSurveys.get(0)).isEqualTo(pickupPoint2.getLogisticPointId());

        var newSurveyTask1 = surveyTaskRepository.findActiveSurveyTask(pickupPoint1.getId());
        var newSurveyTask2 = surveyTaskRepository.findActiveSurveyTask(pickupPoint2.getId());

        assertThat(newSurveyTask1.isPresent()).isTrue();
        assertThat(newSurveyTask1.get().getId()).isNotEqualTo(surveyTask1.getId());
        assertThat(newSurveyTask2.isPresent()).isTrue();
        assertThat(newSurveyTask2.get().getId()).isEqualTo(surveyTask2.getId());
    }

    @Test
    void failedForPickupPint_WhenPickupPointNotPvzOrNotBranded() {
        var pickupPointNotPvz = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 3L, 3, true)
        );
        var pickupPointNotBranded = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 4L, 4L, false)
        );

        assertThatThrownBy(() -> pickupPointSurveyService.createNewSurveysOnLogisticPoints(
                List.of(pickupPointNotPvz.getLogisticPointId(), pickupPointNotBranded.getLogisticPointId()),
                PickupPointSurveyType.PVZ_MARKET_BRANDED,
                true
        )).isInstanceOf(TplIllegalArgumentException.class).hasMessage(SURVEY_SERVICE_NO_VALID_PICKUP_POINTS);
    }

    @Test
    void returnsInvalidLogisticPoints_WhenNotForceCreateAndTaskIsActiveAndInvalidLogisticPoints() {
        pickupPointSurveyService.assignSurveyAndGetSurveyTaskDto(user1, deliveryTask1.getId());

        assertThat(surveyTaskRepository.findByIdOrThrow(surveyTask1.getId()).getStatus())
                .isEqualTo(PickupPointSurveyTaskSubtaskStatus.CREATED);

        var pickupPointNotPvz = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 3L, 3, true)
        );
        var pickupPointNotBranded = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 4L, 4L, false)
        );

        var invalidLogisticPoints = pickupPointSurveyService.createNewSurveysOnLogisticPoints(
                List.of(
                        pickupPointNotPvz.getLogisticPointId(),
                        pickupPointNotBranded.getLogisticPointId(),
                        pickupPoint1.getLogisticPointId()
                ),
                PickupPointSurveyType.PVZ_MARKET_BRANDED,
                false
        );

        assertThat(invalidLogisticPoints).hasSize(3);
        assertThat(invalidLogisticPoints).containsExactlyInAnyOrderElementsOf(List.of(
                pickupPointNotPvz.getLogisticPointId(),
                pickupPointNotBranded.getLogisticPointId(),
                pickupPoint1.getLogisticPointId()
        ));
    }

    @Test
    void returnsInvalidLogisticPoints_WhenForceCreateAndTaskIsActiveAndInvalidPoints() {
        pickupPointSurveyService.assignSurveyAndGetSurveyTaskDto(user1, deliveryTask1.getId());

        assertThat(surveyTaskRepository.findByIdOrThrow(surveyTask1.getId()).getStatus())
                .isEqualTo(PickupPointSurveyTaskSubtaskStatus.CREATED);

        var pickupPointNotPvz = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 3L, 3, true)
        );
        var pickupPointNotBranded = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 4L, 4L, false)
        );

        var invalidLogisticPoints = pickupPointSurveyService.createNewSurveysOnLogisticPoints(
                List.of(
                        pickupPointNotPvz.getLogisticPointId(),
                        pickupPointNotBranded.getLogisticPointId(),
                        pickupPoint1.getLogisticPointId()
                ),
                PickupPointSurveyType.PVZ_MARKET_BRANDED,
                true
        );

        assertThat(invalidLogisticPoints).hasSize(2);
        assertThat(invalidLogisticPoints).containsExactlyInAnyOrderElementsOf(List.of(
                pickupPointNotPvz.getLogisticPointId(),
                pickupPointNotBranded.getLogisticPointId()
        ));
    }
}
