package ru.yandex.market.crm.campaign.services.actions;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.campaign.domain.actions.PeriodicAction;
import ru.yandex.market.crm.campaign.domain.actions.periodic.ActionExecutedEvent;
import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.domain.periodic.Event;
import ru.yandex.market.crm.campaign.domain.periodic.EventType;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.actions.periodic.PeriodicActionService;
import ru.yandex.market.crm.campaign.services.actions.periodic.StartPeriodicActionTask;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.test.AbstractServiceMediumTest;
import ru.yandex.market.crm.campaign.test.utils.CampaignTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PeriodicActionsTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PeriodicEntitiesTestUtils;
import ru.yandex.market.crm.core.domain.segment.Segment;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.foldByCrypta;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.plusFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;

public class StartPeriodicActionTaskTest extends AbstractServiceMediumTest {
    @Inject
    private CampaignTestHelper campaignTestHelper;
    @Inject
    private SegmentService segmentService;
    @Inject
    private PeriodicActionsTestHelper actionsTestHelper;
    @Inject
    private PeriodicActionService periodicActionService;
    @Inject
    private StartPeriodicActionTask task;

    /**
     * Если регулярная акция запущена, и происходит попытка повторного запуска до её завершения,
     * тогда новый запуск отменяется, при этом добавляется событие об отмене конкурирующего запуска акции
     */
    @Test
    public void testCancelConcurrentPeriodicActionLaunchIfActionAlreadyInProgress() {
        Campaign campaign = campaignTestHelper.prepareCampaign();
        Segment segment = segmentService.addSegment(segment(plusFilter()));
        PeriodicAction action = actionsTestHelper.prepareAction(campaign, segment, foldByCrypta());

        periodicActionService.enable(action.getId());

        var executedEvent = new ActionExecutedEvent();
        executedEvent.setStatus(StageStatus.IN_PROGRESS);
        periodicActionService.addEvent(action.getKey(), executedEvent);

        PeriodicEntitiesTestUtils.startTask(task, action.getId());

        List<Event> events = periodicActionService.getEvents(action.getKey(), 0);
        MatcherAssert.assertThat(events, not(empty()));
        Assertions.assertEquals(
                Set.of(EventType.CONCURRENT_LAUNCH_CANCELLED, EventType.ACTION_EXECUTED, EventType.ENABLED),
                events.stream().map(Event::getType).collect(Collectors.toSet())
        );
    }
}
