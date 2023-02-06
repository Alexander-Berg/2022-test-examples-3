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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasBid;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasFee;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasHyperId;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasMinBid;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasMinFee;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasPullupFlag;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasWareMd5;

public class AuctionMarketSearchMarketReportXmlParserTest extends AbstractParserTest {

    private static final String FILE_OK = "type-search_ok.xml";
    private static final String FILE_RECOMMENDATIONS_WITH_CODE_ERRORS = "type-search_code_errors.xml";
    private static final String FILE_EMPTY_RESULTS_TAG = "search_results_is_empty.xml";
    private static final String FILE_EMPTY_RECOMMENDATIONS = "type-search_recommendations_tag_is_empty.xml";
    private static final String FILE_RECOMMENDATIONS_UNAVAILABLE = "type-search_recommendations_unavailable.xml";
    private static AuctionMarketSearchMarketReportXmlParserSettings PARSER_SETTINGS;
    private AuctionMarketSearchMarketReportXmlParser PARSER;

    public static AuctionMarketSearchMarketReportXmlParserSettings createMinimalTestSettings() {
        AuctionMarketSearchMarketReportXmlParserSettings settings = new AuctionMarketSearchMarketReportXmlParserSettings();
        settings.setTagSearchResults("search_results");

        settings.setTagOffers("offers");

        settings.setTagOffer("offer");
        settings.setTagHyperId("hyper_id");
        settings.setTagWareMd5("ware_md5");
        settings.setTagActualBids("bids");
        settings.setAttrActualFee("fee");
        settings.setAttrActualBid("bid");

        settings.setTagRecommendations("recommendations");

        settings.setTagRecommendationPosition("position");
        settings.setAttrRecommendationCurrentPosAll("current-pos-all");
        settings.setAttrRecommendationModelCount("model-count");
        settings.setAttrRecommendationMinBid("min-bid");
        settings.setAttrRecommendationMinFee("min-fee");

        settings.setAttrRecommendationError("error");

        settings.setAttrRecommendationBid("bid");
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
        PARSER = new AuctionMarketSearchMarketReportXmlParser(PARSER_SETTINGS);
    }

    @Test
    void test_parser_should_parseOffer_when_xmlIsOk() throws IOException, SAXException {
        try (InputStream in = getContentStream(FILE_OK)) {
            PARSER.parseXmlStream(in);
            SearchResults actual = PARSER.getSearchResults();

            List<FoundOffer> shopOffers = PARSER.getOffers();
            assertThat(shopOffers, hasSize(1));
            assertThat(actual.getTotalOffers(), is(1));

            assertThat(shopOffers.get(0),
                    allOf(
                            hasFee(3400),
                            hasBid(1200),
                            hasPullupFlag(true),
                            hasWareMd5("RpuauqOcG5z9flabsnzGjA"),
                            hasHyperId(7021673L)
                    )
            );
        }
    }

    @Test
    void test_parser_should_parseRecommendations_when_xmlIsOk() throws IOException, SAXException {
        try (InputStream in = getContentStream(FILE_OK)) {
            PARSER.parseXmlStream(in);
            FoundOffer offer = PARSER.getOffers().get(0);

            assertThat(offer,
                    allOf(
                            hasMinBid(29),
                            hasMinFee(200),
                            hasBid(1200),
                            hasFee(3400)
                    )
            );

            assertNotNull("Offer recommendation block must not be null", offer.getRecommendations());
            Map<Integer, Recommendation> actualRecommendations = offer.getRecommendations();
            Recommendation firstPosRec = actualRecommendations.get(1);
            assertThat(firstPosRec.getFee(), is(2000));
            assertThat(firstPosRec.getBid(), is(200));
            assertThat(firstPosRec.getCode(), is(0));

            FoundOffer.RecommendationBlock searchRecommendationBlock = offer.getRecommendationBlock(RecommendationType.MARKET_SEARCH);
            assertNotNull("Offer must contain MARKET_SEARCH recommendations block", searchRecommendationBlock);

            assertThat(searchRecommendationBlock.getModelCount(), is(10));
            assertThat(searchRecommendationBlock.getCurrentPosAll(), is(5));

            assertTrue("Default recommendations must be of type MARKET_SEARCH", offer.getDefaultRecommendationBlock().getRecommendations() == offer.getRecommendationBlock(RecommendationType.MARKET_SEARCH).getRecommendations());
        }
    }

    @Test
    void test_parser_should_handleRecErrorCode_when_recommendationContainsErrorMarker() throws IOException, SAXException {
        try (InputStream in = getContentStream(FILE_RECOMMENDATIONS_WITH_CODE_ERRORS)) {
            PARSER.parseXmlStream(in);

            Recommendation recWithErrorCode = PARSER.getOffers().get(0)
                    .getRecommendations()
                    .get(1);

            assertThat(recWithErrorCode.getCode(), is(1));
            assertNull(recWithErrorCode.getBid());
            assertNull(recWithErrorCode.getFee());
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

            FoundOffer offer = PARSER.getOffers().get(0);

            assertNull(
                    "Default recommendations must be null",
                    offer.getRecommendations()
            );
        }
    }

    @Test
    void test_parser_should_returnOfferWithoutRecommendations_when_xmlHasEmptyErrorMarkerInRecommendationsTag() throws IOException, SAXException {
        try (InputStream in = getContentStream(FILE_RECOMMENDATIONS_UNAVAILABLE)) {
            PARSER.parseXmlStream(in);
            FoundOffer offer = PARSER.getOffers().get(0);
            assertThat(offer.getDefaultRecommendationBlock().getError(), is("unavailable"));
        }
    }
}
