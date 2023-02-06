package ru.yandex.market.ff4shops.lgw;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferStockInfo;
import com.google.protobuf.Timestamp;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.environment.EnvironmentService;
import ru.yandex.market.ff4shops.partner.PartnerFFLinkService;
import ru.yandex.market.ff4shops.repository.PartnerFulfillmentRepository;
import ru.yandex.market.ff4shops.stocks.service.StocksWarehouseGroupService;
import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.common.request.RequestState;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;
import ru.yandex.market.logistic.api.model.fulfillment.request.PushStocksRequest;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.monlib.metrics.primitives.Counter;
import ru.yandex.monlib.metrics.primitives.Histogram;
import ru.yandex.monlib.metrics.registry.MetricId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.web.solomon.pull.SolomonUtils.getMetricRegistry;

@DbUnitDataSet(before = "environment.before.csv")
public class StocksLogbrokerDataProcessorTest extends FunctionalTest {
    private static final int DELETED_WRH_ID = 999;
    private static final DateTime STOCKS_TIME = new DateTime("2020-07-10T00:00:00");
    private static final Set<Long> RESET_STOCKS_TIME_SUPPLIER = Set.of(1L, 2L);

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private PartnerFFLinkService partnerFFLinkService;

    @Autowired
    private PartnerFulfillmentRepository partnerFulfillmentRepository;

    @Autowired
    private StocksWarehouseGroupService stocksWarehouseGroupService;

    private StocksLogbrokerDataProcessor dataProcessor;

    private final HttpTemplate httpTemplate = mock(HttpTemplate.class);

    private final RetryListener listener = mock(RetryListener.class);

    private final LMSClient lmsClient = mock(LMSClient.class);

    private final HttpTemplateByPartnerIdFactory httpTemplateByPartnerIdFactory =
        mock(HttpTemplateByPartnerIdFactory.class);

    private RetryTemplate logbrokerRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        retryTemplate.setBackOffPolicy(backOffPolicy);
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(5);
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setThrowLastExceptionOnExhausted(true);
        return retryTemplate;
    }

    @BeforeEach
    void init() {
        when(listener.open(any(), any())).thenReturn(true);
        var retryTemplate = logbrokerRetryTemplate();
        retryTemplate.registerListener(listener);
        LogisticApiRequestsClientService logisticApiRequestsClientService =
            new LogisticApiRequestsClientService(5000, 7,
                lmsClient,
                partnerFulfillmentRepository,
                retryTemplate,
                httpTemplateByPartnerIdFactory
            );
        doReturn(httpTemplate)
            .when(httpTemplateByPartnerIdFactory)
            .create(any());
        dataProcessor = new StocksLogbrokerDataProcessor(
            environmentService, partnerFFLinkService, logisticApiRequestsClientService, stocksWarehouseGroupService);
    }

    @Test
    @DisplayName("Все данные будут записаны")
    @DbUnitDataSet(before = "StocksLogbrokerDataProcessorTest.before.csv")
    public void testRetry() {
        ResponseWrapper wrapper = mock(ResponseWrapper.class);
        doThrow(new FulfillmentApiException(new ErrorPair()))
                .doThrow(new FulfillmentApiException(new ErrorPair()))
                .doThrow(new FulfillmentApiException(new ErrorPair()))
                .doThrow(new FulfillmentApiException(new ErrorPair()))
                .doReturn(wrapper)
                .when(httpTemplate).executePost(any(),
                any(Class.class),
                anyString(),
                anyString());
        doReturn(new RequestState())
                .when(wrapper).getRequestState();
        dataProcessor.process(generateMessage());
    }

    @Test
    @DisplayName("Все данные будут записаны. Получение оффера из Json файла")
    @DbUnitDataSet(before = "StocksLogbrokerDataProcessorTest.before.csv")
    public void testMessageFromJson() {
        ResponseWrapper wrapper = mock(ResponseWrapper.class);
        willReturn(wrapper)
                .given(httpTemplate).executePost(any(),
                any(Class.class),
                anyString(),
                anyString());
        willReturn(new RequestState())
                .given(wrapper).getRequestState();
        dataProcessor.process(generateMessage("OfferStock.json"));
        ArgumentCaptor<RequestWrapper> captor = ArgumentCaptor.forClass(RequestWrapper.class);
        then(httpTemplate).should()
                .executePost(captor.capture(), any(Class.class), anyString(), anyString());
    }

    @Test
    @DisplayName("Все данные будут записаны")
    @DbUnitDataSet(before = "StocksLogbrokerDataProcessorTest.before.csv")
    public void testPartnerStocks() {
        ArgumentCaptor<RequestWrapper> captor = ArgumentCaptor.forClass(RequestWrapper.class);
        ResponseWrapper wrapper = mock(ResponseWrapper.class);
        doReturn(wrapper)
                .when(httpTemplate).executePost(any(),
                any(Class.class),
                anyString(),
                anyString());
        doReturn(new RequestState())
                .when(wrapper).getRequestState();
        dataProcessor.process(generateMessage());
        verify(httpTemplate, times(5))
                .executePost(captor.capture(), any(Class.class), anyString(), anyString());
        List<ItemStocks> resultList = captor.getAllValues()
                .stream()
                .map(requestWrapper -> (PushStocksRequest) requestWrapper.getRequest())
                .map(PushStocksRequest::getItemStocksList)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertThat(resultList)
                .hasSize(6)
                // для поставщиков не из RESET_STOCKS_TIME_SUPPLIER время из события
                .filteredOn(is -> !RESET_STOCKS_TIME_SUPPLIER.contains(is.getUnitId().getVendorId()))
                .allMatch(is -> is.getStocks().stream()
                        .allMatch(s -> s.getUpdated().equals(STOCKS_TIME)));
        assertThat(resultList)
                // для поставщиков из RESET_STOCKS_TIME_SUPPLIER время не из события
                .filteredOn(is -> RESET_STOCKS_TIME_SUPPLIER.contains(is.getUnitId().getVendorId()))
                .allMatch(is -> is.getStocks().stream()
                        .noneMatch(s -> s.getUpdated().equals(STOCKS_TIME)));

        validateMetrics();
    }

    @Test
    @DisplayName("Ошибка в ЛМС при получении токена. Упали. Записали ошибки")
    @DbUnitDataSet(before = "StocksLogbrokerDataProcessorTest.before.csv")
    public void testPartnerStocksWitLMSFail() {
        //given
        willThrow(RuntimeException.class).given(lmsClient).getPartnerApiSettings(anyLong());

        //when
        assertThrows(RuntimeException.class, () -> dataProcessor.process(generateMessage()));

        //then
        //не ходим в лгв без токена
        verifyNoMoreInteractions(httpTemplate);

        Counter countMetric = (Counter) getMetricRegistry()
                .getMetric(new MetricId("push_stocks_errors_counter", Labels.of()));
        assertNotNull(countMetric);
        assertEquals(2L, countMetric.get());
    }


    private void validateMetrics() {
        Counter countMetric = (Counter) getMetricRegistry()
                .getMetric(new MetricId("push_stocks_counter", Labels.of()));
        Histogram dbTimingMetric = (Histogram) getMetricRegistry()
                .getMetric(new MetricId("push_stocks_db_timing", Labels.of()));
        Counter writtenMetric = (Counter) getMetricRegistry()
                .getMetric(new MetricId("push_stocks_lgw_written_counter", Labels.of()));
        Histogram lgwTimingMetric = (Histogram) getMetricRegistry()
                .getMetric(new MetricId("push_stocks_lgw_timing", Labels.of()));

        assertNotNull(countMetric);
        assertTrue(countMetric.get() >= 0);
        assertNotNull(writtenMetric);
        assertTrue(writtenMetric.get() >= 0);
        assertNotNull(dbTimingMetric);
        assertTrue(dbTimingMetric.snapshot().count() >= 0);
        assertNotNull(lgwTimingMetric);
        assertTrue(lgwTimingMetric.snapshot().count() >= 0);
    }

    @Test
    @DisplayName("Проверить, что записи не было")
    @DbUnitDataSet(before = {"logbroker_skip_reading.before.csv", "StocksLogbrokerDataProcessorTest.before.csv"})
    public void testEnvironmentSkipReading() {
        ArgumentCaptor<RequestWrapper> captor = ArgumentCaptor.forClass(RequestWrapper.class);
        ResponseWrapper wrapper = mock(ResponseWrapper.class);
        doReturn(wrapper)
                .when(httpTemplate).executePost(any(),
                any(Class.class),
                anyString(),
                anyString());
        doReturn(new RequestState())
                .when(wrapper).getRequestState();
        dataProcessor.process(mock(MessageBatch.class));
        verify(httpTemplate, times(0))
                .executePost(captor.capture(), any(Class.class), anyString(), anyString());
    }

    @Test
    @DisplayName("Пуш стоков для DBS")
    @DbUnitDataSet(before = "StocksLogbrokerDataProcessorTest.DBS.before.csv")
    public void testPartnerStocksDBSEnabled() {
        ArgumentCaptor<RequestWrapper> captor = ArgumentCaptor.forClass(RequestWrapper.class);
        ResponseWrapper wrapper = mock(ResponseWrapper.class);
        doReturn(wrapper)
                .when(httpTemplate).executePost(any(),
                any(Class.class),
                anyString(),
                anyString());
        doReturn(new RequestState())
                .when(wrapper).getRequestState();
        dataProcessor.process(generateWhiteMessage());
        verify(httpTemplate, times(8))
                .executePost(captor.capture(), any(Class.class), anyString(), anyString());
        List<ItemStocks> resultList = captor.getAllValues()
                .stream()
                .map(requestWrapper -> (PushStocksRequest) captor.getValue().getRequest())
                .map(PushStocksRequest::getItemStocksList)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        Assert.assertEquals(8, resultList.size());
        validateMetrics();
    }

    @Test
    @DisplayName("Записываем офера минуя фильтр МБОК")
    @DbUnitDataSet(before = {"StocksLogbrokerDataProcessorTest.testFilter.before.csv",
            "StocksLogbrokerDataProcessorTest.environment.csv"})
    void getPartnerStockWithoutMappingFilter() {
        ArgumentCaptor<RequestWrapper> captor = ArgumentCaptor.forClass(RequestWrapper.class);
        ResponseWrapper wrapper = mock(ResponseWrapper.class);
        doReturn(wrapper)
                .when(httpTemplate).executePost(any(),
                any(Class.class),
                anyString(),
                anyString());
        doReturn(new RequestState())
                .when(wrapper).getRequestState();
        dataProcessor.process(generateMessage());
        verify(httpTemplate, times(4))
                .executePost(captor.capture(), any(Class.class), anyString(), anyString());
        List<ItemStocks> resultList = captor.getAllValues()
                .stream()
                .map(requestWrapper -> (PushStocksRequest) captor.getValue().getRequest())
                .map(PushStocksRequest::getItemStocksList)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        Assert.assertEquals(4, resultList.size());
    }

    private MessageBatch generateMessage() {
        DataCampOffer.OffersBatch offersBatchOne = DataCampOffer.OffersBatch.newBuilder()
                // синий офер, берем сток
                .addOffer(createOffer(0, "0", 10, 0, 1, 1, DataCampOfferMeta.MarketColor.BLUE))
                // белый офер, берём сток, так как цвет игнорируется
                .addOffer(createOffer(1, "1", 1, 1, 1, 1, DataCampOfferMeta.MarketColor.WHITE))
                // синий офер, берем сток
                .addOffer(createOffer(3, "3", 3, 3, 0, 1, DataCampOfferMeta.MarketColor.BLUE))
                // синий офер, но не берем сток, т.к. дубликат по offerId, feedId, shopId, warehouseId
                .addOffer(createOffer(3, "3", 3, 0, 0, 1, DataCampOfferMeta.MarketColor.BLUE))
                // синий офер, но не берем сток, т.к. hasCount = false
                .addOffer(createOffer(3, "0", 3, 3, 0, -1, DataCampOfferMeta.MarketColor.BLUE))
                // синий офер, но не берем сток, т.к. warehouse_id = 0
                .addOffer(createOffer(3, "3", 0, 3, 0, 1, DataCampOfferMeta.MarketColor.BLUE))
                // синий офер, но не берем сток, т.к. warehouse_id не существует
                .addOffer(createOffer(3, "3", DELETED_WRH_ID, 3, 0, 1, DataCampOfferMeta.MarketColor.BLUE))
                // синий офер, со стоком, не главный склад в группе
                .addOffer(createOffer(111, "two",122, 100, 1, 1, DataCampOfferMeta.MarketColor.BLUE))
                .build();
        DataCampOffer.OffersBatch offersBatchTwo = DataCampOffer.OffersBatch.newBuilder()
                // синий офер, но сток пустой
                .addOffer(createOffer(2, "2", 2, 2, 1, 0, DataCampOfferMeta.MarketColor.BLUE))
                // синий офер, берем сток
                .addOffer(createOffer(3, "3", 3, 3, 0, 1, DataCampOfferMeta.MarketColor.BLUE))
                // синий офер, со стоком, главный склад в группе
                .addOffer(createOffer(110, "one",12, 100, 1, 1, DataCampOfferMeta.MarketColor.BLUE))
                .build();
        MessageMeta metaOne = new MessageMeta("test".getBytes(), 0, 0, 0, "::1", CompressionCodec.RAW,
                Collections.emptyMap());
        MessageMeta metaTwo = new MessageMeta("test".getBytes(), 1, 0, 0, "::1", CompressionCodec.RAW,
                Collections.emptyMap());
        return new MessageBatch("", 0,
                List.of(new MessageData(createMessage(offersBatchOne).toByteArray(), 0, metaOne),
                        new MessageData(createMessage(offersBatchTwo).toByteArray(),
                                offersBatchOne.getSerializedSize(), metaTwo)));
    }

    private MessageBatch generateWhiteMessage() {
        DataCampOffer.OffersBatch offersBatchOne = DataCampOffer.OffersBatch.newBuilder()
                // белый офер, берем сток
                .addOffer(createOffer(0, "0", 10, 0, 1, 1, DataCampOfferMeta.MarketColor.WHITE))
                .addOffer(createOffer(1, "0", 1, 1, 1, 1, DataCampOfferMeta.MarketColor.WHITE))
                // синий офер, берём сток, так как цвет игнорируется
                .addOffer(createOffer(2, "1", 2, 1, 1, 1, DataCampOfferMeta.MarketColor.BLUE))
                .addOffer(createOffer(3, "1", 3, 1, 1, 1, DataCampOfferMeta.MarketColor.BLUE))
                // белый офер, берем сток
                .addOffer(createOffer(4, "0", 4, 4, 0, 1, DataCampOfferMeta.MarketColor.WHITE))
                .addOffer(createOffer(5, "0", 5, 5, 0, 1, DataCampOfferMeta.MarketColor.WHITE))
                // белый офер, но не берем сток, т.к. дубликат по offerId, feedId, shopId, warehouseId
                .addOffer(createOffer(1, "3", 1, 1, 0, 1, DataCampOfferMeta.MarketColor.WHITE))
                // белый офер, но не берем сток, т.к. hasCount = false
                .addOffer(createOffer(6, "0", 6, 6, 0, -1, DataCampOfferMeta.MarketColor.WHITE))
                .addOffer(createOffer(7, "0", 7, 7, 0, -1, DataCampOfferMeta.MarketColor.WHITE))
                // белый офер, но не берем сток, т.к. warehouseId = 0
                .addOffer(createOffer(4, "0", 0, 4, 0, 1, DataCampOfferMeta.MarketColor.WHITE))
                // белый офер, но не берем сток, т.к. warehouseId не существует
                .addOffer(createOffer(4, "0", DELETED_WRH_ID, 4, 0, 1, DataCampOfferMeta.MarketColor.WHITE))
                .build();
        DataCampOffer.OffersBatch offersBatchTwo = DataCampOffer.OffersBatch.newBuilder()
                // белый офер, но сток пустой
                .addOffer(createOffer(0, "2", 0, 0, 1, 0, DataCampOfferMeta.MarketColor.WHITE))
                // белый офер, берем сток
                .addOffer(createOffer(1, "3", 1, 1, 0, 1, DataCampOfferMeta.MarketColor.WHITE))
                // белый офер, со стоком, не главный склад в группе
                .addOffer(createOffer(110, "one",9, 100, 1, 1, DataCampOfferMeta.MarketColor.WHITE))
                .build();
        MessageMeta metaOne = new MessageMeta("test".getBytes(), 0, 0, 0, "::1", CompressionCodec.RAW,
                Collections.emptyMap());
        MessageMeta metaTwo = new MessageMeta("test".getBytes(), 1, 0, 0, "::1", CompressionCodec.RAW,
                Collections.emptyMap());
        return new MessageBatch("", 0,
                List.of(new MessageData(createMessage(offersBatchOne).toByteArray(), 0, metaOne),
                        new MessageData(createMessage(offersBatchTwo).toByteArray(),
                                offersBatchOne.getSerializedSize(), metaTwo)));
    }

    private MessageBatch generateMessage(String filename) {
        DataCampOffer.Offer offer = ProtoTestUtil.getProtoMessageByJson(
                DataCampOffer.Offer.class,
                filename,
                getClass()
        );
        DataCampOffer.OffersBatch offersBatchOne = DataCampOffer.OffersBatch.newBuilder()
                .addOffer(offer)
                .build();

        MessageMeta meta = new MessageMeta("test".getBytes(), 0, 0, 0, "::1", CompressionCodec.RAW,
                Collections.emptyMap());
        return new MessageBatch("", 0,
                List.of(new MessageData(createMessage(offersBatchOne).toByteArray(), 0, meta)));
    }

    private DatacampMessageOuterClass.DatacampMessage createMessage(DataCampOffer.OffersBatch offersBatch) {
        return DatacampMessageOuterClass.DatacampMessage.newBuilder()
                .addOffers(offersBatch)
                .build();
    }

    private DataCampOffer.Offer createOffer(int shopId, String offerId, int warehouseId, int feedId,
                                            int marketStocksCount, int partnersStocksCount,
                                            DataCampOfferMeta.MarketColor color) {
        return DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setShopId(shopId)
                        .setOfferId(offerId)
                        .setWarehouseId(warehouseId)
                        .setFeedId(feedId))
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(1234)
                                        .build())
                                .build())
                        .build())
                .setMeta(DataCampOfferMeta.OfferMeta.newBuilder()
                        .setCreationTs(Instant.now().toEpochMilli())
                        .setRgb(color))
                .setStockInfo(DataCampOfferStockInfo.OfferStockInfo.newBuilder()
                        .setMarketStocks(DataCampOfferStockInfo.OfferStocks.newBuilder()
                                .setCount(marketStocksCount)
                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                        .setTimestamp(Timestamp.newBuilder().setSeconds(
                                                STOCKS_TIME.getOffsetDateTime().toInstant().getEpochSecond()))))
                        .setPartnerStocks(DataCampOfferStockInfo.OfferStocks.newBuilder()
                                .setCount(partnersStocksCount)
                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                        .setTimestamp(Timestamp.newBuilder().setSeconds(
                                                STOCKS_TIME.getOffsetDateTime().toInstant().getEpochSecond())))))
                .build();
    }
}
