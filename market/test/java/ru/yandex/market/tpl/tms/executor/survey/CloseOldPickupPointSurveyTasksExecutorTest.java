package ru.yandex.market.tpl.tms.executor.survey;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveySubtaskRepository;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTask;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTaskRepository;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTaskSubtaskStatus;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.tracker.TrackerService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.service.pickup_point_survey.PickupPointSurveyGeneratorService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

@RequiredArgsConstructor
public class CloseOldPickupPointSurveyTasksExecutorTest extends TplTmsAbstractTest {
    private static final Long UID = 1234L;
    private final PickupPointSurveyGeneratorService pickupPointSurveyGeneratorService;
    private final PickupPointSurveyTaskRepository surveyTaskRepository;
    private final PickupPointSurveySubtaskRepository surveySubtaskRepository;
    private final TestDataFactory testDataFactory;
    private final PickupPointRepository pickupPointRepository;
    private final CloseOldPickupPointSurveyTasksExecutor executor;
    private final TestUserHelper testUserHelper;
    private final TrackerService trackerService;
    private final CompanyRepository companyRepository;
    private final Clock clock;
    private User user;
    private UserShift userShift;
    private PickupPoint pickupPoint;
    private Company company;

    @BeforeEach
    public void init() {
        user = testUserHelper.findOrCreateUser(UID);
        userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
        pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L)
        );
        company = testUserHelper.findOrCreateSuperCompany();
        company.setSuperCompany(true);
        company = companyRepository.save(company);
    }

    /**
     * Даже пустой {@link AfterEach} нужен
     * для срабатывания {@link ru.yandex.market.tpl.core.test.TplAbstractTest#clearAfterTest(Object)}
     */
    @AfterEach
    void afterEach() {
        clearInvocations(trackerService);
        clearAfterTest(pickupPoint);
        clearAfterTest(company);
    }

    @Test
    public void successfullyClosedTask() {
        var surveyTask = pickupPointSurveyGeneratorService.generateSurveyTask(pickupPoint, null);
        //Устанавливаем такое время startedAt чтобы экзекьютор подобрал таску как старую и закрыл её
        surveyTask.setStartedAt(Instant.EPOCH);
        surveyTaskRepository.save(surveyTask);
        assertThat(surveyTask.getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.CREATED);
        assertThat(surveyTask.getSubtasks().get(0).getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.IN_PROGRESS);

        executor.doRealJob(null);

        assertTaskIsFinished(surveyTask);
    }

    @Test
    public void taskShouldNotBeClosed() {
        var surveyTask = pickupPointSurveyGeneratorService.generateSurveyTask(pickupPoint, null);
        assertThat(surveyTask.getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.CREATED);

        executor.doRealJob(null);
        var task = surveyTaskRepository.findByIdOrThrow(surveyTask.getId());

        assertThat(task.getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.CREATED);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void successfullyClosedTaskAndAddedSTComment(boolean stTicketPresent) {
        int wantedNumberOfInvocations = 0;
        var surveyTask = pickupPointSurveyGeneratorService.generateSurveyTask(pickupPoint, null);
        //Устанавливаем такое время startedAt чтобы экзекьютор подобрал таску как старую и закрыл её
        surveyTask.setStartedAt(Instant.EPOCH);
        surveyTask.getSubtasks().get(0).setUserShift(userShift);

        if (stTicketPresent) {
            surveyTask.setTicketUrl("TICKET-1");
            wantedNumberOfInvocations = 1;
        }
        surveyTaskRepository.save(surveyTask);

        assertThat(surveyTask.getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.CREATED);
        assertThat(surveyTask.getSubtasks().get(0).getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.IN_PROGRESS);

        executor.doRealJob(null);
        verify(trackerService, Mockito.times(wantedNumberOfInvocations))
                .addCommentForTicket(any(), any());
        assertTaskIsFinished(surveyTask);
    }

    private void assertTaskIsFinished(PickupPointSurveyTask task) {
        task = surveyTaskRepository.findByIdOrThrow(task.getId());
        var subtasks = surveySubtaskRepository.findByPickupPointSurveyTaskId(task.getId());
        assertThat(task.getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.FINISHED);
        assertThat(subtasks).isNotEmpty();
        assertThat(subtasks.get(0).getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.FINISHED);
        assertThat(task.getClosedAt()).isNotNull();
    }
}
