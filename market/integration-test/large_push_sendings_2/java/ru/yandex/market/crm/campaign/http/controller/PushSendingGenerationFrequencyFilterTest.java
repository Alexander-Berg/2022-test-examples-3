package ru.yandex.market.crm.campaign.http.controller;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import com.google.common.base.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.campaign.domain.sending.PushPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.SendingStage;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingVariantConf;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.sending.PushSendingDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.MobileTablesHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.test.utils.YtTableForTestExtension;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.mapreduce.domain.push.PushRow;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.mcrm.utils.PropertiesProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithSubscription;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithToken;
import static ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper.config;
import static ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper.variant;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.mobilesFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.utils.MobileTablesHelper.genericSubscription;
import static ru.yandex.market.crm.core.test.utils.MobileTablesHelper.mobileAppInfo;

/**
 * @author zloddey
 */
public class PushSendingGenerationFrequencyFilterTest extends AbstractControllerLargeTest {

    private static final String UUID_1 = "uuid-1";
    private static final String UUID_2 = "uuid-2";

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
    private MobileTablesHelper mobileTablesHelper;
    @Inject
    private PushSendingTestHelper pushSendingTestHelper;
    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;
    @Inject
    private PropertiesProvider propertiesProvider;

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
     * В данные для отправки прорастают лимиты на число коммуникаций из внешней таблицы
     */
    @Test
    public void testApplyFrequencyLimitsFromExternalTable() throws Exception {
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2)
        );

        mobileTablesHelper.prepareMobileAppInfos(
                mobileAppInfo(UUID_1),
                mobileAppInfo(UUID_2)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1),
                chytUuidWithSubscription(UUID_2)
        );

        mobileTablesHelper.prepareGenericSubscriptions(
                genericSubscription(UUID_1),
                genericSubscription(UUID_2)
        );

        Segment segment = segment(
                mobilesFilter()
        );

        prepareFrequencyLimitTable(
                frequencyLimitRow(UUID_1, "1"),
                frequencyLimitRow(UUID_2, "0-2")
        );

        PushSendingVariantConf variant = variant();
        PushPlainSending sending = createSending(segment, variant);
        List<PushRow> results = generate(sending);

        assertEquals(2, results.size());
        assertEquals(Option.of(1), results.get(0).getLimit());
        assertEquals(Option.of(2), results.get(1).getLimit());
    }

    /**
     * Если внешней таблицы с ограничениями нет, то лимит должен отсутствовать
     */
    @Test
    public void testApplyDefaultFrequencyLimitsWhenExternalTableIsMissing() throws Exception {
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2)
        );

        mobileTablesHelper.prepareMobileAppInfos(
                mobileAppInfo(UUID_1),
                mobileAppInfo(UUID_2)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1),
                chytUuidWithSubscription(UUID_2)
        );

        mobileTablesHelper.prepareGenericSubscriptions(
                genericSubscription(UUID_1),
                genericSubscription(UUID_2)
        );

        Segment segment = segment(
                mobilesFilter()
        );

        PushSendingVariantConf variant = variant();
        PushPlainSending sending = createSending(segment, variant);
        List<PushRow> results = generate(sending);

        assertEquals(2, results.size());
        assertEquals(Option.empty(), results.get(0).getLimit());
        assertEquals(Option.empty(), results.get(1).getLimit());
    }

    private YTreeMapNode frequencyLimitRow(String uuid, String limit) {
        return YTree.mapBuilder()
                .key("crypta_id").value("crypta id for " + uuid)
                .key("quantity").value(limit)
                .key("split").value("test-generated")
                .key("uuid").value(uuid)
                .buildMap();
    }

    private void prepareFrequencyLimitTable(YTreeMapNode... rows) {
        var now = LocalDateTime.now();
        String day = now.toLocalDate().toString();
        var path = YPath.simple(propertiesProvider.get("var.frequency_limits_dir")).child(day);
        ytTableForTest.create(ytClient, path, "frequency_filter.yson");
        ytClient.link(path, path.parent().child("latest"), true);
        ytClient.write(path, YTableEntryTypes.YSON, List.of(rows));
    }

    private List<PushRow> generate(PushPlainSending sending) throws Exception {
        mockMvc.perform(post("/api/sendings/push/{id}/generate", sending.getId()))
                .andExpect(status().isOk())
                .andDo(print());

        waitGenerated(sending.getId());

        YPath resultPath = ytFolders.getPushSendingPath(sending.getId()).child("sending");

        List<PushRow> results = ytClient.read(resultPath, PushRow.class);
        results.sort(Comparator.comparing(PushRow::getUuid));
        return results;
    }

    private PushPlainSending createSending(Segment segment, PushSendingVariantConf... variants) {
        segment = segmentService.addSegment(segment);
        PushSendingConf config = config(segment, variants);
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
}
