package ru.yandex.market.crm.campaign.services.sending.periodic;

import java.time.Duration;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.periodic.Event;
import ru.yandex.market.crm.campaign.domain.sending.PushPeriodicSending;
import ru.yandex.market.crm.campaign.domain.sending.SendingGenerationResult;
import ru.yandex.market.crm.campaign.domain.sending.periodic.GeneratedEvent;
import ru.yandex.market.crm.campaign.domain.sending.periodic.UploadedEvent;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.sending.PushSendingGenerationResult;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.BannedPromocodesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.campaign.test.utils.ClusterTasksTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PeriodicEntitiesTestUtils;
import ru.yandex.market.crm.campaign.test.utils.PushPeriodicSendingTestHelper;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.segment.SegmentPart;
import ru.yandex.market.crm.core.test.utils.AppMetricaHelper;
import ru.yandex.market.crm.core.test.utils.MobileTablesHelper;
import ru.yandex.market.crm.core.test.utils.SecurityUtils;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.util.yt.CommonAttributes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.crm.campaign.test.utils.BannedPromocodesTestHelper.promocodeRecord;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportUuid;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithSubscription;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithToken;
import static ru.yandex.market.crm.campaign.test.utils.PushPeriodicSendingTestHelper.config;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.mobilesFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.passportGender;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.utils.MobileTablesHelper.genericSubscription;
import static ru.yandex.market.crm.core.test.utils.MobileTablesHelper.mobileAppInfo;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.STORE_PUSH_PERSONAL_ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.passportProfile;

/**
 * @author apershukov
 */
public class StartPeriodicPushSendingTaskTest extends AbstractServiceLargeTest {

    private static final String UUID_1 = "uuid-1";
    private static final String UUID_2 = "uuid-2";
    private static final String UUID_3 = "uuid-3";
    private static final String UUID_4 = "uuid-4";
    private static final String UUID_5 = "uuid-5";

    private static final long PUID_1 = 111;
    private static final long PUID_2 = 222;
    private static final long PUID_3 = 333;
    private static final long PUID_4 = 444;
    private static final long PUID_5 = 555;

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
    private PushPeriodicSendingTestHelper sendingTestHelper;

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private MobileTablesHelper mobileTablesHelper;

    @Inject
    private UserTestHelper userTestHelper;

    @Inject
    private ClusterTasksTestHelper clusterTasksTestHelper;

    @Inject
    private AppMetricaHelper appMetricaHelper;

    @Inject
    private SegmentService segmentService;

    @Inject
    private PushPeriodicSendingService sendingService;

    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;

    @Inject
    private BannedPromocodesTestHelper bannedPromocodesTestHelper;

    @Inject
    private YtFolders ytFolders;

    @Inject
    private StartPeriodicPushSendingTask task;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareMetrikaAppFactsTable();
        ytSchemaTestHelper.prepareMobileAppInfoFactsTable();
        ytSchemaTestHelper.prepareGenericSubscriptionFactsTable();
        ytSchemaTestHelper.preparePlusDataTable();
        ytSchemaTestHelper.prepareGlobalControlSplitsTable();
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.UUID, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.preparePushTokenStatusesTable();
        ytSchemaTestHelper.prepareChytPassportUuidsTable();
        ytSchemaTestHelper.preparePassportProfilesTable();
        ytSchemaTestHelper.prepareCommunicationsTable();
        ytSchemaTestHelper.prepareChytUuidsWithTokensTable();
        ytSchemaTestHelper.prepareChytUuidsWithSubscriptionsTable();
    }

    @Test
    public void testGenerateAndSendSending() {
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
                chytUuidWithSubscription(UUID_3, STORE_PUSH_GENERAL_ADVERTISING, true),
                chytUuidWithSubscription(UUID_4, STORE_PUSH_GENERAL_ADVERTISING, false),
                chytUuidWithSubscription(UUID_5, STORE_PUSH_PERSONAL_ADVERTISING, true)
        );

        chytDataTablesHelper.preparePassportUuids(
                chytPassportUuid(PUID_1, UUID_1),
                chytPassportUuid(PUID_2, UUID_2),
                chytPassportUuid(PUID_3, UUID_3),
                chytPassportUuid(PUID_5, UUID_5)
        );

        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m"),
                passportProfile(PUID_2, "m"),
                passportProfile(PUID_3, "m"),
                passportProfile(PUID_4, "m"),
                passportProfile(PUID_5, "m")
        );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);
        appMetricaHelper.expectDevice(DEVICE_ID_HASH_2);
        appMetricaHelper.expectDevice(DEVICE_ID_HASH_3);

        var segment = prepareSegment(passportGender("m"));
        var sending = sendingTestHelper.prepareSending(segment);
        generateAndSend(sending);

        appMetricaHelper.verify();

        var events = sendingService.getEvents(sending.getKey(), 0);
        assertThat(events, hasSize(2));

        UploadedEvent uploadedEvent = (UploadedEvent) events.get(0);
        assertEquals(3, uploadedEvent.getTotalRows());
        assertEquals(3, uploadedEvent.getUploadedRows());
        assertEquals(StageStatus.FINISHED, uploadedEvent.getStatus());
        assertNotNull(uploadedEvent.getTime());
        assertNotNull(uploadedEvent.getFinishTime());

        var generatedEvent = (GeneratedEvent) events.get(1);
        assertEquals(StageStatus.FINISHED, generatedEvent.getStatus());

        SendingGenerationResult generationResult = generatedEvent.getGenerationResult();
        assertThat(generationResult, is(instanceOf(PushSendingGenerationResult.class)));

        PushSendingGenerationResult pushGenerationResult = (PushSendingGenerationResult) generationResult;
        assertEquals(4, (long) pushGenerationResult.getSegmentSize());
        assertEquals(3, (long) pushGenerationResult.getTargetGroupSize());
        assertEquals(3, (long) pushGenerationResult.getAppMetricaLoadedSize());
        assertEquals(0, (long) pushGenerationResult.getControlGroupSize());

        sending = sendingService.getById(sending.getId());
        assertTrue(sending.isExecuted(), "Sent flag is not set after first sending");

        var sendingPath = ytFolders.getPushSendingPath(sending.getKey());
        var iterationPath = sendingPath.child("1");

        assertAll(
                () -> assertExpirationTimeIsSet(sendingPath),
                () -> assertExpirationTimeIsSet(iterationPath)
        );
    }

    /**
     * Установки с промокодом, указанным в настройках рассылки не попадают в неё
     */
    @Test
    void testUuidsFromBannedWithBannedPromocodeDoNotGetIncluded() {
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2),
                chytUuidWithToken(UUID_3, DEVICE_ID_3, DEVICE_ID_HASH_3)
        );

        mobileTablesHelper.prepareMobileAppInfos(
                mobileAppInfo(UUID_1),
                mobileAppInfo(UUID_2),
                mobileAppInfo(UUID_3)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1),
                chytUuidWithSubscription(UUID_2),
                chytUuidWithSubscription(UUID_3)
        );

        mobileTablesHelper.prepareGenericSubscriptions(
                genericSubscription(UUID_1),
                genericSubscription(UUID_2),
                genericSubscription(UUID_3)
        );

        var bannedPromocode = 1L;

        bannedPromocodesTestHelper.prepareBannedTable(
                promocodeRecord(2, null, UUID_1),
                promocodeRecord(bannedPromocode, null, UUID_2),
                promocodeRecord(bannedPromocode, null, UUID_3)
        );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        var segment = prepareSegment(mobilesFilter());
        var config = config(segment);
        config.setBannedPromocode(bannedPromocode);
        var sending = sendingTestHelper.prepareSending(config);
        generateAndSend(sending);

        appMetricaHelper.verify();
    }

    private void generateAndSend(PushPeriodicSending sending) {
        SecurityUtils.setAuthentication(SecurityUtils.profile("admin"));

        PeriodicEntitiesTestUtils.startTask(task, sending.getId());

        List<Event> events = sendingService.getEvents(sending.getKey(), 0);
        assertThat(events, not(empty()));

        GeneratedEvent generatedEvent = (GeneratedEvent) events.get(0);
        assertEquals(StageStatus.IN_PROGRESS, generatedEvent.getStatus());

        long taskId = generatedEvent.getTaskId();
        assertThat("Sending task is not specified in event", taskId, greaterThan(0L));

        clusterTasksTestHelper.waitCompleted(taskId, Duration.ofMinutes(30));
    }

    private Segment prepareSegment(SegmentPart part) {
        return segmentService.addSegment(segment(part));
    }

    private void assertExpirationTimeIsSet(YPath path) {
        var value = ytClient.getAttribute(path, CommonAttributes.EXPIRATION_TIME)
                .filter(YTreeNode::isStringNode);

        assertTrue(value.isPresent(), "Expiration time is not set for path " + path);
    }
}
