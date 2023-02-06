package ru.yandex.market.tpl.core.domain.yaforms;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.util.LinkedMultiValueMap;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTask;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTaskRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.tracker.TrackerService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.yaforms.dbqueue.survey.ProcessYaFormsSurveyAnswersPayload;
import ru.yandex.market.tpl.core.domain.yaforms.dbqueue.survey.ProcessYaFormsSurveyAnswersProcessingService;
import ru.yandex.market.tpl.core.service.pickup_point_survey.PickupPointSurveyGeneratorService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class YaFormsProcessingServiceTest extends TplAbstractTest {
    private static final Long UID = 1234L;
    private static final String SURVEY_TASK_ID = "survey_task_id";
    private static final String USER_ID = "user_id";
    private static final String PVZ_ID = "pvz_id";

    private final TrackerService trackerService;
    private final TestUserHelper testUserHelper;
    private final PickupPointRepository pickupPointRepository;
    private final TestDataFactory testDataFactory;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointSurveyGeneratorService pickupPointSurveyGeneratorService;
    private final ProcessYaFormsSurveyAnswersProcessingService processingService;
    private final PickupPointSurveyTaskRepository surveyTaskRepository;
    private final CompanyRepository companyRepository;
    private final Clock clock;

    private User user;
    private PickupPoint pickupPoint;
    private PickupPointSurveyTask surveyTask;
    private Company company;

    @BeforeEach
    public void init() {
        user = testUserHelper.findOrCreateUser(UID);
        var userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));

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

    @Test
    public void happyPathCreateNewTicketAndAssignTicketKey() {
        String newTicketKey = "TEST-1";
        when(trackerService.createPickupPointSurveyAnswersTicket(any(), any(), any(), any())).thenReturn(newTicketKey);

        LinkedMultiValueMap<String, String> surveyAnswers = getSurveyAnswersWithMandatoryFields();
        surveyAnswers.add(SURVEY_TASK_ID, String.valueOf(surveyTask.getId()));
        processingService.processPayload(new ProcessYaFormsSurveyAnswersPayload("1", surveyAnswers));

        surveyTask = surveyTaskRepository.findByIdOrThrow(surveyTask.getId());

        assertThat(surveyTask.getTicketUrl()).isEqualTo(newTicketKey);
        verify(trackerService, Mockito.times(1)).createPickupPointSurveyAnswersTicket(any(), any(), any(), any());
        verify(trackerService, Mockito.times(0)).addCommentForTicket(any(), any());
    }

    @Test
    public void happyPathAddCommentToTicket() {
        String ticketKey = "TEST-1";
        surveyTask.setTicketUrl(ticketKey);
        surveyTaskRepository.save(surveyTask);

        LinkedMultiValueMap<String, String> surveyAnswers = getSurveyAnswersWithMandatoryFields();
        processingService.processPayload(new ProcessYaFormsSurveyAnswersPayload("1", surveyAnswers));

        surveyTask = surveyTaskRepository.findByIdOrThrow(surveyTask.getId());

        assertThat(surveyTask.getTicketUrl()).isEqualTo(ticketKey);
        verify(trackerService, Mockito.times(0)).createPickupPointSurveyAnswersTicket(any(), any(), any(), any());
        verify(trackerService, Mockito.times(1)).addCommentForTicket(any(), any());
    }

    @ParameterizedTest
    @MethodSource("getMandatoryFields")
    public void unhappyPathWithoutSurveyTaskId(String fieldToExclude) {
        LinkedMultiValueMap<String, String> surveyAnswers = getSurveyAnswersWithMandatoryFields();

        surveyAnswers.remove(fieldToExclude);

        assertDoesNotThrow(
                () -> processingService.processPayload(new ProcessYaFormsSurveyAnswersPayload("1", surveyAnswers))
        );
    }

    private LinkedMultiValueMap<String, String> getSurveyAnswersWithMandatoryFields() {
        LinkedMultiValueMap<String, String> surveyAnswers = new LinkedMultiValueMap<>();
        surveyAnswers.add(SURVEY_TASK_ID, String.valueOf(surveyTask.getId()));
        surveyAnswers.add(USER_ID, String.valueOf(user.getId()));
        surveyAnswers.add(PVZ_ID, String.valueOf(pickupPoint.getId()));
        return surveyAnswers;
    }

    private static List<String> getMandatoryFields() {
        return List.of(SURVEY_TASK_ID, USER_ID, PVZ_ID);
    }
}
