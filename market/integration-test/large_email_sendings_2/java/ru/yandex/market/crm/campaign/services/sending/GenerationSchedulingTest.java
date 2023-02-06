package ru.yandex.market.crm.campaign.services.sending;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.sending.AbstractPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.EmailPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.PushPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.SendingStage;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.sending.email.EmailSendingYtPaths;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.BlockTemplateTestHelper;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.yasender.YaSenderCampaign;
import ru.yandex.market.crm.core.domain.yasender.YaSenderSendingState;
import ru.yandex.market.crm.core.test.utils.AppMetricaHelper;
import ru.yandex.market.crm.core.test.utils.MobileTablesHelper;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper;
import ru.yandex.market.crm.core.test.utils.YaSenderHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.util.yt.CommonAttributes;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.misc.thread.ThreadUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithSubscription;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithToken;
import static ru.yandex.market.crm.campaign.test.utils.EmailSendingConfigUtils.variant;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.mobilesFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.subscriptionFilter;
import static ru.yandex.market.crm.core.test.utils.MobileTablesHelper.genericSubscription;
import static ru.yandex.market.crm.core.test.utils.MobileTablesHelper.mobileAppInfo;
import static ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper.subscription;

/**
 * @author apershukov
 */
public class GenerationSchedulingTest extends AbstractServiceLargeTest {

    private static final String EMAIL_1 = "email1@yandex.ru";
    private static final String EMAIL_2 = "email2@yandex.ru";
    private static final String EMAIL_3 = "email3@yandex.ru";

    private static final String UUID_1 = "uuid-1";
    private static final String UUID_2 = "uuid-2";
    private static final String UUID_3 = "uuid-3";

    private static final String DEVICE_ID_1 = "device_id_1";
    private static final String DEVICE_ID_2 = "device_id_2";
    private static final String DEVICE_ID_3 = "device_id_3";

    private static final String DEVICE_ID_HASH_1 = "device_id_hash_1";
    private static final String DEVICE_ID_HASH_2 = "device_id_hash_2";
    private static final String DEVICE_ID_HASH_3 = "device_id_hash_3";

    private static final String CAMPAIGN_SLUG = "campaign_slug";

    @Inject
    private EmailPlainSendingService emailSendingService;

    @Inject
    private PushPlainSendingService pushSendingService;

    @Inject
    private EmailSendingDAO emailSendingDAO;

    @Inject
    private PushSendingDAO pushSendingDAO;

    @Inject
    private SegmentService segmentService;

    @Inject
    private EmailSendingTestHelper sendingTestHelper;

    @Inject
    private BlockTemplateTestHelper blockTemplateTestHelper;

    @Inject
    private SubscriptionsTestHelper subscriptionsTestHelper;

    @Inject
    private YaSenderHelper yaSenderHelper;

    @Inject
    private YtFolders ytFolders;

    @Inject
    private YtClient ytClient;

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private PushSendingTestHelper pushSendingTestHelper;

    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;

    @Inject
    private MobileTablesHelper mobileTablesHelper;

    @Inject
    private AppMetricaHelper appMetricaHelper;

    @Test
    void testGenerateAndSendEmailSending() throws Exception {
        ytSchemaTestHelper.prepareUserTables();
        ytSchemaTestHelper.prepareEmailsGeoInfo();
        ytSchemaTestHelper.prepareSubscriptionFactsTable();

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1),
                subscription(EMAIL_2),
                subscription(EMAIL_3)
        );

        var sending = prepareScheduledEmailSending();

        var campaignQueue = new ArrayBlockingQueue<YaSenderCampaign>(1);
        yaSenderHelper.onSendOrCreatePromo(CAMPAIGN_SLUG, campaignQueue::put);
        yaSenderHelper.prepareCampaignState(CAMPAIGN_SLUG, YaSenderSendingState.SENT);

        emailSendingService.processScheduledSendings();

        waitSent(sending, emailSendingService);

        var campaign = campaignQueue.poll(1, TimeUnit.MINUTES);
        assertNotNull(campaign, "No campaign was received");

        var path = (String) campaign.getSegment().getParams().get("path");
        assertNotNull(path);

        var directoryPath = ytFolders.getCampaignPath(sending.getId());
        var expectedPath = new EmailSendingYtPaths(directoryPath).getYaSenderDataTable();

        assertEquals(expectedPath, YPath.simple(path));

        var emails = ytClient.read(expectedPath, YTableEntryTypes.YSON).stream()
                .map(row -> row.getString("email"))
                .collect(Collectors.toSet());

        assertEquals(emails, Set.of(EMAIL_1, EMAIL_2, EMAIL_3));

        assertExpirationTimeIsSet(directoryPath);
    }

    @Test
    void testGenerateAndSendPushSending() {
        ytSchemaTestHelper.prepareChytUuidsWithTokensTable();
        ytSchemaTestHelper.prepareChytUuidsWithSubscriptionsTable();
        ytSchemaTestHelper.prepareGenericSubscriptionFactsTable();
        ytSchemaTestHelper.prepareMobileAppInfoFactsTable();

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

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);
        appMetricaHelper.expectDevice(DEVICE_ID_HASH_2);
        appMetricaHelper.expectDevice(DEVICE_ID_HASH_3);

        var sending= prepareScheduledPushSending();
        pushSendingService.processScheduledSendings();
        waitSent(sending, pushSendingService);

        appMetricaHelper.verify();

        assertExpirationTimeIsSet(ytFolders.getPushSendingPath(sending.getId()));
    }

    private EmailPlainSending prepareScheduledEmailSending() {
        var segment = segmentService.addSegment(segment(subscriptionFilter(SubscriptionTypes.ADVERTISING)));
        var messageTemplateId = blockTemplateTestHelper.prepareMessageTemplate();
        var creative = blockTemplateTestHelper.prepareCreativeBlock();
        var variant = variant("variant_a", 100, messageTemplateId, creative);
        var sending = sendingTestHelper.prepareSending(segment, LinkingMode.NONE, variant);

        emailSendingDAO.updateSchedulingFields(
                sending.getId(),
                LocalDateTime.now().minusMinutes(1),
                SendingStage.GENERATE,
                StageStatus.SCHEDULED,
                true
        );

        return sending;
    }

    private PushPlainSending prepareScheduledPushSending() {
        var segment = segmentService.addSegment(segment(mobilesFilter()));
        var sending = pushSendingTestHelper.prepareSending(segment);

        pushSendingDAO.updateSchedulingFields(
                sending.getId(),
                LocalDateTime.now().minusMinutes(1),
                SendingStage.GENERATE,
                StageStatus.SCHEDULED,
                true
        );

        return sending;
    }

    private void waitSent(AbstractPlainSending<?> sending,
                          AbstractPlainSendingService<?, ?, ?> sendingService) {
        while (true) {
            sending = sendingService.getSending(sending.getId());
            var stageStatus = sending.getStageStatus();
            if (sending.getStage() == SendingStage.UPLOAD && stageStatus == StageStatus.FINISHED) {
                return;
            }

            if (stageStatus == StageStatus.ERROR) {
                fail("Sending is failed");
            }

            ThreadUtils.sleep(1, TimeUnit.SECONDS);
        }
    }

    private void assertExpirationTimeIsSet(YPath directoryPath) {
        var value = ytClient.getAttribute(directoryPath, CommonAttributes.EXPIRATION_TIME)
                .filter(YTreeNode::isStringNode);

        assertTrue(value.isPresent(), "Expiration time is not set for sending directory");
    }
}
