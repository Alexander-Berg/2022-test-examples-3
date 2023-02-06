package ru.yandex.market.samovar;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Market.DataCamp.API.UpdateTask;
import Market.DataCamp.DataCampOfferMeta;
import NKwYT.Queries;
import NZoraPb.Statuscodes;
import com.google.protobuf.Int64Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.mockito.MemCachedClientFactoryMock;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.datacamp.feed.DataCampFeedHistoryService;
import ru.yandex.market.core.feed.assortment.AssortmentValidationService;
import ru.yandex.market.core.feed.assortment.model.FeedProcessingResult;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.core.feed.validation.model.FeedValidationLogbrokerEvent;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.samovar.SamovarFeedDownloadErrorsService;
import ru.yandex.market.core.samovar.model.SamovarFeedDownloadError;
import ru.yandex.market.core.supplier.service.IndexerFeedIdAndWarehouseId;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarUtils;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.shop.FunctionalTest;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.common.test.util.ProtoTestUtil.getProtoMessageByJson;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataSet(before = "SamovarResultDataProcessorTest.before.csv")
class SamovarResultDataProcessorTest extends FunctionalTest {
    private static final int NOT_EXISTED_CODE = 999999;

    @Autowired
    @Qualifier("samovarLogbrokerDataProcessor")
    private SamovarResultDataProcessor samovarResultDataProcessor;
    @Autowired
    private DataCampFeedHistoryService dataCampFeedHistoryService;
    @Autowired
    private SamovarFeedDownloadErrorsService samovarFeedDownloadErrorsService;
    @Autowired
    @Qualifier("qParserLogBrokerService")
    private LogbrokerService qParserLogBrokerService;
    @Autowired
    private ParamService paramService;
    @Autowired
    private AssortmentValidationService assortmentValidationService;
    @Autowired
    private MemCachedClientFactoryMock memCachedClientFactoryMock;
    @Autowired
    private EnvironmentService environmentService;
    @Value("${market.mbi.samovar.partUrlTemplate}")
    private String partUrlTemplate;

    private static Stream<Arguments> testFFprogram() {
        return Stream.of(
                Arguments.of(false, UpdateTask.FFProgram.FF_PROGRAM_REAL),
                Arguments.of(true, UpdateTask.FFProgram.FF_PROGRAM_NO)
        );
    }

    private static Stream<Arguments> provideHttpCodes() {
        return Stream.of(
                Arguments.of(200, true, true),                           //на 2xx чистим базу и отправляем сообщение
                Arguments.of(302, false, true),                          //на 3xx чистим базу, но не отправляем
                // сообщение
                Arguments.of(404, false, false),                         //на 4xx повышаем счетчик ошибок и не
                // отправляем сообщение
                Arguments.of(501, false, false)                         //на 5xx повышаем счетчик ошибок и не
                // отправляем сообщение
        );
    }

    private static Stream<Arguments> provideZoraCodes() {
        return Stream.of(
                Arguments.of(Statuscodes.EZoraStatus.ZS_OK_VALUE, Statuscodes.EFetchStatus.FS_MESSAGE_EOF_VALUE,
                        true, false),
                Arguments.of(Statuscodes.EZoraStatus.ZS_OK_VALUE, Statuscodes.EFetchStatus.FS_FETCHER_MB_ERROR_VALUE,
                        false, true),
                // проверяем что нормально обрабатывается фетч код, неизвестный для нас
                Arguments.of(Statuscodes.EZoraStatus.ZS_OK_VALUE, NOT_EXISTED_CODE, false, true),
                Arguments.of(Statuscodes.EZoraStatus.ZS_QUOTA_INTERNAL_ERROR_VALUE,
                        Statuscodes.EFetchStatus.FS_OK_VALUE, false, true),
                Arguments.of(Statuscodes.EZoraStatus.ZS_TIMED_OUT_VALUE,
                        Statuscodes.EFetchStatus.FS_OK_VALUE, false, true),
                // проверяем что нормально обрабатывается фетч код, неизвестный для нас
                Arguments.of(NOT_EXISTED_CODE, Statuscodes.EFetchStatus.FS_OK_VALUE, false, true)
        );
    }

    private Map<String, Object> getMemCache() {
        return memCachedClientFactoryMock.getCaches().values().stream().findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Парсинг сообщения из Самовара проходит успешно. Вытаскиваем зазипованный контекст")
    void messageParsing() throws IOException {
        var mock2 = Mockito.mock(SamovarValidationFeedProcessor.class);
        var processor = new SamovarResultDataProcessor(
                new SamovarMessageFactory(environmentService, partUrlTemplate),
                a -> true,
                a -> true,
                mock2,
                samovarFeedDownloadErrorsService,
                assortmentValidationService,
                3);

        // Готовим сжатое событие, которое будет присылать самовар.
        // Это будет зазипованный протобуф.
        var feedInfo = new FeedInfoBuilder().setFeedId(1001).setShopId(1).build();
        var original1 = new ItemBuilder().addFeedInfo(feedInfo).build();
        var validationFeedInfo = new ValidationFeedInfoBuilder().addValidationFeedInfo(1L).build();
        var original2 = new ItemBuilder().addValidationFeedInfo(validationFeedInfo).build();
        var batch = new MessageBatchBuilder().addItem(original1).addItem(original2).build();

        // Вызываем парсер сообщения
        processor.process(batch);

        var validationCaptor = ArgumentCaptor.forClass(SamovarValidationFeedInfo.class);
        var context = SamovarContextOuterClass.SamovarContext.parseFrom(original2.getContext());
        verify(mock2).accept(validationCaptor.capture());
        var expectedValidationFeedIds = context.getValidationFeedsList().stream()
                .map(SamovarContextOuterClass.ValidationFeedInfo::getValidationId)
                .collect(Collectors.toSet());
        var actualValidationFeedIds = validationCaptor.getAllValues().stream()
                .map(SamovarValidationFeedInfo::getValidationId)
                .collect(Collectors.toSet());
        assertThat(actualValidationFeedIds).isEqualTo(expectedValidationFeedIds);
    }

    @ParameterizedTest
    @MethodSource("testFFprogram")
    void testValidationFeedSending(boolean isDropship, UpdateTask.FFProgram expectedProgram) {
        paramService.setParam(new BooleanParamValue(ParamType.DROPSHIP_AVAILABLE, 2, isDropship), 1L);

        var validationFeedInfo = new ValidationFeedInfoBuilder()
                .addValidationFeedInfo(1L)
                .build();
        var original = new ItemBuilder()
                .addValidationFeedInfo(validationFeedInfo)
                .setNumberOfParts(3)
                .setMdsKeys("123/asdf|456/q|789/zxcvb")
                .build();
        var batch = new MessageBatchBuilder().addItem(original).build();

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch);

        var captor = ArgumentCaptor.forClass(FeedValidationLogbrokerEvent.class);
        verify(qParserLogBrokerService).publishEvent(captor.capture());
        var event = captor.getAllValues();
        assertThat(event).hasSize(1);
        var message = event.get(0).getPayload();
        UpdateTask.FeedParsingTask.Builder feedParsingTaskBuilder = UpdateTask.FeedParsingTask.newBuilder()
                .setShopId(2)
                .setIsCheckTask(true)
                .setBusinessId(100)
                .setFeedurl("http://storage-int.mds.yandex.net/get-turbo-commodity-feed/123/asdf")
                .addAllPartUrls(List.of(
                        "http://storage-int.mds.yandex.net/get-turbo-commodity-feed/123/asdf",
                        "http://storage-int.mds.yandex.net/get-turbo-commodity-feed/456/q",
                        "http://storage-int.mds.yandex.net/get-turbo-commodity-feed/789/zxcvb"
                ))
                .setType(UpdateTask.FeedClass.FEED_CLASS_COMPLETE)
                .setShopsDatParameters(
                        UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.BLUE)
                                .setIsSiteMarket(true)
                                .setVat(2)
                                .setIgnoreStocks(false)
                                .setIsUpload(false)
                                .setClickAndCollect(false)
                                .setDirectShipping(false)
                                .setIsDiscountsEnabled(true)
                                .setEnableAutoDiscounts(true)
                                .setFfProgram(expectedProgram)
                                .setLocalRegionTzOffset(10800)
                                .setCpa(UpdateTask.ProgramStatus.PROGRAM_STATUS_REAL)
                                .build())
                .setCheckFeedTaskParameters(
                        UpdateTask.CheckFeedTaskParameters
                                .newBuilder()
                                .setRequiredDimensions(isDropship)
                                .setFormat("pbsn")
                )
                .setTimestamp(message.getFeedParsingTask().getTimestamp())
                .setCheckFeedTaskIdentifiers(
                        UpdateTask.CheckFeedTaskIdentifiers
                                .newBuilder()
                                .setValidationId(1)
                                .build())
                .setIsDbs(false);
        if (isDropship) {
            feedParsingTaskBuilder.putPartnerWarehousesMapping("10", UpdateTask.PartnerWarehouseInfo.newBuilder()
                    .setWarehouseId(10)
                    .setFeedId(1002)
                    .build());
        }
        assertThat(message.getFeedParsingTask()).isEqualTo(feedParsingTaskBuilder.build());
    }

    @DisplayName("Проверка на то, что настройка отправки белых фидов в qParser выключена")
    @Test
    void process_whiteValidationFeedWithoutEnv_correct() {
        var validationFeedInfo = new ValidationFeedInfoBuilder()
                .addValidationFeedInfo(3L)
                .setCampaignType(CampaignType.SHOP)
                .build();
        var original = new ItemBuilder()
                .addValidationFeedInfo(validationFeedInfo)
                .setNumberOfParts(3)
                .setMdsKeys("123/asdf|456/q|789/zxcvb")
                .build();
        var batch = new MessageBatchBuilder().addItem(original).build();

        samovarResultDataProcessor.process(batch);

        verify(qParserLogBrokerService, Mockito.never())
                .publishEvent(any());
    }

    @DisplayName("Проверка отправки белых фидов в qParser")
    @Test
    void process_shopValidationFeed_correct() {
        processShopValidationFeed(42L, 1002L);
    }

    void processShopValidationFeed(long validationId, long partnerId) {
        var validationFeedInfo = new ValidationFeedInfoBuilder()
                .addValidationFeedInfo(validationId)
                .setCampaignType(CampaignType.SHOP)
                .setPartnerId(partnerId)
                .build();
        var original = new ItemBuilder()
                .addValidationFeedInfo(validationFeedInfo)
                .setNumberOfParts(3)
                .setMdsKeys("123/asdf|456/q|789/zxcvb")
                .build();
        var batch = new MessageBatchBuilder()
                .addItem(original)
                .build();

        samovarResultDataProcessor.process(batch);

        var captor = ArgumentCaptor.forClass(FeedValidationLogbrokerEvent.class);
        verify(qParserLogBrokerService).publishEvent(captor.capture());

        var event = captor.getAllValues();

        assertThat(event).hasSize(1);

        var feedParsingTask = getProtoMessageByJson(UpdateTask.FeedParsingTask.class,
                "SamovarResultDataProcessor/proto/shop." + validationId + ".json", getClass());

        ProtoTestUtil.assertThat(event.get(0).getPayload().getFeedParsingTask())
                .ignoringFields("timestamp_")
                .isEqualTo(feedParsingTask);
    }

    @Test
    void testMessageWasProcessedByFP() {
        var feedInfo = new FeedInfoBuilder()
                .setFeedId(1001)
                .setShopId(1)
                .setBusinessId(134L)
                .setCampaignType(CampaignType.SHOP)
                .setProcessedByFP(true)
                .build();
        var original = new ItemBuilder()
                .addFeedInfo(feedInfo)
                .build();
        var batch = new MessageBatchBuilder().addItem(original).build();

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch);
        verifyNoMoreInteractions(qParserLogBrokerService);
    }

    @DisplayName("Получение информации из самовара по белым с неверным url.")
    @Test
    @DbUnitDataSet(
            before = "SamovarResultDataProcessor.wrongUrl.before.csv",
            after = "SamovarResultDataProcessor.wrongUrl.before.csv"
    )
    void process_wrongFeedUrl_zeroInteractions() {
        var feedInfo = new FeedInfoBuilder()
                .setFeedId(1001)
                .setShopId(1)
                .setUrl("http://updatedUrl.ru")
                .setBusinessId(134L)
                .setCampaignType(CampaignType.SHOP)
                .build();
        var original = new ItemBuilder()
                .addFeedInfo(feedInfo)
                .build();
        var batch = new MessageBatchBuilder().addItem(original).build();

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch);
        verifyNoMoreInteractions(qParserLogBrokerService);
    }

    @Test
    void testAlienMessage() {
        var feedInfo1 = new FeedInfoBuilder()
                .setFeedId(1200)
                .setShopId(200)
                .setCampaignType(CampaignType.SUPPLIER)// запрещенный тип партнера из другого окружения
                .build();
        var feedInfo2 = new FeedInfoBuilder()
                .setFeedId(1100)
                .setShopId(100)
                .setCampaignType(CampaignType.SHOP)
                .build();

        var batch = new MessageBatchBuilder()
                .addItem(new ItemBuilder()
                        .addFeedInfo(feedInfo1)
                        .setEnvironment(EnvironmentType.PRODUCTION)
                        .build())
                .addItem(new ItemBuilder()
                        .addFeedInfo(feedInfo2)
                        .setEnvironment(EnvironmentType.TESTING) // запрещенное окружение
                        .build())
                .build();

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch);
        verifyNoMoreInteractions(qParserLogBrokerService);

        assertThat(dataCampFeedHistoryService.getPushInfos(100L, List.of(1100L))).isEmpty();
        assertThat(dataCampFeedHistoryService.getPushInfos(200, List.of(1200L))).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideHttpCodes")
    @DbUnitDataSet(before = "SamovarPushProcessorTest.httpCodeProcessing.before.csv")
    void httpCodeProcessingTest(int code, boolean send, boolean clear) {
        long feedId = 1002;
        long shopId = 2;
        var feedInfo = new FeedInfoBuilder()
                .setFeedId(feedId)
                .setShopId(shopId)
                .setCampaignType(CampaignType.SUPPLIER)
                .build();
        var original1 = new ItemBuilder()
                .addFeedInfo(feedInfo)
                .setNumberOfParts(3)
                .setMdsKeys("123/asdf|456/q|789/zxcvb")
                .setHttpCode(code)
                .build();
        var validationFeedInfo = new ValidationFeedInfoBuilder()
                .addValidationFeedInfo(1L)
                .build();
        var original2 = new ItemBuilder()
                .addValidationFeedInfo(validationFeedInfo)
                .setNumberOfParts(3)
                .setMdsKeys("123/asdf|456/q|789/zxcvb")
                .setHttpCode(code)
                .build();
        var batch = new MessageBatchBuilder().addItem(original1).addItem(original2).build();

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch);

        if (send) {
            verify(qParserLogBrokerService).publishEvent(any());
        } else {
            verifyNoMoreInteractions(qParserLogBrokerService);
        }

        SamovarFeedDownloadError samovarFeedDownloadError = samovarFeedDownloadErrorsService.get(feedId).get();
        long expectedExternalErrorsCount = clear ? 0 : 5;
        long expectedInternalErrorsCount = clear ? 0 : 2;
        var expectedStatus = send ? FeedProcessingResult.PROCESSING : FeedProcessingResult.ERROR;
        var actualStatus = assortmentValidationService.getValidationInfo(1L).get().status();
        assertThat(actualStatus).isEqualTo(expectedStatus);
        assertThat(samovarFeedDownloadError.getExternalErrorCount()).isEqualTo(expectedExternalErrorsCount);
        assertThat(samovarFeedDownloadError.getInternalErrorCount()).isEqualTo(expectedInternalErrorsCount);
    }

    @ParameterizedTest
    @MethodSource("provideZoraCodes")
    @DbUnitDataSet(before = "SamovarPushProcessorTest.httpCodeProcessing.before.csv",
            after = "SamovarPushProcessorTest.httpCodeProcessing.after.csv")
    void zoraCodeProcessingTest(int zoraCode, int fetchCode, boolean incrementExternal, boolean incrementInternal) {
        long feedId = 1002;
        long shopId = 2;
        var feedInfo = new FeedInfoBuilder()
                .setFeedId(feedId)
                .setShopId(shopId)
                .setCampaignType(CampaignType.SUPPLIER)
                .build();
        var original1 = new ItemBuilder()
                .addFeedInfo(feedInfo)
                .setNumberOfParts(3)
                .setMdsKeys("123/asdf|456/q|789/zxcvb")
                .setZoraCode(zoraCode)
                .setFetchCode(fetchCode)
                .build();
        var validationFeedInfo = new ValidationFeedInfoBuilder()
                .addValidationFeedInfo(1L)
                .build();
        var original2 = new ItemBuilder()
                .addValidationFeedInfo(validationFeedInfo)
                .setNumberOfParts(3)
                .setMdsKeys("123/asdf|456/q|789/zxcvb")
                .setZoraCode(zoraCode)
                .setFetchCode(fetchCode)
                .build();
        var batch = new MessageBatchBuilder().addItem(original1).addItem(original2).build();

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch);

        verifyNoMoreInteractions(qParserLogBrokerService);

        SamovarFeedDownloadError samovarFeedDownloadError = samovarFeedDownloadErrorsService.get(feedId).get();
        long expectedExternalErrorsCount = incrementExternal ? 5 : 4;
        long expectedInternalErrorsCount = incrementInternal ? 3 : 2;
        assertThat(samovarFeedDownloadError.getExternalErrorCount()).isEqualTo(expectedExternalErrorsCount);
        assertThat(samovarFeedDownloadError.getInternalErrorCount()).isEqualTo(expectedInternalErrorsCount);
    }

    @Test
    @DisplayName("От Самовара пришло сообщение без mds ключей на скаченный файл. Чекфид")
    @DbUnitDataSet(
            after = "SamovarFeedUrlNotExist.after.csv"
    )
    void testFeedUrlNotExist() {
        var validationFeedInfo = new ValidationFeedInfoBuilder()
                .addValidationFeedInfo(1L)
                .build();
        var original = new ItemBuilder()
                .addValidationFeedInfo(validationFeedInfo)
                .setNumberOfParts(0)
                .setZoraCode(1)
                .setFetchCode(0)
                .setMdsKeys(null)
                .build();
        var batch = new MessageBatchBuilder().addItem(original).build();

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch);

        verifyNoMoreInteractions(qParserLogBrokerService);
    }

    @Test
    @DisplayName("От Самовара пришло сообщение без mds ключей на скаченный файл. Парсинг")
    @DbUnitDataSet(
            before = "SamovarResultDataProcessorTest.supplier_uc.before.csv",
            after = "testFeedUrlNotExistForParsingTasks.after.csv"
    )
    void testFeedUrlNotExistForParsingTasks() {
        long feedId = 1002;
        long shopId = 2;
        SamovarContextOuterClass.FeedInfo feedInfo = new FeedInfoBuilder()
                .setUrl("http://ya.ru/utility_feed")
                .setFeedId(feedId)
                .setShopId(shopId)
                .setCampaignType(CampaignType.SUPPLIER)
                .setFeedType(FeedType.PRICES)
                .build();
        Queries.TMarketFeedsItem original = new ItemBuilder()
                .setUrl("http://ya.ru/utility_feed")
                .addFeedInfo(feedInfo)
                .setNumberOfParts(0)
                .setMdsKeys(null)
                .setZoraCode(0)
                .setFetchCode(1)
                .build();

        var batch = new MessageBatchBuilder().addItem(original).build();

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch);

        verifyNoMoreInteractions(qParserLogBrokerService);
    }

    static class ValidationFeedInfoBuilder {
        long validationFeedId;
        CampaignType campaignType;
        Long partnerId;

        public ValidationFeedInfoBuilder setCampaignType(CampaignType campaignType) {
            this.campaignType = campaignType;
            return this;
        }

        public ValidationFeedInfoBuilder addValidationFeedInfo(long validationFeedId) {
            this.validationFeedId = validationFeedId;
            return this;
        }

        public ValidationFeedInfoBuilder setPartnerId(Long partnerId) {
            this.partnerId = partnerId;
            return this;
        }

        public SamovarContextOuterClass.ValidationFeedInfo build() {
            var builder = SamovarContextOuterClass.ValidationFeedInfo.newBuilder();

            if (campaignType != null) {
                builder.setCampaignType(campaignType.getId());
            }
            if (partnerId != null) {
                builder.setPartnerId(partnerId);
            }

            return builder
                    .setValidationId(validationFeedId)
                    .build();
        }
    }

    static class FeedInfoBuilder {
        CampaignType campaignType = CampaignType.SUPPLIER;
        long shopId;
        long businessId;
        long feedId;
        String url = "http://ya.ru";
        List<IndexerFeedIdAndWarehouseId> warehouses = new ArrayList<>();
        FeedType feedType;
        boolean processedByFP = false;

        public FeedInfoBuilder setCampaignType(CampaignType campaignType) {
            this.campaignType = campaignType;
            return this;
        }

        public FeedInfoBuilder setShopId(long shopId) {
            this.shopId = shopId;
            return this;
        }

        public FeedInfoBuilder setFeedId(long feedId) {
            this.feedId = feedId;
            return this;
        }

        public FeedInfoBuilder setUrl(String url) {
            this.url = url;
            return this;
        }

        public FeedInfoBuilder addWarehouse(IndexerFeedIdAndWarehouseId warehouse) {
            warehouses.add(warehouse);
            return this;
        }

        public FeedInfoBuilder setBusinessId(long businessId) {
            this.businessId = businessId;
            return this;
        }

        public FeedInfoBuilder setFeedType(FeedType feedType) {
            this.feedType = feedType;
            return this;
        }

        public FeedInfoBuilder setProcessedByFP(boolean processedByFP) {
            this.processedByFP = processedByFP;
            return this;
        }

        SamovarContextOuterClass.FeedInfo build() {
            return SamovarContextOuterClass.FeedInfo.newBuilder()
                    .setUrl(url)
                    .setFeedId(feedId)
                    .setShopId(shopId)
                    .setBusinessId(businessId)
                    .setCampaignType(campaignType.getId())
                    .setFeedType(SamovarUtils.toFeedType(feedType == null
                            ? CampaignType.SUPPLIER == campaignType ? FeedType.PRICES : FeedType.ASSORTMENT
                            : feedType))
                    .setProcessedInFeedProcessor(processedByFP)
                    .addAllWarehouses(warehouses.stream()
                            .map(wh -> SamovarContextOuterClass.FeedInfo
                                    .WarehouseInfo.newBuilder()
                                    .setFeedId(wh.feedId())
                                    .setWarehouseId(wh.warehouseId())
                                    .build())
                            .collect(Collectors.toList())).build();
        }
    }

    static class ItemBuilder {
        Int64Value forceRefresh;
        EnvironmentType environment = EnvironmentType.DEVELOPMENT;
        int numberOfParts = 1;
        String mdsKeys = "123/asdf|";
        String url = "http://ya.ru";
        int httpCode = 200;
        int zoraCode = Statuscodes.EZoraStatus.ZS_OK_VALUE;
        int fetchCode = Statuscodes.EFetchStatus.FS_OK_VALUE;
        long lastAccess = Instant.now().getEpochSecond();
        int crc32;

        List<SamovarContextOuterClass.FeedInfo> feedInfos = new ArrayList<>();
        List<SamovarContextOuterClass.ValidationFeedInfo> validationFeedInfos = new ArrayList<>();


        public ItemBuilder setEnvironment(EnvironmentType environment) {
            this.environment = environment;
            return this;
        }

        public ItemBuilder setNumberOfParts(int numberOfParts) {
            this.numberOfParts = numberOfParts;
            return this;
        }

        public ItemBuilder setUrl(String url) {
            this.url = url;
            return this;
        }

        public ItemBuilder setHttpCode(int httpCode) {
            this.httpCode = httpCode;
            return this;
        }

        public ItemBuilder setZoraCode(int zoraCode) {
            this.zoraCode = zoraCode;
            return this;
        }

        public ItemBuilder setFetchCode(int fetchCode) {
            this.fetchCode = fetchCode;
            return this;
        }

        public ItemBuilder setMdsKeys(String mdsKeys) {
            this.mdsKeys = mdsKeys;
            return this;
        }

        public ItemBuilder addFeedInfo(SamovarContextOuterClass.FeedInfo feedInfo) {
            feedInfos.add(feedInfo);
            return this;
        }

        public ItemBuilder addValidationFeedInfo(SamovarContextOuterClass.ValidationFeedInfo validationFeedInfo) {
            validationFeedInfos.add(validationFeedInfo);
            return this;
        }

        public ItemBuilder setLastAccess(long lastAccess) {
            this.lastAccess = lastAccess;
            return this;
        }

        public ItemBuilder setForceRefresh(long forceRefresh) {
            this.forceRefresh = Int64Value.of(forceRefresh);
            return this;
        }

        public ItemBuilder setCrc32(int crc32) {
            this.crc32 = crc32;
            return this;
        }

        Queries.TMarketFeedsItem build() {
            var contextBuilder = SamovarContextOuterClass.SamovarContext.newBuilder()
                    .setEnvironment(environment.getValue())
                    .addAllFeeds(feedInfos)
                    .addAllValidationFeeds(validationFeedInfos);
            if (forceRefresh != null) {
                contextBuilder.setForceRefreshStart(forceRefresh);
            }

            var context = contextBuilder.build();

            var originalBld = Queries.TMarketFeedsItem.newBuilder()
                    .setUrl(url)
                    .setHttpCode(httpCode)
                    .setZoraStatus(zoraCode)
                    .setFetchStatus(fetchCode)
                    .setLastAccess(lastAccess)
                    .setNumberOfParts(numberOfParts)
                    .setCrc32(crc32);
            if (mdsKeys != null) {
                originalBld.setMdsKeys(mdsKeys);
            }
            return originalBld.setContext(context.toByteString()).build();
        }
    }

    static class MessageBatchBuilder {

        MessageMeta meta = new MessageMeta("test".getBytes(), 0, 0, 0, "::1", CompressionCodec.RAW,
                Collections.emptyMap());

        private final List<Queries.TMarketFeedsItem> items = new ArrayList<>();

        public MessageBatchBuilder addItem(Queries.TMarketFeedsItem item) {
            items.add(item);
            return this;
        }

        public MessageBatchBuilder setMeta(MessageMeta meta) {
            this.meta = meta;
            return this;
        }

        MessageBatch build() {
            return items.stream()
                    .map(item -> new MessageData(IOUtils.zip(item.toByteArray()), 0, meta))
                    .collect(Collectors.collectingAndThen(
                            Collectors.toList(),
                            msgs -> new MessageBatch("topic", 1, msgs)
                    ));
        }
    }
}
