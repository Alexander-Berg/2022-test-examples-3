package ru.yandex.market.partner.auction.servantlet.search.actions;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.SAXException;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.partner.auction.BidComponentsInferenceManager;
import ru.yandex.market.partner.auction.OfferSummary;
import ru.yandex.market.partner.auction.ReportRecommendationService;
import ru.yandex.market.partner.auction.SearchAuctionOffersServantlet;
import ru.yandex.market.partner.auction.servantlet.AuctionServantletMockBase;
import ru.yandex.market.partner.auction.servantlet.bulk.PartiallyRecommendatorsFactory;
import ru.yandex.market.partner.auction.view.SerializationGate;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

/**
 * Ответы репорта замоканы в файлах. Для типа запроса без явного type используются рекомендации для параллельного и
 * для карточного.
 * <p>
 * Проверяем кейсы для поисковой выдачи для офферов, для которых нет существующих ставок.
 *
 * @author vbudnev
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class DefaultLinkTypeAtSearchResultsTest extends AuctionServantletMockBase {

    @InjectMocks
    private SearchAuctionOffersServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> searchAuctionOffersServantlet;
    @InjectMocks
    private BidComponentsInferenceManager bidComponentsInferenceManager;

    private MockServResponse usefullServResponse;

    @Before
    public void before() throws IOException, SAXException {

        ReportRecommendationService recommendationsService = new ReportRecommendationService(
                PartiallyRecommendatorsFactory.buildParallelSearchRecommendator(
                        this.getClass().getResourceAsStream("./resources/parallel_search_ok.xml")
                ),
                marketSearchBidRecommendator,
                PartiallyRecommendatorsFactory.buildCardRecommendator(
                        this.getClass().getResourceAsStream("./resources/hybrid_card_ok.xml")
                ),
                mockedExistenceChecker
        );

        searchAuctionOffersServantlet.setBidComponentsInferenceManager(bidComponentsInferenceManager);
        searchAuctionOffersServantlet.setRecommendationsService(recommendationsService);
        mockBidLimits();

        usefullServResponse = new MockServResponse();

        mockRegionsAndTariff();

        mockRecommendationServiceEmptyCalculateResult();
        mockOfferExists();

        mockServRequestCrudActionREAD();
        mockServRequestIdentificationParams();
        mockShopAuctionType(AuctionOfferIdType.TITLE);

        mockSearchReportAnswers();
        mockAuctionHasNoBids();
    }

    /**
     * Проверяем, что тип связи по умолчанию для офферов, которых нет в биддинге - {@link AuctionBidComponentsLink#DEFAULT_LINK_TYPE}
     */
    @Disabled
    @Test
    public void test_getSearchOffer_when_thereIsNoOfferInBidding_should_returnDefaultLinkType() {
        mockAuctionHasNoBids();
        mockServantletPassedArgs("q=" + SOME_SEARCH_QUERY);

        searchAuctionOffersServantlet.process(servRequest, usefullServResponse);

        List<SerializationGate.AuctionOffer> auctionBidResults = extractBidsFromServResponse(usefullServResponse);
        assertThat(auctionBidResults, hasSize(1));
        OfferSummary offerSummary = auctionBidResults.get(0).getOfferSummary();

        //ставки как таковой тут нет, просто структурно информаицю о типе связи располагаюется в этом блоке
        assertThat(offerSummary.getBid().getStatus(), is(nullValue()));
        assertThat(offerSummary.getOffer().getId(), is(SOME_OFFER_NAME));
    }

}
