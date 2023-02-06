package ru.yandex.market.crm.campaign.http.controller;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.PushPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.SendingStage;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingVariantConf;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.dto.sending.SendRequest;
import ru.yandex.market.crm.campaign.services.appmetrica.ServerDateTimeProvider;
import ru.yandex.market.crm.campaign.services.appmetrica.TimeDeltaService;
import ru.yandex.market.crm.campaign.services.grouping.campaign.CampaignDAO;
import ru.yandex.market.crm.campaign.services.sending.PushSendingDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.core.domain.messages.AndroidPushConf;
import ru.yandex.market.crm.core.domain.mobile.MobileApplication;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.AndroidPushMessageContent;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.AppMetricaPushMessage;
import ru.yandex.market.crm.core.test.loggers.TestSentPushesLogWriter;
import ru.yandex.market.crm.core.test.utils.AppMetricaHelper;
import ru.yandex.market.crm.core.test.utils.AppMetricaHelper.Batch;
import ru.yandex.market.crm.core.test.utils.AppMetricaHelper.DeviceSet;
import ru.yandex.market.crm.core.test.utils.AppMetricaHelper.SendPushesRequest;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.mapreduce.domain.mobileapp.MobilePlatform;
import ru.yandex.market.crm.mapreduce.domain.push.ActionType;
import ru.yandex.market.crm.mapreduce.domain.push.PushMessageData;
import ru.yandex.market.crm.mapreduce.domain.push.PushRow;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.mcrm.http.HttpRequest;
import ru.yandex.market.mcrm.http.ResponseBuilder;
import ru.yandex.market.tsum.event.EventId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author zloddey
 */
@ContextConfiguration(classes = {PushSendingTimeToLiveTest.Configuration.class})
public class PushSendingTimeToLiveTest extends AbstractControllerMediumTest {

    // Текущее время на момент выполнения теста. Фиксировано, чтобы не было флапаний
    private static final LocalDateTime FAKE_CURRENT_TIME = LocalDateTime.of(2020, 9, 3, 20, 55);
    // Ограничение на время доставки событий
    private static final LocalTime FINISH_LIMIT = LocalTime.of(21, 30);
    private static final String CONTROL = "variant_control";
    // Граница оффсета часового пояса устройства. Если оффсет устройства ниже этой границы, пуш не должен отправляться
    private Integer minimalAllowedOffset;

    public static class Configuration {
        @Bean
        public ServerDateTimeProvider serverDateTimeProvider() {
            return new ServerDateTimeProvider() {
                @Override
                public LocalDateTime getDateTime() {
                    return FAKE_CURRENT_TIME;
                }
            };
        }
    }

    private static PushSendingVariantConf variant(String id) {
        var variantConf = new PushSendingVariantConf()
                .setId(id)
                .setPercent(40);

        var pushConf = new AndroidPushConf();
        pushConf.setTitle("Test push title");
        pushConf.setText("Test push text");
        pushConf.setActionType(ActionType.URL);
        pushConf.setAction("https://market.yandex.ru/product/111");
        variantConf.setPushConfigs(Map.of(pushConf.getPlatform(), pushConf));

        return variantConf;
    }

    private PushRow pushRow(String variant, int tzOffset, PushMessageData data) {
        if (data != null) {
            data.setTimeToLive(timeDeltaService.countTimeToLive(tzOffset, FINISH_LIMIT));
        }
        PushRow row = new PushRow();
        row.setUuid(UUID.randomUUID().toString());
        row.setDeviceId(UUID.randomUUID().toString());
        row.setDeviceIdHash(UUID.randomUUID().toString());
        row.setVariant(variant);
        row.setData(data);
        row.setTZOffset(tzOffset);
        row.setPlatform(MobilePlatform.ANDROID);
        return row;
    }

    private static void assertBatchItem(PushRow row, List<Batch> batches) {
        // Порядок строк внутри одного батча не определён, поэтому находим подходящий по id устройства.
        Batch batch = batches.stream()
                .filter(b -> b.getDevices().stream().flatMap(d -> d.getIdValues().stream()).anyMatch(s -> row.getDeviceIdHash().equals(s)))
                .findFirst()
                .orElseThrow();

        List<DeviceSet> devices = batch.getDevices();
        Assertions.assertEquals(1, devices.size());
        Assertions.assertEquals(List.of(row.getDeviceIdHash()), devices.get(0).getIdValues());

        PushMessageData data = row.getData();

        AppMetricaPushMessage<AndroidPushMessageContent> message = batch.getPushMessages()
                .getAndroidAppMetricaPushMessage();

        if (data.getActionType() == ActionType.URL) {
            Assertions.assertTrue(message.getOpenUrl().startsWith(data.getAction()));
        } else if (data.getActionType() == ActionType.DEEPLINK) {
            Assertions.assertEquals(data.getAction(), message.getOpenDeepLink());
        }

        AndroidPushMessageContent content = message.getContent();
        Assertions.assertEquals(data.getTitle(), content.getTitle());
        Assertions.assertEquals(data.getText(), content.getText());
        Assertions.assertEquals(data.getTimeToLive(), message.getContent().getTimeToLive());
    }

    @Inject
    private CampaignDAO campaignDAO;
    @Inject
    private PushSendingDAO sendingDAO;
    @Inject
    private JsonSerializer jsonSerializer;
    @Inject
    private YtFolders ytFolders;
    @Inject
    private YtClient ytClient;
    @Inject
    private AppMetricaHelper appMetricaHelper;
    @Inject
    private TestSentPushesLogWriter sentPushesLogWriter;
    @Inject
    private TimeDeltaService timeDeltaService;
    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareCommunicationsTable();
        httpEnvironment.when(HttpRequest.post("https://tsum-api.market.yandex.net:4203/events/addEvent"))
                .then(
                        ResponseBuilder.newBuilder()
                                .body(jsonSerializer.writeObjectAsString(
                                        EventId.newBuilder()
                                                .setId(UUID.randomUUID().toString())
                                                .build()
                                ))
                                .build()
                );
        minimalAllowedOffset = -timeDeltaService.countTimeToLive(0, FINISH_LIMIT);
    }

    /**
     * Девайсы, у которых offset слишком большой, не должны выгружаться в АппМетрику.
     */
    @Test
    public void testRejectSomeDevicesByTTL() throws Exception {
        Assertions.assertEquals(Integer.valueOf(-1500), minimalAllowedOffset);
        PushSendingVariantConf variant1 = variant("variant_a");
        PushSendingVariantConf variant2 = variant("variant_b");

        PushPlainSending sending = prepareSending(variant1, variant2);

        var rows = List.of(
                // Записи, которые не пройдут фильтр по TTL
                pushRow(variant1.getId(), minimalAllowedOffset - 900, pushMessageData(1)),
                pushRow(CONTROL, minimalAllowedOffset - 900, null),
                pushRow(variant2.getId(), minimalAllowedOffset, pushMessageData(3)),
                pushRow(CONTROL, minimalAllowedOffset, null),
                // Записи, которые пройдут фильтр по TTL
                pushRow(variant1.getId(), minimalAllowedOffset + 1, pushMessageData(5)),
                pushRow(CONTROL, minimalAllowedOffset + 1, null),
                pushRow(variant2.getId(), minimalAllowedOffset + 1800, pushMessageData(7)),
                pushRow(CONTROL, minimalAllowedOffset + 1800, null)
        );

        YPath pushPath = ytFolders.getPushSendingPath(sending.getId()).child("sending");
        ytClient.write(pushPath, PushRow.class, rows);

        rows.subList(4, 8).stream()
                .map(PushRow::getDeviceIdHash)
                .forEach(appMetricaHelper::expectDevice);

        requestSend(sending);

        SendPushesRequest request = appMetricaHelper.pollForSendRequest(60);
        assertSentItems(request, rows.get(4), rows.get(6));

        var logRecords = fetchLogRecords(rows.size());
        for (var row : rows) {
            assertInLog(logRecords, row);
        }
    }

    private void assertSentItems(SendPushesRequest request, PushRow... rows) {
        Assertions.assertNotNull(request, "No push message was sent");
        List<Batch> items = request.getSendBatchRequest().getBatches();
        Assertions.assertNotNull(items);
        Assertions.assertEquals(rows.length, items.size());

        for (PushRow row : rows) {
            assertBatchItem(row, items);
        }
    }

    private void assertInLog(Map<String, Map<String, String>> logRecords, PushRow row) {
        Assertions.assertTrue(logRecords.containsKey(row.getUuid()));
        boolean isControl = CONTROL.equalsIgnoreCase(row.getVariant());
        Map<String, String> logRecord = logRecords.get(row.getUuid());
        Assertions.assertEquals(isControl, Boolean.valueOf(logRecord.get("control")));
    }

    private PushMessageData pushMessageData(int messageId) {
        return new PushMessageData()
                .setTitle("Title " + messageId)
                .setText("Text " + messageId)
                .setActionType(ActionType.URL)
                .setAction("https://market.yandex.ru/category/312");
    }

    private void requestSend(PushPlainSending sending) throws Exception {
        SendRequest request = new SendRequest();
        request.setFinishTime(sending.getConfig().getFinishLimit());
        mockMvc.perform(post("/api/sendings/push/{id}/send", sending.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSerializer.writeObjectAsBytes(request)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Nonnull
    private PushPlainSending prepareSending(PushSendingVariantConf... variants) {
        Campaign campaign = new Campaign();
        campaign.setName("Test Campaign");
        campaign = campaignDAO.insert(campaign);

        PushSendingConf config = new PushSendingConf();
        config.setApplication(MobileApplication.MARKET_APP);
        config.setTarget(new TargetAudience(LinkingMode.NONE, "segment_id"));
        config.setVariants(Arrays.asList(variants));
        config.setFinishLimit(FINISH_LIMIT);
        config.setSubscriptionType(SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING.getId());

        PushPlainSending sending = new PushPlainSending();
        sending.setCampaignId(campaign.getId());
        sending.setConfig(config);
        sending.setId(UUID.randomUUID().toString());
        sending.setName("sending");
        sending.setAuthorUid(0L);

        sendingDAO.createSending(sending);

        sending.setStageAndStatus(SendingStage.GENERATE, StageStatus.FINISHED);
        sendingDAO.updateSendingStates(sending.getId(), sending);
        return sending;
    }

    private Map<String, Map<String, String>> fetchLogRecords(int expectedSize) throws InterruptedException {
        Map<String, Map<String, String>> result = new HashMap<>();

        for (int i = 0; i < expectedSize; ++i) {
            Map<String, String> record = sentPushesLogWriter.getRecords().poll(10, TimeUnit.SECONDS);
            String message = String.format("Not enough log records. Expected: %d, Received: %d", expectedSize, i);
            Assertions.assertNotNull(record, message);
            result.put(record.get("uuid"), record);
        }

        return result;
    }
}
