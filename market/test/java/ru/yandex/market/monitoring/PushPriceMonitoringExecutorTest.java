package ru.yandex.market.monitoring;

import java.util.List;

import Market.DataCamp.DataCampOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.datacamp.DataCampFeedPartnerService;
import ru.yandex.market.core.datacamp.DefaultDataCampService;
import ru.yandex.market.core.datacamp.OfferUpdateDataCampService;
import ru.yandex.market.core.datacamp.PushSettingsService;
import ru.yandex.market.core.environment.UnitedCatalogEnvironmentService;
import ru.yandex.market.core.feed.supplier.db.SupplierSummaryDao;
import ru.yandex.market.core.logbroker.MarketQuickLogbrokerSendingService;
import ru.yandex.market.core.offer.OfferUpdate;
import ru.yandex.market.core.offer.PapiMarketSkuOfferService;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.mbi.datacamp.combine.DataCampCombinedClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link PushPriceMonitoringExecutor}.
 */
@DbUnitDataSet(before = "pushMonitoringExecutorTest.before.csv")
class PushPriceMonitoringExecutorTest extends FunctionalTest {

    @Autowired
    private PushPriceMonitoringExecutor pushPriceMonitoringExecutor;

    @Autowired
    private PushMonitoringHelpService pushMonitoringHelpService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private DataCampFeedPartnerService dataCampFeedPartnerService;

    @Autowired
    private RetryTemplate retryTemplate;

    @Autowired
    private PushSettingsService pushSettingsService;

    @Autowired
    private PapiMarketSkuOfferService papiMarketSkuOfferService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private BusinessService businessService;

    private DataCampClient dataCampClientMock;
    private MarketQuickLogbrokerSendingService logbrokerSendingService;

    @Autowired
    private SupplierSummaryDao supplierSummaryDao;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private DataCampCombinedClient dataCampCommonCombinedClient;

    @Autowired
    private UnitedCatalogEnvironmentService unitedCatalogEnvironmentService;

    @BeforeEach
    void init() {
        dataCampClientMock = mock(DataCampClient.class);
        DefaultDataCampService defaultDataCampService = new DefaultDataCampService(
                dataCampClientMock,
                dataCampCommonCombinedClient,
                unitedCatalogEnvironmentService,
                campaignService,
                dataCampFeedPartnerService,
                businessService,
                retryTemplate,
                supplierSummaryDao,
                eventPublisher,
                protocolService,
                () -> false);
        logbrokerSendingService = mock(MarketQuickLogbrokerSendingService.class);
        OfferUpdateDataCampService offerUpdateDataCampService = new OfferUpdateDataCampService(
                papiMarketSkuOfferService, logbrokerSendingService, transactionTemplate, pushSettingsService
        );
        pushPriceMonitoringExecutor = new PushPriceMonitoringExecutor(
                defaultDataCampService,
                offerUpdateDataCampService,
                pushMonitoringHelpService
        );
    }

    @DisplayName("Проверяем, что если в енв пропертях будут неизвестные фид или ид магаза, то проверка упадет")
    @Test
    void checkDoJobEmpty() {
        RuntimeException runtimeException = assertThrows(RuntimeException.class,
                () -> pushPriceMonitoringExecutor.doJob(null));
        assertEquals("Price feed not found for supplier 666. Check environment properties",
                runtimeException.getMessage());
    }

    @DisplayName("Проверяем нормальную работу мониторинга")
    @Test
    @DbUnitDataSet(before = "pushMonitoringExecutorTestDoJob.before.csv")
    void checkDoJob() {
        long supplierId = 666L;
        long businessId = 11670L;
        long businessShopId = 670L;


        String offerId = "push-monitor-check-http-price";
        String offerHiddenId = "push-monitor-check-http-hidden";

        String businessPriceChangeHttpOfferId = "business-price-http-offer";
        String businessHidingHttpOfferId = "business-hiding-http-offer";

        ArgumentCaptor<List<OfferUpdate>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        doNothing().when(logbrokerSendingService).sendUpdateOfferEvents(argumentCaptor.capture());

        ArgumentCaptor<DataCampOffer.Offer> httpPriceCaptor = ArgumentCaptor.forClass(DataCampOffer.Offer.class);
        pushPriceMonitoringExecutor.doJob(null);
        verify(dataCampClientMock).changeBusinessServiceOffer(eq(businessId), eq(offerId),
                eq(supplierId), httpPriceCaptor.capture());
        verify(dataCampClientMock).changeBusinessServiceOffer(eq(businessId), eq(businessPriceChangeHttpOfferId),
                eq(businessShopId), httpPriceCaptor.capture());
        verify(dataCampClientMock).changeBusinessServiceOffer(eq(businessId), eq(businessHidingHttpOfferId),
                eq(businessShopId), any());
        verify(dataCampClientMock).changeBusinessServiceOffer(eq(businessId), eq(offerHiddenId),
                eq(supplierId), any());
        verify(logbrokerSendingService, times(5)).sendUpdateOfferEvents(any());

        List<List<OfferUpdate>> captorValues = argumentCaptor.getAllValues();
        assertEquals(5, captorValues.size());
        OfferUpdate offerUpdate = captorValues.get(0).get(0);
        assertEquals(supplierId, offerUpdate.partnerId());
        assertEquals("push-monitor-check-api-price",
                offerUpdate.offerKey().withoutPartnerKnowledge().shopSku());

        offerUpdate = captorValues.get(1).get(0);
        assertEquals(supplierId, offerUpdate.partnerId());
        assertEquals("push-monitor-check-api-hidden",
                offerUpdate.offerKey().withoutPartnerKnowledge().shopSku());

        offerUpdate = captorValues.get(2).get(0);
        assertEquals(businessShopId, offerUpdate.partnerId());
        assertEquals("business-price-logbroker-offer",
                offerUpdate.offerKey().withoutPartnerKnowledge().shopSku());

        offerUpdate = captorValues.get(3).get(0);
        assertEquals(businessShopId, offerUpdate.partnerId());
        assertEquals("business-price-inclusive-logbroker-offer",
                offerUpdate.offerKey().withoutPartnerKnowledge().shopSku());

        offerUpdate = captorValues.get(4).get(0);
        assertEquals(businessShopId, offerUpdate.partnerId());
        assertEquals("business-hiding-logbroker-offer",
                offerUpdate.offerKey().withoutPartnerKnowledge().shopSku());

        List<DataCampOffer.Offer> actualHttpPriceOffers = httpPriceCaptor.getAllValues();
        assertEquals(2, actualHttpPriceOffers.size());
        for (DataCampOffer.Offer actualHttpPriceOffer : actualHttpPriceOffers) {
            assertEquals(1, actualHttpPriceOffer.getStatus().getFieldsPlacementVersion().getValue());
        }

        verifyNoMoreInteractions(dataCampClientMock, logbrokerSendingService);
    }
}
