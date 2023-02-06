package ru.yandex.market.crm.campaign.http.controller;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import ru.yandex.market.crm.campaign.domain.pluggabletable.PluggableTable;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.PushPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.SendingStage;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingVariantConf;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.sending.PushSendingDAO;
import ru.yandex.market.crm.campaign.services.sending.push.PushSendingYtPaths;
import ru.yandex.market.crm.campaign.test.AbstractControllerLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper;
import ru.yandex.market.crm.core.domain.messages.AbstractPushConf;
import ru.yandex.market.crm.core.domain.messages.PluggedTable;
import ru.yandex.market.crm.core.domain.mobile.MobileApplication;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.MobileTablesHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper.IdRelation;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.test.utils.YtTableForTestExtension;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.mapreduce.domain.push.PushRow;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.platform.models.GenericSubscription;
import ru.yandex.market.crm.platform.models.MobileAppInfo;
import ru.yandex.market.crm.util.LiluCollectors;
import ru.yandex.market.crm.yt.client.YtClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportUuid;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithSubscription;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithToken;
import static ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper.pluggedTableRow;
import static ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper.config;
import static ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper.variant;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.mobilesFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.passportGender;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.passportProfile;

/**
 * @author apershukov
 */
public class PushSendingGenerationTest1 extends AbstractControllerLargeTest {

    private static String randomUuid() {
        return UUID.randomUUID().toString();
    }

    private static final String UUID_1 = "uuid-1";
    private static final String UUID_2 = "uuid-2";

    private static final long PUID_1 = 111;
    private static final long PUID_2 = 222;

    private static final String DEVICE_ID_1 = "device_id_1";
    private static final String DEVICE_ID_2 = "device_id_2";

    private static final String DEVICE_ID_HASH_1 = "device_id_hash_1";
    private static final String DEVICE_ID_HASH_2 = "device_id_hash_2";

    @Inject
    private PushSendingDAO sendingDAO;
    @Inject
    private SegmentService segmentService;
    @Inject
    private YtFolders ytFolders;
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
    private ChytDataTablesHelper chytDataTablesHelper;

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

    /**
     * При генерации рассылки с несколькими вариантами в сообщение определенного варианта
     * попадают свойства именно его варианта. Например, заголовок.
     */
    @Test
    public void testGenerateMultivariantSending() throws Exception {
        final int count = 100;

        YTreeMapNode[] apps = prepareMobileAppsData(count);
        prepareGenericSubscriptions(count);

        Segment segment = segment(
                mobilesFilter()
        );

        PushSendingVariantConf variant1 = variant("variant_1", 50, "Title 1");
        PushSendingVariantConf variant2 = variant("variant_2", 50, "Title 2");
        PushPlainSending sending = createSending(segment, variant1, variant2);
        List<PushRow> results = generate(sending);

        assertEquals(apps.length, results.size());

        AbstractPushConf pushConf1 = variant1.getPushConfigs().values().iterator().next();
        AbstractPushConf pushConf2 = variant2.getPushConfigs().values().iterator().next();

        PushRow variant1Row = results.stream()
                .filter(row -> variant1.getId().equals(row.getVariant()))
                .findFirst().orElseThrow(() -> new IllegalStateException("No row for variant " + variant1.getId()));

        assertEquals(pushConf1.getTitle(), variant1Row.getData().getTitle());

        PushRow variant2Row = results.stream()
                .filter(row -> variant2.getId().equals(row.getVariant()))
                .findFirst().orElseThrow(() -> new IllegalStateException("No row for variant " + variant2.getId()));

        assertEquals(pushConf2.getTitle(), variant2Row.getData().getTitle());
    }

    /**
     * После генерации рассылки на странице показываются метрики с результатами
     */
    @Test
    public void testDisplayGenerationResults() throws Exception {
        final int count = 100;

        YTreeMapNode[] apps = prepareMobileAppsData(count);
        prepareGenericSubscriptions(count);

        Segment segment = segment(
                mobilesFilter()
        );

        PushSendingVariantConf variant1 = variant("variant_1", 50, "Title 1");
        PushSendingVariantConf variant2 = variant("variant_2", 30, "Title 2");
        PushPlainSending sending = createSending(segment, variant1, variant2);
        List<PushRow> results = generate(sending);

        assertEquals(apps.length, results.size());

        List<PushRow> variant1Rows = results.stream()
                .filter(row -> variant1.getId().equals(row.getVariant()))
                .collect(Collectors.toList());

        List<PushRow> variant2Rows = results.stream()
                .filter(row -> variant2.getId().equals(row.getVariant()))
                .collect(Collectors.toList());

        List<PushRow> controlRows = results.stream()
                .filter(row -> !variant1.getId().equals(row.getVariant()) && !variant2.getId().equals(row.getVariant()))
                .collect(Collectors.toList());

        int variant1Size = variant1Rows.size();
        int variant2Size = variant2Rows.size();
        int controlSize = controlRows.size();
        int targetGroupSize = variant1Size + variant2Size;

        // На данный момент appMetricaLoadedSize и targetGroupSize совпадают, хотя и вычислаются
        // по разным таблицам. В будущем это может измениться
        // Размеры групп обычно рандомные, но мы можем проверить, что их сумма укладывается в пределы 80±10
        assertThat(targetGroupSize, allOf(
                greaterThanOrEqualTo(70),
                lessThanOrEqualTo(90)
        ));

        mockMvc.perform(get("/api/sendings/push/{id}", sending.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generationResult.segmentSize").value(100))
                .andExpect(jsonPath("$.generationResult.targetGroupSize").value(targetGroupSize))
                .andExpect(jsonPath("$.generationResult.controlGroupSize").value(controlSize))
                .andExpect(jsonPath("$.generationResult.appMetricaLoadedSize").value(targetGroupSize));
    }

    /**
     * Uuid'ы, связанные с одинаковым crypta id попадают в один и тот же вариант
     */
    @Test
    public void testDistributeVariantsByCryptaId() throws Exception {
        final int count = 10;

        var apps = IntStream.rangeClosed(1, count)
                .mapToObj(i -> chytUuidWithToken(randomUuid(), randomUuid(), randomUuid()))
                .toArray(YTreeMapNode[]::new);

        chytDataTablesHelper.prepareUuidsWithTokens(apps);

        mobileTablesHelper.prepareMobileAppInfos(
                Stream.of(apps)
                        .map(x -> x.getString("uuid"))
                        .map(MobileTablesHelper::mobileAppInfo)
                        .toArray(MobileAppInfo[]::new)
        );

        var subscriptions = Stream.of(apps)
                .map(x -> x.getString("uuid"))
                .map(ChytDataTablesHelper::chytUuidWithSubscription)
                .toArray(YTreeMapNode[]::new);

        chytDataTablesHelper.prepareUuidsWithSubscriptions(subscriptions);

        mobileTablesHelper.prepareGenericSubscriptions(
                Stream.of(subscriptions)
                        .map(x -> x.getString("uuid"))
                        .map(MobileTablesHelper::genericSubscription)
                        .toArray(GenericSubscription[]::new)
        );

        userTestHelper.saveLinks(
                UserTestHelper.UUID,
                UserTestHelper.CRYPTA_ID,
                Stream.of(apps)
                        .map(x -> x.getString("uuid"))
                        .map(uuid -> new IdRelation(uuid, "crypta_user"))
                        .toArray(IdRelation[]::new)
        );

        Segment segment = segment(
                mobilesFilter()
        );

        PushPlainSending sending = createSending(segment,
                variant("a", 50),
                variant("b", 50)
        );
        List<PushRow> results = generate(sending);

        assertEquals(10, results.size());

        String commonVariantId = null;

        for (PushRow row : results) {
            String variantId = row.getVariant();

            assertTrue(
                    commonVariantId == null || commonVariantId.equals(variantId),
                    "Uuids are in different variants"
            );

            commonVariantId = variantId;
        }
    }

    /**
     * У строк, попавших в контрольную группу, информация об отправляемом push-сообщении
     * не заполняется
     */
    @Test
    public void testBuildSendingWithControlGroup() throws Exception {
        final int count = 100;

        prepareMobileAppsData(count);
        prepareGenericSubscriptions(count);

        Segment segment = segment(
                mobilesFilter()
        );

        PushSendingVariantConf variant = variant("variant_a", 10);

        PushPlainSending sending = createSending(segment, variant);
        PushRow controlRow = generate(sending).stream()
                .filter(row -> row.getVariant().endsWith("_control"))
                .findFirst().orElseThrow(() -> new AssertionError("No control rows"));

        assertNotNull(controlRow.getUuid());
        assertNotNull(controlRow.getDeviceId());
        assertNotNull(controlRow.getDeviceIdHash());
        assertNull(controlRow.getData());
    }

    /**
     * Системные переменные lastname и firstname доступны для использования
     * в push-рассылках
     */
    @Test
    public void testUserUsernameVariables() throws Exception {
        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m", "Иван", "Иванов"),
                passportProfile(PUID_2, "m", "Петр", "Петров")
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

        Segment segment = segment(
                passportGender("m")
        );

        PushSendingVariantConf variant = variant(
                "variant_a",
                100,
                "Здравствуйте, ${vars.firstname} ${vars.lastname}"
        );

        PushPlainSending sending = createSending(segment, variant);
        Map<String, PushRow> results = generate(sending).stream()
                .collect(LiluCollectors.index(PushRow::getUuid));

        assertEquals(2, results.size());

        assertEquals("Здравствуйте, Иван Иванов", results.get(UUID_1).getData().getTitle());
        assertEquals("Здравствуйте, Петр Петров", results.get(UUID_2).getData().getTitle());
    }

    /**
     * В случае если к рассылке подключена внешняя таблица с идентификаторами пользователя типа PUID,
     * значения её колонок можно использовать в полях уведомления. При этом:
     * <p>
     * 1. Если значение нашлось, оно будет доступно в переменной вида
     * u_vars.${алиас таблицы, указанный в рассылке}.${название колонки}
     * 2. Если значение не нашлось, письмо все равно попадает в рассылку. При этом переменная
     * u_vars.${алиас таблицы, указанный в рассылке} заполнена пустым объектом
     */
    @Test
    public void testBuildWithPluggedPuidVars() throws Exception {
        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable(UidType.PUID,
                pluggedTableRow(String.valueOf(PUID_1), "100500")
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
        assertEquals("null", results.get(UUID_2).getData().getTitle());
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
