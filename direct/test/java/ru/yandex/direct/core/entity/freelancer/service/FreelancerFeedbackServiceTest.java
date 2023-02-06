package ru.yandex.direct.core.entity.freelancer.service;

import java.time.ZonedDateTime;
import java.util.List;

import NUgc.NSchema.Moderation;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Timestamp;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerFeedback;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerSkill;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerUgcDeclineReason;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerUgcModerationStatus;
import ru.yandex.direct.core.entity.freelancer.service.validation.FreelancerDefects;
import ru.yandex.direct.core.entity.freelancer.service.validation.FreelancerFeedbackValidationService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.ugcdb.client.UgcDbClient;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.kernel.ugc.protos.direct.TDirectReview;
import ru.yandex.kernel.ugc.protos.direct.TDirectService;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.DateTimeUtils.MSK;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class FreelancerFeedbackServiceTest {

    private FreelancerFeedbackService feedbackService;

    @Mock
    private UgcDbClient ugcDbClient;

    @Mock
    private FreelancerFeedbackValidationService feedbackValidationService =
            mock(FreelancerFeedbackValidationService.class);

    @Captor
    private ArgumentCaptor<Long> freelancerIdCaptor;
    @Captor
    private ArgumentCaptor<String> feeedbackIdCaptor;
    @Captor
    private ArgumentCaptor<TDirectReview> messageCaptor;

    private static final String SUCCESS_RESPONSE = "{}\n";
    private static final String FEEDBACK_ID = "1";
    private static final Long AUTHOR_UID = 1L;
    private static final Long FREELANCER_ID = 1L;
    private static final Long OVERALL_MARK = 1L;
    private static final boolean WILL_RECOMMEND = true;
    private static final FreelancerUgcModerationStatus MODERATION_STATUS = FreelancerUgcModerationStatus.IN_PROGRESS;
    private static final FreelancerSkill FREELANCER_SKILL = FreelancerSkill.SETTING_UP_CAMPAIGNS_FROM_SCRATCH;
    private static final String FEEDBACK_TEXT = "feedback text";
    private static final ZonedDateTime CREATED_TIME = ZonedDateTime.of(2018, 1, 2, 3, 4, 5, 6, MSK);

    private FreelancerFeedback feedback;
    private TDirectReview directReview;
    private TDirectService directService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(ValidationResult.success(feedback)).when(feedbackValidationService)
                .validateAddFeedbackForFreelancer(any(), any());
        doReturn(ValidationResult.success(feedback)).when(feedbackValidationService)
                .validateUpdateFreelancerFeedback(any(), any());
        doReturn(ValidationResult.success(feedback)).when(feedbackValidationService)
                .validateDeleteFreelancerFeedback(any(), any());
        feedbackService = new FreelancerFeedbackService(feedbackValidationService, ugcDbClient);

        Timestamp reviewCreatedTime =
                Timestamp.newBuilder().setSeconds(CREATED_TIME.toEpochSecond()).setNanos(CREATED_TIME.getNano())
                        .build();
        TDirectReview.ERecommendationType recommendationType = TDirectReview.ERecommendationType.WILL_RECOMMEND;
        Moderation.TReviewModerationData moderationData = Moderation.TReviewModerationData.newBuilder()
                .setStatus(Moderation.TModerationStatus.EType.valueOf(MODERATION_STATUS.name())).build();
        feedback = new FreelancerFeedback()
                .withFeedbackId(FEEDBACK_ID)
                .withAuthorUid(AUTHOR_UID)
                .withFreelancerId(FREELANCER_ID)
                .withCreatedTime(CREATED_TIME)
                .withOverallMark(OVERALL_MARK)
                .withWillRecommend(WILL_RECOMMEND)
                .withModerationStatus(MODERATION_STATUS)
                .withSkills(singletonList(FREELANCER_SKILL))
                .withFeedbackText(FEEDBACK_TEXT);
        directReview = TDirectReview.newBuilder()
                .setReviewId(FEEDBACK_ID)
                .setAuthorUserId(AUTHOR_UID.intValue())
                .setContractorId(FREELANCER_ID.toString())
                .setCreateTime(reviewCreatedTime)
                .setOverallMark(OVERALL_MARK.intValue())
                .setRecommendation(recommendationType)
                .setModeration(moderationData)
                .addSkillIds(FREELANCER_SKILL.getSkillId().toString())
                .setFeedbackText(FEEDBACK_TEXT)
                .build();
        directService = TDirectService.newBuilder()
                .addReviews(directReview)
                .setContractorId(FREELANCER_ID.toString())
                .build();
    }

    @Test
    public void addFeedbackForFreelancer_success() {
        SoftAssertions sa = new SoftAssertions();
        feedback.setModerationStatus(null);

        Result<String> result = feedbackService.addFeedbackForFreelancer(ClientId.fromLong(1L), feedback);

        verify(ugcDbClient)
                .saveFeedback(freelancerIdCaptor.capture(), feeedbackIdCaptor.capture(), messageCaptor.capture());

        MessageOrBuilder message = messageCaptor.getValue();

        sa.assertThat(message.getClass()).isEqualTo(TDirectReview.class);
        sa.assertThat(((TDirectReview) message).hasModeration()).isFalse();
        sa.assertThat(feeedbackIdCaptor.getValue()).isEqualTo(feedback.getFeedbackId());
        sa.assertThat(freelancerIdCaptor.getValue()).isEqualTo(FREELANCER_ID);
        sa.assertThat(result.getResult()).isEqualTo(feedback.getFeedbackId());
        sa.assertAll();
    }

    @Test
    public void deleteFreelancerFeedback_success() {
        SoftAssertions sa = new SoftAssertions();
        doReturn(directReview).when(ugcDbClient).getFeedback(anyLong(), anyString());

        Result<String> result = feedbackService
                .deleteFreelancerFeedback(feedback.getAuthorUid(), feedback.getFeedbackId(),
                        feedback.getFreelancerId());

        verify(ugcDbClient).deleteFeedback(freelancerIdCaptor.capture(), feeedbackIdCaptor.capture());

        sa.assertThat(feeedbackIdCaptor.getValue()).isEqualTo(feedback.getFeedbackId());
        sa.assertThat(freelancerIdCaptor.getValue()).isEqualTo(FREELANCER_ID);
        sa.assertThat(result.getResult()).isEqualTo(feedback.getFeedbackId());
        sa.assertAll();
    }

    @Test
    public void deleteFreelancerFeedback_feedbackNotFound_exception() {
        doReturn(null).when(ugcDbClient).getFeedback(anyLong(), anyString());

        Result<String> result = feedbackService
                .deleteFreelancerFeedback(feedback.getAuthorUid(), feedback.getFeedbackId(),
                        feedback.getFreelancerId());

        assertThat(result.getValidationResult()).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(), FreelancerDefects.feedbackNotFound())
        )));
    }

    @Test
    public void updateFreelancerFeedback_success() {
        SoftAssertions sa = new SoftAssertions();
        doReturn(directReview).when(ugcDbClient).getFeedback(anyLong(), anyString());

        Result<String> result = feedbackService.updateFreelancerFeedback(feedback.getAuthorUid(), feedback);

        verify(ugcDbClient)
                .saveFeedback(freelancerIdCaptor.capture(), feeedbackIdCaptor.capture(), messageCaptor.capture());
        MessageOrBuilder message = messageCaptor.getValue();

        sa.assertThat(message.getClass()).isEqualTo(TDirectReview.class);
        sa.assertThat(((TDirectReview) message).hasModeration()).isFalse();
        sa.assertThat(feeedbackIdCaptor.getValue()).isEqualTo(feedback.getFeedbackId());
        sa.assertThat(freelancerIdCaptor.getValue()).isEqualTo(FREELANCER_ID);
        sa.assertThat(result.getResult()).isEqualTo(feedback.getFeedbackId());
        sa.assertAll();
    }

    @Test
    public void updateFreelancerFeedback_feedbackNotFound_exception() {
        doReturn(null).when(ugcDbClient).getFeedback(anyLong(), anyString());

        Result<String> result = feedbackService.updateFreelancerFeedback(feedback.getAuthorUid(), feedback);

        assertThat(result.getValidationResult()).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(), FreelancerDefects.feedbackNotFound())
        )));
    }

    @Test
    public void getFreelancerFeedbackList_success() {
        SoftAssertions sa = new SoftAssertions();
        doReturn(directService.getReviewsList()).when(ugcDbClient).getFeedbackList(anyLong());

        List<FreelancerFeedback> freelancerFeedbackList =
                feedbackService.getFreelancerFeedbackList(feedback.getFreelancerId());

        //noinspection unchecked
        verify(ugcDbClient).getFeedbackList(freelancerIdCaptor.capture());
        sa.assertThat(freelancerIdCaptor.getValue()).isEqualTo(FREELANCER_ID);
        sa.assertThat(freelancerFeedbackList).is(matchedBy(beanDiffer(singletonList(feedback))));
        sa.assertAll();
    }

    @Test
    public void getFreelancerFeedbackList_nullResponse_success() {
        doReturn(null).when(ugcDbClient).getFeedbackList(anyLong());

        List<FreelancerFeedback> freelancerFeedbackList =
                feedbackService.getFreelancerFeedbackList(feedback.getFreelancerId());

        assertThat(freelancerFeedbackList).isEmpty();
    }

    @Test
    public void getFreelancerFeedback_success() {
        SoftAssertions sa = new SoftAssertions();
        doReturn(directReview).when(ugcDbClient).getFeedback(anyLong(), anyString());

        FreelancerFeedback freelancerFeedback =
                feedbackService.getFreelancerFeedback(feedback.getFeedbackId(), feedback.getFreelancerId());

        //noinspection unchecked
        verify(ugcDbClient).getFeedback(freelancerIdCaptor.capture(), feeedbackIdCaptor.capture());

        sa.assertThat(feeedbackIdCaptor.getValue()).isEqualTo(feedback.getFeedbackId());
        sa.assertThat(freelancerIdCaptor.getValue()).isEqualTo(FREELANCER_ID);
        sa.assertThat(freelancerFeedback).is(matchedBy(beanDiffer(feedback)));
        sa.assertAll();
    }

    @Test
    public void getFreelancerFeedback_nullResponse_success() {
        doReturn(null).when(ugcDbClient).getFeedback(anyLong(), anyString());

        FreelancerFeedback freelancerFeedback =
                feedbackService.getFreelancerFeedback(feedback.getFeedbackId(), feedback.getFreelancerId());

        assertThat(freelancerFeedback).isEqualTo(null);
    }

    @Test
    public void updateFreelancerFeedbackModeration_success() {
        SoftAssertions sa = new SoftAssertions();
        doReturn(directReview).when(ugcDbClient).getFeedback(anyLong(), anyString());

        Result<String> result = feedbackService.updateFreelancerFeedbackModeration(feedback.getFeedbackId(),
                feedback.getFreelancerId(), FreelancerUgcModerationStatus.DECLINED,
                FreelancerUgcDeclineReason.NOT_OPINION);

        verify(ugcDbClient)
                .saveFeedback(freelancerIdCaptor.capture(), feeedbackIdCaptor.capture(), messageCaptor.capture());
        MessageOrBuilder message = messageCaptor.getValue();

        sa.assertThat(message.getClass()).isEqualTo(TDirectReview.class);
        sa.assertThat(feeedbackIdCaptor.getValue()).isEqualTo(feedback.getFeedbackId());
        sa.assertThat(freelancerIdCaptor.getValue()).isEqualTo(FREELANCER_ID);
        sa.assertThat(result.getResult()).isEqualTo(feedback.getFeedbackId());
        sa.assertAll();
    }

    @Test
    public void updateFreelancerFeedbackModeration_feedbackNotFound_exception() {
        doReturn(null).when(ugcDbClient).getFeedback(anyLong(), anyString());

        Result<String> result = feedbackService.updateFreelancerFeedbackModeration(feedback.getFeedbackId(),
                feedback.getFreelancerId(), FreelancerUgcModerationStatus.ACCEPTED, null);

        assertThat(result.getValidationResult()).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(), FreelancerDefects.feedbackNotFound())
        )));
    }
}
