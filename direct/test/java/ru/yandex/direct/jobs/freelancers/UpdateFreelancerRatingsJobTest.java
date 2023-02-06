package ru.yandex.direct.jobs.freelancers;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerFeedback;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerUgcModerationStatus;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerRepository;
import ru.yandex.direct.core.entity.freelancer.service.FreelancerFeedbackService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.common.db.PpcPropertyNames.ENABLE_UPDATE_FREELANCER_RATINGS;
import static ru.yandex.direct.common.db.PpcPropertyNames.MIN_FREELANCER_FEEDBACKS_COUNT;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;


@JobsTest
@ExtendWith(SpringExtension.class)
class UpdateFreelancerRatingsJobTest {
    @Autowired
    private Steps steps;

    @Autowired
    private FreelancerRepository freelancerRepository;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private FreelancerFeedbackService freelancerFeedbackService;
    private UpdateFreelancerRatingsJob job;

    private FreelancerInfo freelancerInfo;
    private int shard;

    private PpcProperty<Boolean> enableUpdateFreelancerRatingsProperty;
    private PpcProperty<Integer> minFreelancerFeedbacksCount;

    @BeforeEach
    void before() {
        freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        shard = freelancerInfo.getShard();

        freelancerFeedbackService = mock(FreelancerFeedbackService.class);

        job = new UpdateFreelancerRatingsJob(shard, freelancerRepository,
                freelancerFeedbackService, ppcPropertiesSupport);

        enableUpdateFreelancerRatingsProperty = ppcPropertiesSupport.get(ENABLE_UPDATE_FREELANCER_RATINGS);
        minFreelancerFeedbacksCount = ppcPropertiesSupport.get(MIN_FREELANCER_FEEDBACKS_COUNT);

        enableUpdateFreelancerRatingsProperty.set(true);
        minFreelancerFeedbacksCount.set(2);
    }

    private void executeJob() {
        assertThatCode(() -> job.execute())
                .doesNotThrowAnyException();
    }

    @Test
    void updateFreelancerRating_success() {
        Long freelancerId = freelancerInfo.getFreelancer().getFreelancerId();
        setFreelancerFeedbackList(freelancerId, asList(
                feedback(FreelancerUgcModerationStatus.ACCEPTED, 4L),
                feedback(FreelancerUgcModerationStatus.ACCEPTED, 3L)
        ));

        executeJob();
        checkFreelancerRatingAndFeedbackCount(freelancerId, 3.5, 2L);
    }

    @Test
    void updateFreelancerRating_whenFeatureIsDisabled_dontChangeRating() {
        enableUpdateFreelancerRatingsProperty.set(false);

        Long freelancerId = freelancerInfo.getFreelancer().getFreelancerId();
        Double oldRating = freelancerInfo.getFreelancer().getRating();
        Long oldFeedbackCount = freelancerInfo.getFreelancer().getFeedbackCount();

        setFreelancerFeedbackList(freelancerId, asList(
                feedback(FreelancerUgcModerationStatus.ACCEPTED, 4L),
                feedback(FreelancerUgcModerationStatus.ACCEPTED, 3L)
        ));

        executeJob();
        checkFreelancerRatingAndFeedbackCount(freelancerId, oldRating, oldFeedbackCount);
    }


    @Test
    void updateFreelancerRating_whenFeatureIsDisabled_dontCallGetFreelancerFeedbackList() {
        enableUpdateFreelancerRatingsProperty.set(false);

        executeJob();
        verify(freelancerFeedbackService, never()).getFreelancerFeedbackList(anyLong());
    }

    @Test
    void updateFreelancerRating_whenAcceptedFeedbacksCountLessThanMin_dontChangeRating() {
        Long freelancerId = freelancerInfo.getFreelancer().getFreelancerId();
        Double oldRating = freelancerInfo.getFreelancer().getRating();

        setFreelancerFeedbackList(freelancerId, asList(
                feedback(FreelancerUgcModerationStatus.ACCEPTED, 4L),
                feedback(FreelancerUgcModerationStatus.IN_PROGRESS, 3L),
                feedback(FreelancerUgcModerationStatus.DECLINED, 2L)
        ));

        executeJob();
        checkFreelancerRatingAndFeedbackCount(freelancerId, oldRating, 1L);
    }

    @Test
    void updateFreelancerRating_whenNoAcceptedFeedbacks_feedbackCountIsZero() {
        Long freelancerId = freelancerInfo.getFreelancer().getFreelancerId();
        Double oldRating = freelancerInfo.getFreelancer().getRating();

        setFreelancerFeedbackList(freelancerId, asList(
                feedback(FreelancerUgcModerationStatus.IN_PROGRESS, 3L),
                feedback(FreelancerUgcModerationStatus.DECLINED, 2L)
        ));

        executeJob();
        checkFreelancerRatingAndFeedbackCount(freelancerId, oldRating, 0L);
    }

    @Test
    void updateFreelancerRating_whenNoFeedbacks_feedbackCountIsZero() {
        Long freelancerId = freelancerInfo.getFreelancer().getFreelancerId();
        Double oldRating = freelancerInfo.getFreelancer().getRating();

        setFreelancerFeedbackList(freelancerId, emptyList());

        executeJob();
        checkFreelancerRatingAndFeedbackCount(freelancerId, oldRating, 0L);
    }

    @Test
    void updateFreelancerRating_whenTwoFreelancers() {
        Long freelancerId = freelancerInfo.getFreelancer().getFreelancerId();

        FreelancerInfo freelancerInfo2 = steps.freelancerSteps().createFreelancer(
                new FreelancerInfo().withClientInfo(
                        new ClientInfo().withShard(shard))
        );
        Long freelancerId2 = freelancerInfo2.getFreelancer().getFreelancerId();

        setFreelancerFeedbackList(freelancerId, asList(
                feedback(FreelancerUgcModerationStatus.ACCEPTED, 4L),
                feedback(FreelancerUgcModerationStatus.ACCEPTED, 5L),
                feedback(FreelancerUgcModerationStatus.ACCEPTED, 5L),
                feedback(FreelancerUgcModerationStatus.IN_PROGRESS, 1L)
        ));
        setFreelancerFeedbackList(freelancerId2, asList(
                feedback(FreelancerUgcModerationStatus.ACCEPTED, 4L),
                feedback(FreelancerUgcModerationStatus.ACCEPTED, 3L),
                feedback(FreelancerUgcModerationStatus.ACCEPTED, 5L),
                feedback(FreelancerUgcModerationStatus.ACCEPTED, 3L),
                feedback(FreelancerUgcModerationStatus.DECLINED, 1L)
        ));
        executeJob();

        List<Freelancer> freelancers = freelancerRepository.getByIds(shard, asList(freelancerId, freelancerId2));
        assumeThat(freelancers, hasSize(2));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(freelancers.get(0).getRating())
                    .describedAs("rating of first freelancer")
                    .isEqualTo(4.5);
            softly.assertThat(freelancers.get(0).getFeedbackCount())
                    .describedAs("feedbackCount of first freelancer")
                    .isEqualTo(3L);

            softly.assertThat(freelancers.get(1).getRating())
                    .describedAs("rating of second freelancer")
                    .isEqualTo(4.0);
            softly.assertThat(freelancers.get(1).getFeedbackCount())
                    .describedAs("feedbackCount of second freelancer")
                    .isEqualTo(4L);
        });
    }

    private FreelancerFeedback feedback(FreelancerUgcModerationStatus moderationStatus, Long overallMark) {
        return new FreelancerFeedback()
                .withModerationStatus(moderationStatus)
                .withOverallMark(overallMark);
    }

    private void checkFreelancerRatingAndFeedbackCount(Long freelancerId, Double expectedRating,
                                                       Long expectedFeedbackCount) {
        List<Freelancer> freelancers = freelancerRepository.getByIds(shard, singletonList(freelancerId));
        assumeThat(freelancers, hasSize(1));

        Freelancer expectedFreelancer = new Freelancer();
        expectedFreelancer
                .withRating(expectedRating)
                .withFeedbackCount(expectedFeedbackCount);

        assertThat(freelancers.get(0),
                beanDiffer(expectedFreelancer).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    private void setFreelancerFeedbackList(Long freelancerId, List<FreelancerFeedback> feedbacks) {
        feedbacks.forEach(f -> f.withFreelancerId(freelancerId));
        when(freelancerFeedbackService.getFreelancerFeedbackList(eq(freelancerId)))
                .thenReturn(feedbacks);
    }
}
