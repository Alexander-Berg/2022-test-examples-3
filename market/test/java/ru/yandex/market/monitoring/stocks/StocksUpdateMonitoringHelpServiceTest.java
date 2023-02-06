package ru.yandex.market.monitoring.stocks;

import java.time.Instant;
import java.util.List;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferStockInfo;
import Market.DataCamp.DataCampUnitedOffer;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.datacamp.DataCampService;
import ru.yandex.market.core.datacamp.model.GetBusinessUnitedOfferRequest;
import ru.yandex.market.core.feed.PartnerFeedService;
import ru.yandex.market.core.feed.datacamp.FeedParsingType;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.core.feed.supplier.SupplierFeedService;
import ru.yandex.market.core.feed.supplier.model.PartnerUtilityFeed;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.shop.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

/**
 * Тесты для {@link StocksUpdateMonitoringHelpService}.
 *
 * @author Zvorygin Andrey don-dron@yandex-team.ru
 */
@DbUnitDataSet(before = "StocksUpdateMonitoringHelpServiceTest.before.csv")
class StocksUpdateMonitoringHelpServiceTest extends FunctionalTest {

    private static final String OFFER_ID = "9999";
    private static final String STOCK_URL = "test_url";
    private static final long BUSINESS_ID = 5555;
    private static final long SUPPLIER_ID = 6666;
    private static final long FEED_ID = 8888;
    private static final long WAREHOUSE_ID = 1010;
    private static final long UPLOAD_ID = 7777;
    private static final long FAIL_SUPPLIER_ID = 2222;
    private static final long FAIL_FEED_ID = 3333;

    @Spy
    @Autowired
    private EnvironmentService environmentService;

    @Spy
    @Autowired
    private BusinessService businessService;

    @Spy
    @Autowired
    private ProtocolService protocolService;

    @Spy
    @Autowired
    private SupplierFeedService supplierFeedService;

    @Spy
    @Autowired
    private DataCampService dataCampService;

    @Spy
    @Autowired
    private PartnerFeedService partnerFeedService;

    private StocksUpdateMonitoringHelpService stocksUpdateMonitoringHelpService;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
        stocksUpdateMonitoringHelpService = new StocksUpdateMonitoringHelpService(
                environmentService,
                businessService,
                supplierFeedService,
                dataCampService,
                protocolService,
                partnerFeedService);
    }

    @Test
    void getEnvironmentIdTest() {
        assertEquals(STOCK_URL, stocksUpdateMonitoringHelpService.getUrlFileStock().get());
        assertEquals(OFFER_ID, stocksUpdateMonitoringHelpService.getOfferId().get());
        assertEquals(FEED_ID, stocksUpdateMonitoringHelpService.getFeedId().get());
    }

    @Test
    void getBusinessIdTest() {
        assertEquals(BUSINESS_ID, stocksUpdateMonitoringHelpService.getBusinessId(SUPPLIER_ID));
        Mockito.verify(businessService).getBusinessIdByPartner(SUPPLIER_ID);
    }

    @Test
    void getSupplierIdTest() {
        assertEquals(SUPPLIER_ID, stocksUpdateMonitoringHelpService.getSupplierId(FEED_ID));
        Mockito.verify(supplierFeedService).getSupplierIdByFeedId(FEED_ID);
    }

    @Test
    void getBusinessIdFailTest() {
        assertThrows(StocksUpdateMonitoringException.class,
                () -> stocksUpdateMonitoringHelpService.getBusinessId(FAIL_SUPPLIER_ID));
    }

    @Test
    void getSupplierIdFailTest() {
        assertThrows(StocksUpdateMonitoringException.class,
                () -> stocksUpdateMonitoringHelpService.getSupplierId(FAIL_FEED_ID));
    }

    @Test
    void updateSuppliersStocksTest() {
        Mockito.when(partnerFeedService.getUtilityFeeds(List.of(SUPPLIER_ID), List.of(FeedType.STOCKS)))
                .thenReturn(
                        List.of(new PartnerUtilityFeed.Builder()
                                .setPartnerId(SUPPLIER_ID)
                                .setBusinessId(BUSINESS_ID)
                                .setUploadId(UPLOAD_ID)
                                .setType(FeedType.STOCKS)
                                .setUpdatedAt(Instant.now())
                                .build())
                );

        stocksUpdateMonitoringHelpService.updateSuppliersStocks(SUPPLIER_ID, STOCK_URL, WAREHOUSE_ID);

        ArgumentCaptor<PartnerUtilityFeed> captor = ArgumentCaptor.forClass(PartnerUtilityFeed.class);

        Mockito.verify(partnerFeedService)
                .getUtilityFeeds(List.of(SUPPLIER_ID), List.of(FeedType.STOCKS));
        Mockito.verify(partnerFeedService)
                .addUtilityFeed(
                        anyLong(),
                        any(PartnerId.class),
                        eq(FeedParsingType.COMPLETE_FEED),
                        anyLong(),
                        captor.capture());

        PartnerUtilityFeed feed = captor.getValue();
        assertEquals(6666L, feed.getPartnerId());
        assertEquals(7777L, feed.getUploadId().orElse(0L));
        assertEquals(FeedType.STOCKS, feed.getType());
    }

    @Test
    void getLastFeedUpdateTimestampTest() {
        long lastTimeStamp1 = 12121;
        long lastTimeStamp2 = 22222121;

        DataCampUnitedOffer.UnitedOffer response = DataCampUnitedOffer.UnitedOffer.newBuilder()
                .putStock(1, DataCampOffer.Offer.newBuilder()
                        .setStockInfo(DataCampOfferStockInfo.OfferStockInfo.newBuilder()
                                .setPartnerStocks(DataCampOfferStockInfo.OfferStocks.newBuilder()
                                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                                .setTimestamp(Timestamp.newBuilder()
                                                        .setSeconds(lastTimeStamp1)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .putActual(2, DataCampUnitedOffer.ActualOffers.newBuilder()
                        .putWarehouse(55,
                                DataCampOffer.Offer.newBuilder()
                                        .setStockInfo(DataCampOfferStockInfo.OfferStockInfo.newBuilder()
                                                .setPartnerStocks(DataCampOfferStockInfo.OfferStocks.newBuilder()
                                                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                                                .setTimestamp(Timestamp.newBuilder()
                                                                        .setSeconds(lastTimeStamp2)
                                                                        .build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                        .build())
                .build();
        doReturn(List.of(response))
                .when(dataCampService)
                .getBusinessUnitedOffers(any(GetBusinessUnitedOfferRequest.class));
        assertEquals(lastTimeStamp2,
                stocksUpdateMonitoringHelpService.getLastFeedUpdateTimestamp(BUSINESS_ID, OFFER_ID, SUPPLIER_ID));
    }
}
