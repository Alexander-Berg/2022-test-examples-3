package ru.yandex.market.pricelabs.tms.services.market_report.model.recommendations;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.misc.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class SearchResultTest {

    private static SearchResult expectCard() {
        var expectResult = new SearchResult();
        var offer = new Offer();
        var bids = new Bids();
        bids.setBidCents(58);
        bids.setPullToMinBid(true);
        offer.setBids(bids);
        offer.setTitle("Полупромышленный кондиционер Hyundai H-ALD3-48H для серверной");

        var recommendations = new Recommendations();
        recommendations.setMinBidCents(1);

        var recommendation = new Recommendation();
        recommendation.setBids(
                IntStream.rangeClosed(1, 10).mapToObj(pos -> {
                    var bid = new Bid();
                    bid.setBidCent(pos == 1 ? 2 : 1);
                    return bid;
                }).collect(Collectors.toList()));
        recommendation.setTopOffersCount(3);
        recommendations.setCardRecommendation(recommendation);

        expectResult.setRecommendations(recommendations);
        expectResult.setOffer(offer);
        return expectResult;
    }

    private static SearchResult expectSearch() {
        var expectResult = new SearchResult();
        var offer = new Offer();
        var bids = new Bids();
        bids.setBidCents(6);
        bids.setPullToMinBid(true);
        offer.setBids(bids);
        offer.setTitle("тажин Pomi D'Oro Nero Naturale TL2498");

        var recommendations = new Recommendations();
        recommendations.setMinBidCents(6);

        var recommendation = new Recommendation();
        recommendation.setCurrentPosAll(1);
        recommendation.setBids(
                IntStream.rangeClosed(1, 12).mapToObj(pos -> {
                    var bid = new Bid();
                    bid.setBidCent(6);
                    return bid;
                }).collect(Collectors.toList()));
        recommendations.setSearchRecommendation(recommendation);

        expectResult.setRecommendations(recommendations);
        expectResult.setOffer(offer);
        return expectResult;
    }

    private static WrappedResult wrap(SearchResult expectResult) {
        return new WrappedResult(new SearchResults(List.of(expectResult)));
    }

    @Test
    void testParseCard() {
        var result = readSearchResult("tms/services/market_report/model/recommendations/result-card.json");
        assertNotNull(result);
        log.info("Result: {}", result);

        assertEquals(wrap(expectCard()), result);
    }

    @Test
    void testParseCards() {
        var results = readSearchResult("tms/services/market_report/model/recommendations/result-cards.json");
        assertNotNull(results);
        log.info("Result: {}", results);

        var expect = expectCard();
        expect.setFeedOfferId("123-o1");
        assertEquals(wrap(expect), results);
    }

    @Test
    void testParseSearch() {
        var result = readSearchResult("tms/services/market_report/model/recommendations/result-search.json");
        assertNotNull(result);
        log.info("Result: {}", result);

        assertEquals(wrap(expectSearch()), result);
    }

    @Test
    void testParseSearches() {
        var results = readSearchResult("tms/services/market_report/model/recommendations/result-searches.json");
        assertNotNull(results);
        log.info("Result: {}", results);

        var expect = expectSearch();
        expect.setFeedOfferId("123-o1");
        assertEquals(wrap(expect), results);
    }

    @Test
    void testParseTopSearch() {
        var result = readSearchResult("tms/services/market_report/model/recommendations/result-top.json");
        assertNotNull(result);
        log.info("Result: {}", result);

        var expectResult = new SearchResult();
        var offer = new Offer();
        var bids = new Bids();
        bids.setBidCents(73);
        bids.setPullToMinBid(true);
        offer.setBids(bids);
        offer.setTitle("Мойка воздуха Venta Venta black/white, LW62 WiFi weiss");

        var recommendations = new Recommendations();
        recommendations.setMinBidCents(73);

        //

        var positions = IntStream.rangeClosed(1, 12).mapToObj(pos -> {
            var posObject = new Bid();
            posObject.setCode(2);
            return posObject;
        }).collect(Collectors.toList());

        var qr1 = new QueryRecommendation();
        qr1.setAverageOfferPosition(27.3333);
        qr1.setOfferShowCount(3);
        qr1.setQueryShowCount(66);
        qr1.setType("top_all");
        qr1.setQuery("VENTA LW25 (ЧЕРНЫЙ)");

        var qrs1 = new Recommendation();
        qr1.setSearchRecommendation(qrs1);
        qrs1.setModelCount(3);
        qrs1.setBids(positions);


        var qr2 = new QueryRecommendation();
        qr2.setAverageOfferPosition(40.2857);
        qr2.setOfferShowCount(7);
        qr2.setQueryShowCount(119);
        qr2.setType("top_all");
        qr2.setQuery("Venta LW15");

        var qrs2 = new Recommendation();
        qr2.setSearchRecommendation(qrs2);
        qrs2.setModelCount(13);
        qrs2.setBids(positions);


        var qr3 = new QueryRecommendation();
        qr3.setAverageOfferPosition(27.3333);
        qr3.setOfferShowCount(3);
        qr3.setQueryShowCount(66);
        qr3.setType("top_offer");
        qr3.setQuery("VENTA LW25 (ЧЕРНЫЙ)");

        var qrs3 = new Recommendation();
        qr3.setSearchRecommendation(qrs3);
        qrs3.setModelCount(3);
        qrs3.setBids(positions);


        var qr4 = new QueryRecommendation();
        qr4.setAverageOfferPosition(40.2857);
        qr4.setOfferShowCount(7);
        qr4.setQueryShowCount(119);
        qr4.setType("top_offer");
        qr4.setQuery("Venta LW15");

        var qrs4 = new Recommendation();
        qr4.setSearchRecommendation(qrs4);
        qrs4.setModelCount(13);
        qrs4.setBids(positions);

        recommendations.setQueryRecommendations(Stream.of(qr1, qr2, qr3, qr4)
                .map(TopQueriesRecommendation::new)
                .collect(Collectors.toList()));

        expectResult.setOffer(offer);
        expectResult.setRecommendations(recommendations);
        assertEquals(wrap(expectResult), result);
    }

    public static WrappedResult readSearchResult(String resource) {
        return Utils.fromJsonResource(resource, WrappedResult.class);
    }
}
