package ru.yandex.market.crm.campaign.test.utils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.campaign.domain.actions.ActionConfig;
import ru.yandex.market.crm.campaign.domain.actions.ActionVariant;
import ru.yandex.market.crm.campaign.domain.actions.PeriodicAction;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.domain.periodic.Schedule;
import ru.yandex.market.crm.campaign.domain.periodic.Schedule.DateTimeInterval;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.services.actions.periodic.PeriodicActionService;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;

import static ru.yandex.market.crm.core.test.SegmentTestUtils.plusFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;

/**
 * @author apershukov
 */
@Component
public class PeriodicActionsTestHelper {

    private final PeriodicActionService actionService;
    private final CampaignTestHelper campaignTestHelper;
    private final SegmentService segmentService;

    public PeriodicActionsTestHelper(PeriodicActionService actionService,
                                     CampaignTestHelper campaignTestHelper,
                                     SegmentService segmentService) {
        this.actionService = actionService;
        this.campaignTestHelper = campaignTestHelper;
        this.segmentService = segmentService;
    }

    public PeriodicAction prepareAction(Campaign campaign, Segment segment, ActionStep... steps) {
        String actionKey = generateActionKey();
        return prepareAction(actionKey, campaign, segment, steps);
    }

    public PeriodicAction prepareAction(String actionKey, Campaign campaign, Segment segment, ActionStep... steps) {
        ActionVariant variant = new ActionVariant();
        variant.setId(actionKey + "_a");
        variant.setPercent(100);
        variant.setSteps(List.of(steps));

        return prepareAction(actionKey, campaign, segment, variant);
    }

    public PeriodicAction prepareAction(ActionStep... steps) {
        Campaign campaign = campaignTestHelper.prepareCampaign();
        Segment segment = segmentService.addSegment(segment(plusFilter()));
        return prepareAction(campaign, segment, steps);
    }

    public PeriodicAction prepareAction(String actionKey, Campaign campaign, Segment segment, ActionVariant... variants) {
        Schedule schedule = new Schedule()
                .setTime(LocalTime.of(13, 30))
                .setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY))
                .setActiveInterval(
                        new DateTimeInterval()
                                .setBegin(LocalDateTime.now())
                                .setEnd(LocalDateTime.now().plusDays(7))
                );

        PeriodicAction action = new PeriodicAction();
        action.setKey(actionKey);
        action.setName("Test Action");
        action.setSchedule(schedule);

        action = actionService.addEntity(campaign.getId(), action);

        ActionConfig config = new ActionConfig();
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setVariants(List.of(variants));

        action.setConfig(config);
        return actionService.update(action);
    }

    public PeriodicAction prepareAction() {
        var key = generateActionKey();
        var campaign = campaignTestHelper.prepareCampaign();
        var segment = segmentService.addSegment(segment(plusFilter()));
        return prepareAction(key, campaign, segment, new ActionVariant[] {});
    }

    private static String generateActionKey() {
        return "test_action_" + IdGenerationUtils.dateTimeId();
    }
}
