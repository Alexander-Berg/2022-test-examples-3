package ru.yandex.market.crm.campaign.services.actions.periodic;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.actions.ActionVariant;
import ru.yandex.market.crm.campaign.domain.actions.PeriodicAction;
import ru.yandex.market.crm.campaign.domain.actions.periodic.ActionExecutedEvent;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.domain.periodic.Event;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.CampaignTestHelper;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.campaign.test.utils.ClusterTasksTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailTemplatesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PeriodicActionsTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PeriodicEntitiesTestUtils;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.yasender.YaSenderSendingState;
import ru.yandex.market.crm.core.services.control.LocalControlSaltModifier;
import ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YaSenderHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.tasks.services.ClusterTasksService;
import ru.yandex.market.crm.util.yt.CommonAttributes;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.misc.thread.ThreadUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.sendEmails;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportEmail;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.passportGender;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper.subscription;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.passportProfile;
import static ru.yandex.market.crm.util.Dates.MOSCOW_ZONE;

/**
 * @author apershukov
 */
public class StartPeriodicActionTaskTest extends AbstractServiceLargeTest {

    private static final long PUID_1 = 111;
    private static final long PUID_2 = 222;
    private static final long PUID_3 = 333;

    private static final String EMAIL_1 = "email_1@yandex-team.ru";
    private static final String EMAIL_2 = "email_2@yandex-team.ru";
    private static final String EMAIL_3 = "email_3@yandex-team.ru";

    private static final String CAMPAIGN_SLUG = "campaign_slug";

    @Inject
    private UserTestHelper userTestHelper;

    @Inject
    private CampaignTestHelper campaignTestHelper;

    @Inject
    private SegmentService segmentService;

    @Inject
    private PeriodicActionsTestHelper actionsTestHelper;

    @Inject
    private PeriodicActionService periodicActionService;

    @Inject
    private ClusterTasksTestHelper tasksTestHelper;

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private EmailTemplatesTestHelper emailTemplatesTestHelper;

    @Inject
    private SubscriptionsTestHelper subscriptionsTestHelper;

    @Inject
    private YaSenderHelper yaSenderHelper;

    @Inject
    private YtFolders ytFolders;

    @Inject
    private YtClient ytClient;

    @Inject
    private StartPeriodicActionTask task;

    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;

    @Inject
    private ClusterTasksService clusterTasksService;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.preparePassportProfilesTable();
        ytSchemaTestHelper.prepareSubscriptionFactsTable();
        ytSchemaTestHelper.prepareEmailOwnershipFactsTable();
        ytSchemaTestHelper.prepareChytPassportEmailsTable();
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.PUID, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.YUID, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.UUID, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.EMAIL_MD5, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.prepareEmailsGeoInfo();
        ytSchemaTestHelper.prepareUserTables();
    }

    @AfterEach
    public void tearDown() {
        LocalControlSaltModifier.setClock(Clock.system(MOSCOW_ZONE));
    }

    @Test
    public void testExecuteSimpleAction() {
        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m"),
                passportProfile(PUID_2, "m"),
                passportProfile(PUID_3, "m")
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1),
                subscription(EMAIL_2, PUID_2),
                subscription(EMAIL_3, PUID_3)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1),
                chytPassportEmail(PUID_2, EMAIL_2),
                chytPassportEmail(PUID_3, EMAIL_3)
        );

        var messageTemplate = emailTemplatesTestHelper.prepareEmailTemplate();
        ActionStep sendStep = sendEmails(messageTemplate.getId());

        Campaign campaign = campaignTestHelper.prepareCampaign();
        Segment segment = segmentService.addSegment(segment(passportGender("m")));
        PeriodicAction action = actionsTestHelper.prepareAction(campaign, segment, sendStep);

        var actionDir = ytFolders.getActionPath(action.getKey());
        var iterationDir = actionDir.child("1");
        var sendingDir = iterationDir.child(sendStep.getId()).child("sending");
        var dataPath = sendingDir.child("senderdata");

        yaSenderHelper.expectPromo(CAMPAIGN_SLUG, dataPath);
        yaSenderHelper.prepareCampaignState(CAMPAIGN_SLUG, YaSenderSendingState.SENT);

        PeriodicEntitiesTestUtils.startTask(task, action.getId());

        List<Event> events = periodicActionService.getEvents(action.getKey(), 0);
        assertThat(events, hasSize(1));

        ActionExecutedEvent actionExecutedEvent = (ActionExecutedEvent) events.get(0);
        assertNotNull(actionExecutedEvent.getActionConfig());

        tasksTestHelper.waitCompleted(actionExecutedEvent.getTaskId(), Duration.ofMinutes(30));

        Set<String> emails = ytClient.read(dataPath, YTableEntryTypes.YSON).stream()
                .map(row -> row.getString("email"))
                .collect(Collectors.toSet());

        assertEquals(Set.of(EMAIL_1, EMAIL_2, EMAIL_3), emails);

        action = periodicActionService.getAction(action.getId());
        assertEquals(1, action.getIteration());
        assertTrue(action.isExecuted());

        events = periodicActionService.getEvents(action.getKey(), 0);

        assertThat(events, hasSize(1));
        actionExecutedEvent = (ActionExecutedEvent) events.get(0);
        assertEquals(StageStatus.FINISHED, actionExecutedEvent.getStatus());

        assertExpirationTimeIsSet(actionDir);
        assertExpirationTimeIsSet(iterationDir);
        assertExpirationTimeIsSet(sendingDir);
    }

    /**
     * Метод recoverTask() должен корректно возобновлять регулярную акцию,
     * включая все параллельно вычисляемые в ней варианты
     */
    @Test
    public void testRecoverPeriodicActionTask() {
        // Фиксируем соль, поскольку она участвует в формировании вариантов, чтобы в каждый вариант попало по 1 пользователю
        var clock = Clock.fixed(
                LocalDateTime.now().withYear(2022).withMonth(2).toInstant(ZoneOffset.UTC), MOSCOW_ZONE
        );
        LocalControlSaltModifier.setClock(clock);

        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m"),
                passportProfile(PUID_3, "m")
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1),
                subscription(EMAIL_2, PUID_3)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1),
                chytPassportEmail(PUID_3, EMAIL_2)
        );

        var messageTemplate = emailTemplatesTestHelper.prepareEmailTemplate();

        ActionStep sendStepV1 = sendEmails(messageTemplate.getId());
        ActionStep sendStepV2 = sendEmails(messageTemplate.getId());

        String actionKey = "actionKey";
        ActionVariant variant1 = new ActionVariant();
        variant1.setId(actionKey + "_a");
        variant1.setPercent(50);
        variant1.setSteps(List.of(sendStepV1));

        ActionVariant variant2 = new ActionVariant();
        variant2.setId(actionKey + "_b");
        variant2.setPercent(50);
        variant2.setSteps(List.of(sendStepV2));

        Campaign campaign = campaignTestHelper.prepareCampaign();
        Segment segment = segmentService.addSegment(segment(passportGender("m")));
        PeriodicAction action = actionsTestHelper.prepareAction(actionKey, campaign, segment, variant1, variant2);

        var iterationPath =  ytFolders.getActionPath(action.getKey()).child("1");

        YPath dataPath1 = iterationPath.child(sendStepV1.getId())
                .child("sending")
                .child("senderdata");

        YPath dataPath2 = iterationPath.child(sendStepV2.getId())
                .child("sending")
                .child("senderdata");

        PeriodicEntitiesTestUtils.startTask(task, action.getId());

        List<Event> events = periodicActionService.getEvents(action.getKey(), 0);
        assertThat(events, hasSize(1));

        ActionExecutedEvent actionExecutedEvent = (ActionExecutedEvent) events.get(0);
        assertNotNull(actionExecutedEvent.getActionConfig());

        long taskId = actionExecutedEvent.getTaskId();
        assertThrows(
                IllegalStateException.class,
                () -> tasksTestHelper.waitCompleted(taskId, Duration.ofMinutes(20)),
                "Expected promo was not set, but task completed"
        );

        ThreadUtils.sleep(10, TimeUnit.SECONDS);
        actionExecutedEvent = (ActionExecutedEvent) periodicActionService.getEvents(action.getKey(), 0).get(0);

        StageStatus sendStepStatus1 = actionExecutedEvent.getStepStatuses()
                .get(sendStepV1.getId())
                .getStageStatus();

        StageStatus sendStepStatus2 = actionExecutedEvent.getStepStatuses()
                .get(sendStepV2.getId())
                .getStageStatus();

        assertTrue(sendStepStatus1 == StageStatus.ERROR || sendStepStatus2 == StageStatus.ERROR);

        yaSenderHelper.expectPromos(Set.of(dataPath1, dataPath2));
        clusterTasksService.recoverTask(taskId);

        tasksTestHelper.waitCompleted(taskId, Duration.ofMinutes(20));

        Set<String> emails1 = ytClient.read(dataPath1, YTableEntryTypes.YSON).stream()
                .map(row -> row.getString("email"))
                .collect(Collectors.toSet());

        Set<String> emails2 = ytClient.read(dataPath2, YTableEntryTypes.YSON).stream()
                .map(row -> row.getString("email"))
                .collect(Collectors.toSet());

        assertEquals(Set.of(EMAIL_1, EMAIL_2), Sets.union(emails1, emails2));

        action = periodicActionService.getAction(action.getId());
        assertEquals(1, action.getIteration());
        assertTrue(action.isExecuted());

        events = periodicActionService.getEvents(action.getKey(), 0);

        assertThat(events, hasSize(1));
        actionExecutedEvent = (ActionExecutedEvent) events.get(0);
        assertEquals(StageStatus.FINISHED, actionExecutedEvent.getStatus());
    }

    private void assertExpirationTimeIsSet(YPath path) {
        var expirationTime = ytClient.getAttribute(path, CommonAttributes.EXPIRATION_TIME)
                .filter(YTreeNode::isStringNode);

        assertTrue(expirationTime.isPresent(), "Expiration time is not set");
    }
}
