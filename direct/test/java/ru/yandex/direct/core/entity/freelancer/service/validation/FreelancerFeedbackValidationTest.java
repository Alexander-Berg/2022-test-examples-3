package ru.yandex.direct.core.entity.freelancer.service.validation;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerFeedback;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProject;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectStatus;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerSkill;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerProjectRepository;
import ru.yandex.direct.core.entity.freelancer.service.utils.FreelancerFeedbackConverter;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.ugcdb.client.UgcDbClient;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.kernel.ugc.protos.direct.TDirectReview;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class FreelancerFeedbackValidationTest {

    private static final Long OPERATOR_UID = 2L;
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);


    private FreelancerFeedbackValidationService feedbackValidationService;
    private FreelancerFeedback correctFeedback;

    private final FreelancerFeedbackValidator feedbackValidator = mock(FreelancerFeedbackValidator.class);

    private final FreelancerProjectRepository freelancerProjectRepository = mock(FreelancerProjectRepository.class);
    private final ShardHelper shardHelper = mock(ShardHelper.class);
    private final ClientService clientService = mock(ClientService.class);
    private final UgcDbClient ugcDbClient = mock(UgcDbClient.class);

    @Before
    public void setUp() {
        feedbackValidationService = new FreelancerFeedbackValidationService(
                freelancerProjectRepository, shardHelper, feedbackValidator, clientService, ugcDbClient);

        correctFeedback = new FreelancerFeedback()
                .withFeedbackId("feedbackId")
                .withAuthorUid(1L)
                .withFreelancerId(2L)
                .withCreatedTime(ZonedDateTime.now())
                .withOverallMark(1L)
                .withWillRecommend(true)
                .withSkills(Collections.singletonList(FreelancerSkill.CAMPAIGN_CONDUCTING))
                .withFeedbackText("feedback text");
        Client client = new Client().withClientId(1L).withChiefUid(1L);
        doReturn(client).when(clientService).getClient(any());
        doReturn(ValidationResult.success(correctFeedback)).when(feedbackValidator).apply(any());
    }

    @Test
    public void validateUpdateFreelancerFeedback_success() {
        ValidationResult<FreelancerFeedback, Defect> result =
                feedbackValidationService
                        .validateUpdateFreelancerFeedback(correctFeedback.getAuthorUid(), correctFeedback);
        verify(feedbackValidator).apply(correctFeedback);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateUpdateFreelancerFeedback_operatorNotFeedbackAuthor() {
        ValidationResult<FreelancerFeedback, Defect> result =
                feedbackValidationService.validateUpdateFreelancerFeedback(OPERATOR_UID, correctFeedback);

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(FreelancerFeedback.AUTHOR_UID)),
                        FreelancerDefects.operatorHasWrongRole()))));
    }

    @Test
    public void validateDeleteFreelancerFeedback_success() {
        ValidationResult<FreelancerFeedback, Defect> result =
                feedbackValidationService
                        .validateDeleteFreelancerFeedback(correctFeedback.getAuthorUid(), correctFeedback);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateDeleteFreelancerFeedback_operatorNotFeedbackAuthor() {
        ValidationResult<FreelancerFeedback, Defect> result =
                feedbackValidationService.validateDeleteFreelancerFeedback(OPERATOR_UID, correctFeedback);

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(FreelancerFeedback.AUTHOR_UID)),
                        FreelancerDefects.operatorHasWrongRole()))));
    }

    @Test
    public void validateAddFeedbackForFreelancer_success() {
        FreelancerProject project = new FreelancerProject()
                .withClientId(CLIENT_ID.asLong())
                .withFreelancerId(correctFeedback.getFreelancerId())
                .withStatus(FreelancerProjectStatus.INPROGRESS)
                .withStartedTime(LocalDateTime.now());

        doReturn(singletonList(project)).when(freelancerProjectRepository).get(anyInt(), any(), any());
        doReturn(emptyList()).when(ugcDbClient).getFeedbackList(anyLong());

        ValidationResult<FreelancerFeedback, Defect> result =
                feedbackValidationService.validateAddFeedbackForFreelancer(CLIENT_ID, correctFeedback);

        verify(feedbackValidator).apply(correctFeedback);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddFeedbackForFreelancer_operatorIsFreelancer() {
        FreelancerFeedback feedback = correctFeedback.withFreelancerId(correctFeedback.getAuthorUid());

        ValidationResult<FreelancerFeedback, Defect> result =
                feedbackValidationService.validateAddFeedbackForFreelancer(CLIENT_ID, feedback);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field(FreelancerFeedback.FREELANCER_ID)),
                        FreelancerDefects.operatorHasWrongRole())
        )));
    }

    @Test
    public void validateAddFeedbackForFreelancer_operatorIsNotChief() {
        Client client = new Client().withClientId(1L).withChiefUid(2L);
        doReturn(client).when(clientService).getClient(any());

        ValidationResult<FreelancerFeedback, Defect> result =
                feedbackValidationService.validateAddFeedbackForFreelancer(CLIENT_ID, correctFeedback);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field(FreelancerFeedback.AUTHOR_UID)),
                        FreelancerDefects.operatorIsNotChief())
        )));
    }

    @Test
    public void validateAddFeedbackForFreelancer_notCommonProjects() {
        ValidationResult<FreelancerFeedback, Defect> result =
                feedbackValidationService.validateAddFeedbackForFreelancer(CLIENT_ID, correctFeedback);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(),
                        FreelancerDefects.operatorHasNoProjectWithFreelancer())
        )));
    }

    @Test
    public void validateAddFeedbackForFreelancer_secondFeedback() {
        FreelancerProject project = new FreelancerProject()
                .withClientId(CLIENT_ID.asLong())
                .withFreelancerId(correctFeedback.getFreelancerId())
                .withStatus(FreelancerProjectStatus.INPROGRESS)
                .withStartedTime(LocalDateTime.now());

        doReturn(singletonList(project)).when(freelancerProjectRepository).get(anyInt(), any(), any());
        TDirectReview tDirectReview =
                FreelancerFeedbackConverter.convertFreelancerFeedbackToTDirectReview(correctFeedback);
        doReturn(singletonList(tDirectReview)).when(ugcDbClient).getFeedbackList(anyLong());

        ValidationResult<FreelancerFeedback, Defect> result =
                feedbackValidationService.validateAddFeedbackForFreelancer(CLIENT_ID, correctFeedback);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(),
                        FreelancerDefects.feedbackAlreadyExist())
        )));
    }
}
