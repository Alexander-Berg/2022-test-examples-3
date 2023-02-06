package ru.yandex.market.crm.campaign.services.appmetrica;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.campaign.domain.promo.entities.PromoEntityConfig;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingConf;
import ru.yandex.market.crm.campaign.loggers.SentPromoPushesLogger;
import ru.yandex.market.crm.campaign.services.sending.FrequencyToggleService;
import ru.yandex.market.crm.campaign.services.throttle.ChannelDescription;
import ru.yandex.market.crm.campaign.services.throttle.Communication;
import ru.yandex.market.crm.campaign.services.throttle.CommunicationThrottleResponse;
import ru.yandex.market.crm.campaign.services.throttle.CommunicationThrottleService;
import ru.yandex.market.crm.campaign.services.throttle.ContactAttempt;
import ru.yandex.market.crm.campaign.services.throttle.DaoFactory;
import ru.yandex.market.crm.campaign.services.throttle.POJOCommunicationsDAO;
import ru.yandex.market.crm.core.domain.mobile.MetricaMobileApp;
import ru.yandex.market.crm.core.domain.mobile.MobileApplication;
import ru.yandex.market.crm.core.domain.mobile.features.FrequencyThrottling;
import ru.yandex.market.crm.core.domain.mobile.features.PushCategoryFeature;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.services.appmetrica.AppMetricaService;
import ru.yandex.market.crm.core.services.external.appmetrica.HttpAppMetricaApiClient;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.IOSPushMessageContent;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushBatchItem;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushBatchRequest;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushMessageContent;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushSendGroup;
import ru.yandex.market.crm.core.services.sending.AndroidPushContentFactory;
import ru.yandex.market.crm.core.services.sending.IOSPushContentFactory;
import ru.yandex.market.crm.core.services.sending.MultiPlatformPushBatchItemFactory;
import ru.yandex.market.crm.core.services.sending.UtmLinks;
import ru.yandex.market.crm.core.test.loggers.TestSentPushesLogWriter;
import ru.yandex.market.crm.mapreduce.domain.mobileapp.IOSAttachmentFileType;
import ru.yandex.market.crm.mapreduce.domain.mobileapp.MobilePlatform;
import ru.yandex.market.crm.mapreduce.domain.push.ActionType;
import ru.yandex.market.crm.mapreduce.domain.push.PushMessageAttachment;
import ru.yandex.market.crm.mapreduce.domain.push.PushMessageData;
import ru.yandex.market.crm.mapreduce.domain.push.PushRow;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.mcrm.lock.LockService;
import ru.yandex.market.mcrm.lock.MockLockService;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author vtarasoff
 * @since 07.04.2021
 */
public class PushMessagesUploaderTest {

    private ContactAttempt push(String id) {
        return new ContactAttempt(id, DAILY_LIMIT);
    }

    private static final String IOS_ATTACH_URL = "http://yandex.com/test.jpg";
    private static final String IOS_ATTACH_ID = Hex.encodeHexString(IOS_ATTACH_URL.getBytes(Hex.DEFAULT_CHARSET));
    private static final int DAILY_LIMIT = 3;
    private static final String SENDING_ID = "sending_id";
    private static final String SEGMENT_ID = "segment_id";

    private final List<PushRow> pushDevices = new LinkedList<>();
    private final ServerDateTimeProvider dateTimeProvider = new FakeTimeProvider(2020, 9, 1, 12, 0);
    private PushBatchRequest sentRequest;

    private final POJOCommunicationsDAO communicationsDAO = new POJOCommunicationsDAO();
    private final DaoFactory daoFactory = channel -> communicationsDAO;
    private CommunicationServiceStub communicationThrottleService;
    private final FrequencyToggleService frequencyToggleService =
            new FrequencyToggleService(new POJOFrequencyToggleDAO());
    private YtClient ytClient;
    private AppMetricaService appMetricaService;
    private MultiPlatformPushBatchItemFactory batchItemFactory;
    private final TimeDeltaService timeDeltaService = new TimeDeltaService(dateTimeProvider);
    private PushMessagesUploader uploader;
    private TestSentPushesLogWriter logWriter;

    @BeforeEach
    public void setUp() {
        ytClient = mock(YtClient.class);
        when(ytClient.read(any(YPath.class), any(Class.class))).thenReturn(pushDevices);

        PushSendGroup pushSendGroup = new PushSendGroup(MetricaMobileApp.BERU.getId(), "sending_1");
        pushSendGroup.setId(1);

        HttpAppMetricaApiClient httpAppMetricaApiClient = mock(HttpAppMetricaApiClient.class);
        when(httpAppMetricaApiClient.getPushSendGroups(anyInt())).thenReturn(List.of(pushSendGroup));
        when(httpAppMetricaApiClient.sendPushBatch(any())).thenAnswer(invocation -> {
            sentRequest = invocation.getArgument(0, PushBatchRequest.class);
            return 0;
        });

        batchItemFactory = new MultiPlatformPushBatchItemFactory(
                List.of(new AndroidPushContentFactory(), new IOSPushContentFactory())
        );

        appMetricaService = new AppMetricaService(httpAppMetricaApiClient, batchItemFactory);
        communicationThrottleService = new CommunicationServiceStub(new MockLockService(), dateTimeProvider,
                daoFactory);
        frequencyToggleService.toggleFrequencyFilter(FrequencyToggleService.GLOBAL_TOGGLE, true);
        uploader = uploader(communicationThrottleService);
        logWriter = new TestSentPushesLogWriter();
    }

    @AfterEach
    public void tearDown() {
        pushDevices.clear();
        sentRequest = null;
    }

    @Test
    public void shouldUploadMultiplatformCorrectBatch() {
        pushDevices.add(pushRow("variant", "uuid_1", pushDataSample(MobilePlatform.ANDROID)));
        pushDevices.add(pushRow("variant", "uuid_2", pushDataSample(MobilePlatform.iOS)));

        uploadInterval(
                null,
                mock(SentPromoPushesLogger.class)
        );

        assertAll(
                () -> assertNotNull(sentRequest),
                () -> assertEquals(2, sentRequest.getBatch().size())
        );

        PushBatchItem androidBatch = sentRequest.getBatch().stream()
                .filter(b -> b.getAndroidPushMessage() != null)
                .findFirst()
                .orElse(null);
        assertNotNull(androidBatch);
        assertDevices(androidBatch, "uuid_1");
        assertGeneralContent(androidBatch);

        PushBatchItem iosBatch = sentRequest.getBatch().stream()
                .filter(b -> b.getIOSPushMessage() != null)
                .findFirst()
                .orElse(null);
        assertNotNull(iosBatch);
        assertDevices(iosBatch, "uuid_2");
        assertGeneralContent(iosBatch);
        assertIOSContent(iosBatch);
    }

    /**
     * Пуши могут быть отброшены по time to live, если время их отправки слишком позднее.
     * Проверка запускается, если в метод uploadInterval передаётся непустой finishLimit.
     * <p>
     * Отброшенные пуши <b>не должны</b> записываться в лог, а их число должно отправляться в listener.
     * Фильтрация осуществляется только для ANDROID-пушей.
     */
    @Test
    public void discardByTTL() {
        pushDevices.add(pushRow("variant", "uuid_1", pushDataSample(MobilePlatform.ANDROID)));
        pushDevices.add(pushRow("variant", "uuid_2", pushDataSample(MobilePlatform.iOS)));
        pushDevices.add(pushRow("variant_control", "uuid_3", pushDataSample(MobilePlatform.ANDROID)));
        pushDevices.add(pushRow("variant_control", "uuid_4", pushDataSample(MobilePlatform.iOS)));

        var result = uploadInterval(LocalTime.of(0, 0, 0), logger());

        assertEquals(1, result.getDiscardedByTtl());

        var entries = getLogEntries();

        assertLogEntry("uuid_1", "SKIPPED_BY_TTL_FILTER", false, "variant", entries);
        assertLogEntry("uuid_2", "UPLOADED", false, "variant", entries);
        assertLogEntry("uuid_3", "SKIPPED_BY_TTL_FILTER", true, null, entries);
        assertLogEntry("uuid_4", "SKIPPED_AS_CONTROL", true, null, entries);
    }

    /**
     * Пуши могут быть отброшены, если окажется, что мы слишком часто взаимодействуем с их получателем.
     * <p>
     * Отброшенные пуши <b>не должны</b> записываться в лог, а их число должно отправляться в листенер.
     */
    @Test
    public void discardByFrequency() {
        pushDevices.add(pushRow("variant", "uuid_1", 1, pushDataSample(MobilePlatform.ANDROID)));
        pushDevices.add(pushRow("variant", "uuid_2", pushDataSample(MobilePlatform.iOS)));
        pushDevices.add(pushRow("variant_control", "uuid_3", pushDataSample(MobilePlatform.ANDROID)));
        pushDevices.add(pushRow("variant_control", "uuid_4", 1, pushDataSample(MobilePlatform.iOS)));

        // Имитируем предыдущие коммуникации с получателями uuid_1 и uuid_4
        var now = dateTimeProvider.getDateTime();
        communicationsDAO.addMany(Map.of(
                new ContactAttempt("uuid_1", 3),
                new Communication(now.minus(1, ChronoUnit.HOURS), "promo", "fake"),
                new ContactAttempt("uuid_4", 3),
                new Communication(now.minus(1, ChronoUnit.HOURS), "promo", "fake")
        ));

        var result = uploadInterval(null, logger());

        assertAll(
                () -> assertNotNull(sentRequest),
                () -> assertEquals(1, sentRequest.getBatch().size())
        );

        PushBatchItem batch = sentRequest.getBatch().get(0);
        assertNotNull(batch.getIOSPushMessage());
        assertDevices(batch, "uuid_2");
        assertGeneralContent(batch);

        assertEquals(1, result.getDiscardedByFrequency());

        var entries = getLogEntries();

        assertLogEntry("uuid_1", "SKIPPED_BY_FREQUENCY_FILTER", false, "variant", entries);
        assertLogEntry("uuid_2", "UPLOADED", false, "variant", entries);
        assertLogEntry("uuid_3", "SKIPPED_AS_CONTROL", true, null, entries);
        assertLogEntry("uuid_4", "SKIPPED_BY_FREQUENCY_FILTER", true, null, entries);
    }

    /**
     * Если флаг для рассылки опущен, то не надо фильтровать пуши по частоте.
     */
    @Test
    public void doNotDiscardByFrequency() {
        pushDevices.add(pushRow("variant", "uuid_1", pushDataSample(MobilePlatform.ANDROID)));
        pushDevices.add(pushRow("variant", "uuid_2", pushDataSample(MobilePlatform.iOS)));
        pushDevices.add(pushRow("variant_control", "uuid_3", pushDataSample(MobilePlatform.ANDROID)));
        pushDevices.add(pushRow("variant_control", "uuid_4", pushDataSample(MobilePlatform.iOS)));

        // Имитируем большое количество предыдущих коммуникаций со всеми получателями
        var now = dateTimeProvider.getDateTime();
        for (int i = 0; i < DAILY_LIMIT; i++) {
            communicationsDAO.addMany(Map.of(
                    push("uuid_1"), new Communication(now.minus(i, ChronoUnit.HOURS), "promo", "fake"),
                    push("uuid_2"), new Communication(now.minus(i, ChronoUnit.HOURS), "promo", "fake"),
                    push("uuid_3"), new Communication(now.minus(i, ChronoUnit.HOURS), "promo", "fake"),
                    push("uuid_4"), new Communication(now.minus(i, ChronoUnit.HOURS), "promo", "fake")
            ));
        }

        // По факту, в рассылке к идентификатору отправки добавляется идентификатор шага,
        // а в таблице фильтрации шаг не сохраняется
        frequencyToggleService.toggleFrequencyFilter("sending", false);
        uploadInterval(null, logger());

        assertAll(
                () -> assertNotNull(sentRequest),
                () -> assertEquals(2L, sentRequest.getBatch().size())
        );

        PushBatchItem androidBatch = sentRequest.getBatch().stream()
                .filter(b -> b.getAndroidPushMessage() != null)
                .findFirst()
                .orElse(null);

        assertNotNull(androidBatch);
        assertDevices(androidBatch, "uuid_1");
        assertGeneralContent(androidBatch);

        PushBatchItem iosBatch = sentRequest.getBatch().stream()
                .filter(b -> b.getIOSPushMessage() != null)
                .findFirst()
                .orElse(null);

        assertNotNull(iosBatch);
        assertDevices(iosBatch, "uuid_2");
        assertGeneralContent(iosBatch);
        assertIOSContent(iosBatch);

        var entries = getLogEntries();

        assertLogEntry("uuid_1", "UPLOADED", false, "variant", entries);
        assertLogEntry("uuid_2", "UPLOADED", false, "variant", entries);
        assertLogEntry("uuid_3", "SKIPPED_AS_CONTROL", true, null, entries);
        assertLogEntry("uuid_4", "SKIPPED_AS_CONTROL", true, null, entries);
    }

    /**
     * Если при фильтрации пушей произошла ошибка, мы должны сделать ещё одну попытку на фильтрацию
     */
    @Test
    public void discardByFrequencyWithRetries() {
        // Первое обращение к сервису должно привести к ошибке
        // А второе обращение должно пройти успешно
        communicationThrottleService.failCounter = 1;

        pushDevices.add(pushRow("variant", "uuid_1", pushDataSample(MobilePlatform.ANDROID)));
        pushDevices.add(pushRow("variant", "uuid_2", pushDataSample(MobilePlatform.iOS)));
        pushDevices.add(pushRow("variant_control", "uuid_3", pushDataSample(MobilePlatform.ANDROID)));
        pushDevices.add(pushRow("variant_control", "uuid_4", pushDataSample(MobilePlatform.iOS)));

        var result = uploadInterval(null, logger());

        var entries = getLogEntries();

        assertAll(
                () -> assertNotNull(sentRequest),
                () -> assertEquals(2, sentRequest.getBatch().size()),
                () -> assertEquals(0, result.getDiscardedByFrequency()),
                () -> assertLogEntry("uuid_1", "UPLOADED", false, "variant", entries),
                () -> assertLogEntry("uuid_2", "UPLOADED", false, "variant", entries),
                () -> assertLogEntry("uuid_3", "SKIPPED_AS_CONTROL", true, null, entries),
                () -> assertLogEntry("uuid_3", "SKIPPED_AS_CONTROL", true, null, entries)
        );
    }

    /**
     * Если у приложения есть фича с указанной push-категорией, то при отправке пуша на это приложение
     * в контенте устанавливается данная категория
     */
    @Test
    public void testUploadPushToIOSAppWithCategory() {
        var category = "some_category";
        var feature = new PushCategoryFeature();
        feature.setCategory(category);

        var application = new MobileApplication();
        application.setId(MobileApplication.MARKET_APP);
        application.setMetricaAppId(111);
        application.setFeatures(List.of(feature));

        pushDevices.add(pushRow("variant", "uuid", pushDataSample(MobilePlatform.iOS)));

        uploadInterval(application, null, logger());

        assertNotNull(sentRequest);
        assertEquals(1, sentRequest.getBatch().size());

        var iosBatch = sentRequest.getBatch().stream()
                .filter(b -> b.getIOSPushMessage() != null)
                .findFirst()
                .orElse(null);
        assertNotNull(iosBatch);
        assertDevices(iosBatch, "uuid");
        assertGeneralContent(iosBatch);
        assertIOSContent(iosBatch);
        assertEquals(iosBatch.getIOSPushMessage().getContent().getCategory(), category);
    }

    /**
     * Если у приложения отсутствует фича с push-категорией, то при отправке пуша на это приложение
     * в контенте категория не устанавливается
     */
    @Test
    public void testUploadPushToIOSAppWithoutCategory() {
        pushDevices.add(pushRow("variant", "uuid", pushDataSample(MobilePlatform.iOS)));

        uploadInterval(null, logger());

        assertNotNull(sentRequest);
        assertEquals(1, sentRequest.getBatch().size());

        var iosBatch = sentRequest.getBatch().stream()
                .filter(b -> b.getIOSPushMessage() != null)
                .findFirst()
                .orElse(null);
        assertNotNull(iosBatch);
        assertDevices(iosBatch, "uuid");
        assertGeneralContent(iosBatch);
        assertIOSContent(iosBatch);
        assertNull(iosBatch.getIOSPushMessage().getContent().getCategory());
    }

    /**
     * В случае, если при обращении к сервису ограничений частоты возникает ошибка, по истечении количества
     * попыток выбрасывается исключение и пуши не отправляются
     * <p>
     * См. https://st.yandex-team.ru/LILUCRM-5177
     */
    @Test
    void testDoNotIgnoreThrottlingErrors() {
        var throttleService = mock(CommunicationThrottleService.class);
        when(throttleService.requestMany(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Service exception"));

        uploader = uploader(throttleService);

        pushDevices.add(pushRow("uuid_1", pushDataSample(MobilePlatform.ANDROID)));
        pushDevices.add(pushRow("uuid_2", pushDataSample(MobilePlatform.iOS)));

        assertThrows(
                IllegalStateException.class,
                () -> uploadInterval(null, logger())
        );

        assertNull(sentRequest);
    }

    /**
     * При отправке пуша на IOS, если выбран динамический тип прикрепляемого файла, то реальный тип файла
     * будет получен из url'а файла
     */
    @Test
    public void testSendIosPushWithDynamicAttachment() {
        var fileUrl = "http://yandex.com/picture.png";

        var data = pushDataSample(MobilePlatform.iOS).setAttachments(List.of(
                new PushMessageAttachment()
                        .setFileUrl(fileUrl)
                        .setFileType(IOSAttachmentFileType.DYNAMIC.getId())
        ));

        pushDevices.add(pushRow("variant", "uuid", data));

        uploadInterval(null, logger());

        assertNotNull(sentRequest);
        assertEquals(1, sentRequest.getBatch().size());

        var iosBatch = sentRequest.getBatch().stream()
                .filter(b -> b.getIOSPushMessage() != null)
                .findFirst()
                .orElse(null);

        assertNotNull(iosBatch);
        assertDevices(iosBatch, "uuid");
        assertGeneralContent(iosBatch);

        IOSPushMessageContent content = iosBatch.getIOSPushMessage().getContent();

        assertEquals(1, content.getMutableContent());
        assertNotNull(content.getAttachments());
        assertEquals(1L, content.getAttachments().size());

        IOSPushMessageContent.Attachment attachment = content.getAttachments().get(0);
        assertEquals(fileUrl, attachment.getFileUrl());
        assertEquals("png", attachment.getFileType());
    }

    @Test
    public void testPushLogData() throws Exception {
        pushDevices.add(pushRow("variant", "uuid", pushDataSample(MobilePlatform.iOS)));

        uploadInterval(null, logger());

        List<Map<String, String>> recordsList = logWriter.getRecordsAsList();
        assertEquals(1, recordsList.size());
        Map<String, String> records = recordsList.get(0);

        assertTskv(records,
                "control", "false",
                "transferId", "0",
                "title", "title",
                "type", "1",
                "uuid", "uuid",
                "deviceId", "uuid_device_id",
                "platform", "iOS",
                "application", "market",
                "sendingId", "sending_id",
                "deviceIdHash", "uuid_device_id_hash",
                "appId", "1389598",
                "globalControl", "false",
                "segmentId", "segment_id",
                "text", "text",
                "variantId", "variant",
                "promo_tags", "TAG,SHMAG",
                "cat_stream", "LUL,KEK",
                "shopPromoId", "shopPromoId",
                "status", "UPLOADED");
    }

    private PushMessageData pushDataSample(MobilePlatform platform) {
        return new PushMessageData()
                .setData("data")
                .setAction("action")
                .setPlatform(platform)
                .setTimeToLive(Integer.MAX_VALUE)
                .setBanner("banner")
                .setActionType(ActionType.URL)
                .setIcon("icon")
                .setIconBackground("iconBackground")
                .setImage("image")
                .setMediaAttachment("mediaAttachment")
                .setAttachments(List.of(
                        new PushMessageAttachment()
                                .setFileUrl(IOS_ATTACH_URL)
                                .setFileType("jpg")))
                .setText("text")
                .setTimeToLiveOnDevice(Long.MAX_VALUE)
                .setTitle("title");
    }

    private void assertDevices(PushBatchItem batch, String uuid) {
        assertAll(
                () -> assertEquals(1L, batch.getDevices().size()),
                () -> assertEquals(1, batch.getDevices().get(0).getDevices().size()),
                () -> assertEquals(uuid, batch.getDevices().get(0).getDevices().get(0).getUuid())
        );
    }

    private void assertGeneralContent(PushBatchItem batch) {
        PushMessageContent content = batch.getAndroidPushMessage() != null
                ? batch.getAndroidPushMessage().getContent()
                : batch.getIOSPushMessage().getContent();

        assertAll(
                () -> assertEquals("text", content.getText()),
                () -> assertEquals("title", content.getTitle())
        );
    }

    private void assertIOSContent(PushBatchItem batch) {
        IOSPushMessageContent content = batch.getIOSPushMessage().getContent();

        assertAll(
                () -> assertEquals(1, content.getMutableContent()),
                () -> assertNotNull(content.getAttachments()),
                () -> assertEquals(1L, content.getAttachments().size())
        );

        IOSPushMessageContent.Attachment attachment = content.getAttachments().get(0);
        assertAll(
                () -> assertEquals(IOS_ATTACH_URL, attachment.getFileUrl()),
                () -> assertEquals("jpg", attachment.getFileType()),
                () -> assertEquals(IOS_ATTACH_ID, attachment.getId())
        );
    }

    private static PushRow pushRow(String variant, String uuid, PushMessageData pushData) {
        return pushRow(variant, uuid, DAILY_LIMIT, pushData);
    }

    private static PushRow pushRow(String uuid, PushMessageData pushData) {
        return pushRow("variant", uuid, pushData);
    }

    private static PushRow pushRow(String variant, String uuid, int limit, PushMessageData pushData) {
        PushRow pushRow = new PushRow();
        pushRow.setLimit(Option.ofNullable(limit));
        pushRow.setData(pushData);
        pushRow.setPlatform(pushData.getPlatform());
        pushRow.setDeviceId(uuid + "_device_id");
        pushRow.setUuid(uuid);
        pushRow.setVariant(variant);
        pushRow.setDeviceIdHash(uuid + "_device_id_hash");

        return pushRow;
    }

    private static class CommunicationServiceStub extends CommunicationThrottleService {
        private int failCounter = 0;

        public CommunicationServiceStub(LockService lockService,
                                        ServerDateTimeProvider dateTimeProvider,
                                        DaoFactory daoFactory) {
            super(lockService, dateTimeProvider, daoFactory);
        }

        @Override
        public Map<ContactAttempt, CommunicationThrottleResponse> requestMany(Collection<ContactAttempt> contactAttempts,
                                                                              ChannelDescription channel,
                                                                              String type,
                                                                              String label) {
            if (failCounter > 0) {
                failCounter--;
                throw new RuntimeException("Whoops");
            }
            return super.requestMany(contactAttempts, channel, type, label);
        }
    }

    private UploadingResult uploadInterval(@Nullable LocalTime finishLimit, SentPromoPushesLogger logger) {
        var feature = new FrequencyThrottling();
        feature.setDefaultDailyLimit(3);

        var application = new MobileApplication();
        application.setId(MobileApplication.MARKET_APP);
        application.setMetricaAppId(111);
        application.setFeatures(List.of(feature));

        return uploadInterval(application, finishLimit, logger);
    }

    private UploadingResult uploadInterval(MobileApplication application,
                                           @Nullable LocalTime finishLimit,
                                           SentPromoPushesLogger logger) {
        return uploader.uploadInterval(
                "sending_1",
                "sending",
                application,
                UtmLinks.forPushSending("sending_1"),
                YPath.cypressRoot(),
                finishLimit,
                logger,
                Pair.of(0, Integer.MAX_VALUE)
        );
    }

    private PushMessagesUploader uploader(CommunicationThrottleService throttleService) {
        return new PushMessagesUploader(
                ytClient,
                batchItemFactory,
                appMetricaService,
                timeDeltaService,
                throttleService,
                frequencyToggleService
        );
    }

    private static void assertTskv(Map<String, String> actualMap, String... expectedMapValues) throws JSONException {
        Map<String, String> expectedMap = new HashMap<>();

        Iterator<String> it = Arrays.asList(expectedMapValues).iterator();
        while (it.hasNext()) {
            expectedMap.put(it.next(), it.next());
        }

        for (var e : expectedMap.entrySet()) {
            var key = e.getKey();
            var expectedValue = e.getValue();
            var actualValue = actualMap.get(key);

            if (expectedValue.startsWith("[") || expectedValue.startsWith("{")) {
                JSONAssert.assertEquals(expectedValue, actualValue, false);
            } else {
                Assertions.assertEquals(expectedValue, actualValue);
            }
        }
    }

    private SentPromoPushesLogger logger() {
        return new SentPromoPushesLogger(
                logWriter,
                "market",
                111,
                SENDING_ID,
                createConfig()
        );
    }

    private static PromoEntityConfig<?> createConfig() {
        var config = new PushSendingConf();
        config.setTarget(new TargetAudience(LinkingMode.NONE, SEGMENT_ID));
        config.setTags(List.of("TAG", "SHMAG"));
        config.setCategories(List.of("LUL", "KEK"));
        config.setShopPromoId("shopPromoId");

        return config;
    }

    private static void assertLogEntry(String uuid,
                                       String status,
                                       boolean isControl,
                                       @Nullable String variant,
                                       Map<String, Map<String, String>> entries) {
        var entry = entries.get(uuid);
        assertNotNull(entry, "No log entry for uuid '" + uuid + "'");
        assertAll(
                () -> assertEquals(status, entry.get("status")),
                () -> assertEquals(String.valueOf(isControl), entry.get("control")),
                () -> assertEquals(variant, entry.get("variantId"))
        );
    }

    private Map<String, Map<String, String>> getLogEntries() {
        return logWriter.getRecordsAsList().stream()
                .collect(Collectors.toMap(row -> row.get("uuid"), Function.identity()));
    }
}
