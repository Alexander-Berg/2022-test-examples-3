package ru.yandex.market.crm.campaign.http.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.google.common.base.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.pluggabletable.PluggableTable;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.PushPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.SendingStage;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingVariantConf;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.sending.PushPlainSendingService;
import ru.yandex.market.crm.campaign.services.sending.PushSendingDAO;
import ru.yandex.market.crm.campaign.services.sending.PushSendingGenerationResult;
import ru.yandex.market.crm.campaign.services.sending.push.PushSendingYtPaths;
import ru.yandex.market.crm.campaign.test.AbstractControllerLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper;
import ru.yandex.market.crm.core.domain.messages.AbstractPushConf;
import ru.yandex.market.crm.core.domain.messages.IosPushConf;
import ru.yandex.market.crm.core.domain.messages.PluggedTable;
import ru.yandex.market.crm.core.domain.mobile.MobileApplication;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.services.control.GlobalControlSaltProvider;
import ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper;
import ru.yandex.market.crm.core.test.utils.MobileAppsTestHelper;
import ru.yandex.market.crm.core.test.utils.MobileTablesHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.test.utils.YtTableForTestExtension;
import ru.yandex.market.crm.core.util.MobileAppInfoUtil;
import ru.yandex.market.crm.core.yt.paths.CrmYtTables;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.mapreduce.domain.mobileapp.IOSAttachmentFileType;
import ru.yandex.market.crm.mapreduce.domain.push.PushMessageAttachment;
import ru.yandex.market.crm.mapreduce.domain.push.PushMessageData;
import ru.yandex.market.crm.mapreduce.domain.push.PushRow;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.platform.models.GenericSubscription;
import ru.yandex.market.crm.platform.models.MobileAppInfo;
import ru.yandex.market.crm.util.LiluCollectors;
import ru.yandex.market.crm.util.yt.CommonAttributes;
import ru.yandex.market.crm.yt.client.YtClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportUuid;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithSubscription;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithToken;
import static ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper.pluggedTableRow;
import static ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper.config;
import static ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper.variant;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.mobilesFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.passportGender;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.pluggableTableFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.plusFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.cryptaMatchingEntry;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.uniformSplitEntry;
import static ru.yandex.market.crm.core.test.utils.MobileTablesHelper.genericSubscription;
import static ru.yandex.market.crm.core.test.utils.MobileTablesHelper.mobileAppInfo;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.STORE_PUSH_PERSONAL_ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.passportProfile;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.plusData;

/**
 * @author apershukov
 */
public class PushSendingGenerationTest2 extends AbstractControllerLargeTest {

    private static void assertResultRow(String uuid,
                                        String deviceId,
                                        String deviceIdHash,
                                        PushSendingVariantConf variant,
                                        PushRow row) {
        assertEquals(uuid, row.getUuid());
        assertEquals(deviceId, row.getDeviceId());
        assertEquals(deviceIdHash, row.getDeviceIdHash());
        assertEquals(variant.getId(), row.getVariant());

        PushMessageData data = row.getData();
        AbstractPushConf pushConf = variant.getPushConfigs().values().iterator().next();
        assertEquals(pushConf.getTitle(), data.getTitle());
        assertEquals(pushConf.getText(), data.getText());
    }

    private static final String UUID_1 = "uuid-1";
    private static final String UUID_2 = "uuid-2";
    private static final String UUID_3 = "uuid-3";
    private static final String UUID_4 = "uuid-4";
    private static final String UUID_5 = "uuid-5";

    private static final long PUID_1 = 111;
    private static final long PUID_2 = 222;
    private static final long PUID_3 = 333;

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

    @Inject
    private PushSendingDAO sendingDAO;
    @Inject
    private SegmentService segmentService;
    @Inject
    private YtFolders ytFolders;
    @Inject
    private CrmYtTables ytTables;
    @Inject
    private YtClient ytClient;
    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;
    @Inject
    private UserTestHelper userTestHelper;
    @Inject
    private MobileTablesHelper mobileTablesHelper;
    @Inject
    private PluggableTablesTestHelper pluggableTablesTestHelper;
    @Inject
    private PushSendingTestHelper pushSendingTestHelper;
    @Inject
    private GlobalSplitsTestHelper globalSplitsTestHelper;
    @Inject
    private GlobalControlSaltProvider saltProvider;
    @Inject
    private PushPlainSendingService sendingService;
    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;
    @Inject
    private MobileAppsTestHelper mobileAppsTestHelper;

    @RegisterExtension
    YtTableForTestExtension ytTableForTest = new YtTableForTestExtension();

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareMetrikaAppFactsTable();
        ytSchemaTestHelper.prepareMobileAppInfoFactsTable();
        ytSchemaTestHelper.prepareGenericSubscriptionFactsTable();
        ytSchemaTestHelper.preparePlusDataTable();
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.UUID, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.prepareGlobalControlSplitsTable();
        ytSchemaTestHelper.preparePushTokenStatusesTable();
        ytSchemaTestHelper.prepareChytPassportUuidsTable();
        ytSchemaTestHelper.preparePassportProfilesTable();
        ytSchemaTestHelper.prepareChytUuidsWithTokensTable();
        ytSchemaTestHelper.prepareCommunicationsTable();
        ytSchemaTestHelper.prepareChytUuidsWithSubscriptionsTable();
    }

    @Test
    public void testGenerateSendingFromUuids() throws Exception {
        mobileTablesHelper.prepareMobileAppInfos(
                mobileAppInfo(UUID_1),
                mobileAppInfo(UUID_2),
                mobileAppInfo(UUID_3),
                mobileAppInfo(UUID_4),
                mobileAppInfo(UUID_5)
        );

        mobileTablesHelper.prepareGenericSubscriptions(
                genericSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, true),
                genericSubscription(UUID_2, STORE_PUSH_GENERAL_ADVERTISING, true),
                genericSubscription(UUID_3, STORE_PUSH_GENERAL_ADVERTISING, false),
                genericSubscription(UUID_4, STORE_PUSH_PERSONAL_ADVERTISING, true)
        );

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

        Segment segment = segment(
                mobilesFilter()
        );

        PushSendingVariantConf variant = variant();
        PushPlainSending sending = createSending(segment, variant);
        List<PushRow> results = generate(sending);

        assertEquals(2, results.size());

        assertResultRow(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1, variant, results.get(0));
        assertResultRow(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2, variant, results.get(1));

        var sendingDir = getSendingDir(sending);

        var expirationTime = ytClient.getAttribute(sendingDir, CommonAttributes.EXPIRATION_TIME)
                .filter(YTreeNode::isStringNode);

        assertTrue(expirationTime.isPresent());
    }

    /**
     * Рассылку можно сгенерировать из сегмента, работающего с puid'ами
     */
    @Test
    public void testGenerateSendingFromPuidSegment() throws Exception {
        userTestHelper.addPlusData(
                plusData(PUID_1),
                plusData(PUID_2),
                plusData(PUID_3)
        );

        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_3, DEVICE_ID_3, DEVICE_ID_HASH_3)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, true),
                chytUuidWithSubscription(UUID_3, STORE_PUSH_GENERAL_ADVERTISING, false)
        );

        chytDataTablesHelper.preparePassportUuids(
                chytPassportUuid(PUID_1, UUID_1),
                chytPassportUuid(PUID_2, UUID_2),
                chytPassportUuid(PUID_3, UUID_3)
        );

        Segment segment = segment(
                plusFilter()
        );

        PushSendingVariantConf variant = variant();
        PushPlainSending sending = createSending(segment, variant);
        List<PushRow> results = generate(sending);

        assertEquals(1, results.size());

        assertResultRow(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1, variant, results.get(0));
    }

    /**
     * В случае если к рассылке подключена внешняя таблица с идентификаторами пользователя типа UUID,
     * значения её колонок можно использовать в настройках оповещения. При этом поведение будет аналогичным
     * подключению таблицы с идентификаторами любого типа за исключением того что сопоставление с
     * оповещениями в рассылке будет идти сразу по его uuid вне зависимости от его происхождения
     * (был вычислен сегментатором напрямую или через puid)
     */
    @Test
    public void testBuildWithPluggedUuidVars() throws Exception {
        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable(UidType.UUID,
                pluggedTableRow(UUID_1, "100500"),
                pluggedTableRow(UUID_2, "Nothing")
        );

        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m"),
                passportProfile(PUID_2, "m")
        );

        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1),
                chytUuidWithSubscription(UUID_2)
        );

        chytDataTablesHelper.preparePassportUuids(
                chytPassportUuid(PUID_1, UUID_1),
                chytPassportUuid(PUID_2, UUID_2)
        );

        Segment segment = segmentService.addSegment(segment(
                passportGender("m")
        ));

        PushSendingVariantConf variant = variant(
                "variant_a",
                100,
                "${u_vars.table.saved_money}"
        );

        PushPlainSending sending = createSendingWithPluggedTable(segment, variant, pluggableTable);
        Map<String, PushRow> results = generate(sending).stream()
                .collect(LiluCollectors.index(PushRow::getUuid));

        assertEquals(2, results.size());
        assertEquals("100500", results.get(UUID_1).getData().getTitle());
        assertEquals("Nothing", results.get(UUID_2).getData().getTitle());
    }

    /**
     * Активные девайсы попадают в сегмент при генерации рассылки, а неактивные - нет.
     */
    @Test
    public void testHitActiveMetricaDevicesIntoSegment() throws Exception {
        List<String> uuids = IntStream.rangeClosed(1, 100)
                .mapToObj(i -> "uuid-" + i)
                .collect(Collectors.toList());

        mobileTablesHelper.prepareMobileAppInfos(
                uuids.stream()
                        .map(MobileTablesHelper::mobileAppInfo)
                        .toArray(MobileAppInfo[]::new)
        );

        Collections.shuffle(uuids);

        String deviceIdHashSuf = "_device-id-hash";

        List<String> uuidsWithTokens = uuids.subList(0, 75);

        chytDataTablesHelper.prepareUuidsWithTokens(
                uuidsWithTokens.stream()
                        .map(uuid -> chytUuidWithToken(uuid, uuid + "_device-id", uuid + deviceIdHashSuf))
                        .toArray(YTreeMapNode[]::new)
        );

        mobileTablesHelper.prepareGenericSubscriptions(
                uuids.stream()
                        .map(MobileTablesHelper::genericSubscription)
                        .toArray(GenericSubscription[]::new)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                uuidsWithTokens.stream()
                        .map(ChytDataTablesHelper::chytUuidWithSubscription)
                        .toArray(YTreeMapNode[]::new)
        );

        Segment segment = segment(
                mobilesFilter()
        );

        PushSendingVariantConf variant = variant();
        PushPlainSending sending = createSending(segment, variant);

        Set<String> devicesInSending = generate(sending).stream()
                .map(PushRow::getDeviceIdHash)
                .collect(Collectors.toSet());

        Set<String> activeDevices = uuidsWithTokens.stream()
                .map(uuid -> uuid + deviceIdHashSuf)
                .collect(Collectors.toSet());

        assertEquals(activeDevices, devicesInSending);
    }

    /**
     * В случае если в рассылке включено вычитание глобального контроля uuid'ы из него
     * не попадают в результат сборки
     */
    @Test
    public void testUuidsFromGlobalControlIsNotIncludedInSending() throws Exception {
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

        Segment segment = segment(
                mobilesFilter()
        );

        PushPlainSending sending = prepareSending(segment, variant());
        enableGlobalControl(sending);

        Set<String> uuids = generate(sending).stream()
                .map(PushRow::getUuid)
                .collect(Collectors.toSet());

        assertEquals(Set.of(UUID_1, UUID_2), uuids);

        sending = sendingService.getSending(sending.getId());
        PushSendingGenerationResult generationResult = sending.getGenerationResult();
        assertNotNull(generationResult);
        assertThat(generationResult.getInGlobalControlGroup(), equalTo(1));
    }

    /**
     * Если задан параметр "Целевая группа не более", суммарное количество пушей во
     * всех вариантах рассылки не будет превышать этот параметр. При этом пропорции
     * между вариантами сохраняются.
     */
    @Test
    public void testMaxTargetGroupSize() throws Exception {
        final int count = 1000;

        prepareMobileAppsData(count);
        prepareGenericSubscriptions(count);

        Segment segment = segment(mobilesFilter());
        PushSendingVariantConf variant1 = variant("variant_a", 45);
        PushSendingVariantConf variant2 = variant("variant_b", 45);

        PushPlainSending sending = createSending(segment, variant1, variant2);
        sending.getConfig().setMaxTargetGroupSize(100);
        pushSendingTestHelper.updateSending(sending);

        Map<String, Integer> variantCounts = generate(sending).stream()
                .collect(Collectors.groupingBy(PushRow::getVariant))
                .entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().size()));

        int variant1Count = variantCounts.getOrDefault(variant1.getId(), 0);
        assertThat(variant1Count, allOf(greaterThan(30), lessThan(70)));

        int variant2Count = variantCounts.getOrDefault(variant2.getId(), 0);
        assertThat(variant2Count, allOf(greaterThan(30), lessThan(70)));

        assertEquals(100, variant1Count + variant2Count);

        int controlCount = variantCounts.entrySet().stream()
                .filter(e -> e.getKey().endsWith("_control"))
                .findFirst()
                .map(Entry::getValue)
                .orElse(0);

        assertEquals(900, controlCount);
    }

    @Test
    public void testIOSAttachment() throws Exception {
        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable(
                UidType.UUID,
                pluggedTableRow(UUID_1, "100500")
        );

        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m")
        );

        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1, MobileAppInfoUtil.APP_INFO_PLATFORM_IPHONE)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1)
        );

        chytDataTablesHelper.preparePassportUuids(
                chytPassportUuid(PUID_1, UUID_1)
        );

        Segment segment = segmentService.addSegment(segment(
                passportGender("m")
        ));

        IosPushConf pushConf = new IosPushConf();
        pushConf.setTitle("Test title");
        pushConf.setText("Test text");
        pushConf.setAttachments(
                List.of(
                        new PushMessageAttachment()
                                .setFileUrl("http://yandex.com/${u_vars.table.saved_money}.jpg")
                                .setFileType(IOSAttachmentFileType.JPG.getId())
                )
        );

        PushSendingVariantConf variant = new PushSendingVariantConf();
        variant.setId("variant_a");
        variant.setPercent(100);
        variant.setPushConfigs(Map.of(pushConf.getPlatform(), pushConf));

        PushPlainSending sending = createSendingWithPluggedTable(segment, variant, pluggableTable);
        Map<String, PushRow> results = generate(sending).stream()
                .collect(LiluCollectors.index(PushRow::getUuid));

        assertEquals(1, results.size());

        PushMessageData data = results.get(UUID_1).getData();
        assertNotNull(data.getAttachments());
        assertEquals(1, data.getAttachments().size());

        PushMessageAttachment attachment = data.getAttachments().get(0);
        assertEquals("http://yandex.com/100500.jpg", attachment.getFileUrl());
        assertEquals("jpg", attachment.getFileType());
    }

    /**
     * При сборке рассылки для вычисления device_id_hash используются данные только
     * того приложения которое было задано в конфигурации
     */
    @Test
    void testUseAppSpecificDeviceIdsTable() throws Exception {
        var testAppId = "test_app";
        var deviceIdsTable = ytFolders.getHome().child("test_app_devices");
        mobileAppsTestHelper.insertApplication(testAppId, 111, deviceIdsTable, List.of());

        ytSchemaTestHelper.prepareChytUuidsWithTokensTable(deviceIdsTable);
        chytDataTablesHelper.prepareUuidsWithTokens(
                deviceIdsTable,
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2),
                chytUuidWithToken(UUID_3, DEVICE_ID_3, DEVICE_ID_HASH_3)
        );

        // Чтобы убедиться что при сборке используются только данные заданного приложения,
        // настраиваем аналогичную таблицу для приложения Маркета
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_3, DEVICE_ID_HASH_3),
                chytUuidWithToken(UUID_2, DEVICE_ID_4, DEVICE_ID_HASH_4),
                chytUuidWithToken(UUID_3, DEVICE_ID_5, DEVICE_ID_HASH_5)
        );

        var pluggableTable = pluggableTablesTestHelper.preparePluggableTable(UidType.UUID,
                pluggedTableRow(UUID_1),
                pluggedTableRow(UUID_2),
                pluggedTableRow(UUID_3)
        );

        var segment = segmentService.addSegment(segment(
                pluggableTableFilter(
                        pluggableTable.getId(),
                        pluggableTable.getPath(),
                        pluggableTable.getUidColumn(),
                        pluggableTable.getUidType()
                )
        ));

        var variant = variant();
        var config = new PushSendingConf();
        config.setApplication(testAppId);
        config.setVariants(List.of(variant));
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));

        var sending = pushSendingTestHelper.prepareSending(config);
        var rows = generate(sending);

        assertThat(rows, hasSize(3));
        assertResultRow(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1, variant, rows.get(0));
        assertResultRow(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2, variant, rows.get(1));
        assertResultRow(UUID_3, DEVICE_ID_3, DEVICE_ID_HASH_3, variant, rows.get(2));
    }

    private List<PushRow> generate(PushPlainSending sending) throws Exception {
        mockMvc.perform(post("/api/sendings/push/{id}/generate", sending.getId()))
                .andExpect(status().isOk())
                .andDo(print());

        waitGenerated(sending.getId());

        var sendingDir = getSendingDir(sending);
        var resultPath = new PushSendingYtPaths(sendingDir).getSendingTable();

        List<PushRow> results = ytClient.read(resultPath, PushRow.class);
        results.sort(Comparator.comparing(PushRow::getUuid));
        return results;
    }

    private PushPlainSending createSending(Segment segment, PushSendingVariantConf... variants) {
        segment = segmentService.addSegment(segment);
        PushSendingConf config = config(segment, variants);
        return pushSendingTestHelper.prepareSending(config);
    }

    private PushPlainSending createSendingWithPluggedTable(Segment segment,
                                                           PushSendingVariantConf variant,
                                                           PluggableTable pluggableTable) {
        PushSendingConf config = new PushSendingConf();
        config.setApplication(MobileApplication.MARKET_APP);
        config.setVariants(Collections.singletonList(variant));
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setPluggedTables(Collections.singletonList(new PluggedTable(pluggableTable.getId(), "table")));
        config.setSubscriptionType(STORE_PUSH_GENERAL_ADVERTISING.getId());
        return pushSendingTestHelper.prepareSending(config);
    }

    private PushPlainSending prepareSending(Segment segment, PushSendingVariantConf... variants) {
        segment = segmentService.addSegment(segment);

        PushSendingConf config = new PushSendingConf();
        config.setApplication(MobileApplication.MARKET_APP);
        config.setVariants(Arrays.asList(variants));
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setSubscriptionType(STORE_PUSH_GENERAL_ADVERTISING.getId());

        return pushSendingTestHelper.prepareSending(config);
    }

    private void waitGenerated(String sendingId) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        while (true) {
            if (System.currentTimeMillis() - startTime > 1800_000) {
                fail("Generation wait timeout");
            }

            PushPlainSending sending = sendingDAO.getSending(sendingId);

            assertEquals(SendingStage.GENERATE, sending.getStage(), "Sending is not generating");
            assertTrue(Strings.isNullOrEmpty(sending.getMessage()), sending.getMessage());

            StageStatus status = sending.getStageStatus();

            assertNotEquals(StageStatus.ERROR, status, "Generation is failed");

            if (status == StageStatus.FINISHED) {
                return;
            }

            Thread.sleep(1_000);
        }
    }

    private void enableGlobalControl(PushPlainSending sending) {
        sending.getConfig().setGlobalControlEnabled(true);
        pushSendingTestHelper.updateSending(sending);
    }

    @Nonnull
    private YTreeMapNode[] prepareMobileAppsData(int count) {
        var apps = IntStream.rangeClosed(1, count)
                .mapToObj(i -> "uuid-" + i)
                .map(uuid -> chytUuidWithToken(uuid, uuid + "_device-id", uuid + "_device-id-hash"))
                .toArray(YTreeMapNode[]::new);

        chytDataTablesHelper.prepareUuidsWithTokens(apps);

        mobileTablesHelper.prepareMobileAppInfos(
                Stream.of(apps)
                        .map(x -> x.getString("uuid"))
                        .map(MobileTablesHelper::mobileAppInfo)
                        .toArray(MobileAppInfo[]::new)
        );

        return apps;
    }

    private void prepareGenericSubscriptions(int count) {
        var subscriptions = IntStream.rangeClosed(1, count)
                .mapToObj(i -> "uuid-" + i)
                .map(ChytDataTablesHelper::chytUuidWithSubscription)
                .toArray(YTreeMapNode[]::new);

        chytDataTablesHelper.prepareUuidsWithSubscriptions(subscriptions);

        mobileTablesHelper.prepareGenericSubscriptions(
                Stream.of(subscriptions)
                        .map(x -> x.getString("uuid"))
                        .map(MobileTablesHelper::genericSubscription)
                        .toArray(GenericSubscription[]::new)
        );
    }

    private YPath getSendingDir(PushPlainSending sending) {
        return ytFolders.getPushSendingPath(sending.getId());
    }
}
