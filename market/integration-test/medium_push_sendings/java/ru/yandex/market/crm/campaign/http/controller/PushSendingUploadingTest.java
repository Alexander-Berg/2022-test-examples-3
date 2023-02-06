package ru.yandex.market.crm.campaign.http.controller;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.common.YtErrorMapping;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.pluggabletable.PluggableTable;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.PushPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.PushSendingFactInfo;
import ru.yandex.market.crm.campaign.domain.sending.SendingFactStatus;
import ru.yandex.market.crm.campaign.domain.sending.SendingStage;
import ru.yandex.market.crm.campaign.domain.sending.TestDevicesGroup;
import ru.yandex.market.crm.campaign.domain.sending.TestPushDevice;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingVariantConf;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.dto.sending.SendRequest;
import ru.yandex.market.crm.campaign.http.response.ErrorResponse;
import ru.yandex.market.crm.campaign.services.appmetrica.ServerDateTimeProvider;
import ru.yandex.market.crm.campaign.services.security.ObjectPermissions;
import ru.yandex.market.crm.campaign.services.security.Roles;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.sending.PushPlainSendingService;
import ru.yandex.market.crm.campaign.services.sending.PushSendingDAO;
import ru.yandex.market.crm.campaign.services.sending.facts.PushSendingFactInfoDAO;
import ru.yandex.market.crm.campaign.services.sending.push.PushSendingYtPaths;
import ru.yandex.market.crm.campaign.services.sending.tasks.UploadPushSendingSettings;
import ru.yandex.market.crm.campaign.services.throttle.Communication;
import ru.yandex.market.crm.campaign.services.throttle.ContactAttempt;
import ru.yandex.market.crm.campaign.services.throttle.DaoFactory;
import ru.yandex.market.crm.campaign.services.throttle.PushChannelDescription;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.campaign.test.utils.AccountsTeslHelper;
import ru.yandex.market.crm.campaign.test.utils.ClusterTasksTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper;
import ru.yandex.market.crm.core.domain.messages.AndroidPushConf;
import ru.yandex.market.crm.core.domain.messages.PluggedTable;
import ru.yandex.market.crm.core.domain.mobile.MobileApplication;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.AndroidPushMessageContent;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.AppMetricaPushMessage;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.DeviceIdType;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushMessages;
import ru.yandex.market.crm.core.test.loggers.TestSentPushesLogWriter;
import ru.yandex.market.crm.core.test.utils.AppMetricaHelper;
import ru.yandex.market.crm.core.test.utils.MobileAppsTestHelper;
import ru.yandex.market.crm.core.test.utils.SecurityUtils;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.dao.UsersRolesDao;
import ru.yandex.market.crm.domain.CompositeUserRole;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.mapreduce.domain.mobileapp.MobilePlatform;
import ru.yandex.market.crm.mapreduce.domain.push.ActionType;
import ru.yandex.market.crm.mapreduce.domain.push.PushMessageData;
import ru.yandex.market.crm.mapreduce.domain.push.PushRow;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.util.yt.CommonAttributes;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.misc.thread.ThreadUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.passportGender;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;

/**
 * @author apershukov
 */
public class PushSendingUploadingTest extends AbstractControllerMediumTest {

    private static PushRow pushRow(String variant, PushMessageData data) {
        PushRow row = new PushRow();
        row.setUuid(UUID.randomUUID().toString());
        row.setDeviceId(UUID.randomUUID().toString());
        row.setDeviceIdHash(UUID.randomUUID().toString());
        row.setVariant(variant);
        row.setData(data);
        row.setTZOffset(0);
        row.setPlatform(MobilePlatform.ANDROID);
        return row;
    }

    private static PushRow pushRow(PushSendingVariantConf variant) {
        return pushRow(variant.getId(), PUSH_MESSAGE_DATA);
    }

    private static PushSendingVariantConf variant(String id, int percent) {
        PushSendingVariantConf variantConf = new PushSendingVariantConf()
                .setId(id)
                .setPercent(percent);
        AndroidPushConf pushConf = new AndroidPushConf();
        pushConf.setTitle("Test push title");
        pushConf.setText("Test push text");
        pushConf.setActionType(ActionType.URL);
        pushConf.setAction("https://market.yandex.ru/product/111");
        variantConf.setPushConfigs(Map.of(pushConf.getPlatform(), pushConf));

        return variantConf;
    }

    private static PushSendingVariantConf variant() {
        return variant("variant_a", 100);
    }

    private static void assertBatchItem(PushRow row, List<AppMetricaHelper.Batch> batches) {
        // Порядок строк внутри одного батча не определён, поэтому находим подходящий по id устройства.
        AppMetricaHelper.Batch batch = batches.stream()
                .filter(b -> b.getDevices().stream().flatMap(d -> d.getIdValues().stream()).anyMatch(s -> row.getDeviceIdHash().equals(s)))
                .findFirst()
                .orElseThrow();

        List<AppMetricaHelper.DeviceSet> devices = batch.getDevices();
        assertEquals(1, devices.size());
        assertEquals(Collections.singletonList(row.getDeviceIdHash()), devices.get(0).getIdValues());

        PushMessageData data = row.getData();

        AppMetricaPushMessage<AndroidPushMessageContent> message = batch.getPushMessages()
                .getAndroidAppMetricaPushMessage();

        if (data.getActionType() == ActionType.URL) {
            assertTrue(message.getOpenUrl().startsWith(data.getAction()));
        } else if (data.getActionType() == ActionType.DEEPLINK) {
            assertEquals(data.getAction(), message.getOpenDeepLink());
        }

        AndroidPushMessageContent content = message.getContent();
        assertEquals(data.getTitle(), content.getTitle());
        assertEquals(data.getText(), content.getText());
    }

    private static final String DEVICE_ID_HASH = "device_id_hash";

    private static final String ACCOUNT_1 = "ACCOUNT_1";
    private static final String ACCOUNT_2 = "ACCOUNT_2";

    private static final String MOBILE_APP_1 = "MOBILE_APP_1";
    private static final String MOBILE_APP_2 = "MOBILE_APP_2";

    private static final PushMessageData PUSH_MESSAGE_DATA = new PushMessageData()
            .setTitle("Title 1")
            .setText("Text 1")
            .setActionType(ActionType.DEEPLINK)
            .setAction("beru://product/111");

    @Inject
    private AppMetricaHelper appMetricaHelper;
    @Inject
    private YtFolders ytFolders;
    @Inject
    private YtClient ytClient;
    @Inject
    private JsonSerializer jsonSerializer;
    @Inject
    private PushSendingTestHelper pushSendingTestHelper;
    @Inject
    private PushSendingDAO sendingDAO;
    @Inject
    private ServerDateTimeProvider dateTimeProvider;
    @Inject
    private PushPlainSendingService pushPlainSendingService;
    @Inject
    private MobileAppsTestHelper mobileAppsTestHelper;
    @Inject
    private TestSentPushesLogWriter sentPushesLogWriter;
    @Inject
    private DaoFactory daoFactory;
    @Inject
    private PluggableTablesTestHelper pluggableTablesTestHelper;
    @Inject
    private SegmentService segmentService;
    @Inject
    private PushSendingFactInfoDAO pushSendingFactInfoDAO;
    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;
    @Inject
    private ClusterTasksTestHelper clusterTasksTestHelper;
    @Inject
    private AccountsTeslHelper accountsTeslHelper;
    @Inject
    private UsersRolesDao usersRolesDao;
    @Inject
    private JsonDeserializer jsonDeserializer;

    @BeforeEach
    void setUp() {
        ytSchemaTestHelper.prepareCommunicationsTable();
    }

    @Test
    public void testSendPushSending() throws Exception {
        PushSendingVariantConf variant1 = variant("variant_a", 40);
        PushSendingVariantConf variant2 = variant("variant_b", 40);

        PushPlainSending sending = prepareBuiltSending(variant1, variant2);

        PushRow row1 = pushRow(
                variant1.getId(),
                new PushMessageData()
                        .setTitle("Title 1")
                        .setText("Text 1")
                        .setActionType(ActionType.DEEPLINK)
                        .setAction("beru://product/111")
        );

        PushRow row2 = pushRow(
                variant2.getId(),
                new PushMessageData()
                        .setTitle("Title 2")
                        .setText("Text 2")
                        .setActionType(ActionType.URL)
                        .setAction("https://market.yandex.ru/category/312")
        );

        PushRow row3 = pushRow("variant_control", null);

        prepareSendingTable(sending, Arrays.asList(row1, row2, row3));

        appMetricaHelper.expectDevice(row1.getDeviceIdHash());
        appMetricaHelper.expectDevice(row2.getDeviceIdHash());

        requestSend(sending);

        AppMetricaHelper.SendPushesRequest request = appMetricaHelper.pollForSendRequest(60);
        assertNotNull(request, "No push message was sent");

        List<AppMetricaHelper.Batch> items = request.getSendBatchRequest().getBatches();
        assertThat(items, hasSize(2));

        assertBatchItem(row1, items);
        assertBatchItem(row2, items);

        Map<String, Map<String, String>> logRecords = expectSentLogRecords();

        assertEquals(3, logRecords.size());

        assertEquals("false", logRecords.get(row1.getUuid()).get("control"));
        assertEquals("false", logRecords.get(row2.getUuid()).get("control"));
        assertEquals("true", logRecords.get(row3.getUuid()).get("control"));

        var sendingId = sending.getId();
        waitForSuccess(sendingId);

        var sendingDir = getSendingDir(sendingId);

        var expirationTime = ytClient.getAttribute(sendingDir, CommonAttributes.EXPIRATION_TIME)
                .filter(YTreeNode::isStringNode);

        assertTrue(expirationTime.isPresent(), "Expiration time is not set");
    }

    /**
     * Если отправляется слишком много сообщений одному пользователю, то лишние не пропускаются
     */
    @Test
    public void filterOutSpam() throws Exception {
        PushSendingVariantConf variant1 = variant("variant_a", 100);

        PushPlainSending sending = prepareBuiltSending(variant1);

        PushRow row1 = pushRow(
                variant1.getId(),
                new PushMessageData()
                        .setTitle("Title 1")
                        .setText("Text 1")
                        .setActionType(ActionType.DEEPLINK)
                        .setAction("beru://product/111")
        );
        var limit = 3;

        // Заполняем таблицу коммуникаций "старыми" записями
        for (int i = 0; i < limit; i++) {
            var application = new PushChannelDescription(MobileApplication.MARKET_APP);
            var dao = daoFactory.create(application);
            dao.addMany(Map.of(
                    new ContactAttempt(row1.getUuid(), limit),
                    new Communication(dateTimeProvider.getDateTime(), "promo", "test")
            ));
        }

        prepareSendingTable(sending, Collections.singletonList(row1));

        appMetricaHelper.expectDevice(row1.getDeviceIdHash());

        enableFrequencyThrottle(sending);
        requestSend(sending);

        AppMetricaHelper.SendPushesRequest request = appMetricaHelper.pollForSendRequest(60);
        assertNull(request, "Push message should not be sent, but it did");
    }

    /**
     * При отправке пуш рассылки в максимальном режиме, батчи должны быть такого же размера, который указан в properties
     */
    @Test
    public void testSendPushSendingWithMaxRateUploadSettings() throws Exception {
        int maxRateBatchSize = 2;
        PushSendingVariantConf variant = variant("variant_a", 100);

        PushPlainSending sending = prepareBuiltSending(variant);

        List<PushRow> pushRows = List.of(
                pushRow(variant.getId(), PUSH_MESSAGE_DATA),
                pushRow(variant.getId(), PUSH_MESSAGE_DATA),
                pushRow(variant.getId(), PUSH_MESSAGE_DATA),
                pushRow(variant.getId(), PUSH_MESSAGE_DATA)
        );
        pushRows.forEach(r -> appMetricaHelper.expectDevice(r.getDeviceIdHash()));

        prepareSendingTable(sending, pushRows);

        requestSetUploadPushSendingSettings(10, 10, maxRateBatchSize, 2);

        SendRequest sendRequest = new SendRequest();
        sendRequest.setEnableMaxRate(true);
        requestSend(sending, sendRequest);

        for (int i = 0; i < pushRows.size() / maxRateBatchSize; i++) {
            AppMetricaHelper.SendPushesRequest request = appMetricaHelper.pollForSendRequest(10);
            assertNotNull(request);
            assertEquals(maxRateBatchSize, request.getSendBatchRequest().getBatches().size());
        }
    }

    /**
     * При отправке пуш рассылки в дефолтном режиме, батчи должны быть такого же размера, который указан в properties
     */
    @Test
    public void testSendPushSendingWithDefaultUploadSettings() throws Exception {
        int defaultBatchSize = 1;
        PushSendingVariantConf variant = variant("variant_a", 100);

        PushPlainSending sending = prepareBuiltSending(variant);

        List<PushRow> pushRows = List.of(
                pushRow(variant),
                pushRow(variant),
                pushRow(variant),
                pushRow(variant)
        );
        pushRows.forEach(r -> appMetricaHelper.expectDevice(r.getDeviceIdHash()));

        prepareSendingTable(sending, pushRows);

        requestSetUploadPushSendingSettings(defaultBatchSize, 2, 20, 10);

        requestSend(sending);

        for (int i = 0; i < pushRows.size() / defaultBatchSize; i++) {
            AppMetricaHelper.SendPushesRequest request = appMetricaHelper.pollForSendRequest(10);
            assertNotNull(request);
            assertEquals(defaultBatchSize, request.getSendBatchRequest().getBatches().size());
        }
    }

    /**
     * При отправке отложенной пуш рассылки с установленным параметром максимального режима,
     * батчи должны быть такого же размера, который указан в properties для максимального режима
     */
    @Test
    @Disabled("Нестабильный тест. Из-за гонки часто падает на CI")
    public void testSendScheduledPushSendingWithMaxRateUploadSettings() throws Exception {
        int maxRateBatchSize = 2;
        PushSendingVariantConf variant = variant("variant_a", 100);

        PushPlainSending sending = prepareBuiltSending(variant);

        List<PushRow> pushRows = List.of(
                pushRow(variant),
                pushRow(variant),
                pushRow(variant),
                pushRow(variant)
        );
        pushRows.forEach(r -> appMetricaHelper.expectDevice(r.getDeviceIdHash()));

        prepareSendingTable(sending, pushRows);

        requestSetUploadPushSendingSettings(10, 10, maxRateBatchSize, 2);

        SendRequest sendRequest = new SendRequest();
        sendRequest.setEnableMaxRate(true);
        sendRequest.setScheduleTime(dateTimeProvider.getDateTime().plusSeconds(1));
        requestSend(sending, sendRequest);

        ThreadUtils.sleep(1, TimeUnit.SECONDS);
        pushPlainSendingService.processScheduledSendings();

        for (int i = 0; i < pushRows.size() / maxRateBatchSize; i++) {
            AppMetricaHelper.SendPushesRequest request = appMetricaHelper.pollForSendRequest(10);
            assertNotNull(request);
            assertEquals(maxRateBatchSize, request.getSendBatchRequest().getBatches().size());
        }
    }

    /**
     * Рассылка отправляется в приложение, заданное в настройках рассылки
     */
    @Test
    void testUploadPushSendingToCertainApp() throws Exception {
        var testAppId = "test_app";
        var metricaAppId = 111222333;
        mobileAppsTestHelper.insertApplication(testAppId, metricaAppId, YPath.cypressRoot(), List.of());

        var variant = variant();
        var config = new PushSendingConf();
        config.setApplication(testAppId);
        config.setVariants(List.of(variant));
        config.setTarget(new TargetAudience(LinkingMode.NONE, "segment"));
        var sending = prepareBuiltSending(config);

        var rows = List.of(
                pushRow(variant),
                pushRow(variant),
                pushRow(variant)
        );

        prepareDevices(sending, rows);

        requestSend(sending);

        var expectedIds = rows.stream()
                .map(PushRow::getDeviceIdHash)
                .collect(Collectors.toSet());

        var sentIds = appMetricaHelper.pollForSendRequest(10).getSendBatchRequest().getBatches().stream()
                .flatMap(batch -> batch.getDevices().stream())
                .flatMap(devices -> devices.getIdValues().stream())
                .collect(Collectors.toSet());

        assertEquals(expectedIds, sentIds);

        var group = appMetricaHelper.getCreatedGroups().poll(1, TimeUnit.SECONDS);
        assertNotNull(group);
        assertEquals(metricaAppId, group.getAppId());

        var logRecords = expectSentLogRecords().values();
        assertThat(logRecords, hasSize(3));

        logRecords.stream()
                .map(record -> record.get("application"))
                .forEach(id -> assertEquals(testAppId, id));
    }

    /**
     * Если во время отправки пушей возникла ошибка (запуск рассылки при отсутствующей таблицы с пушами),
     * то отправка завершается с ошибкой, при этом устанавливается статус FINISHED для информации о факте отправки
     */
    @Test
    public void testErrorSendPushSending() throws Exception {
        PushPlainSending sending = prepareBuiltSending(variant());

        requestSend(sending);

        AppMetricaHelper.SendPushesRequest request = appMetricaHelper.pollForSendRequest(30);
        assertNull(request);

        List<PushSendingFactInfo> sendingFactInfos = pushSendingFactInfoDAO.getSendingFacts(sending.getId());
        assertEquals(1, sendingFactInfos.size());
        assertEquals(SendingFactStatus.ERROR, sendingFactInfos.get(0).getStatus());
        assertThat(
                sendingFactInfos.get(0).getErrorMessage(),
                CoreMatchers.containsString(YtErrorMapping.ResolveError.class.getName())
        );
    }

    @Test
    public void testSendPushWithVarsPreview() throws Exception {
        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable(UidType.PUID);

        Segment segment = segmentService.addSegment(segment(
                passportGender("m")
        ));

        PushSendingVariantConf variant = PushSendingTestHelper.variant(
                "variant_a",
                100,
                "${vars.lastname} ${u_vars.table.saved_money}"
        );

        PushSendingConf config = new PushSendingConf();
        config.setApplication(MobileApplication.MARKET_APP);
        config.setVariants(Collections.singletonList(variant));
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setPluggedTables(Collections.singletonList(new PluggedTable(pluggableTable.getId(), "table")));
        config.setSubscriptionType(SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING.getId());

        PushPlainSending sending = pushSendingTestHelper.prepareSending(config);

        TestDevicesGroup group = new TestDevicesGroup()
                .setId("id-1")
                .setName("Group 1")
                .setItems(Collections.singletonList(
                        new TestPushDevice(DeviceIdType.APPMETRICA_DEVICE_ID, DEVICE_ID_HASH, "username")
                                .setSelected(true)
                ));

        appMetricaHelper.expectDevice(DEVICE_ID_HASH);

        mockMvc.perform(post(
                "/api/sendings/push/{id}/variants/{variantId}/send-preview",
                sending.getId(),
                variant.getId()
        )
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsBytes(List.of(group))))
                .andDo(print())
                .andExpect(status().isOk());

        AppMetricaHelper.SendPushesRequest request = appMetricaHelper.pollForSendRequest(60);
        assertNotNull(request, "No push message was sent");

        PushMessages messages = request.getSendBatchRequest().getBatches().get(0).getPushMessages();
        assertNotNull(messages);

        AppMetricaPushMessage<AndroidPushMessageContent> message = messages.getAndroidAppMetricaPushMessage();
        assertNotNull(message);

        assertEquals("Иванов saved_money", message.getContent().getTitle());
    }

    /**
     * Ссылки которые открывает push должны быть помечены
     * параметром clid=621
     */
    @Test
    public void testSetClidInPushUrl() throws Exception {
        PushSendingVariantConf variant = variant();

        PushPlainSending sending = prepareBuiltSending(variant);

        PushMessageData data = new PushMessageData()
                .setTitle("Push title")
                .setText("Push text")
                .setActionType(ActionType.URL)
                .setAction("https://market.yandex.ru/product/111");

        PushRow row = new PushRow();
        row.setUuid("a1b2c3d4");
        row.setVariant(variant.getId());
        row.setDeviceId("device_id");
        row.setDeviceIdHash("device_id_hash");
        row.setData(data);
        row.setTZOffset(0);
        row.setPlatform(MobilePlatform.ANDROID);

        prepareSendingTable(sending, Collections.singletonList(row));

        requestSend(sending);

        AppMetricaHelper.SendPushesRequest request = appMetricaHelper.pollForSendRequest(60);
        assertNotNull(request, "No push message was sent");

        PushMessages messages = request.getSendBatchRequest().getBatches().get(0).getPushMessages();
        assertNotNull(messages);

        AppMetricaPushMessage<AndroidPushMessageContent> message = messages.getAndroidAppMetricaPushMessage();
        assertNotNull(message);

        String url = message.getOpenUrl();
        assertNotNull(url);

        assertThat("Url must contain correct clid. Url: " + url, url, containsString("clid=621"));
    }

    /**
     * Если таблица с пушами для отправки пустая, то отправка завершается корректно, при этом устанавливается
     * статус FINISHED для информации о факте отправки
     */
    @Test
    public void testSendEmptyPushSending() throws Exception {
        PushPlainSending sending = prepareBuiltSending(variant());

        YPath pushPath = ytFolders.getPushSendingPath(sending.getId()).child("sending");

        ytClient.createTable(pushPath, "campaign/push_sending.yson");

        requestSend(sending);

        AppMetricaHelper.SendPushesRequest request = appMetricaHelper.pollForSendRequest(30);
        assertNull(request);

        List<PushSendingFactInfo> sendingFactInfos = pushSendingFactInfoDAO.getSendingFacts(sending.getId());
        assertEquals(1, sendingFactInfos.size());
        assertEquals(SendingFactStatus.FINISHED, sendingFactInfos.get(0).getStatus());
    }

    /**
     * Пользователь с ролью оператор не может отправлять push-рассылки на мобильные приложения,
     * отсутствующие в его аккаунте
     */
    @Test
    void testOperatorCantSendPushPromoToMobileAppNotFromHisAccount() throws Exception {
        mobileAppsTestHelper.insertApplication(MOBILE_APP_1, 111222333, YPath.cypressRoot(), List.of());
        mobileAppsTestHelper.insertApplication(MOBILE_APP_2, 444555666, YPath.cypressRoot(), List.of());

        accountsTeslHelper.prepareAccount(ACCOUNT_1, Set.of(MOBILE_APP_1));
        accountsTeslHelper.prepareAccount(ACCOUNT_2, Set.of(MOBILE_APP_2));

        //TODO: нужно, чтобы тесты работали. Убрать после выпиливания ограничений
        var user = SecurityUtils.profile("operator_profile", 1120000000039960L);
        usersRolesDao.addRole(user.getUid(), new CompositeUserRole(ACCOUNT_1, Roles.OPERATOR));


        var variant = variant();
        var config = new PushSendingConf();
        config.setApplication(MOBILE_APP_2);
        config.setVariants(List.of(variant));
        config.setTarget(new TargetAudience(LinkingMode.NONE, "segment"));
        var sending = prepareBuiltSending(config);

        SecurityUtils.setAuthentication(user);

        var response = mockMvc.perform(post("/api/sendings/push/{id}/send", sending.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsBytes(new SendRequest())))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andReturn().getResponse();

        var errorResponse = jsonDeserializer.readObject(
                ErrorResponse.class,
                response.getContentAsString()
        );

        assertEquals(
                "Access denied for object. Required permission: " + ObjectPermissions.SEND,
                errorResponse.getMessage()
        );
    }

    /**
     * Пользователь с ролью оператор имеет доступ на отправку push-рассылки на мобильные приложения,
     * присутствующие в его аккаунте
     */
    @Test
    void testOperatorCanSendPushPromoToMobileAppFromHisAccount() throws Exception {
        var metricaAppId = 111222333;
        mobileAppsTestHelper.insertApplication(MOBILE_APP_1, metricaAppId, YPath.cypressRoot(), List.of());

        accountsTeslHelper.prepareAccount(ACCOUNT_1, Set.of(MOBILE_APP_1));

        //TODO: uid нужен, чтобы тесты работали. Убрать после выпиливания ограничений
        var user = SecurityUtils.profile("operator_profile", 1120000000039960L);
        usersRolesDao.addRole(user.getUid(), new CompositeUserRole(ACCOUNT_1, Roles.OPERATOR));

        var variant = variant();
        var config = new PushSendingConf();
        config.setApplication(MOBILE_APP_1);
        config.setVariants(List.of(variant));
        config.setTarget(new TargetAudience(LinkingMode.NONE, "segment"));
        var sending = prepareBuiltSending(config);

        SecurityUtils.setAuthentication(user);

        var rows = List.of(
                pushRow(variant),
                pushRow(variant),
                pushRow(variant)
        );

        prepareDevices(sending, rows);

        requestSend(sending);

        var expectedIds = rows.stream()
                .map(PushRow::getDeviceIdHash)
                .collect(Collectors.toSet());

        var sentIds = appMetricaHelper.pollForSendRequest(10).getSendBatchRequest()
                .getBatches()
                .stream()
                .flatMap(batch -> batch.getDevices().stream())
                .flatMap(devices -> devices.getIdValues().stream())
                .collect(Collectors.toSet());

        assertEquals(expectedIds, sentIds);

        var group = appMetricaHelper.getCreatedGroups().poll(1, TimeUnit.SECONDS);
        assertNotNull(group);
        assertEquals(metricaAppId, group.getAppId());

        var logRecords = expectSentLogRecords().values();
        assertThat(logRecords, hasSize(3));

        logRecords.stream()
                .map(record -> record.get("application"))
                .forEach(id -> assertEquals(MOBILE_APP_1, id));
    }

    private void prepareSendingTable(PushPlainSending sending, List<PushRow> pushRows) {
        var sendingDir = getSendingDir(sending.getId());
        var paths = new PushSendingYtPaths(sendingDir);
        ytClient.write(paths.getSendingTable(), PushRow.class, pushRows);
    }

    private void requestSetUploadPushSendingSettings(int defaultBatchSize,
                                                     int defaultWaitTime,
                                                     int maxRateBatchSize,
                                                     int maxRateWaitTime) throws Exception {
        UploadPushSendingSettings settings = new UploadPushSendingSettings();
        settings.setDefaultBatchSize(defaultBatchSize);
        settings.setDefaultWaitTime(defaultWaitTime);
        settings.setMaxRateBatchSize(maxRateBatchSize);
        settings.setMaxRateWaitTime(maxRateWaitTime);

        mockMvc.perform(post("/api/admin/push/upload/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsBytes(settings)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    private void requestSend(PushPlainSending sending) throws Exception {
        requestSend(sending, new SendRequest());
    }

    private void requestSend(PushPlainSending sending, SendRequest sendRequest) throws Exception {
        mockMvc.perform(post("/api/sendings/push/{id}/send", sending.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsBytes(sendRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Nonnull
    private PushPlainSending prepareBuiltSending(PushSendingVariantConf... variants) {
        var config = new PushSendingConf();
        config.setApplication(MobileApplication.MARKET_APP);
        config.setTarget(new TargetAudience(LinkingMode.NONE, "segment_id"));
        config.setVariants(Arrays.asList(variants));
        config.setSubscriptionType(SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING.getId());

        return prepareBuiltSending(config);
    }

    @Nonnull
    private PushPlainSending prepareBuiltSending(PushSendingConf config) {
        var sending = pushSendingTestHelper.prepareSending(config);

        sending.setStageAndStatus(SendingStage.GENERATE, StageStatus.FINISHED);
        sendingDAO.updateSendingStates(sending.getId(), sending);
        return sending;
    }

    private void prepareDevices(PushPlainSending sending, List<PushRow> pushRows) {
        pushRows.forEach(r -> appMetricaHelper.expectDevice(r.getDeviceIdHash()));
        prepareSendingTable(sending, pushRows);
    }

    private Map<String, Map<String, String>> expectSentLogRecords() throws InterruptedException {
        Map<String, Map<String, String>> result = new HashMap<>();

        for (int i = 0; i < 3; ++i) {
            Map<String, String> record = sentPushesLogWriter.getRecords().poll(10, TimeUnit.SECONDS);
            assertNotNull(record, "Not enough log records. Expected: 3, Received: " + i);
            result.put(record.get("uuid"), record);
        }

        return result;
    }

    private void enableFrequencyThrottle(PushPlainSending sending) throws Exception {
        mockMvc.perform(post("/api/sendings/push/{id}/activateThrottle", sending.getId()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    private YPath getSendingDir(String sendingId) {
        return ytFolders.getPushSendingPath(sendingId);
    }

    private void waitForSuccess(String sendingId) {
        var sending = sendingDAO.getSending(sendingId);

        clusterTasksTestHelper.waitCompleted(sending.getTaskId(), Duration.ofMinutes(1));
    }
}
