package ru.yandex.market.feed.validation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import Market.DataCamp.API.GeneralizedMessageOuterClass.GeneralizedMessage;
import Market.DataCamp.API.UpdateTask;
import Market.DataCamp.API.UpdateTask.FeedParsingTaskReport;
import Market.DataCamp.DataCampExplanation.Explanation;
import Market.DataCamp.DataCampOfferMeta;
import com.googlecode.protobuf.format.JsonFormat;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.Magics;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.matcher.HttpGetMatcher;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.feed.assortment.AssortmentValidationService;
import ru.yandex.market.core.feed.assortment.model.AssortmentValidationInfo;
import ru.yandex.market.core.feed.assortment.model.AssortmentValidationResult;
import ru.yandex.market.core.feed.assortment.model.FeedProcessingResult;
import ru.yandex.market.core.feed.mds.FeedFileStorage;
import ru.yandex.market.core.feed.mds.StoreInfo;
import ru.yandex.market.core.feed.validation.result.FeedXlsService;
import ru.yandex.market.core.supplier.model.IndexerErrorInfo;
import ru.yandex.market.core.supplier.model.OfferInfo;
import ru.yandex.market.core.upload.FileUploadService;
import ru.yandex.market.core.upload.model.FileInfo;
import ru.yandex.market.core.util.io.Protobuf;
import ru.yandex.market.mbi.core.feed.FeedProcessingStats;
import ru.yandex.market.proto.indexer.v2.BlueAssortment;
import ru.yandex.market.shop.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Тесты для {@link QParserFeedValidationDataProcessor}.
 */
class QParserFeedValidationDataProcessorTest extends FunctionalTest {

    private static final String CHECK_FEED_PBUF_URL_OK = "http://idx.ru/ok.pbuf.sn";
    private static final String CHECK_FEED_PBUF_URL_WARN = "http://idx.ru/warn.pbuf.sn";
    private static final String CHECK_FEED_FILE_PBUF_URL_OK = "http://idx.ru/file.ok.pbuf.sn";
    private static final String CHECK_FEED_FILE_PBUF_URL_WARN = "http://idx.ru/file.warn.pbuf.sn";
    private static final String CHECK_FEED_ZERO_ACCEPTED_PBUF_URL_WARN = "http://idx.ru/zero.accepted.pbuf.sn";
    private static final String CHECK_FEED_ZERO_OFFERS_PBUF_URL_ERROR = "http://idx.ru/error.pbuf.sn";

    private static final String CHECK_FEED_WHITE_PBUF_URL_OK = "http://idx.ru/white.ok.pbuf.sn";
    private static final String CHECK_FEED_WHITE_PBUF_URL_WARN = "http://idx.ru/white.warn.pbuf.sn";

    private static final String CHECK_FEED_PBUF_URL_EXCEPTION = "http://idx.ru/exception.pbuf.sn";
    private static final String CHECK_FEED_UNITED_XLS_PBUF_EXCEPTION_OK = "http://idx.ru/united.exception.xls.pbuf.sn";

    private static final String CHECK_FEED_PBUF_URL_STREAM_OK = "http://idx.ru/ok.stream.pbuf.sn";
    private static final String CHECK_FEED_PBUF_URL_STREAM_WARN = "http://idx.ru/warn.stream.pbuf.sn";
    private static final String CHECK_FEED_PBUF_URL_STREAM_WARN_YML = "http://idx.ru/warn.stream.yml.pbuf.sn";
    private static final String CHECK_FEED_ZERO_OFFERS_PBUF_URL_STREAM_ERROR = "http://idx.ru/error.stream.pbuf.sn";

    /**
     * URL для объединенных результатов валидации в виде протобуфа
     */
    private static final String CHECK_FEED_UNITED_PBUF_URL_OK = "http://idx.ru/united.ok.pbuf.sn";
    private static final String CHECK_FEED_UNITED_PBUF_URL_WARN = "http://idx.ru/united.warn.pbuf.sn";
    private static final String CHECK_FEED_UNITED_XLS_PBUF_URL_OK = "http://idx.ru/united.ok.xls.pbuf.sn";

    /**
     * URL для результатов валидации utility фидов в виде протобуфа
     */
    private static final String CHECK_PRICE_FEED_WITH_TYPE_URL = "http://idx.ru/price.with.type.sn";
    private static final String CHECK_PRICE_FEED_WITHOUT_TYPE_URL = "http://idx.ru/price.without.type.sn";
    private static final String CHECK_STOCK_FEED_WITH_TYPE_URL = "http://idx.ru/stock.with.type.pbuf.sn";
    private static final String CHECK_STOCK_FEED_WITHOUT_TYPE_URL = "http://idx.ru/stock.without.type.pbuf.sn";
    private static final String CHECK_SUPPLIER_FEED_AS_PRICE_FEED = "http://idx.ru/supplier.as.price.type.sn";
    private static final String CHECK_WHITE_YML_FEED_WITH_300 = "http://idx.ru/white.yml.with.300.sn";

    public static final MessageMeta MESSAGE_META = new MessageMeta("testSourceId".getBytes(), 1,
            0, 0, "::1", CompressionCodec.RAW, Collections.emptyMap());

    @Autowired
    private QParserFeedValidationDataProcessor qParserFeedValidationDataProcessor;
    @Autowired
    private HttpClient validationResultHttpClient;
    @Autowired
    private FeedFileStorage feedFileStorage;
    @Autowired
    private FileUploadService uploadService;
    @Autowired
    private AssortmentValidationService assortmentValidationService;
    @Autowired
    private FeedXlsService<IndexerErrorInfo> feedErrorInfoXlsService;
    @Autowired
    private FeedXlsService<OfferInfo> unitedFeedTemplateXlsService;

    @BeforeEach
    void before() throws IOException {
        //протобуфина для урла
        mockHttpClientExecute(CHECK_FEED_PBUF_URL_OK, "SupplierValidationCheckFeedResponse.json");
        mockHttpClientExecute(CHECK_FEED_PBUF_URL_WARN, "SupplierValidationCheckFeedWarn.json");
        //протобуфина для файла
        mockHttpClientExecute(CHECK_FEED_FILE_PBUF_URL_OK, "SupplierValidationCheckFeedFileResponse.json");
        mockHttpClientExecute(CHECK_FEED_FILE_PBUF_URL_WARN, "SupplierValidationCheckFeedFileWarn.json");
        //протобуфина для файла
        mockHttpClientExecute(CHECK_FEED_ZERO_ACCEPTED_PBUF_URL_WARN, "SupplierZeroAcceptedOffersWarn.json");
        mockHttpClientExecute(CHECK_FEED_ZERO_OFFERS_PBUF_URL_ERROR, "SupplierValidationCheckFeedError.json");
        //протобуфина для белых
        mockHttpClientExecute(CHECK_FEED_WHITE_PBUF_URL_OK, "ShopValidationCheckFeedResponse.json");
        mockHttpClientExecute(CHECK_FEED_WHITE_PBUF_URL_WARN, "ShopValidationCheckFeedWarn.json");

        mockHttpClientExecute(CHECK_FEED_UNITED_PBUF_URL_OK, "UnitedValidationCheckFeedResponse.json");
        mockHttpClientExecute(CHECK_FEED_UNITED_PBUF_URL_WARN, "UnitedValidationCheckFeedWarn.json");
        mockHttpClientExecute(CHECK_FEED_UNITED_XLS_PBUF_URL_OK, "UnitedValidationCheckFeedXlsResponse.json");

        mockHttpClientExecute(CHECK_PRICE_FEED_WITH_TYPE_URL, "UnitedValidationPriceCheckFeedWarn.json");
        mockHttpClientExecute(CHECK_PRICE_FEED_WITHOUT_TYPE_URL, "UnitedValidationPriceCheckFeedWarnWithoutType.json");
        mockHttpClientExecute(CHECK_SUPPLIER_FEED_AS_PRICE_FEED, "SupplierValidationCheckSupplierFeedAsPriceFeed.json");
        mockHttpClientExecute(CHECK_STOCK_FEED_WITH_TYPE_URL, "UnitedValidationStockCheckFeedWarn.json");
        mockHttpClientExecute(CHECK_STOCK_FEED_WITHOUT_TYPE_URL, "UnitedValidationStockCheckFeedWarnWithoutType.json");

        // yml с 3хх (MBI-72822)
        mockHttpClientExecute(CHECK_WHITE_YML_FEED_WITH_300, "UnitedValidationYmlFeedWith300.json");

        mockHttpClientExecuteStream(CHECK_FEED_PBUF_URL_STREAM_OK, "SupplierValidationCheckFeedResponse", 2);
        mockHttpClientExecuteStream(CHECK_FEED_PBUF_URL_STREAM_WARN, "SupplierValidationCheckFeedResponseWarn", 5);
        mockHttpClientExecuteStream(CHECK_FEED_PBUF_URL_STREAM_WARN_YML, "SupplierValidationCheckFeedResponseWarnYml", 5);
        mockHttpClientExecuteStream(CHECK_FEED_ZERO_OFFERS_PBUF_URL_STREAM_ERROR,
                "SupplierValidationCheckFeedResponseZeroOffers", 2);

        mockHttpClientThrowException(CHECK_FEED_PBUF_URL_EXCEPTION, new IllegalArgumentException());
        mockHttpClientThrowException(CHECK_FEED_UNITED_XLS_PBUF_EXCEPTION_OK, new IOException());
    }

    private void mockHttpClientExecute(String protobufUrl, String fileName) throws IOException {
        Mockito.doReturn(createHttpOkResponse(getProtobufResponse(fileName)))
                .when(validationResultHttpClient)
                .execute(Mockito.argThat(new HttpGetMatcher(protobufUrl, "GET")));
    }

    private void mockHttpClientExecuteStream(String protobufUrl, String dir, int parts) throws IOException {
        Mockito.doReturn(createHttpOkResponse(getProtobufResponseStream(dir, parts)))
                .when(validationResultHttpClient)
                .execute(Mockito.argThat(new HttpGetMatcher(protobufUrl, "GET")));
    }


    private void mockHttpClientThrowException(String protobufUrl, Exception exception) throws IOException {
        Mockito.doThrow(exception)
                .when(validationResultHttpClient)
                .execute(Mockito.argThat(new HttpGetMatcher(protobufUrl, "GET")));
    }

    private HttpResponse createHttpOkResponse(byte[] body) {
        BasicHttpResponse response = new BasicHttpResponse(new BasicStatusLine(new HttpVersion(1, 1), 200, "OK"));
        response.setEntity(new ByteArrayEntity(body, ContentType.APPLICATION_OCTET_STREAM));
        return response;
    }

    private byte[] getProtobufResponse(String filename) throws IOException {
        String input = StringTestUtil.getString(this.getClass(), filename);
        BlueAssortment.CheckResult.Builder builder = BlueAssortment.CheckResult.newBuilder();
        JsonFormat.merge(input, builder);
        return Protobuf.messagesSnappyLenvalStreamBytes(Magics.MagicConstants.FCHR.name(), builder.build());
    }

    private byte[] getProtobufResponseStream(String filename, int countParts) throws IOException {
        var arrayCheckResult = new BlueAssortment.CheckResult[countParts];
        for (int i = 0; i < countParts; i++) {
            String input = StringTestUtil.getString(this.getClass(), filename + "/part" + (i + 1) + ".json");
            BlueAssortment.CheckResult.Builder builder = BlueAssortment.CheckResult.newBuilder();
            JsonFormat.merge(input, builder);
            arrayCheckResult[i] = builder.build();
        }
        return Protobuf.messagesSnappyLenvalStreamBytes(Magics.MagicConstants.FCHR.name(), arrayCheckResult);
    }

    @DbUnitDataSet(
            before = "supplierValidationDataProcessor.before.csv"
    )
    @Test
    @DisplayName("Проверка чтения сообщений из ЛБ для синих")
    void process_blue_successful() throws IOException {
        mockIdxResult();

        qParserFeedValidationDataProcessor.process(new MessageBatch(
                "testTopic",
                1,
                Arrays.asList(
                        //по урлу. Стоковые и ценовые
                        buildMessageData(generateMessage(1, CHECK_FEED_PBUF_URL_OK, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        buildMessageData(generateMessage(2, CHECK_FEED_PBUF_URL_WARN, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        //Ошибочный код ответа и нет урла
                        buildMessageData(generateMessage(5, null, 3,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        //из файла
                        buildMessageData(generateMessage(6, CHECK_FEED_FILE_PBUF_URL_OK, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        buildMessageData(generateMessage(7, CHECK_FEED_FILE_PBUF_URL_WARN, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        buildMessageData(generateMessage(8, CHECK_FEED_FILE_PBUF_URL_OK, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        buildMessageData(generateMessage(9, CHECK_FEED_ZERO_ACCEPTED_PBUF_URL_WARN, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        buildMessageData(generateMessage(10, CHECK_FEED_ZERO_OFFERS_PBUF_URL_ERROR, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        buildMessageData(generateMessage(19, CHECK_PRICE_FEED_WITHOUT_TYPE_URL, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        buildMessageData(generateMessage(21, CHECK_STOCK_FEED_WITHOUT_TYPE_URL, 0,
                                DataCampOfferMeta.MarketColor.BLUE))
                )
        ));

        assertSupplierValidationInfo(1, FeedProcessingResult.OK, 5, false, null, 0, 0);
        assertSupplierValidationInfo(2, FeedProcessingResult.WARNING, 1, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(5, FeedProcessingResult.ERROR, 0, false, null, 0, 0);
        assertSupplierValidationInfo(6, FeedProcessingResult.OK, 10, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(7, FeedProcessingResult.WARNING, 2, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(8, FeedProcessingResult.OK, 751, true, null, 0, 0);
        assertSupplierValidationInfo(9, FeedProcessingResult.WARNING, 0, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(10, FeedProcessingResult.ERROR, 0, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(19, FeedProcessingResult.WARNING, 2, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(21, FeedProcessingResult.WARNING, 2, true, ".xlsx", 0, 0);
    }

    @DbUnitDataSet(
            before = "supplierValidationDataProcessor.before.csv"
    )
    @Test
    @DisplayName("Проверка чтения сообщений из ЛБ для синих, Стрим")
    void process_stream_blue_successful() throws IOException {
        mockIdxResult();

        qParserFeedValidationDataProcessor.process(new MessageBatch(
                "testTopic",
                1,
                Arrays.asList(
                        buildMessageData(generateMessage(1, CHECK_FEED_PBUF_URL_STREAM_OK, 0,
                                DataCampOfferMeta.MarketColor.BLUE, true)),
                        //Ошибочный код ответа и нет урла
                        buildMessageData(generateMessage(5, null, 3,
                                DataCampOfferMeta.MarketColor.BLUE, true)),
                        buildMessageData(generateMessage(10, CHECK_FEED_ZERO_OFFERS_PBUF_URL_STREAM_ERROR, 0,
                                DataCampOfferMeta.MarketColor.BLUE, true))
                )
        ));

        assertSupplierValidationInfo(1, FeedProcessingResult.OK, 5, false, null, 0, 0);
        assertSupplierValidationInfo(5, FeedProcessingResult.ERROR, 0, false, null, 0, 0);
        assertSupplierValidationInfo(10, FeedProcessingResult.ERROR, 0, true, ".xlsx", 0, 0);
    }

    @Test
    @DisplayName("Проверка чтения через стрим сообщений из ЛБ для синих")
    @DbUnitDataSet(before = "supplierValidationDataProcessor.before.csv")
    void process_stream_blue_check_offer_info() throws IOException {
        mockIdxResult();

        List<OfferInfo> actual = runAndCapture(
                unitedFeedTemplateXlsService,
                List.of(
                        buildMessageData(generateMessage(2, CHECK_FEED_PBUF_URL_STREAM_WARN, 0,
                                DataCampOfferMeta.MarketColor.BLUE, true))
                )
        );

        assertSupplierValidationInfo(2, FeedProcessingResult.WARNING, 1, true, ".xlsx", 0, 0);

        Assertions.assertThat(actual).hasSize(2);
    }


    @Test
    @DisplayName("Проверка чтения через стрим сообщений из ЛБ для синих yml")
    @DbUnitDataSet(before = "supplierValidationDataProcessor.before.csv")
    void process_stream_blue_check_offer_info_yml() throws IOException {
        mockIdxResult();

        List<IndexerErrorInfo> actualYml = runAndCapture(
                feedErrorInfoXlsService,
                List.of(
                        buildMessageData(generateMessage(2, CHECK_FEED_PBUF_URL_STREAM_WARN_YML, 0,
                                DataCampOfferMeta.MarketColor.BLUE, true))
                )
        );
        Assertions.assertThat(actualYml).hasSize(2);
    }

    @Test
    @DisplayName("3хх ошибки без offer_id выводятся в отчет для YML фидов")
    @DbUnitDataSet(before = "testYmlFeedWith300.before.csv")
    void testYmlFeedWith300() throws IOException {
        mockIdxResult();

        List<IndexerErrorInfo> actual = runAndCapture(
                feedErrorInfoXlsService,
                List.of(
                        // warnings без offer_id
                        buildMessageData(generateMessage(1, CHECK_WHITE_YML_FEED_WITH_300, 1,
                                DataCampOfferMeta.MarketColor.WHITE))
                )
        );

        assertSupplierValidationInfo(1, FeedProcessingResult.WARNING, 39, true, ".xlsx", 0, 0);

        List<String> codes = actual.stream()
                .map(IndexerErrorInfo::getVerdictCode)
                .collect(Collectors.toList());
        Assertions.assertThat(codes)
                .containsExactlyInAnyOrder("343", "343", "333");
    }

    private <T> List<T> runAndCapture(FeedXlsService<T> service, List<MessageData> data) {
        List<T> result = new ArrayList<>();
        // вытаскиваем значения из стрима и не пишем excel
        doAnswer(invocation -> {
            Stream<T> resultStream = invocation.getArgument(1);
            resultStream.forEach(result::add);

            Path tempFilePath = Files.createTempFile("QParserFeedValidationDataProcessorTest", ".xlsx");
            Consumer<Path> pathConsumer = invocation.getArgument(2);
            pathConsumer.accept(tempFilePath);

            return null;
        }).when(service).fillTemplate(any(), any(), any());

        qParserFeedValidationDataProcessor.process(new MessageBatch("testTopic", 1, data));
        return result;
    }

    @DbUnitDataSet(
            before = "supplierValidationDataProcessor.before.csv"
    )
    @Test
    @DisplayName("Проверка чтения сообщений из ЛБ для объедининных белых")
    void process_unitedWhite_successful() throws IOException {
        mockIdxResult();

        qParserFeedValidationDataProcessor.process(new MessageBatch(
                "testTopic",
                1,
                Arrays.asList(
                        //по урлу. Стоковые и ценовые
                        buildMessageData(generateMessage(11, 10811, CHECK_FEED_UNITED_XLS_PBUF_URL_OK, 0,
                                DataCampOfferMeta.MarketColor.WHITE)),
                        buildMessageData(generateMessage(12, 10812, CHECK_FEED_UNITED_PBUF_URL_WARN, 0,
                                DataCampOfferMeta.MarketColor.WHITE)),
                        //Ошибочный код ответа и нет урла
                        buildMessageData(generateMessage(15, 10815, null, 3,
                                DataCampOfferMeta.MarketColor.WHITE)),
                        //из файла
                        buildMessageData(generateMessage(16, 10816, CHECK_FEED_UNITED_PBUF_URL_OK, 0,
                                DataCampOfferMeta.MarketColor.WHITE)),
                        buildMessageData(generateMessage(17, 10817, CHECK_FEED_UNITED_PBUF_URL_WARN, 0,
                                DataCampOfferMeta.MarketColor.WHITE)),
                        buildMessageData(generateMessage(18, 10817, CHECK_PRICE_FEED_WITH_TYPE_URL, 0,
                                DataCampOfferMeta.MarketColor.WHITE)),
                        buildMessageData(generateMessage(20, 10817, CHECK_STOCK_FEED_WITH_TYPE_URL, 0,
                                DataCampOfferMeta.MarketColor.WHITE))
                )
        ));

        assertSupplierValidationInfo(11, FeedProcessingResult.OK, 10, false, null, 0, 0);
        assertSupplierValidationInfo(12, FeedProcessingResult.WARNING, 2, true, ".xlsx", 3, 5);
        assertSupplierValidationInfo(15, FeedProcessingResult.ERROR, 0, false, null, 0, 0);
        assertSupplierValidationInfo(16, FeedProcessingResult.OK, 10, false, null, 0, 0);
        assertSupplierValidationInfo(17, FeedProcessingResult.WARNING, 2, true, ".xlsx", 3, 5);
        assertSupplierValidationInfo(18, FeedProcessingResult.WARNING, 2, true, ".xlsm", 0, 0);
        assertSupplierValidationInfo(20, FeedProcessingResult.WARNING, 2, true, ".xlsx", 0, 0);
    }

    @DbUnitDataSet(
            before = {
                    "supplierValidationDataProcessor.before.csv",
                    "validation.blacklist.before.csv"
            },
            after = "validation.blacklist.after.csv"
    )
    @Test
    @DisplayName("Валидация из черного списка сохраняется с ошибкой")
    void process_blacklist() throws IOException {
        mockIdxResult();

        qParserFeedValidationDataProcessor.process(new MessageBatch(
                "testTopic",
                1,
                List.of(
                        buildMessageData(generateMessage(16, 10816, CHECK_FEED_UNITED_PBUF_URL_OK, 0,
                                DataCampOfferMeta.MarketColor.WHITE))
                )
        ));
    }

    private void mockIdxResult() throws IOException {
        Mockito.doReturn(new StoreInfo(530944, "http://mds.url/"))
                .when(feedFileStorage)
                .upload(Mockito.any(), Mockito.anyLong());

        ClassPathResource uploadedResource = new ClassPathResource("supplier/feed/Stock_xls-sku.xls");
        Mockito.doAnswer(invocation -> uploadedResource.getInputStream())
                .when(feedFileStorage)
                .open(Mockito.any());
    }

    @DbUnitDataSet(
            before = "supplierValidationDataProcessor.before.csv",
            after = "QParserFeedValidationDataProcessor/whiteEmpty.after.csv"
    )
    @Test
    @DisplayName("Проверка чтения сообщений из ЛБ для белых при выключенном функционале или неизвестном цвете")
    void process_emptyEnv_nothing() throws IOException {
        mockIdxResult();

        qParserFeedValidationDataProcessor.process(new MessageBatch(
                "testTopic",
                1,
                Arrays.asList(
                        buildMessageData(generateMessage(14, CHECK_FEED_WHITE_PBUF_URL_OK, 0,
                                DataCampOfferMeta.MarketColor.WHITE)),
                        buildMessageData(generateMessage(15, CHECK_FEED_WHITE_PBUF_URL_WARN, 0,
                                DataCampOfferMeta.MarketColor.UNKNOWN_COLOR))
                )
        ));
    }

    @DbUnitDataSet(
            before = "supplierValidationDataProcessor.before.csv",
            after = "validationDataProcessorWithGlobalError.after.csv"
    )
    @Test
    @DisplayName("Проверка на корректность чтения сообщений с глобальными ошибками из ЛБ")
    void process_unitedWithGlobalError_successful() throws IOException {
        mockIdxResult();

        Collection<Explanation> singletonExplanationsSetWithParams = Set.of(
                Explanation.newBuilder()
                        .setCode("551")
                        .setLevel(Explanation.Level.FATAL)
                        .addAllParams(List.of(
                                Explanation.Param.newBuilder()
                                        .setName("attrName")
                                        .setValue("age")
                                        .build()
                        ))
                        .build()
        );
        Collection<Explanation> singletonExplanationsSetWithoutParams = Set.of(
                Explanation.newBuilder()
                        .setCode("531")
                        .setLevel(Explanation.Level.FATAL)
                        .build()
        );

        qParserFeedValidationDataProcessor.process(new MessageBatch(
                "testTopic",
                1,
                Arrays.asList(
                        buildMessageData(generateMessage(2, 0, CHECK_FEED_PBUF_URL_WARN, 0,
                                DataCampOfferMeta.MarketColor.BLUE, singletonExplanationsSetWithoutParams)),
                        buildMessageData(generateMessage(5, 0, null, 3,
                                DataCampOfferMeta.MarketColor.BLUE, singletonExplanationsSetWithParams)),
                        buildMessageData(generateMessage(7, 0, CHECK_FEED_FILE_PBUF_URL_WARN, 3,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        buildMessageData(generateMessage(9, 0, CHECK_FEED_ZERO_ACCEPTED_PBUF_URL_WARN, 0,
                                DataCampOfferMeta.MarketColor.BLUE, singletonExplanationsSetWithoutParams)),
                        buildMessageData(generateMessage(12, 10812, CHECK_FEED_WHITE_PBUF_URL_OK, 3,
                                DataCampOfferMeta.MarketColor.WHITE)),
                        buildMessageData(generateMessage(15, 10815, null, 3,
                                DataCampOfferMeta.MarketColor.WHITE, singletonExplanationsSetWithParams)),
                        buildMessageData(generateMessage(17, 10817, CHECK_FEED_UNITED_PBUF_URL_WARN, 0,
                                DataCampOfferMeta.MarketColor.WHITE, singletonExplanationsSetWithoutParams))
                )
        ));

        assertSupplierValidationInfo(2, FeedProcessingResult.ERROR, 1, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(5, FeedProcessingResult.ERROR, 0, false, null, 0, 0);
        assertSupplierValidationInfo(7, FeedProcessingResult.ERROR, 2, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(9, FeedProcessingResult.ERROR, 0, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(12, FeedProcessingResult.ERROR, 10, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(15, FeedProcessingResult.ERROR, 0, false, null, 0, 0);
        assertSupplierValidationInfo(17, FeedProcessingResult.ERROR, 2, true, ".xlsx", 3, 5);
    }

    @DbUnitDataSet(
            before = {
                    "supplierValidationDataProcessor.before.csv",
                    "unitedCatalogStatus.before.csv"
            }
    )
    @Test
    @DisplayName("Проверка на корректность чтения сообщений из ЛБ при включенном UNITED_CATALOG_STATUS")
    void process_unitedWithCatalogStatus_successful() throws IOException {
        mockIdxResult();

        qParserFeedValidationDataProcessor.process(new MessageBatch(
                "testTopic",
                1,
                Arrays.asList(
                        //по урлу. Стоковые и ценовые
                        buildMessageData(generateMessage(1, CHECK_FEED_PBUF_URL_OK, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        buildMessageData(generateMessage(2, CHECK_FEED_PBUF_URL_WARN, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        //Ошибочный код ответа и нет урла
                        buildMessageData(generateMessage(5, null, 3,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        //из файла
                        buildMessageData(generateMessage(6, CHECK_FEED_FILE_PBUF_URL_OK, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        buildMessageData(generateMessage(7, CHECK_FEED_FILE_PBUF_URL_WARN, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        buildMessageData(generateMessage(8, CHECK_FEED_FILE_PBUF_URL_OK, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        buildMessageData(generateMessage(9, CHECK_FEED_ZERO_ACCEPTED_PBUF_URL_WARN, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        buildMessageData(generateMessage(10, CHECK_FEED_ZERO_OFFERS_PBUF_URL_ERROR, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),

                        //по урлу. Стоковые и ценовые
                        buildMessageData(generateMessage(11, 10811, CHECK_FEED_UNITED_XLS_PBUF_URL_OK, 0,
                                DataCampOfferMeta.MarketColor.WHITE)),
                        buildMessageData(generateMessage(12, 10812, CHECK_FEED_UNITED_PBUF_URL_WARN, 0,
                                DataCampOfferMeta.MarketColor.WHITE)),
                        //Ошибочный код ответа и нет урла
                        buildMessageData(generateMessage(15, 10815, null, 3,
                                DataCampOfferMeta.MarketColor.WHITE)),
                        //из файла
                        buildMessageData(generateMessage(16, 10816, CHECK_FEED_UNITED_PBUF_URL_OK, 0,
                                DataCampOfferMeta.MarketColor.WHITE)),
                        buildMessageData(generateMessage(17, 10817, CHECK_FEED_UNITED_PBUF_URL_WARN, 0,
                                DataCampOfferMeta.MarketColor.WHITE)),
                        buildMessageData(generateMessage(19, CHECK_PRICE_FEED_WITHOUT_TYPE_URL, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        buildMessageData(generateMessage(21, CHECK_STOCK_FEED_WITHOUT_TYPE_URL, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),
                        buildMessageData(generateMessage(18, 10817, CHECK_PRICE_FEED_WITH_TYPE_URL, 0,
                                DataCampOfferMeta.MarketColor.WHITE)),
                        buildMessageData(generateMessage(20, 10817, CHECK_STOCK_FEED_WITH_TYPE_URL, 0,
                                DataCampOfferMeta.MarketColor.WHITE)),
                        buildMessageData(generateMessage(23, 10784, CHECK_SUPPLIER_FEED_AS_PRICE_FEED, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),

                        // ценовой фид по ссылке
                        buildMessageData(generateMessage(22, CHECK_FEED_PBUF_URL_OK, 0,
                                DataCampOfferMeta.MarketColor.BLUE))
                )
        ));

        assertSupplierValidationInfo(1, FeedProcessingResult.OK, 5, false, null, 0, 0);
        assertSupplierValidationInfo(2, FeedProcessingResult.WARNING, 1, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(5, FeedProcessingResult.ERROR, 0, false, null, 0, 0);
        assertSupplierValidationInfo(6, FeedProcessingResult.OK, 10, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(7, FeedProcessingResult.WARNING, 2, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(8, FeedProcessingResult.OK, 751, true, null, 0, 0);
        assertSupplierValidationInfo(9, FeedProcessingResult.WARNING, 0, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(10, FeedProcessingResult.ERROR, 0, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(11, FeedProcessingResult.OK, 10, false, null, 0, 0);
        assertSupplierValidationInfo(12, FeedProcessingResult.WARNING, 2, true, ".xlsx", 3, 5);
        assertSupplierValidationInfo(15, FeedProcessingResult.ERROR, 0, false, null, 0, 0);
        assertSupplierValidationInfo(16, FeedProcessingResult.OK, 10, false, null, 0, 0);
        assertSupplierValidationInfo(17, FeedProcessingResult.WARNING, 2, true, ".xlsx", 3, 5);
        assertSupplierValidationInfo(19, FeedProcessingResult.WARNING, 2, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(21, FeedProcessingResult.WARNING, 2, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(18, FeedProcessingResult.WARNING, 2, true, ".xlsm", 0, 0);
        assertSupplierValidationInfo(20, FeedProcessingResult.WARNING, 2, true, ".xlsx", 0, 0);
        assertSupplierValidationInfo(22, FeedProcessingResult.OK, 5, false, null, 0, 0);
        assertSupplierValidationInfo(23, FeedProcessingResult.OK, 10, true, ".xlsx", 0, 0);
    }

    @DbUnitDataSet(
            before = {
                    "supplierValidationDataProcessor.before.csv",
                    "unitedCatalogStatus.before.csv"
            }
    )
    @Test
    @DisplayName("Проверка на сохранение в БД ошибки, в случае возникновения Exception")
    void process_exception_saveError() throws IOException {
        mockIdxResult();

        qParserFeedValidationDataProcessor.process(new MessageBatch(
                "testTopic",
                1,
                Arrays.asList(
                        //по урлу. Стоковые и ценовые
                        buildMessageData(generateMessage(1, CHECK_FEED_PBUF_URL_EXCEPTION, 0,
                                DataCampOfferMeta.MarketColor.BLUE)),

                        //по урлу. Стоковые и ценовые
                        buildMessageData(generateMessage(11, 10811, CHECK_FEED_UNITED_XLS_PBUF_EXCEPTION_OK, 0,
                                DataCampOfferMeta.MarketColor.WHITE))
                )
        ));

        assertSupplierValidationInfo(1, FeedProcessingResult.ERROR, 0, false, null, 0, 0);
        assertSupplierValidationInfo(11, FeedProcessingResult.ERROR, 0, false, null, 0, 0);
    }

    @Nonnull
    private MessageData buildMessageData(@Nonnull GeneralizedMessage message) {
        return new MessageData(message.toByteArray(), 2, MESSAGE_META);
    }

    @Nonnull
    private GeneralizedMessage generateMessage(int validationId,
                                               @Nullable String url,
                                               int returnCode,
                                               DataCampOfferMeta.MarketColor color) {
        return generateMessage(validationId, 0, url, returnCode, color, Collections.emptyList());
    }

    @Nonnull
    private GeneralizedMessage generateMessage(int validationId,
                                               @Nullable String url,
                                               int returnCode,
                                               DataCampOfferMeta.MarketColor color,
                                               boolean stream) {
        return generateMessage(validationId, 0, url, returnCode, color, Collections.emptyList(), stream);
    }

    @Nonnull
    private GeneralizedMessage generateMessage(int validationId,
                                               int shopId,
                                               @Nullable String url,
                                               int returnCode,
                                               DataCampOfferMeta.MarketColor color) {
        return generateMessage(validationId, shopId, url, returnCode, color, Collections.emptyList());
    }

    @Nonnull
    private GeneralizedMessage generateMessage(int validationId,
                                               int shopId,
                                               @Nullable String url,
                                               int returnCode,
                                               DataCampOfferMeta.MarketColor color,
                                               Collection<Explanation> explanations) {
        return generateMessage(validationId, shopId, url, returnCode, color, explanations, false);
    }

    @Nonnull
    private GeneralizedMessage generateMessage(int validationId,
                                               int shopId,
                                               @Nullable String url,
                                               int returnCode,
                                               DataCampOfferMeta.MarketColor color,
                                               Collection<Explanation> explanations,
                                               boolean stream) {
        return GeneralizedMessage.newBuilder()
                .setFeedParsingTaskReport(
                        generateReport(validationId, shopId, url, returnCode, color, explanations, stream))
                .build();
    }

    @Nonnull
    private FeedParsingTaskReport generateReport(int validationId, int shopId, @Nullable String url,
                                                 int returnCode, DataCampOfferMeta.MarketColor color,
                                                 Collection<Explanation> explanations,
                                                 boolean stream) {
        FeedParsingTaskReport.Builder builder = FeedParsingTaskReport.newBuilder();

        if (url != null) {
            builder.setUrlToParserOutput(url);
        }

        for (Explanation explanation : explanations) {
            builder.addFeedParsingErrorMessages(explanation);
        }

        return builder
                .setFeedParsingTask(UpdateTask.FeedParsingTask.newBuilder()
                        .setShopId(shopId)
                        .setCheckFeedTaskIdentifiers(UpdateTask.CheckFeedTaskIdentifiers.newBuilder()
                                .setValidationId(validationId)
                                .build())
                        .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(color)
                                .build())
                        .setStreamingCheckResult(stream)
                        .build())
                .setParserReturnCode(returnCode)
                .build();
    }

    private void assertSupplierValidationInfo(int validationId, FeedProcessingResult result,
                                              int acceptedOffers, boolean isPresentResultFile,
                                              @Nullable String fileExtension, long priceIncreaseCount,
                                              long priceDecreaseCount) {
        var oValidationInfo = assortmentValidationService.getValidationInfo(validationId);

        //Статусы
        assertTrue(oValidationInfo
                .map(AssortmentValidationInfo::status)
                .filter(status -> status == result)
                .isPresent());

        //Результаты валидации
        assertTrue(oValidationInfo
                .flatMap(AssortmentValidationInfo::result)
                .isPresent());

        //Файл для скачивани информации по результатам
        assertEquals(isPresentResultFile, oValidationInfo
                .flatMap(AssortmentValidationInfo::result)
                .map(AssortmentValidationResult::enrichedUploadId)
                .filter(OptionalLong::isPresent)
                .isPresent());

        if (fileExtension != null) {
            //noinspection OptionalGetWithoutIsPresent
            FileInfo file = uploadService.getFile(oValidationInfo
                    .flatMap(AssortmentValidationInfo::result)
                    .map(AssortmentValidationResult::enrichedUploadId)
                    .map(OptionalLong::getAsLong)
                    .get());

            assertNotNull(file);
            assertEquals(fileExtension, file.name().substring(file.name().lastIndexOf('.')));
        }

        //Кол-во хороших офферов
        assertTrue(oValidationInfo
                .flatMap(AssortmentValidationInfo::result)
                .map(AssortmentValidationResult::stats)
                .map(FeedProcessingStats::acceptedOffers)
                .filter(offers -> offers == acceptedOffers)
                .isPresent());

        assertTrue(oValidationInfo
                .flatMap(AssortmentValidationInfo::result)
                .map(AssortmentValidationResult::stats)
                .map(FeedProcessingStats::priceIncreaseHitThresholdCount)
                .filter(increase -> increase == priceIncreaseCount)
                .isPresent());

        assertTrue(oValidationInfo
                .flatMap(AssortmentValidationInfo::result)
                .map(AssortmentValidationResult::stats)
                .map(FeedProcessingStats::priceDecreaseHitThresholdCount)
                .filter(decrease -> decrease == priceDecreaseCount)
                .isPresent());
    }
}
