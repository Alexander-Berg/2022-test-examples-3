package ru.yandex.market.tpl.core.service.pickup_point_survey;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.util.LinkedMultiValueMap;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.question.PickupPointSurveyQuestion;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.question.PickupPointSurveyQuestionRepository;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTask;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTaskRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.tracker.TrackerService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.yaforms.dbqueue.survey.ProcessYaFormsSurveyAnswersPayload;
import ru.yandex.market.tpl.core.domain.yaforms.dbqueue.survey.ProcessYaFormsSurveyAnswersProcessingService;
import ru.yandex.market.tpl.core.service.pickup_point_survey.ticket.PickupPointSurveyTicketService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.tracker.TrackerService.CRITICAL_PRIORITY;
import static ru.yandex.market.tpl.core.domain.tracker.TrackerService.NORMAL_PRIORITY;
import static ru.yandex.market.tpl.core.service.pickup_point_survey.ticket.PickupPointSurveyDescriptionService.SURVEY_TASK_ID;

@RequiredArgsConstructor
public class PickupPointSurveyTicketServiceTest extends TplAbstractTest {
    private final PickupPointSurveyTicketService ticketService;
    private static final Long UID = 1234L;
    private static final String SURVEY_TASK_ID = "survey_task_id";
    private static final String USER_ID = "user_id";
    private static final String PVZ_ID = "pvz_id";

    private final TestUserHelper testUserHelper;
    private final PickupPointRepository pickupPointRepository;
    private final TestDataFactory testDataFactory;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointSurveyGeneratorService pickupPointSurveyGeneratorService;
    private final TrackerService trackerService;
    private final PickupPointSurveyTaskRepository surveyTaskRepository;
    private final PickupPointSurveyQuestionRepository surveyQuestionRepository;
    private final CompanyRepository companyRepository;
    private final Clock clock;
    private final ProcessYaFormsSurveyAnswersProcessingService processYaFormsSurveyAnswersProcessingService;

    private User user;
    private PickupPoint pickupPoint;
    private PickupPointSurveyTask surveyTask;
    private Company company;
    private UserShift userShift;

    @BeforeEach
    public void init() {
        user = testUserHelper.findOrCreateUser(UID);
        userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));

        pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L)
        );
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);

        surveyTask = pickupPointSurveyGeneratorService.generateSurveyTask(pickupPoint, null);
        company = testUserHelper.findOrCreateSuperCompany();
        company.setSuperCompany(true);
        company = companyRepository.save(company);
    }

    @AfterEach
    public void after() {
        clearInvocations(trackerService);
        clearAfterTest(pickupPoint);
        clearAfterTest(company);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void happyPathCreateNewTicketAndAssignTicketKey(boolean validAnswers) {
        String newTicketKey = "TEST-1";
        when(trackerService.createPickupPointSurveyAnswersTicket(any(), any(), any(), any())).thenReturn(newTicketKey);

        LinkedMultiValueMap<String, String> surveyAnswers = getSurveyAnswersWithMandatoryFields();
        addSurveyAnswers(surveyAnswers, validAnswers);
        surveyAnswers.add(SURVEY_TASK_ID, String.valueOf(surveyTask.getId()));
        processYaFormsSurveyAnswersProcessingService.processPayload(new ProcessYaFormsSurveyAnswersPayload("1", surveyAnswers));

        surveyTask = surveyTaskRepository.findByIdOrThrow(surveyTask.getId());

        assertThat(surveyTask.getTicketUrl()).isEqualTo(newTicketKey);
        verify(trackerService, Mockito.times(1)).createPickupPointSurveyAnswersTicket(any(), any(), any(),
                eq(validAnswers ? NORMAL_PRIORITY : CRITICAL_PRIORITY));
        verify(trackerService, Mockito.never()).addCommentForTicket(any(), any());
        verify(trackerService, Mockito.times(validAnswers ? 1 : 0)).closeTicket(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void happyPathAddCommentToTicket(boolean validAnswers) {
        String ticketKey = "TEST-1";
        surveyTask.setTicketUrl(ticketKey);
        surveyTaskRepository.save(surveyTask);

        LinkedMultiValueMap<String, String> surveyAnswers = getSurveyAnswersWithMandatoryFields();
        addSurveyAnswers(surveyAnswers, validAnswers);
        ticketService.createStTicketOnCompletion(surveyAnswers, surveyTask);

        surveyTask = surveyTaskRepository.findByIdOrThrow(surveyTask.getId());

        assertThat(surveyTask.getTicketUrl()).isEqualTo(ticketKey);
        verify(trackerService, Mockito.never()).createPickupPointSurveyAnswersTicket(any(), any(), any(), any());
        verify(trackerService, Mockito.times(1)).addCommentForTicket(any(), any());
        verify(trackerService, Mockito.times(validAnswers ? 1 : 0)).closeTicket(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void successfullyClosedTaskAndAddedSTComment(boolean stTicketPresent) {
        int wantedNumberOfInvocations = 0;
        surveyTask.getSubtasks().get(0).setUserShift(userShift);

        if (stTicketPresent) {
            surveyTask.setTicketUrl("TICKET-1");
            wantedNumberOfInvocations = 1;
        }
        surveyTaskRepository.save(surveyTask);

        ticketService.addClosedCommentForStTicket(surveyTask, 3);
        verify(trackerService, Mockito.times(wantedNumberOfInvocations))
                .addCommentForTicket(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {USER_ID, PVZ_ID, SURVEY_TASK_ID})
    public void doNotThrowExceptionIfMandatoryFieldsAreMissing(String fieldToExclude) {
        LinkedMultiValueMap<String, String> surveyAnswers = getSurveyAnswersWithMandatoryFields();

        surveyAnswers.remove(fieldToExclude);
        var payload = new ProcessYaFormsSurveyAnswersPayload("1", surveyAnswers);
        assertDoesNotThrow(() -> processYaFormsSurveyAnswersProcessingService.processPayload(payload));
    }

    private LinkedMultiValueMap<String, String> getSurveyAnswersWithMandatoryFields() {
        LinkedMultiValueMap<String, String> surveyAnswers = new LinkedMultiValueMap<>();
        surveyAnswers.add(SURVEY_TASK_ID, String.valueOf(surveyTask.getId()));
        surveyAnswers.add(USER_ID, String.valueOf(user.getId()));
        surveyAnswers.add(PVZ_ID, String.valueOf(pickupPoint.getId()));
        return surveyAnswers;
    }

    private void addSurveyAnswers(LinkedMultiValueMap<String, String> survey, boolean validAnswers) {
        List<PickupPointSurveyQuestion> allQuestions = surveyQuestionRepository.findAll();
        for (PickupPointSurveyQuestion question : allQuestions) {
            if (!validAnswers) {
                survey.add(question.getQuestion(), "INVALID_ANSWER");
                continue;
            }
            survey.addAll(question.getQuestion(), question.getAnswers());
        }
    }
}
