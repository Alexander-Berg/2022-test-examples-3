package ru.yandex.market.partner.auction.servantlet.search.actions;

import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.partner.auction.AuctionBulkOfferBidsServantlet;
import ru.yandex.market.partner.auction.BidComponentsInferenceManager;
import ru.yandex.market.partner.auction.BulkReadQueryType;
import ru.yandex.market.partner.auction.OfferSummary;
import ru.yandex.market.partner.auction.ReportRecommendationService;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;
import ru.yandex.market.partner.auction.view.SerializationGate;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.market.core.auction.AuctionService.DEFAULT_GROUP_ID;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.OFFER_NAME_1;

/**
 * Проверяем, что ответ содержит нужные параметры ставок и рекомендаций
 * ТП берется из мока ставок для группы в {@link AuctionService}.
 */
@ExtendWith(MockitoExtension.class)
public class GetAuctionBulkBidsTest extends AuctionBulkServantletlMockBase {
    @InjectMocks
    private AuctionBulkOfferBidsServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> auctionBulkOfferBidsServantlet;
    @InjectMocks
    private ReportRecommendationService recommendationsService;
    @InjectMocks
    private BidComponentsInferenceManager bidComponentsInferenceManager;

    @BeforeEach
    void beforeEach() {
        auctionBulkOfferBidsServantlet.configure();
        mockBidLimits();

        usefullServResponse = new MockServResponse();

        auctionBulkOfferBidsServantlet.setBidComponentsInferenceManager(bidComponentsInferenceManager);
        auctionBulkOfferBidsServantlet.setRecommendationsService(recommendationsService);
        mockRegionsAndTariff();

        mockCheckHomeRegionInIndex();
        mockRecommendationServiceEmptyCalculateResult();

        mockServRequestCrudActionREAD();
        mockServRequestIdentificationParams();
        mockOfferExists();
    }

    @Test
    void test_getRecommendationsByGroupId_when_unknownSearchQuery() {
        // в группе offer id ставка с unknown поисковым запросом
        mockAuctionServicePartialBidsForDefaultGroup(false);
        mockServantletPassedArgs(
                "type=" + BulkReadQueryType.MARKET_SEARCH_GROUP
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<SerializationGate.AuctionOffer> auctionBidResults = extractBidsFromServResponse(usefullServResponse);
        assertThat(auctionBidResults, hasSize(1));

        OfferSummary offerSummary = auctionBidResults.get(0).getOfferSummary();

        // unknown поисковый запрос заменился именем оффера
        assertThat(offerSummary.getOffer().getSearchQuery(), is(OFFER_NAME_1));
        assertThat(offerSummary.getOffer().getGroupId(), is(DEFAULT_GROUP_ID));
        // offer id тоже заменился человекочитаемым именем оффера
        assertThat(offerSummary.getOffer().getOfferId(), is(OFFER_NAME_1));
        assertThat(offerSummary.getOffer().getFeedId(), is(SOME_FEED_ID));
        assertNotNull(offerSummary.getRecommendations());
    }
}
