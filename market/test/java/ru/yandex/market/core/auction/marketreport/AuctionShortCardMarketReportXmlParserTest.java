package ru.yandex.market.core.auction.marketreport;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.Recommendation;
import ru.yandex.market.common.report.model.RecommendationType;
import ru.yandex.market.common.report.model.SearchResults;
import ru.yandex.market.core.AbstractParserTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasBid;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasFee;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasHyperId;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasMinBid;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasMinFee;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasPullupFlag;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasUrl;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasWareMd5;

public class AuctionShortCardMarketReportXmlParserTest extends AbstractParserTest {
    private static final String FILE_OK = "type-card_card-cpa_ok.xml";
    private static final String FILE_CARD_CPC_ONLY_OK = "type-card_ok.xml";
    private static final String FILE_CARD_CPA_ONLY_OK = "type-card-cpa_ok.xml";
    private static final String FILE_RECOMMENDATIONS_WITH_CODE_ERRORS = "type-card_card-cpa_code_errors.xml";
    private static final String FILE_EMPTY_RESULTS_TAG = "search_results_is_empty.xml";
    private static final String FILE_EMPTY_RECOMMENDATIONS = "type-card_card-cpa_recommendations_tag_is_empty.xml";
    private static final String FILE_RECOMMENDATIONS_UNAVAILABLE = "type-card_card-cpa_recommendations_unavailable.xml";
    private static AuctionShortCardMarketReportXmlParserSettings PARSER_SETTINGS;
    private AuctionShortCardMarketReportXmlParser PARSER;

    public static AuctionShortCardMarketReportXmlParserSettings createMinimalTestSettings() {
        AuctionShortCardMarketReportXmlParserSettings settings = new AuctionShortCardMarketReportXmlParserSettings();
        settings.setTagSearchResults("search_results");

        settings.setTagOffers("offers");

        settings.setTagOffer("offer");
        settings.setTagHyperId("hyper_id");
        settings.setTagWareMd5("ware_md5");
        settings.setTagActualBids("bids");
        settings.setAttrActualFee("fee");
        settings.setAttrActualBid("bid");
        settings.setTagPlainUrl("url");

        settings.setTagRecommendations("recommendations");

        settings.setTagRecommendationPosition("position");
        settings.setAttrRecommendationCurrentPosAll("current-pos-all");
        settings.setAttrRecommendationCurrentPosTop("current-pos-top");
        settings.setAttrRecommendationTopOffersCount("top-offers-count");
        settings.setAttrRecommendationMinBid("min-bid");
        settings.setAttrRecommendationMinFee("min-fee");
        settings.setAttrRecommendationError("error");

        settings.setAttrRecommendationCbid("bid");
        settings.setAttrRecommendationCode("code");
        settings.setAttrRecommendationFee("fee");
        settings.setAttrRecommendationPosition("pos");
        settings.setAttrPullUpBids("pull_to_min_bid");

        return settings;
    }

    @BeforeAll
    static void initOnce() {
        PARSER_SETTINGS = createMinimalTestSettings();
    }


    @BeforeEach
    void initMethodEnv() {
        PARSER = new AuctionShortCardMarketReportXmlParser(PARSER_SETTINGS);
    }

    @Test
    void test_parser_should_parseOffer_when_xmlIsOk() throws IOException, SAXException {
        try (InputStream in = getContentStream(FILE_OK)) {
            PARSER.parseXmlStream(in);

            SearchResults actual = PARSER.getSearchResults();

            List<FoundOffer> shopOffers = PARSER.getOffers();
            assertThat(shopOffers.size(), is(1));
            assertThat(actual.getTotalOffers(), is(1));

            FoundOffer offer = shopOffers.get(0);

            assertThat(offer,
                    allOf(
                            hasFee(210),
                            hasBid(836),
                            hasPullupFlag(true),
                            hasWareMd5("8wADCSNMnKbfD33alWtGkQ"),
                            hasHyperId(6324417L),
                            hasUrl("www.example.yandex.ru")
                    )
            );
        }
    }

    @Test
    void test_parser_should_parseBothRecommendations_when_xmlIsOk() throws IOException, SAXException {
        try (InputStream in = getContentStream(FILE_OK)) {
            PARSER.parseXmlStream(in);
            FoundOffer offer = (FoundOffer) PARSER.getOffers().get(0);

            assertThat(offer, allOf(hasMinBid(57), hasMinFee(200)));

            //cpc block
            assertNotNull("Offer must contain card_cpc recommendations", offer.getRecommendationBlock(RecommendationType.CARD));
            FoundOffer.RecommendationBlock cpcRecommendationBlock = offer.getRecommendationBlock(RecommendationType.CARD);
            assertThat(cpcRecommendationBlock.getCurrentPosAll(), is(1));
            assertThat(cpcRecommendationBlock.getCurrentPosTop(), is(2));
            assertThat(cpcRecommendationBlock.getTopOffersCount(), is(6));


            Map<Integer, Recommendation> actualCardCpcRecommendations = cpcRecommendationBlock.getRecommendations();
            Recommendation firstPosCpcRec = actualCardCpcRecommendations.get(1);
            assertThat(firstPosCpcRec.getBid(), is(701));
            assertThat(firstPosCpcRec.getCode(), is(0));

            //cpa block
            assertNotNull("Offer must contain card_cpa recommendations", offer.getRecommendationBlock(RecommendationType.CARD_CPA));
            FoundOffer.RecommendationBlock cpaRecommendationBlock = offer.getRecommendationBlock(RecommendationType.CARD_CPA);
            assertThat(cpaRecommendationBlock.getCurrentPosAll(), is(3));
            assertThat(cpaRecommendationBlock.getCurrentPosTop(), is(4));
            assertThat(cpaRecommendationBlock.getTopOffersCount(), is(5));


            Map<Integer, Recommendation> actualCardCpaRecommendations = cpaRecommendationBlock.getRecommendations();
            Recommendation firstPosCpaRec = actualCardCpaRecommendations.get(1);
            assertThat(firstPosCpaRec.getFee(), is(210));
            assertThat(firstPosCpaRec.getCode(), is(0));

            //other
            assertTrue("Default recommendations are of type card_cpc", offer.getDefaultRecommendationBlock().getRecommendations() == offer.getRecommendationBlock(RecommendationType.CARD).getRecommendations());
            assertNotNull("Offer recommendation block must not be null", offer.getRecommendations());
        }
    }

    @Test
    void test_parser_should_parseCpcRecommendations_when_xmlIsOk() throws IOException, SAXException {
        try (InputStream in = getContentStream(FILE_CARD_CPC_ONLY_OK)) {
            PARSER.parseXmlStream(in);
            FoundOffer offer = (FoundOffer) PARSER.getOffers().get(0);

            assertNotNull("Offer must contain card_cpc recommendations", offer.getRecommendationBlock(RecommendationType.CARD));
            assertTrue("Default recommendations must be of type card_cpc", offer.getDefaultRecommendationBlock().getRecommendations() == offer.getRecommendationBlock(RecommendationType.CARD).getRecommendations());
            assertNotNull("Offer recommendation block must not be null if there are cpc recommendations", offer.getRecommendations());
        }
    }

    @Test
    void test_parser_should_parseCpaRecommendations_when_xmlIsOk() throws IOException, SAXException {
        try (InputStream in = getContentStream(FILE_CARD_CPA_ONLY_OK)) {
            PARSER.parseXmlStream(in);
            FoundOffer offer = (FoundOffer) PARSER.getOffers().get(0);

            assertNotNull("Offer must contain card_cpa recommendations", offer.getRecommendationBlock(RecommendationType.CARD_CPA));
            //if no cpc tag data then no default block is null .. is it ok?
            assertNull("Default recommendations must be null if there are no cpc recommendations", offer.getDefaultRecommendationBlock().getRecommendations());
        }
    }


    @Test
    void test_parser_should_handleRecErrorCode_when_recommendationContainsErrorMarker() throws IOException, SAXException {
        try (InputStream in = getContentStream(FILE_RECOMMENDATIONS_WITH_CODE_ERRORS)) {
            PARSER.parseXmlStream(in);

            Recommendation cpcRecWithErrorCode = ((FoundOffer) PARSER.getOffers().get(0))
                    .getRecommendationBlock(RecommendationType.CARD)
                    .getRecommendations()
                    .get(1);

            assertThat(cpcRecWithErrorCode.getCode(), is(1));
            assertNull(cpcRecWithErrorCode.getBid());
            assertNull(cpcRecWithErrorCode.getFee());

            Recommendation cpaRecWithErrorCode = ((FoundOffer) PARSER.getOffers().get(0))
                    .getRecommendationBlock(RecommendationType.CARD_CPA)
                    .getRecommendations()
                    .get(1);

            assertThat(cpaRecWithErrorCode.getCode(), is(1));
            assertNull(cpaRecWithErrorCode.getBid());
            assertNull(cpaRecWithErrorCode.getFee());
        }
    }

    @Test
    void test_parser_should_returnEmptyOffersCollection_when_xmlHasEmptyResultsTag() throws IOException, SAXException {
        try (InputStream in = getContentStream(FILE_EMPTY_RESULTS_TAG)) {
            PARSER.parseXmlStream(in);

            List<FoundOffer> shopOffers = PARSER.getOffers();
            assertThat(shopOffers, hasSize(0));
        }
    }

    @Test
    void test_parser_should_returnOfferWithoutRecommendations_when_xmlHasEmptySearchRecommendationsTag() throws IOException, SAXException {
        try (InputStream in = getContentStream(FILE_EMPTY_RECOMMENDATIONS)) {
            PARSER.parseXmlStream(in);

            FoundOffer offer = (FoundOffer) PARSER.getOffers().get(0);

            assertNull(
                    "Default recommendations must be null",
                    offer.getRecommendations()
            );

            assertNull(
                    "Card_cpc recommendations must be null",
                    offer.getRecommendationBlock(RecommendationType.CARD)
            );

            assertNull(
                    "Card_cpa recommendations must be null",
                    offer.getRecommendationBlock(RecommendationType.CARD_CPA)
            );
        }
    }

    @Test
    void test_parser_should_returnOfferWithoutRecommendations_when_xmlHasEmptyErrorMarkerInRecommendationsTag() throws IOException, SAXException {
        try (InputStream in = getContentStream(FILE_RECOMMENDATIONS_UNAVAILABLE)) {
            PARSER.parseXmlStream(in);
            FoundOffer offer = (FoundOffer) PARSER.getOffers().get(0);

            assertThat(
                    offer.getRecommendationBlock(RecommendationType.CARD).getError(),
                    is("unavailable")
            );

            assertThat(
                    offer.getRecommendationBlock(RecommendationType.CARD_CPA).getError(),
                    is("unavailable")
            );
        }
    }
}
