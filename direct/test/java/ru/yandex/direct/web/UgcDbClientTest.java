package ru.yandex.direct.web;

import java.util.List;

import com.google.common.collect.Iterables;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.configuration.CoreConfiguration;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerFeedback;
import ru.yandex.direct.core.entity.freelancer.service.FreelancerFeedbackService;
import ru.yandex.direct.core.entity.freelancer.service.utils.FreelancerFeedbackConverter;
import ru.yandex.direct.core.entity.freelancer.service.validation.FreelancerFeedbackValidationService;
import ru.yandex.direct.ugcdb.client.UgcDbClient;
import ru.yandex.kernel.ugc.protos.direct.TDirectReview;
import ru.yandex.kernel.ugc.protos.direct.TDirectService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

/**
 * Проверяем, что все запросы корретно доходят до UGC DB.
 * Для подключения tvm необходимо в файле app-development.conf прописать tvm.enabled: true
 * и указать корректный путь к файлу secret.
 */
@Ignore("For manual run")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CoreConfiguration.class)
public class UgcDbClientTest {

    private static final String FEEDBACK_PATH = "/direct-service/1/reviews/1";
    private static final String SUCCESS_RESPONSE = "{}\n";

    private static final String MESSAGE = "{\n"
            + "    \"ContractorId\": \"1\",\n"
            + "    \"Reviews\": [\n"
            + "        {\n"
            + "            \"ReviewId\": \"1\",\n"
            + "            \"ContractorId\": \"1\",\n"
            + "            \"AuthorUserId\": 1,\n"
            + "            \"ProjectId\": \"1\",\n"
            + "            \"Moderation\": {\n"
            + "                \"Status\": \"IN_PROGRESS\",\n"
            + "                \"DeclineReason\": \"NONE\"\n"
            + "            },\n"
            + "            \"IsAnonymous\": false,\n"
            + "            \"Recommendation\": \"WILL_RECOMMEND\",\n"
            + "            \"FeedbackText\": \"8\",\n"
            + "            \"OverallMark\": 4,\n"
            + "            \"CommunicationMark\": 5,\n"
            + "            \"TimingMark\": 6,\n"
            + "            \"QualityMark\": 7,\n"
            + "            \"SkillIds\": [\n"
            + "                \"3\"\n"
            + "            ],\n"
            + "            \"InteractionDurationWeek\": 0,\n"
            + "            \"Comments\": [],\n"
            + "            \"CreateTime\": \"2017-06-28T23:28:20.000001Z\",\n"
            + "            \"UpdatedTime\": \"2017-06-28T23:28:20.000001Z\"\n"
            + "        }\n"
            + "    ]\n"
            + "}";

    private final List<FreelancerFeedback> expectedFeedbackList =
            FreelancerFeedbackConverter.convertFreelancerFeedbackFromUgcDb(parseJson(MESSAGE).getReviewsList());

    @Autowired
    UgcDbClient ugcDbClient;
    @Autowired
    FreelancerFeedbackValidationService feedbackValidationService;

    @Test
    public void ugcDbClientSmokeTest() {
        ugcDbClient.saveFeedback(1L, "1", parseJson(MESSAGE).getReviews(0));

        TDirectReview review = ugcDbClient.getFeedback(1L, "1");
        FreelancerFeedback feedback = Iterables.getFirst(
                FreelancerFeedbackConverter.convertFreelancerFeedbackFromUgcDb(review), null);
        assertThat(feedback).describedAs("Get feedback from UGC DB")
                .is(matchedBy(beanDiffer(expectedFeedbackList.get(0))));

        ugcDbClient.deleteFeedback(1L, "1");
    }

    @Test
    public void ugcDbServiceGetFeedback_smokeTest() {
        Long freelancerId = 1L;
        String feedbackId = "1";
        FreelancerFeedbackService freelancerFeedbackService =
                new FreelancerFeedbackService(feedbackValidationService, ugcDbClient);
        FreelancerFeedback freelancerFeedback =
                freelancerFeedbackService.getFreelancerFeedback(feedbackId, freelancerId);

        assertThat(freelancerFeedback).is(matchedBy(beanDiffer(expectedFeedbackList.get(0))));
    }

    @Test
    public void ugcDbServiceGetFeedbackList_smokeTest() {
        Long freelancerId = 1L;
        FreelancerFeedbackService freelancerFeedbackService =
                new FreelancerFeedbackService(feedbackValidationService, ugcDbClient);
        List<FreelancerFeedback> freelancerFeedbackList =
                freelancerFeedbackService.getFreelancerFeedbackList(freelancerId);

        assertThat(freelancerFeedbackList).is(matchedBy(beanDiffer(expectedFeedbackList)));
    }

    private TDirectService parseJson(String json) {
        JsonFormat.Parser parser = JsonFormat.parser();
        TDirectService.Builder builder = TDirectService.newBuilder();
        try {
            parser.merge(json, builder);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        return builder.build();
    }
}
