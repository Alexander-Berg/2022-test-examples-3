package ru.yandex.market.crm.campaign.http.controller;

import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.crm.campaign.domain.actions.StepType;
import ru.yandex.market.crm.campaign.domain.activity.ActionStepExecutionActivityData;
import ru.yandex.market.crm.campaign.domain.activity.Activity;
import ru.yandex.market.crm.campaign.domain.activity.ActivityData;
import ru.yandex.market.crm.campaign.domain.activity.ActivityType;
import ru.yandex.market.crm.campaign.domain.activity.DefaultActivityData;
import ru.yandex.market.crm.campaign.domain.activity.SendingGenerationActivityData;
import ru.yandex.market.crm.campaign.domain.promo.entities.UsageType;
import ru.yandex.market.crm.campaign.domain.sending.SendingType;
import ru.yandex.market.crm.campaign.services.actions.ActionConstants;
import ru.yandex.market.crm.campaign.services.activity.ActivityDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumWithoutYtTest;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.tasks.domain.TaskStatus;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author apershukov
 */
public class ActivityControllerTest extends AbstractControllerMediumWithoutYtTest {

    private static <T extends ActivityData<?>> void assertActivity(Activity actual, Consumer<T> dataChecker) {
        Assertions.assertNotNull(actual.getStartTime());
        T data = (T) actual.getData();
        dataChecker.accept(data);
    }

    private static <T extends ActivityData<?>> Consumer<T> assertData(T expected) {
        return actual -> {
            Assertions.assertEquals(expected.getEntityId(), actual.getEntityId());
            Assertions.assertEquals(expected.getEntityName(), actual.getEntityName());
        };
    }

    private static Consumer<SendingGenerationActivityData> assertSendingData(SendingGenerationActivityData expected) {
        return assertData(expected)
                .andThen(actual -> {
                    Assertions.assertEquals(expected.getSendingType(), actual.getSendingType());
                    Assertions.assertEquals(expected.getUsageType(), actual.getUsageType());
                });
    }

    private static Consumer<ActionStepExecutionActivityData> assertActionData(ActionStepExecutionActivityData expected) {
        return assertData(expected)
                .andThen(actual -> {
                    Assertions.assertEquals(expected.getStepType(), actual.getStepType());
                    Assertions.assertEquals(expected.getStepId(), actual.getStepId());
                });
    }

    @Inject
    private ActivityDAO activityDAO;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Test
    public void testGetCurrentActivities() throws Exception {
        SendingGenerationActivityData sendingActivityData = new SendingGenerationActivityData()
                .setEntityId("email_sending_id")
                .setEntityName("Email Sending")
                .setSendingType(SendingType.EMAIL)
                .setUsageType(UsageType.DISPOSABLE);

        activityDAO.startActivity(sendingActivityData);

        ActionStepExecutionActivityData actionActivityData = new ActionStepExecutionActivityData()
                .setEntityId("plain_action")
                .setEntityName("Plain Action")
                .setStepType(StepType.BUILD_SEGMENT)
                .setStepId(ActionConstants.SEGMENT_STEP_ID);

        activityDAO.startActivity(actionActivityData);

        DefaultActivityData segmentActivityData = new DefaultActivityData(ActivityType.SEGMENT_BUILDING)
                .setEntityId("seg_1")
                .setEntityName("Building Segment 1");

        activityDAO.startActivity(segmentActivityData);

        long segment2ActivityId = activityDAO.startActivity(
                new DefaultActivityData(ActivityType.SEGMENT_BUILDING)
                        .setEntityId("seg_2")
                        .setEntityName("Building Segment 2")
        );
        activityDAO.completeActivity(segment2ActivityId, TaskStatus.COMPLETED);

        MvcResult result = mockMvc.perform(get("/api/activities/current"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        List<Activity> activities = jsonDeserializer.readObject(
                new TypeReference<>() {
                },
                result.getResponse().getContentAsString()
        );

        MatcherAssert.assertThat(activities, hasSize(3));
        assertActivity(activities.get(0), assertSendingData(sendingActivityData));
        assertActivity(activities.get(1), assertActionData(actionActivityData));
        assertActivity(activities.get(2), assertData(segmentActivityData));
    }
}
