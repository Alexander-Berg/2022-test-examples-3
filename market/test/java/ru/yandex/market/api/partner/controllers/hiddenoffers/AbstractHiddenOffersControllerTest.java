package ru.yandex.market.api.partner.controllers.hiddenoffers;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.logbroker.event.datacamp.PapiHiddenOfferDataCampEvent;
import ru.yandex.market.core.logbroker.event.datacamp.SyncChangeOfferLogbrokerEvent;
import ru.yandex.market.core.offer.IndexerOfferKey;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.util.MbiAsserts;

import static ru.yandex.market.api.partner.logbroker.util.ChangeOfferLogbrokerTestUtil.assertEvent;
import static ru.yandex.market.api.partner.logbroker.util.ChangeOfferLogbrokerTestUtil.assertHiddenOffer;
import static ru.yandex.market.api.partner.logbroker.util.ChangeOfferLogbrokerTestUtil.getLogbrokerEvents;
import static ru.yandex.market.api.partner.logbroker.util.ChangeOfferLogbrokerTestUtil.getOffers;
import static ru.yandex.market.core.logbroker.event.datacamp.DataCampEvent.toIdentifiers;

/**
 * Date: 27.11.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
public abstract class AbstractHiddenOffersControllerTest extends AbstractHiddenOffersControllerFunctionalTest {

    private final String testFolder;
    private final boolean expectedFlag;
    @Autowired
    @Qualifier("marketQuickLogbrokerService")
    protected LogbrokerEventPublisher<SyncChangeOfferLogbrokerEvent> marketQuickLogbrokerService;

    public AbstractHiddenOffersControllerTest(String testFolder,
                                              boolean expectedFlag) {
        this.testFolder = testFolder;
        this.expectedFlag = expectedFlag;
    }

    @Nonnull
    protected List<SyncChangeOfferLogbrokerEvent> sendShopRequest(String test, long campaignId) {
        assertResponse(test, campaignId);

        List<SyncChangeOfferLogbrokerEvent> defaultLogbrokerEvents =
                getLogbrokerEvents(marketQuickLogbrokerService, 1);

        Assertions.assertThat(defaultLogbrokerEvents).hasSize(1);

        assertEvent(defaultLogbrokerEvents, 0, PapiHiddenOfferDataCampEvent.class);

        return defaultLogbrokerEvents;
    }

    private void assertShopOrDirect(SyncChangeOfferLogbrokerEvent supplierLogbrokerEvent,
                              long feedId,
                              long partnerId,
                              DataCampOfferMeta.DataSource prioritySource,
                              DataCampOfferMeta.DataSource defaultSource,
                              CampaignType campaignType,
                              DataCampOfferMeta.MarketColor marketColor) {
        Map<DataCampOfferIdentifiers.OfferIdentifiers, DataCampOffer.Offer> dataCampEvents =
                getOffers(supplierLogbrokerEvent);

        Assertions.assertThat(dataCampEvents).hasSize(6);

        assertHiddenOffer(
                dataCampEvents.get(toIdentifiers(null, partnerId, campaignType,
                        IndexerOfferKey.offerId(feedId, "1"))),
                expectedFlag,
                prioritySource,
                marketColor
        );
        assertHiddenOffer(
                dataCampEvents.get(toIdentifiers(null, partnerId, campaignType,
                        IndexerOfferKey.offerId(feedId, "3"))),
                expectedFlag,
                defaultSource,
                marketColor
        );
        assertHiddenOffer(
                dataCampEvents.get(toIdentifiers(null, partnerId, campaignType,
                        IndexerOfferKey.offerId(feedId, "5"))),
                expectedFlag,
                prioritySource,
                marketColor
        );
        assertHiddenOffer(
                dataCampEvents.get(toIdentifiers(null, partnerId, campaignType,
                        IndexerOfferKey.offerId(feedId, "7"))),
                expectedFlag,
                defaultSource,
                marketColor
        );
        assertHiddenOffer(
                dataCampEvents.get(toIdentifiers(null, partnerId, campaignType,
                        IndexerOfferKey.offerId(feedId, "10"))),
                expectedFlag,
                prioritySource,
                marketColor
        );
        assertHiddenOffer(
                dataCampEvents.get(toIdentifiers(null, partnerId, campaignType,
                        IndexerOfferKey.offerId(feedId, "11"))),
                expectedFlag,
                defaultSource,
                marketColor
        );
    }

    protected void assertShop(SyncChangeOfferLogbrokerEvent supplierLogbrokerEvent,
                              long feedId,
                              long partnerId,
                              DataCampOfferMeta.DataSource prioritySource,
                              DataCampOfferMeta.DataSource defaultSource) {
        assertShopOrDirect(
            supplierLogbrokerEvent,
            feedId,
            partnerId,
            prioritySource,
            defaultSource,
            CampaignType.SHOP,
            DataCampOfferMeta.MarketColor.WHITE
        );
    }

    protected void assertDirect(SyncChangeOfferLogbrokerEvent supplierLogbrokerEvent,
                              long feedId,
                              long partnerId,
                              DataCampOfferMeta.DataSource prioritySource,
                              DataCampOfferMeta.DataSource defaultSource) {
        assertShopOrDirect(
                supplierLogbrokerEvent,
                feedId,
                partnerId,
                prioritySource,
                defaultSource,
                CampaignType.DIRECT,
                DataCampOfferMeta.MarketColor.UNKNOWN_COLOR
        );
    }

    protected void assertSupplierRequest(String test,
                                         long campaignId,
                                         long feedId,
                                         long partnerId,
                                         DataCampOfferMeta.DataSource dataSource,
                                         boolean isMarketSku) {
        mockUltraControllerClient();

        assertResponse(test, campaignId);

        List<SyncChangeOfferLogbrokerEvent> defaultLogbrokerEvents =
                getLogbrokerEvents(marketQuickLogbrokerService, 1);

        Assertions.assertThat(defaultLogbrokerEvents).hasSize(1);

        assertEvent(defaultLogbrokerEvents, 0, PapiHiddenOfferDataCampEvent.class);

        assertSupplier(defaultLogbrokerEvents.get(0), feedId, partnerId, null, dataSource, isMarketSku);
    }

    protected void assertSupplier(SyncChangeOfferLogbrokerEvent supplierLogbrokerEvent,
                                  long feedId,
                                  long partnerId,
                                  @Nullable Long businessId,
                                  DataCampOfferMeta.DataSource dataSource,
                                  boolean isMarketSku) {
        Map<DataCampOfferIdentifiers.OfferIdentifiers, DataCampOffer.Offer> dataCampEvents =
                getOffers(supplierLogbrokerEvent);

        Assertions.assertThat(dataCampEvents).hasSize(4);

        assertSupplier(dataCampEvents, feedId, partnerId, businessId, dataSource, isMarketSku);
    }

    protected void assertSupplier(Map<DataCampOfferIdentifiers.OfferIdentifiers, DataCampOffer.Offer> dataCampEvents,
                                  long feedId,
                                  long partnerId,
                                  @Nullable Long businessId,
                                  DataCampOfferMeta.DataSource dataSource,
                                  boolean isMarketSku) {
        assertHiddenOffer(
                dataCampEvents.get(toIdentifiers(businessId, partnerId, CampaignType.SUPPLIER,
                        new IndexerOfferKey(feedId, isMarketSku ? 1 : 0, isMarketSku ? "###" : "1", null))),
                expectedFlag,
                dataSource,
                DataCampOfferMeta.MarketColor.BLUE
        );
        assertHiddenOffer(
                dataCampEvents.get(toIdentifiers(businessId, partnerId, CampaignType.SUPPLIER,
                        new IndexerOfferKey(feedId, isMarketSku ? 3 : 0, isMarketSku ? "###" : "3", null))),
                expectedFlag,
                dataSource,
                DataCampOfferMeta.MarketColor.BLUE
        );
        assertHiddenOffer(
                dataCampEvents.get(toIdentifiers(businessId, partnerId, CampaignType.SUPPLIER,
                        new IndexerOfferKey(feedId, isMarketSku ? 5 : 0, isMarketSku ? "###" : "5", null))),
                expectedFlag,
                dataSource,
                DataCampOfferMeta.MarketColor.BLUE
        );
        assertHiddenOffer(
                dataCampEvents.get(toIdentifiers(businessId, partnerId, CampaignType.SUPPLIER,
                        new IndexerOfferKey(feedId, isMarketSku ? 7 : 0, isMarketSku ? "###" : "7", null))),
                expectedFlag,
                dataSource,
                DataCampOfferMeta.MarketColor.BLUE
        );
    }

    protected void assertResponse(String test, long campaignId) {
        ResponseEntity<String> response = getResponseEntity(fileToString(test), campaignId);

        Assertions.assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        MbiAsserts.assertJsonEquals(getSuccessfulResponse(), response.getBody());
    }

    @Nonnull
    protected abstract ResponseEntity<String> getResponseEntity(String file, long campaignId);

    @Nonnull
    private String getSuccessfulResponse() {
        return fileToString("successful.response");
    }

    @Nonnull
    private String fileToString(@Nonnull String test) {
        return fileToString(testFolder, test);
    }
}
