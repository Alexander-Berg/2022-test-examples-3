package ru.yandex.market.core.auction.marketreport;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.Recommendation;
import ru.yandex.market.common.report.model.SearchResults;
import ru.yandex.market.core.AbstractParserTest;
import ru.yandex.market.core.auction.recommend.ParallelSearchRecommendationParser;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.common.report.model.RecommendationType.SEARCH;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasBid;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasFee;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasHidd;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasMinBid;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasMinFee;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasName;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasPriceCurrency;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasPullupFlag;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasUrl;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasWareMd5;

/**
 * @author zoom
 */
public class ParallelSearchRecommendationParserTest extends AbstractParserTest {

    private static void assertOfferEquals(FoundOffer expected, FoundOffer actual) {
        assertThat(actual,
                allOf(
                        hasUrl(expected.getUrl()),
                        hasWareMd5(expected.getWareMd5()),
                        hasBid(expected.getBid()),
                        hasFee(expected.getFee()),
                        hasMinFee(expected.getMinFee()),
                        hasMinBid(expected.getMinBid()),
                        hasPullupFlag(expected.getPullUpBids()),
                        hasHidd(expected.getHyperCategoryId()),
                        hasName(expected.getName()),
                        hasPriceCurrency(expected.getPriceCurrency())
                )
        );

        FoundOffer.RecommendationBlock actualRecommendationBlock = actual.getRecommendationBlock(SEARCH);
        FoundOffer.RecommendationBlock expectedRecommendationBlock = expected.getRecommendationBlock(SEARCH);

        if (expectedRecommendationBlock == null) {
            assertThat("null recommendation block", actualRecommendationBlock, nullValue());
            return;
        }

        assertThat("not null recommendation block", actualRecommendationBlock, Matchers.notNullValue());

        assertThat(
                "current pos all",
                actualRecommendationBlock.getCurrentPosAll(),
                is(expectedRecommendationBlock.getCurrentPosAll())
        );

        Map<Integer, Recommendation> actualRecommendations = actualRecommendationBlock.getRecommendations();
        Map<Integer, Recommendation> expectedRecommendations = expectedRecommendationBlock.getRecommendations();
        if (expectedRecommendations == null) {
            assertNull("null actual recommendations", actualRecommendations);
            return;
        }
        assertNotNull("not null actual recommendations", actualRecommendations);
        assertThat("recommendations size", actualRecommendations.entrySet(), hasSize(expectedRecommendations.size()));
        for (Map.Entry<Integer, Recommendation> entry : expectedRecommendations.entrySet()) {
            assertBidEquals(entry.getKey(), entry.getValue(), actualRecommendations);
        }

    }

    private static void assertBidEquals(int position,
                                        Recommendation expected,
                                        Map<Integer, Recommendation> recommendations) {
        Recommendation actual = recommendations.get(position);
        assertThat("recommendation for pos " + position, actual, notNullValue());
        assertThat("bid for pos " + position, actual.getBid(), is(expected.getBid()));
        assertThat("code for pos " + position, actual.getCode(), is(expected.getCode()));
    }

    @Test
    void shouldReturnEmptyOfferListWhenXmlDoesNotContainAnyOffers() throws IOException {
        try (InputStream stream = getContentStream("empty-search-results.xml")) {
            ParallelSearchRecommendationParser parser = new ParallelSearchRecommendationParser();
            parser.parse(stream);
            List<FoundOffer> offers = parser.getOffers();
            assertThat(offers, Matchers.empty());
            SearchResults results = parser.getSearchResults();
            assertThat(results.getTotalOffers(), Matchers.is(0));
        }
    }

    @Test
    void shouldReturnOfferWithUnavailableRecommendationErrorWhenRecommendationUnavailable() throws IOException {
        try (InputStream stream = getContentStream("recommendations-unavailable.xml")) {
            ParallelSearchRecommendationParser parser = new ParallelSearchRecommendationParser();
            parser.parse(stream);

            SearchResults results = parser.getSearchResults();
            assertThat("total offers", results.getTotalOffers(), Matchers.is(1));

            List<FoundOffer> offers = parser.getOffers();
            assertThat(offers, Matchers.hasSize(1));

            FoundOffer actual = offers.get(0);
            FoundOffer expected = createExpectedOffer();
            expected.getRecommendationBlock(SEARCH).setCurrentPosAll(null);
            expected.setRecommendationBlock(SEARCH, null);
            assertOfferEquals(expected, actual);
            assertThat("error text", actual.getDefaultRecommendationBlock().getError(), is("unavailable"));
        }
    }

    @Nonnull
    private FoundOffer createExpectedOffer() {
        FoundOffer expected = new FoundOffer();
        expected.setName("Часы Casio EF-131D-7A");
        expected.setUrl("www.example.yandex.ru");
        expected.setHyperId(7021673L);
        expected.setHyperCategoryId(91259);
        expected.setWareMd5("RpuauqOcG5z9flabsnzGjA");
        expected.setPriceCurrency(Currency.RUR);
        expected.setBid(1100);
        expected.setFee(3400);
        expected.setMinBid(29);
        expected.setMinFee(200);
        expected.setPullUpBids(true);
        expected.setQualityFactor(BigDecimal.valueOf(0.612));
        FoundOffer.RecommendationBlock block = new FoundOffer.RecommendationBlock();
        expected.setRecommendationBlock(SEARCH, block);
        block.setCurrentPosAll(5);
        Map<Integer, Recommendation> recommendations = new HashMap<>();
        block.setRecommendations(recommendations);
        recommendations.put(1, new Recommendation(null, null, 200, 0));
        recommendations.put(2, new Recommendation(null, null, 175, 0));
        recommendations.put(3, new Recommendation(null, null, 150, 0));
        recommendations.put(4, new Recommendation(null, null, 125, 0));
        recommendations.put(5, new Recommendation(null, null, 100, 0));
        recommendations.put(6, new Recommendation(null, null, 80, 0));
        recommendations.put(7, new Recommendation(null, null, 70, 0));
        recommendations.put(8, new Recommendation(null, null, 50, 0));
        recommendations.put(9, new Recommendation(null, null, 45, 0));
        recommendations.put(10, new Recommendation(null, null, 29, 0));
        recommendations.put(11, new Recommendation(null, null, 23, 0));
        recommendations.put(12, new Recommendation(null, null, 15, 0));
        return expected;
    }

    @Test
    void shouldParseOkBecauseSmokeTest() throws IOException {
        try (InputStream stream = getContentStream("ok.xml")) {
            ParallelSearchRecommendationParser parser = new ParallelSearchRecommendationParser();
            parser.parse(stream);

            SearchResults results = parser.getSearchResults();
            assertThat("total offers", results.getTotalOffers(), Matchers.is(1));

            List<FoundOffer> offers = parser.getOffers();
            assertThat(offers, Matchers.hasSize(1));

            FoundOffer actual = offers.get(0);
            assertOfferEquals(createExpectedOffer(), actual);
        }
    }

    @Test
    void shouldNotReturnSearchRecommendationsWhenRecommendationsTagIsAbsent() throws IOException {
        try (InputStream stream = getContentStream("missing-search-recommendations-tag.xml")) {
            ParallelSearchRecommendationParser parser = new ParallelSearchRecommendationParser();
            parser.parse(stream);

            SearchResults results = parser.getSearchResults();
            assertThat("total offers", results.getTotalOffers(), Matchers.is(1));

            List<FoundOffer> offers = parser.getOffers();
            assertThat(offers, Matchers.hasSize(1));

            FoundOffer actual = offers.get(0);
            FoundOffer expected = createExpectedOffer();
            expected.setRecommendationBlock(SEARCH, null);
            assertOfferEquals(expected, actual);
        }
    }

    @Test
    void shouldReturnRecommendationWithErrorCode1ForAllPositions() throws IOException {
        try (InputStream stream = getContentStream("type-search-code-errors.xml")) {
            ParallelSearchRecommendationParser parser = new ParallelSearchRecommendationParser();
            parser.parse(stream);

            SearchResults results = parser.getSearchResults();
            assertThat("total offers", results.getTotalOffers(), Matchers.is(1));

            List<FoundOffer> offers = parser.getOffers();
            assertThat(offers, Matchers.hasSize(1));

            FoundOffer actual = offers.get(0);
            FoundOffer expected = createExpectedOffer();
            Map<Integer, Recommendation> recommendations = new HashMap<>();
            FoundOffer.RecommendationBlock recommendationBlock = expected.getRecommendationBlock(SEARCH);
            recommendationBlock.setRecommendations(recommendations);

            for (int i = 1; i < 13; i++) {
                recommendations.put(i, new Recommendation(null, null, 1));
            }
            assertOfferEquals(expected, actual);
        }
    }
}
