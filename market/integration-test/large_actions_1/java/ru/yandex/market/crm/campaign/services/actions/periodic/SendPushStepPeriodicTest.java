package ru.yandex.market.crm.campaign.services.actions.periodic;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.campaign.domain.actions.PeriodicAction;
import ru.yandex.market.crm.campaign.domain.actions.periodic.ActionExecutedEvent;
import ru.yandex.market.crm.campaign.domain.actions.status.BuildSegmentStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.SendPushesStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.steps.SendPushesStep;
import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.actions.PeriodicActionDAO;
import ru.yandex.market.crm.campaign.services.actions.tasks.ExecuteActionTaskData;
import ru.yandex.market.crm.campaign.services.actions.tasks.ExecuteActionTaskData.StepData;
import ru.yandex.market.crm.campaign.services.actions.tasks.ExecutePeriodicActionTask;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.CampaignTestHelper;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.campaign.test.utils.ClusterTasksTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PeriodicActionsTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PushTemplatesTestHelper;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.AppMetricaHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.action.StepOutputRow;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.tasks.domain.ExecutionResult;
import ru.yandex.market.crm.tasks.domain.TaskStatus;
import ru.yandex.market.crm.tasks.services.ClusterTasksService;
import ru.yandex.market.crm.yt.client.YtClient;

import static ru.yandex.market.crm.campaign.services.actions.ActionConstants.SEGMENT_STEP_ID;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.sendPushes;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithSubscription;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithToken;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.mobilesFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.STORE_PUSH_PERSONAL_ADVERTISING;

/**
 * @author apershukov
 */
public class SendPushStepPeriodicTest extends AbstractServiceLargeTest {

    private static final String UUID_1 = "uuid-1";
    private static final String UUID_2 = "uuid-2";
    private static final String UUID_3 = "uuid-3";
    private static final String UUID_4 = "uuid-4";
    private static final String UUID_5 = "uuid-5";

    private static final String DEVICE_ID_1 = "device_id_1";
    private static final String DEVICE_ID_2 = "device_id_2";
    private static final String DEVICE_ID_3 = "device_id_3";
    private static final String DEVICE_ID_4 = "device_id_4";
    private static final String DEVICE_ID_5 = "device_id_5";

    private static final String DEVICE_ID_HASH_1 = "device_id_hash_1";
    private static final String DEVICE_ID_HASH_2 = "device_id_hash_2";
    private static final String DEVICE_ID_HASH_3 = "device_id_hash_3";
    private static final String DEVICE_ID_HASH_4 = "device_id_hash_4";
    private static final String DEVICE_ID_HASH_5 = "device_id_hash_5";

    @Inject
    private PushTemplatesTestHelper pushTemplatesTestHelper;

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private CampaignTestHelper campaignTestHelper;

    @Inject
    private SegmentService segmentService;

    @Inject
    private PeriodicActionsTestHelper actionsTestHelper;

    @Inject
    private PeriodicActionYtPathsFactory ytPathsFactory;

    @Inject
    private PeriodicActionDAO periodicActionDAO;

    @Inject
    private YtClient ytClient;

    @Inject
    private ActionEventsDAO actionEventsDAO;

    @Inject
    private ClusterTasksService clusterTasksService;

    @Inject
    private AppMetricaHelper appMetricaHelper;

    @Inject
    private ClusterTasksTestHelper clusterTasksTestHelper;

    @Inject
    private ExecutePeriodicActionTask executeActionTask;

    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareUserTables();
        ytSchemaTestHelper.prepareMobileAppInfoFactsTable();
        ytSchemaTestHelper.prepareMetrikaAppFactsTable();
        ytSchemaTestHelper.preparePushTokenStatusesTable();
        ytSchemaTestHelper.prepareChytPassportUuidsTable();
        ytSchemaTestHelper.prepareCommunicationsTable();
        ytSchemaTestHelper.prepareChytUuidsWithTokensTable();
        ytSchemaTestHelper.prepareChytUuidsWithSubscriptionsTable();
    }

    @Test
    public void testSendPushes() throws Exception {
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2),
                chytUuidWithToken(UUID_3, DEVICE_ID_3, DEVICE_ID_HASH_3),
                chytUuidWithToken(UUID_4, DEVICE_ID_4, DEVICE_ID_HASH_4),
                chytUuidWithToken(UUID_5, DEVICE_ID_5, DEVICE_ID_HASH_5)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, true),
                chytUuidWithSubscription(UUID_2, STORE_PUSH_GENERAL_ADVERTISING, true),
                chytUuidWithSubscription(UUID_3, STORE_PUSH_GENERAL_ADVERTISING, false),
                chytUuidWithSubscription(UUID_4, STORE_PUSH_PERSONAL_ADVERTISING, true)
        );

        var template = pushTemplatesTestHelper.prepare();
        SendPushesStep sendStep = sendPushes(template.getId());

        PeriodicAction action = prepareAction(sendStep);

        prepareInput(action, UUID_1, UUID_2, UUID_3, UUID_4, UUID_5);

        appMetricaHelper.expectDevices(DEVICE_ID_HASH_1, DEVICE_ID_HASH_2);

        executeStep(sendStep, action);

        appMetricaHelper.verify();

        String groupName = appMetricaHelper.getCreatedGroups().poll(5, TimeUnit.SECONDS).getName();
        Assertions.assertEquals(action.getOuterId() + "_" + sendStep.getId() + "_1", groupName);
    }

    @NotNull
    private PeriodicAction prepareAction(SendPushesStep sendStep) {
        Campaign campaign = campaignTestHelper.prepareCampaign();
        Segment segment = segmentService.addSegment(segment(mobilesFilter()));
        PeriodicAction action = actionsTestHelper.prepareAction(campaign, segment, sendStep);
        action.setIteration(1);
        periodicActionDAO.updateIteration(action.getId(), action.getIteration());
        return action;
    }

    /**
     * Эмулирует ситуацию в которой шаг сегментации уже выполнен и пришла очередь шага отправки пушей
     */
    private void executeStep(SendPushesStep sendStep, PeriodicAction action) {
        ActionExecutedEvent event = new ActionExecutedEvent();
        event.setActionConfig(action.getConfig());
        event.setStatus(StageStatus.IN_PROGRESS);
        event.setStepStatuses(Map.of(
                SEGMENT_STEP_ID, new BuildSegmentStepStatus().setStageStatus(StageStatus.FINISHED),
                sendStep.getId(), new SendPushesStepStatus().setStageStatus(StageStatus.IN_PROGRESS)
        ));

        long eventId = actionEventsDAO.addEvent(action.getKey(), event);

        StepData segmentStepData = new StepData();
        segmentStepData.setExecutionResult(ExecutionResult.ofStatus(TaskStatus.COMPLETED));

        ExecuteActionTaskData taskData = new ExecuteActionTaskData()
                .setActionId(action.getId())
                .setIteration(1)
                .setEventId(eventId)
                .setGlobalControlSalt("111")
                .setStep(SEGMENT_STEP_ID, segmentStepData)
                .setStep(sendStep.getId(), new StepData());

        long taskId = clusterTasksService.submitTask(executeActionTask.getId(), taskData);
        event.setTaskId(taskId);
        actionEventsDAO.updateEvent(eventId, event);

        clusterTasksTestHelper.waitCompleted(taskId, Duration.ofMinutes(30));
    }

    private void prepareInput(PeriodicAction action, String... uuids) {
        YPath inputPath = ytPathsFactory.create(action)
                .getStepDirectory(SEGMENT_STEP_ID)
                .child(action.getConfig().getVariants().get(0).getId());

        List<StepOutputRow> rows = Stream.of(uuids)
                .map(uuid -> {
                    StepOutputRow row = new StepOutputRow();
                    row.setIdValue(uuid);
                    row.setIdType(UidType.UUID);
                    row.setData(new StepOutputRow.Data());
                    return row;
                })
                .collect(Collectors.toList());

        ytClient.write(inputPath, StepOutputRow.class, rows);
    }
}
