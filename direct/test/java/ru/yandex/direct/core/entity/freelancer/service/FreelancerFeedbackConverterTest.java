package ru.yandex.direct.core.entity.freelancer.service;

import java.time.ZonedDateTime;
import java.util.EnumSet;

import NUgc.NSchema.Moderation;
import com.google.protobuf.Timestamp;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerFeedback;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerSkill;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerUgcModerationStatus;
import ru.yandex.direct.core.entity.freelancer.service.utils.FreelancerFeedbackConverter;
import ru.yandex.kernel.ugc.protos.direct.TDirectReview;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.DateTimeUtils.MSK;

@RunWith(JUnitParamsRunner.class)
public class FreelancerFeedbackConverterTest {
    private static final String FEEDBACK_ID = "1";
    private static final Long AUTHOR_UID = 1L;
    private static final Long FREELANCER_ID = 1L;
    private static final Long OVERALL_MARK = 1L;
    private static final boolean WILL_RECOMMEND = true;
    private static final FreelancerUgcModerationStatus MODERATION_STATUS = FreelancerUgcModerationStatus.IN_PROGRESS;
    private static final FreelancerSkill FREELANCER_SKILL = FreelancerSkill.SETTING_UP_CAMPAIGNS_FROM_SCRATCH;
    private static final String FEEDBACK_TEXT = "feedback text";
    private static final ZonedDateTime CREATED_TIME = ZonedDateTime.of(2018, 1, 2, 3, 4, 5, 6, MSK);
    private static final ZonedDateTime UPDATED_TIME = CREATED_TIME.plusDays(2).plusHours(4).plusNanos(100);

    private FreelancerFeedback feedback;
    private TDirectReview directReview;

    @Before
    public void setUp() {
        Timestamp reviewCreatedTime = Timestamp.newBuilder().setSeconds(1514851445).setNanos(6).build();
        Timestamp reviewUpdatedTime = Timestamp.newBuilder().setSeconds(1515038645).setNanos(106).build();
        TDirectReview.ERecommendationType recommendationType = TDirectReview.ERecommendationType.WILL_RECOMMEND;
        Moderation.TReviewModerationData moderationData = Moderation.TReviewModerationData.newBuilder()
                .setStatus(Moderation.TModerationStatus.EType.valueOf(MODERATION_STATUS.name())).build();
        feedback = new FreelancerFeedback()
                .withFeedbackId(FEEDBACK_ID)
                .withAuthorUid(AUTHOR_UID)
                .withFreelancerId(FREELANCER_ID)
                .withCreatedTime(CREATED_TIME)
                .withUpdatedTime(UPDATED_TIME)
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
                .setUpdatedTime(reviewUpdatedTime)
                .setOverallMark(OVERALL_MARK.intValue())
                .setRecommendation(recommendationType)
                .setModeration(moderationData)
                .addSkillIds(FREELANCER_SKILL.getSkillId().toString())
                .setFeedbackText(FEEDBACK_TEXT)
                .build();
    }

    @Test
    public void convertFreelancerFeedbackFromUgcDb_success() {
        FreelancerFeedback actual =
                FreelancerFeedbackConverter.convertFreelancerFeedbackFromUgcDb(directReview).get(0);
        assertThat(actual).is(matchedBy(beanDiffer(feedback)));
    }

    @SuppressWarnings("unused")
    private Object tDirectReviewFieldsToCompare() {
        return Arrays.array(newPath("reviewId"),
                newPath("authorUserId"),
                newPath("contractorId"),
                newPath("createTime").join("seconds"),
                newPath("createTime").join("nanos"),
                newPath("updatedTime").join("seconds"),
                newPath("updatedTime").join("nanos"),
                newPath("overallMark"),
                newPath("recommendation"),
                newPath("moderation").join("status"),
                newPath("moderation").join("declineReason"),
                newPath("skillIds"),
                newPath("feedbackText"));
    }

    @Test
    @Parameters(method = "tDirectReviewFieldsToCompare")
    public void convertFreelancerFeedbackToTDirectReview_success(BeanFieldPath pathToCompare) {
        TDirectReview actual =
                FreelancerFeedbackConverter.convertFreelancerFeedbackToTDirectReview(feedback);
        DefaultCompareStrategy strategy = DefaultCompareStrategies.onlyFields(pathToCompare);
        actual.getContractorId();
        assertThat(actual).is(matchedBy(beanDiffer(directReview).useCompareStrategy(strategy)));
    }

    @Test
    public void convertFreelancerFeedbackFromUgcDb_incorrectSkillId_exception() {
        TDirectReview incorrectReview = directReview.toBuilder().addSkillIds("-5").build();
        assertThatThrownBy(() -> FreelancerFeedbackConverter.convertFreelancerFeedbackFromUgcDb(incorrectReview))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @SuppressWarnings("unused")
    private Object existingModerationTypesFromUgc() {
        EnumSet<Moderation.TModerationStatus.EType> types = EnumSet.allOf(Moderation.TModerationStatus.EType.class);
        //При генерации модели по proto-файлу с таким типом выбрасыватся исключение
        types.remove(Moderation.TModerationStatus.EType.UNRECOGNIZED);
        return types.toArray();
    }

    @Test
    @Parameters(method = "existingModerationTypesFromUgc")
    public void convertFreelancerFeedbackFromUgcDb_checkModerationStatus(
            Moderation.TModerationStatus.EType moderationType) {
        TDirectReview review = directReview.toBuilder().setModeration(Moderation.TReviewModerationData.newBuilder()
                .setStatus(moderationType)).build();
        FreelancerFeedback actual =
                FreelancerFeedbackConverter.convertFreelancerFeedbackFromUgcDb(review).get(0);
        assertThat(actual).isNotNull();
    }

    @SuppressWarnings("unused")
    private Object existingDeclineReasonTypesFromUgc() {
        EnumSet<Moderation.TTextDeclineReason.EType> types = EnumSet.allOf(Moderation.TTextDeclineReason.EType.class);
        //При генерации модели по proto-файлу с таким типом выбрасыватся исключение
        types.remove(Moderation.TTextDeclineReason.EType.UNRECOGNIZED);
        return types.toArray();
    }

    @Test
    @Parameters(method = "existingDeclineReasonTypesFromUgc")
    public void convertFreelancerFeedbackFromUgcDb_checkDeclineReason(
            Moderation.TTextDeclineReason.EType declineReason) {
        TDirectReview review = directReview.toBuilder().setModeration(Moderation.TReviewModerationData.newBuilder()
                .setDeclineReason(declineReason)).build();
        FreelancerFeedback actual =
                FreelancerFeedbackConverter.convertFreelancerFeedbackFromUgcDb(review).get(0);
        assertThat(actual).isNotNull();
    }
}
