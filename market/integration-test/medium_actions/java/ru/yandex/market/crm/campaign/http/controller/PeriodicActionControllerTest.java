package ru.yandex.market.crm.campaign.http.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;

import ru.yandex.market.crm.campaign.domain.actions.ActionConfig;
import ru.yandex.market.crm.campaign.domain.actions.ActionVariant;
import ru.yandex.market.crm.campaign.domain.actions.PeriodicAction;
import ru.yandex.market.crm.campaign.domain.actions.StepType;
import ru.yandex.market.crm.campaign.domain.actions.periodic.ActionExecutedEvent;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.domain.periodic.Event;
import ru.yandex.market.crm.campaign.domain.periodic.EventType;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.dto.actions.PeriodicActionDto;
import ru.yandex.market.crm.campaign.services.actions.periodic.MessageTemplatesLinksDAO;
import ru.yandex.market.crm.campaign.services.actions.periodic.PeriodicActionService;
import ru.yandex.market.crm.campaign.services.actions.periodic.StartPeriodicActionTask;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.campaign.test.tms.TestScheduler;
import ru.yandex.market.crm.campaign.test.utils.CampaignTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailTemplatesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PeriodicActionsTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PeriodicEntitiesTestUtils;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.json.serialization.JsonSerializer;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.foldByCrypta;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.issueCoins;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.sendEmails;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.variant;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.plusFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;

/**
 * @author apershukov
 */
public class PeriodicActionControllerTest extends AbstractControllerMediumTest {

    @Inject
    private PeriodicActionsTestHelper actionsTestHelper;

    @Inject
    private CampaignTestHelper campaignTestHelper;

    @Inject
    private EmailTemplatesTestHelper emailTemplatesTestHelper;

    @Inject
    private MessageTemplatesLinksDAO messageTemplatesLinksDAO;

    @Inject
    private TestScheduler scheduler;

    @Inject
    private SegmentService segmentService;

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private PeriodicActionService actionService;

    @Inject
    @Qualifier("startPeriodicActionTask")
    private StartPeriodicActionTask task;

    /**
     * Регулярную акцию можно копировать
     */
    @Test
    public void testCopyAction() throws Exception {
        Campaign campaign = campaignTestHelper.prepareCampaign();
        Segment segment = segmentService.addSegment(segment(plusFilter()));
        PeriodicAction original = actionsTestHelper.prepareAction(campaign, segment, foldByCrypta());

        String copyKey = original.getKey() + "_copy";

        PeriodicActionDto dto = new PeriodicActionDto();
        dto.setCampaign(campaign);
        dto.setName("[copy] " + original.getName());
        dto.setSchedule(original.getSchedule());
        dto.setKey(copyKey);
        dto.setConfig(original.getConfig());

        mockMvc.perform(post("/api/periodic_actions/by_id/{actionId}/copy", original.getId())
                        .contentType("application/json")
                        .content(jsonSerializer.writeObjectAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk());

        PeriodicAction copy = actionService.getByKey(copyKey);
        Assertions.assertEquals(dto.getName(), copy.getName());
        Assertions.assertNotEquals(original.getId(), copy.getId());
        Assertions.assertEquals(1, copy.getVersion());
        Assertions.assertEquals(0, copy.getIteration());
        Assertions.assertFalse(copy.isEnabled());
        Assertions.assertNotNull(copy.getSchedule());

        ActionConfig config = copy.getConfig();
        Assertions.assertNotNull(config);

        TargetAudience target = config.getTarget();
        Assertions.assertNotNull(target);
        Assertions.assertEquals(segment.getId(), target.getSegment());

        List<ActionVariant> variants = config.getVariants();
        MatcherAssert.assertThat(variants, hasSize(1));

        ActionVariant variant = variants.get(0);
        Assertions.assertEquals(100, variant.getPercent());
        Assertions.assertEquals(copyKey + "_a", variant.getId());

        List<ActionStep> steps = variant.getSteps();
        MatcherAssert.assertThat(steps, hasSize(1));
        Assertions.assertEquals(StepType.FOLD_BY_CRYPTA, steps.get(0).getType());
    }

    /**
     * При удалении акции вместе с ней удаляются её связи с шаблонами сообщений
     */
    @Test
    public void testMessageTemplateLinkIsDeletedAlongWithAction() throws Exception {
        MessageTemplate<?> messageTemplate = emailTemplatesTestHelper.prepareEmailTemplate();
        ActionStep sendStep = sendEmails(messageTemplate.getId());
        PeriodicAction action = actionsTestHelper.prepareAction(sendStep);

        String actionKey = action.getKey();

        mockMvc.perform(delete("/api/periodic_actions/by_key/{actionKey}", actionKey))
                .andDo(print())
                .andExpect(status().isOk());

        Assertions.assertFalse(actionService.existsWithKey(actionKey), "Action still exists");

        MatcherAssert.assertThat(messageTemplatesLinksDAO.getActionsIds(messageTemplate.getId()), hasSize(0));
    }

    @Test
    public void testDisablePeriodicAction() throws Exception {
        Campaign campaign = campaignTestHelper.prepareCampaign();
        Segment segment = segmentService.addSegment(segment(plusFilter()));
        PeriodicAction action = actionsTestHelper.prepareAction(campaign, segment, foldByCrypta());

        actionService.enable(action.getId());

        PeriodicEntitiesTestUtils.startTask(task, action.getId());

        mockMvc.perform(post("/api/periodic_actions/by_id/{id}/disable", action.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        assertTrue(scheduler.getJobs().isEmpty());
        assertTrue(scheduler.getTriggers().isEmpty());

        action = actionService.getById(action.getId());
        Assertions.assertNull(action.getSendJobName());
        Assertions.assertNull(action.getEndNotificationJobName());

        List<Event> events = actionService.getEvents(action.getKey(), 0);
        MatcherAssert.assertThat(events, not(empty()));
        Assertions.assertEquals(Set.of(EventType.DISABLED, EventType.ACTION_EXECUTED, EventType.ENABLED),
                events.stream().map(Event::getType).collect(Collectors.toSet()));
        assertTrue(events.stream()
                .filter(event -> event instanceof ActionExecutedEvent)
                .map(event -> (ActionExecutedEvent) event)
                .allMatch(event -> event.getStatus() != StageStatus.IN_PROGRESS));
    }

    @Test
    public void testActivateThrottleEvenInEnabledSending() throws Exception {
        MessageTemplate<?> messageTemplate = emailTemplatesTestHelper.prepareEmailTemplate();
        ActionStep sendStep = sendEmails(messageTemplate.getId());
        PeriodicAction action = actionsTestHelper.prepareAction(sendStep);

        Assertions.assertFalse(action.getConfig().isFrequencyThrottleEnabled());

        mockMvc.perform(post("/api/periodic_actions/by_id/{id}/enable", action.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/periodic_actions/by_key/{key}/activateThrottle", action.getKey()))
                .andDo(print())
                .andExpect(status().isOk());

        action = actionService.getAction(action.getId());
        assertTrue(action.getConfig().isFrequencyThrottleEnabled());
    }

    /**
     * Нельзя сохранить акцию в конфигурации которой присутствуют два шага с одинаковым id
     */
    @Test
    void test400OnSavingActionWithDuplicatedStepIds() throws Exception {
        var action = actionsTestHelper.prepareAction();

        var stepId = "step_id";

        var step1 = issueCoins(111L);
        step1.setId(stepId);

        var step2 = issueCoins(222L);
        step2.setId(stepId);

        var newConfig = new ActionConfig();
        newConfig.setTarget(action.getConfig().getTarget());
        newConfig.setVariants(List.of(
                variant("a", 40, step1),
                variant("b", 40, step2)
        ));

        var dto = new PeriodicActionDto();
        dto.setName(action.getName());
        dto.setConfig(newConfig);

        mockMvc.perform(put("/api/periodic_actions/by_key/{id}", action.getKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsString(dto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
