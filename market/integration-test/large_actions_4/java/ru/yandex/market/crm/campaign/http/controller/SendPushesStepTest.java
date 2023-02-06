package ru.yandex.market.crm.campaign.http.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.actions.PlainAction;
import ru.yandex.market.crm.campaign.domain.actions.status.FoldByCryptaStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.IssueBunchStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.SendPushesStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.StepStatus;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.domain.pluggabletable.PluggableTable;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.actions.StepsStatusDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ActionTestHelper;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PushTemplatesTestHelper;
import ru.yandex.market.crm.core.domain.messages.AbstractPushConf;
import ru.yandex.market.crm.core.domain.messages.AndroidPushConf;
import ru.yandex.market.crm.core.domain.messages.IosPushConf;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateVar;
import ru.yandex.market.crm.core.domain.messages.PluggedTable;
import ru.yandex.market.crm.core.domain.messages.PushMessageConf;
import ru.yandex.market.crm.core.domain.mobile.MetricaMobileApp;
import ru.yandex.market.crm.core.domain.mobile.MobileApplication;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.AndroidPushMessageContent;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.AppMetricaPushMessage;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushMessageContent;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushMessages;
import ru.yandex.market.crm.core.test.loggers.TestSentPushesLogWriter;
import ru.yandex.market.crm.core.test.utils.AppMetricaHelper;
import ru.yandex.market.crm.core.test.utils.AppMetricaHelper.Batch;
import ru.yandex.market.crm.core.test.utils.AppMetricaHelper.SendPushesRequest;
import ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.action.StepOutputRow;
import ru.yandex.market.crm.mapreduce.domain.mobileapp.IOSAttachmentFileType;
import ru.yandex.market.crm.mapreduce.domain.mobileapp.MobilePlatform;
import ru.yandex.market.crm.mapreduce.domain.push.PushMessageAttachment;
import ru.yandex.market.crm.mapreduce.domain.user.IdsGraph;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.mapreduce.domain.user.User;
import ru.yandex.market.crm.platform.commons.SendingType;
import ru.yandex.market.crm.util.yt.CommonAttributes;
import ru.yandex.market.crm.yt.client.YtClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.foldByCrypta;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.issueCoins;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.outputRow;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.sendPushes;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportUuid;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithSubscription;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithToken;
import static ru.yandex.market.crm.campaign.test.utils.CoinStepTestUtils.outputRowWithCoin;
import static ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper.pluggedTableRow;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.cryptaMatchingEntry;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.uniformSplitEntry;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.STORE_PUSH_PERSONAL_ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.passportProfile;
import static ru.yandex.market.crm.core.util.MobileAppInfoUtil.APP_INFO_PLATFORM_IPHONE;

/**
 * @author apershukov
 */
public class SendPushesStepTest extends AbstractControllerLargeTest {

    private void assertPushBatch(String expectedTitle, String expectedText, Batch batch) {
        AppMetricaPushMessage<AndroidPushMessageContent> message = batch.getPushMessages()
                .getAndroidAppMetricaPushMessage();

        AndroidPushMessageContent content = message.getContent();
        assertEquals(expectedTitle, content.getTitle());
        assertEquals(expectedText, content.getText());
    }

    private void assertPushLogRecord(String expectedUuid,
                                     String expectedDeviceIdHash,
                                     String expectedDeviceId,
                                     String expectedSendingId,
                                     String expectedStepId,
                                     String expectedSegment,
                                     String expectedTitle,
                                     String expectedText,
                                     Map<String, String> record) {
        assertEquals(expectedUuid, record.get("uuid"));
        assertEquals(expectedDeviceIdHash, record.get("deviceIdHash"));
        assertEquals(expectedDeviceId, record.get("deviceId"));
        assertEquals(expectedSendingId, record.get("sendingId"));
        assertEquals(expectedStepId, record.get("stepId"));
        assertEquals(expectedSegment, record.get("segmentId"));
        assertEquals(expectedTitle, record.get("title"));
        assertEquals(expectedText, record.get("text"));
        assertEquals(String.valueOf(MetricaMobileApp.BERU.getId()), record.get("appId"));
        assertEquals(MobileApplication.MARKET_APP, record.get("application"));
        assertEquals(MobilePlatform.ANDROID.name(), record.get("platform"));
        assertEquals("false", record.get("control"));
        assertEquals(ActionTestHelper.DEFAULT_VARIANT, record.get("variantId"));
        assertEquals(SendingType.ACTION.getNumber(), Integer.parseInt(record.get("type")));
        assertNotNull(record.get("transferId"));
        assertNotNull(record.get("timestamp"));
    }

    private static final long PUID_1 = 111;
    private static final long PUID_2 = 222;
    private static final long PUID_3 = 333;
    private static final long PUID_4 = 444;

    private static final String UUID_1 = "uuid-1";
    private static final String UUID_2 = "uuid-2";
    private static final String UUID_3 = "uuid-3";
    private static final String UUID_4 = "uuid-4";
    private static final String UUID_5 = "uuid-5";

    private static final String YUID_1 = "111";
    private static final String YUID_2 = "222";
    private static final String YUID_3 = "333";

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

    private static final String CRYPTA_ID_1 = "crypta-id-1";
    private static final String CRYPTA_ID_2 = "crypta-id-2";
    private static final String CRYPTA_ID_3 = "crypta-id-3";

    private static final long PROMO_ID = 12345678;

    @Inject
    private PushTemplatesTestHelper pushTemplatesTestHelper;

    @Inject
    private ActionTestHelper actionTestHelper;

    @Inject
    private AppMetricaHelper appMetricaHelper;

    @Inject
    private StepsStatusDAO stepsStatusDAO;

    @Inject
    private TestSentPushesLogWriter sentPushesLogWriter;

    @Inject
    private PluggableTablesTestHelper pluggableTablesTestHelper;

    @Inject
    private UserTestHelper userTestHelper;

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private GlobalSplitsTestHelper globalSplitsTestHelper;

    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;

    @Inject
    private YtClient ytClient;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareUserTables();
        ytSchemaTestHelper.prepareMobileAppInfoFactsTable();
        ytSchemaTestHelper.prepareMetrikaAppFactsTable();
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.UUID, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.prepareGlobalControlSplitsTable();
        ytSchemaTestHelper.preparePushTokenStatusesTable();
        ytSchemaTestHelper.prepareChytPassportUuidsTable();
        ytSchemaTestHelper.preparePassportProfilesTable();
        ytSchemaTestHelper.prepareCommunicationsTable();
        ytSchemaTestHelper.prepareChytUuidsWithTokensTable();
        ytSchemaTestHelper.prepareChytUuidsWithSubscriptionsTable();
    }

    /**
     * Проверка отправки статичного push-уведомления небольшого размера
     */
    @Test
    public void testSendStaticPushNotifications() throws Exception {
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

        var template = pushTemplatesTestHelper.prepare();

        ActionStep issueCoins = issueCoins(PROMO_ID);
        ActionStep sendStep = sendPushes(template.getId());
        PlainAction action = prepareAction(issueCoins, sendStep);

        var actionId = action.getId();
        finishSegmentation(actionId);
        finishStep(actionId, issueCoins.getId(), IssueBunchStepStatus::new);

        prepareStepOutput(action, issueCoins,
                outputRowWithCoin(UidType.UUID, UUID_1),
                outputRowWithCoin(UidType.UUID, UUID_2),
                outputRowWithCoin(UidType.UUID, UUID_3),
                outputRowWithCoin(UidType.UUID, UUID_4),
                outputRowWithCoin(UidType.UUID, UUID_5)
        );

        appMetricaHelper.expectDevices(DEVICE_ID_HASH_1, DEVICE_ID_HASH_2, DEVICE_ID_HASH_3);

        execute(action, sendStep);

        SendPushesRequest request = appMetricaHelper.pollForSendRequest(5);
        assertNotNull(request);

        List<Batch> batches = request.getSendBatchRequest().getBatches();
        assertEquals(3, batches.size());

        AbstractPushConf pushConf = template.getConfig().getPushConfigs().get(MobilePlatform.ANDROID);
        String pushTitle = pushConf.getTitle();
        String pushText = pushConf.getText();

        batches.forEach(batch -> assertPushBatch(pushTitle, pushText, batch));

        var sendStepId = sendStep.getId();
        SendPushesStepStatus status = (SendPushesStepStatus) stepsStatusDAO.get(actionId, sendStepId);

        assertEquals(StageStatus.FINISHED, status.getStageStatus());

        SendPushesStepStatus.UploadProgress uploadProgress = status.getUploadProgress();
        assertNotNull(uploadProgress);
        assertEquals(3, uploadProgress.getTotal());
        assertEquals(3, uploadProgress.getUploaded());

        List<Map<String, String>> sentPushesLogRecords = sentPushesLogWriter.getRecordsAsList();
        sentPushesLogRecords.sort(Comparator.comparing(x -> x.get("uuid")));

        assertEquals(3, sentPushesLogRecords.size());

        String segmentId = action.getConfig().getTarget().getSegment();

        assertPushLogRecord(
                UUID_1,
                DEVICE_ID_HASH_1,
                DEVICE_ID_1,
                actionId,
                sendStepId,
                segmentId,
                pushTitle,
                pushText,
                sentPushesLogRecords.get(0)
        );

        assertPushLogRecord(
                UUID_2,
                DEVICE_ID_HASH_2,
                DEVICE_ID_2,
                actionId,
                sendStepId,
                segmentId,
                pushTitle,
                pushText,
                sentPushesLogRecords.get(1)
        );

        assertPushLogRecord(
                UUID_3,
                DEVICE_ID_HASH_3,
                DEVICE_ID_3,
                actionId,
                sendStepId,
                segmentId,
                pushTitle,
                pushText,
                sentPushesLogRecords.get(2)
        );

        var sendingDir = actionTestHelper.getStepDirectory(actionId, sendStepId)
                .child("sending");

        var expirationTime = ytClient.getAttribute(sendingDir, CommonAttributes.EXPIRATION_TIME)
                .filter(YTreeNode::isStringNode);

        assertTrue(expirationTime.isPresent(), "Expiration time is not set");
    }

    /**
     * Проверка отправки push-сообщений с индивидуальным содержимым для каждого получателя
     */
    @Test
    public void testSendDynamicPushNotifications() throws Exception {
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, true),
                chytUuidWithSubscription(UUID_2, STORE_PUSH_GENERAL_ADVERTISING, true)
        );

        var template = pushTemplatesTestHelper.prepare("Coin: ${vars.COINS[0].id}");

        ActionStep issueCoins = issueCoins(PROMO_ID);
        ActionStep sendStep = sendPushes(template.getId());
        PlainAction action = prepareAction(issueCoins, sendStep);

        finishSegmentation(action.getId());
        finishStep(action.getId(), issueCoins.getId(), IssueBunchStepStatus::new);

        prepareStepOutput(action, issueCoins,
                outputRowWithCoin(UidType.UUID, UUID_1),
                outputRowWithCoin(UidType.UUID, UUID_2)
        );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);
        appMetricaHelper.expectDevice(DEVICE_ID_HASH_2);

        execute(action, sendStep);

        assertSentPushes(2);
    }

    /**
     * Для паспортных идентификаторов девайсы резолвятся через персовые данные
     * о мобильных приложениях
     */
    @Test
    public void testSendPushesToPuids() throws Exception {
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, true),
                chytUuidWithSubscription(UUID_2, STORE_PUSH_GENERAL_ADVERTISING, true)
        );

        chytDataTablesHelper.preparePassportUuids(
                chytPassportUuid(PUID_1, UUID_1),
                chytPassportUuid(PUID_2, UUID_2),
                chytPassportUuid(PUID_3, UUID_3)
        );

        var template = pushTemplatesTestHelper.prepare("Coin: ${vars.COINS[0].id}");

        ActionStep issueCoins = issueCoins(PROMO_ID);
        ActionStep sendStep = sendPushes(template.getId());
        PlainAction action = prepareAction(issueCoins, sendStep);

        finishSegmentation(action.getId());
        finishStep(action.getId(), issueCoins.getId(), IssueBunchStepStatus::new);

        prepareStepOutput(action, issueCoins,
                outputRowWithCoin(UidType.PUID, String.valueOf(PUID_1)),
                outputRowWithCoin(UidType.PUID, String.valueOf(PUID_2)),
                outputRowWithCoin(UidType.PUID, String.valueOf(PUID_3)),
                outputRowWithCoin(UidType.PUID, String.valueOf(PUID_4))
        );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        Set<Uid> outputUids = execute(action, sendStep).stream()
                .map(row -> Uid.of(row.getIdType(), row.getIdValue()))
                .collect(Collectors.toSet());

        Set<Uid> expectedUids = Set.of(
                Uid.asPuid(PUID_1),
                Uid.asPuid(PUID_2),
                Uid.asPuid(PUID_3),
                Uid.asPuid(PUID_4)
        );

        assertEquals(expectedUids, outputUids);

        assertSentPushes(1);
    }

    /**
     * В случае если в оповещении задействованы системные переменные их значения будут вычислены и
     * подставлены вместо плейсхолдеров.
     * <p>
     * Если вычислить значение для конкретного пуша не удалось, он все равно будет отправлен
     */
    @Test
    public void testUseUsernameVariablesInPushNotifications() throws Exception {
        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m", "Иван", "Иванов")
        );

        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, true),
                chytUuidWithSubscription(UUID_2, STORE_PUSH_GENERAL_ADVERTISING, true)
        );

        chytDataTablesHelper.preparePassportUuids(
                chytPassportUuid(PUID_1, UUID_1),
                chytPassportUuid(PUID_2, UUID_2)
        );

        var template = pushTemplatesTestHelper.prepare(
                "Username: ${vars.firstname} ${vars.lastname} Coin: ${vars.COINS[0].title}"
        );

        ActionStep issueCoins = issueCoins(PROMO_ID);
        ActionStep sendStep = sendPushes(template.getId());
        PlainAction action = prepareAction(issueCoins, sendStep);

        finishSegmentation(action.getId());
        finishStep(action.getId(), issueCoins.getId(), IssueBunchStepStatus::new);

        prepareStepOutput(action, issueCoins,
                outputRowWithCoin(UidType.PUID, String.valueOf(PUID_1)),
                outputRowWithCoin(UidType.PUID, String.valueOf(PUID_2))
        );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);
        appMetricaHelper.expectDevice(DEVICE_ID_HASH_2);

        execute(action, sendStep);

        SendPushesRequest request = appMetricaHelper.pollForSendRequest(5);
        assertNotNull(request);

        Map<String, String> titles = request.getSendBatchRequest().getBatches().stream()
                .collect(Collectors.toMap(
                        x -> x.getDevices().get(0).getIdValues().get(0),
                        x -> x.getPushMessages().getAndroidAppMetricaPushMessage().getContent().getTitle()
                ));

        assertEquals(2, titles.size());
        assertEquals("Username: Иван Иванов Coin: Coin", titles.get(DEVICE_ID_HASH_1));
        assertEquals("Username: null null Coin: Coin", titles.get(DEVICE_ID_HASH_2));
    }

    /**
     * Если к шаблону подключена пользовательская таблица значения её колонок будут доступны
     * в переменных вида u_vars.${Алиас таблицы}.${Название колонки}
     */
    @Test
    public void testSendPushNotificationWithPluggedTables() throws Exception {
        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable(UidType.PUID,
                pluggedTableRow(String.valueOf(PUID_1), "100500")
        );

        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1, APP_INFO_PLATFORM_IPHONE),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2, APP_INFO_PLATFORM_IPHONE)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, true),
                chytUuidWithSubscription(UUID_2, STORE_PUSH_GENERAL_ADVERTISING, true)
        );

        chytDataTablesHelper.preparePassportUuids(
                chytPassportUuid(PUID_1, UUID_1),
                chytPassportUuid(PUID_2, UUID_2)
        );

        PushMessageConf config = new PushMessageConf();
        IosPushConf pushConf = new IosPushConf();
        pushConf.setTitle("Test push");
        pushConf.setText("User var: ${u_vars.table.saved_money}, Coin: ${vars.COINS[0].title}");
        config.setPushConfigs(Map.of(pushConf.getPlatform(), pushConf));
        config.setPluggedTables(List.of(new PluggedTable(pluggableTable.getId(), "table")));

        pushConf.setAttachments(
                List.of(
                        new PushMessageAttachment()
                                .setFileUrl("http://yandex.com/${u_vars.table.saved_money}.jpg")
                                .setFileType(IOSAttachmentFileType.JPG.getId())
                )
        );

        var template = pushTemplatesTestHelper.prepare(config);

        ActionStep issueCoins = issueCoins(PROMO_ID);
        ActionStep sendStep = sendPushes(template.getId());
        PlainAction action = prepareAction(issueCoins, sendStep);

        finishSegmentation(action.getId());
        finishStep(action.getId(), issueCoins.getId(), IssueBunchStepStatus::new);

        prepareStepOutput(action, issueCoins,
                outputRowWithCoin(UidType.PUID, String.valueOf(PUID_1)),
                outputRowWithCoin(UidType.PUID, String.valueOf(PUID_2))
        );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);
        appMetricaHelper.expectDevice(DEVICE_ID_HASH_2);

        execute(action, sendStep);

        SendPushesRequest request = appMetricaHelper.pollForSendRequest(5);
        assertNotNull(request);

        Map<String, String> texts = request.getSendBatchRequest().getBatches().stream()
                .collect(Collectors.toMap(
                        x -> x.getDevices().get(0).getIdValues().get(0),
                        x -> x.getPushMessages().getiOSAppMetricaPushMessage().getContent().getText()
                ));

        Map<String, String> fileUrls = request.getSendBatchRequest().getBatches().stream()
                .collect(Collectors.toMap(
                        x -> x.getDevices().get(0).getIdValues().get(0),
                        x -> x.getPushMessages().getiOSAppMetricaPushMessage().getContent().getAttachments().get(0)
                                .getFileUrl()
                ));

        assertEquals(2, texts.size());
        assertEquals("User var: 100500, Coin: Coin", texts.get(DEVICE_ID_HASH_1));
        assertEquals("http://yandex.com/100500.jpg", fileUrls.get(DEVICE_ID_HASH_1));
        assertEquals("User var: null, Coin: Coin", texts.get(DEVICE_ID_HASH_2));
        assertEquals("http://yandex.com/null.jpg", fileUrls.get(DEVICE_ID_HASH_2));
    }

    /**
     * Для yuid'ов девайсы резолвятся методом поиска связанных идентификаторов
     * в таблице users
     */
    @Test
    public void testSendPushesToYuids() throws Exception {
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_3, DEVICE_ID_3, DEVICE_ID_HASH_3)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, true),
                chytUuidWithSubscription(UUID_2, STORE_PUSH_GENERAL_ADVERTISING, true),
                chytUuidWithSubscription(UUID_3, STORE_PUSH_GENERAL_ADVERTISING, true)
        );

        User user = new User(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.asYuid(YUID_1))
                                .addNode(Uid.asUuid(UUID_1))
                                .addNode(Uid.asUuid(UUID_2))
                                .addNode(Uid.asUuid(UUID_3))
                                .addEdge(0, 1)
                                .addEdge(0, 2)
                                .addEdge(0, 3)
                );

        userTestHelper.addUsers(user);
        userTestHelper.finishUsersPreparation();

        var template = pushTemplatesTestHelper.prepare("Coin: ${vars.COINS[0].id}");

        ActionStep issueCoins = issueCoins(PROMO_ID);
        ActionStep sendStep = sendPushes(template.getId());
        PlainAction action = prepareAction(issueCoins, sendStep);

        finishSegmentation(action.getId());
        finishStep(action.getId(), issueCoins.getId(), IssueBunchStepStatus::new);

        prepareStepOutput(action, issueCoins,
                outputRowWithCoin(UidType.YUID, YUID_1),
                outputRowWithCoin(UidType.YUID, YUID_2),
                outputRowWithCoin(UidType.YUID, YUID_3)
        );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_3);

        Set<Uid> outputUids = execute(action, sendStep).stream()
                .map(row -> Uid.of(row.getIdType(), row.getIdValue()))
                .collect(Collectors.toSet());

        Set<Uid> expectedUids = Set.of(
                Uid.asYuid(YUID_1),
                Uid.asYuid(YUID_2),
                Uid.asYuid(YUID_3)
        );

        assertEquals(expectedUids, outputUids);

        assertSentPushes(1);
    }

    /**
     * В случае если у одного идентификатора для отправки сообщения подходят сразу
     * несколько устройств, отправка будет произведена только на одно из них
     */
    @Test
    public void testSendSingleNotificationForOneId() throws Exception {
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2),
                chytUuidWithToken(UUID_3, DEVICE_ID_3, DEVICE_ID_HASH_3)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, true),
                chytUuidWithSubscription(UUID_2, STORE_PUSH_GENERAL_ADVERTISING, true),
                chytUuidWithSubscription(UUID_3, STORE_PUSH_GENERAL_ADVERTISING, true)
        );

        User user = new User(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.asYuid(YUID_1))
                                .addNode(Uid.asUuid(UUID_1))
                                .addNode(Uid.asUuid(UUID_2))
                                .addNode(Uid.asUuid(UUID_3))
                                .addEdge(0, 1)
                                .addEdge(0, 2)
                                .addEdge(0, 3)
                );

        userTestHelper.addUsers(user);
        userTestHelper.finishUsersPreparation();

        var template = pushTemplatesTestHelper.prepare();

        ActionStep issueCoins = issueCoins(PROMO_ID);
        ActionStep sendStep = sendPushes(template.getId());
        PlainAction action = prepareAction(issueCoins, sendStep);

        finishSegmentation(action.getId());
        finishStep(action.getId(), issueCoins.getId(), IssueBunchStepStatus::new);

        prepareStepOutput(action, issueCoins,
                outputRowWithCoin(UidType.YUID, YUID_1)
        );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);
        appMetricaHelper.expectDevice(DEVICE_ID_HASH_2);
        appMetricaHelper.expectDevice(DEVICE_ID_HASH_3);

        execute(action, sendStep);

        SendPushesRequest request = appMetricaHelper.pollForSendRequest(5);
        assertNotNull(request);

        List<Batch> batches = request.getSendBatchRequest().getBatches();
        assertEquals(1, batches.size());
        assertEquals(1, batches.get(0).getDevices().size());
    }

    /**
     * Отправка push-сообщений с индивидуальным содержимым не будет произведена получателям, id-шники устройств
     * которых помечены в АппМетрике как неактивные
     */
    @Test
    public void testDontSendDynamicPushNotificationsForNonActiveDevices() throws Exception {
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, true)
        );

        var template = pushTemplatesTestHelper.prepare("Coin: ${vars.COINS[0].id}");

        ActionStep issueCoins = issueCoins(PROMO_ID);
        ActionStep sendStep = sendPushes(template.getId());
        PlainAction action = prepareAction(issueCoins, sendStep);

        finishSegmentation(action.getId());
        finishStep(action.getId(), issueCoins.getId(), IssueBunchStepStatus::new);

        prepareStepOutput(action, issueCoins,
                outputRowWithCoin(UidType.UUID, UUID_1),
                outputRowWithCoin(UidType.UUID, UUID_2)
        );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        execute(action, sendStep);

        assertSentPushes(1);
    }

    /**
     * В случае если в акции включено вычитание глобального контроля на устройства
     * из него пуши не отправляются
     * <p>
     * При этом установки, попавшие в глобальный контроль, логгируются для отправки в Платформу
     * с признаком глобального контроля
     */
    @Test
    public void testDoNotSendPushesToControlGroup() throws Exception {
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2),
                chytUuidWithToken(UUID_3, DEVICE_ID_3, DEVICE_ID_HASH_3)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, true),
                chytUuidWithSubscription(UUID_2, STORE_PUSH_GENERAL_ADVERTISING, true),
                chytUuidWithSubscription(UUID_3, STORE_PUSH_GENERAL_ADVERTISING, true)
        );

        globalSplitsTestHelper.prepareGlobalControlSplits(
                uniformSplitEntry(CRYPTA_ID_1, true),
                uniformSplitEntry(CRYPTA_ID_2, true),
                uniformSplitEntry(CRYPTA_ID_3, false)
        );

        globalSplitsTestHelper.prepareCryptaMatchingEntries(
                UserTestHelper.UUID,
                cryptaMatchingEntry(UUID_1, UserTestHelper.UUID, CRYPTA_ID_1),
                cryptaMatchingEntry(UUID_2, UserTestHelper.UUID, CRYPTA_ID_2),
                cryptaMatchingEntry(UUID_3, UserTestHelper.UUID, CRYPTA_ID_3)
        );

        var template = pushTemplatesTestHelper.prepare();

        ActionStep issueCoins = issueCoins(PROMO_ID);
        ActionStep sendStep = sendPushes(template.getId());
        PlainAction action = prepareAction(issueCoins, sendStep);
        actionTestHelper.enableGlobalControl(action);

        finishSegmentation(action.getId());
        finishStep(action.getId(), issueCoins.getId(), IssueBunchStepStatus::new);

        prepareStepOutput(action, issueCoins,
                outputRowWithCoin(UidType.UUID, UUID_1),
                outputRowWithCoin(UidType.UUID, UUID_2),
                outputRowWithCoin(UidType.UUID, UUID_3)
        );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);
        appMetricaHelper.expectDevice(DEVICE_ID_HASH_2);

        execute(action, sendStep);

        SendPushesRequest request = appMetricaHelper.pollForSendRequest(5);

        assertNotNull(request);
        assertEquals(2, request.getSendBatchRequest().getBatches().size());

        // Проверка того что глобальный контроль был залогирован
        List<Map<String, String>> sentPushesLogRecords = sentPushesLogWriter.getRecordsAsList();
        assertEquals(3, sentPushesLogRecords.size());

        Map<String, String> globalControlRow = sentPushesLogRecords.stream()
                .filter(row -> UUID_3.equals(row.get("uuid")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Global control has now been logged"));

        assertEquals("true", globalControlRow.get("control"));
        assertEquals("true", globalControlRow.get("globalControl"));
        assertEquals(MobilePlatform.ANDROID.name(), globalControlRow.get("platform"));
    }

    /**
     * Если в шаблоне пуша присутствуют секретные переменные, то при логировании пуш отправки на шаге ации
     * секретные переменные в данных заменяются строкой из *, длина которой равна длине значения секретной переменной
     */
    @Test
    public void testHidingSecretVarsInLog() throws Exception {
        chytDataTablesHelper.prepareUuidsWithTokens(chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1));

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, true)
        );

        String title = "secretVar: ${vars.secretVar1} and notSecretVar: ${vars.notSecretVar1}";
        String text = "secretVar: ${vars.secretVar2} and notSecretVar: ${vars.notSecretVar2}";
        List<MessageTemplateVar> vars = List.of(
                new MessageTemplateVar("secretVar1", MessageTemplateVar.Type.STRING, true),
                new MessageTemplateVar("secretVar2", MessageTemplateVar.Type.NUMBER, true),
                new MessageTemplateVar("notSecretVar1", MessageTemplateVar.Type.STRING, false),
                new MessageTemplateVar("notSecretVar2", MessageTemplateVar.Type.NUMBER, false)
        );

        PushMessageConf config = new PushMessageConf();
        AndroidPushConf pushConf = new AndroidPushConf();
        pushConf.setTitle(title);
        pushConf.setText(text);
        config.setPushConfigs(Map.of(pushConf.getPlatform(), pushConf));
        config.setVars(vars);
        var template = pushTemplatesTestHelper.prepare(config);

        ActionStep someStep = foldByCrypta();
        ActionStep sendStep = sendPushes(template.getId());
        PlainAction action = prepareAction(someStep, sendStep);

        finishSegmentation(action.getId());
        finishStep(action.getId(), someStep.getId(), FoldByCryptaStepStatus::new);

        Map<String, YTreeNode> dataVars = Map.of(
                "secretVar1", YTree.stringNode("secret_key"),
                "secretVar2", YTree.integerNode(123),
                "notSecretVar1", YTree.stringNode("public_key"),
                "notSecretVar2", YTree.integerNode(456)
        );

        StepOutputRow row = outputRow(UidType.UUID, UUID_1);
        row.getData().setVars(dataVars);

        prepareStepOutput(action, someStep, row);

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        execute(action, sendStep);

        SendPushesRequest request = appMetricaHelper.pollForSendRequest(5);
        assertNotNull(request);

        List<Batch> batches = request.getSendBatchRequest().getBatches();
        assertEquals(1, batches.size());

        AndroidPushMessageContent content = batches.get(0)
                .getPushMessages()
                .getAndroidAppMetricaPushMessage()
                .getContent();

        String expectedTitle = title
                .replace("${vars.secretVar1}", "secret_key")
                .replace("${vars.notSecretVar1}", "public_key");
       assertEquals(expectedTitle, content.getTitle());

        String expectedText = text
                .replace("${vars.secretVar2}", "123")
                .replace("${vars.notSecretVar2}", "456");
        assertEquals(expectedText, content.getText());

        List<Map<String, String>> records = sentPushesLogWriter.getRecordsAsList();
        assertEquals(1, records.size());

        Map<String, String> record = records.get(0);

        String expectedLogTitle = title
                .replace("${vars.secretVar1}", "*".repeat("secret_key".length()))
                .replace("${vars.notSecretVar1}", "public_key");
        assertEquals(expectedLogTitle, record.get("title"));

        String expectedLogText = text
                .replace("${vars.secretVar2}", "***")
                .replace("${vars.notSecretVar2}", "456");
        assertEquals(expectedLogText, record.get("text"));
    }

    private PlainAction prepareAction(ActionStep... steps) {
        return actionTestHelper.prepareAction("segment_id", LinkingMode.NONE, steps);
    }

    private void finishStep(String actionId, String stepId, Supplier<? extends StepStatus<?>> statusSupplier) {
        actionTestHelper.finishStep(actionId, stepId, statusSupplier);
    }

    private void finishSegmentation(String actionId) {
        actionTestHelper.finishSegmentation(actionId);
    }

    private void prepareStepOutput(PlainAction action, ActionStep step, StepOutputRow... rows) {
        actionTestHelper.prepareStepOutput(action, step, rows);
    }

    private List<StepOutputRow> execute(PlainAction action, ActionStep step) throws Exception {
        return actionTestHelper.execute(action, step);
    }

    private void assertSentPushes(int expectedCount) throws InterruptedException {
        SendPushesRequest request = appMetricaHelper.pollForSendRequest(5);
        assertNotNull(request);

        List<Batch> batches = request.getSendBatchRequest().getBatches();
        assertEquals(expectedCount, batches.size());

        Pattern pattern = Pattern.compile("Coin: \\d+");

        batches.stream()
                .map(Batch::getPushMessages)
                .map(PushMessages::getAndroidAppMetricaPushMessage)
                .map(AppMetricaPushMessage::getContent)
                .map(PushMessageContent::getTitle)
                .forEach(title -> assertTrue(pattern.matcher(title).matches(),
                        "Unexpected coin title: " + title));
    }
}
