package ru.yandex.market.pricelabs.tms.api;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.generated.server.pub.model.CommonRecommendations;
import ru.yandex.market.pricelabs.generated.server.pub.model.GetPhraseResponse;
import ru.yandex.market.pricelabs.generated.server.pub.model.GetPhraseResponseResult;
import ru.yandex.market.pricelabs.generated.server.pub.model.GetRecommendationsResponse;
import ru.yandex.market.pricelabs.generated.server.pub.model.GetRecommendationsResponseResult;
import ru.yandex.market.pricelabs.generated.server.pub.model.SingleBid;
import ru.yandex.market.pricelabs.generated.server.pub.model.SingleRecommendation;
import ru.yandex.market.pricelabs.generated.server.pub.model.TopQueryRecommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.pricelabs.tms.services.market_report.model.recommendations.SearchResultTest.readSearchResult;

@Slf4j
class MessageTransformersTest {

    @Test
    void testParseCard() {
        var result = readSearchResult("tms/services/market_report/model/recommendations/result-card.json");
        assertNotNull(result);
        log.info("Result: {}", result);
        var singleResult = result.getSearchResults().getResults().get(0);
        singleResult.setFeedOfferId("1-offer");

        var transformed = MessageTransformers.transformToRecommendations(singleResult);
        assertNotNull(transformed);
        log.info("Result: {}", transformed);

        var bids = IntStream.rangeClosed(1, 10).mapToObj(pos -> {
            var bid = new SingleBid();
            bid.setBid(pos == 1 ? 0.02 : 0.01);
            bid.setPos(pos);
            bid.setCode(0);
            return bid;
        }).collect(Collectors.toList());

        var card = new SingleRecommendation();
        card.setTopOffersCount(3);
        card.setModelCount(0);
        card.setCurrentPosTop(0);
        card.setCurrentPosAll(0);
        card.setPosRecommendations(bids);

        var rec = new CommonRecommendations();
        rec.setBid(0.58);
        rec.setDontPullUpBids(true);
        rec.setFeedId(1);
        rec.setOfferId("offer");
        rec.setMinBid(0.01);
        rec.setName("Полупромышленный кондиционер Hyundai H-ALD3-48H для серверной");
        rec.setModelCard(card);

        var expect = new GetRecommendationsResponse()
                .status("OK")
                .result(new GetRecommendationsResponseResult()
                        .recommendations(rec));
        assertEquals(expect, transformed);
    }


    @Test
    void testParseSearch() {
        var result = readSearchResult("tms/services/market_report/model/recommendations/result-search.json");
        assertNotNull(result);
        log.info("Result: {}", result);
        var singleResult = result.getSearchResults().getResults().get(0);
        singleResult.setFeedOfferId("1-offer");

        var transformed = MessageTransformers.transformToRecommendations(singleResult);
        assertNotNull(transformed);
        log.info("Result: {}", transformed);

        var bids = IntStream.rangeClosed(1, 12).mapToObj(pos -> {
            var bid = new SingleBid();
            bid.setBid(0.06);
            bid.setPos(pos);
            bid.setCode(0);
            return bid;
        }).collect(Collectors.toList());

        var search = new SingleRecommendation();
        search.setTopOffersCount(0);
        search.setModelCount(0);
        search.setCurrentPosTop(0);
        search.setCurrentPosAll(1);
        search.setPosRecommendations(bids);

        var rec = new CommonRecommendations();
        rec.setBid(0.06);
        rec.setDontPullUpBids(true);
        rec.setFeedId(1);
        rec.setOfferId("offer");
        rec.setMinBid(0.06);
        rec.setName("тажин Pomi D'Oro Nero Naturale TL2498");
        rec.setMarketSearch(search);

        var expect = new GetRecommendationsResponse()
                .status("OK")
                .result(new GetRecommendationsResponseResult()
                        .recommendations(rec));
        assertEquals(expect, transformed);
    }

    @Test
    void testParseTopSearch() {
        var result = readSearchResult("tms/services/market_report/model/recommendations/result-top.json");
        assertNotNull(result);
        log.info("Result: {}", result);
        var singleResult = result.getSearchResults().getResults().get(0);
        singleResult.setFeedOfferId("1-offer");

        var transformed = MessageTransformers.transformToPhrases(singleResult);
        assertNotNull(transformed);
        log.info("Result: {}", transformed);


        var bids = IntStream.rangeClosed(1, 12).mapToObj(pos -> {
            var bid = new SingleBid();
            bid.setBid(0.);
            bid.setPos(pos);
            bid.setCode(2);
            return bid;
        }).collect(Collectors.toList());

        var q1 = new TopQueryRecommendation();
        q1.setModelCount(3);
        q1.setAverageOfferPos(27);
        q1.setOfferShowCount(3);
        q1.setCurrentPosAll(0);
        q1.setQueryShowCount(66);
        q1.setType(TopQueryRecommendation.TypeEnum.ALL);
        q1.setText("VENTA LW25 (ЧЕРНЫЙ)");
        q1.setPositions(bids);

        var q2 = new TopQueryRecommendation();
        q2.setModelCount(13);
        q2.setAverageOfferPos(40);
        q2.setOfferShowCount(7);
        q2.setCurrentPosAll(0);
        q2.setQueryShowCount(119);
        q2.setType(TopQueryRecommendation.TypeEnum.ALL);
        q2.setText("Venta LW15");
        q2.setPositions(bids);

        var q3 = new TopQueryRecommendation();
        q3.setModelCount(3);
        q3.setAverageOfferPos(27);
        q3.setOfferShowCount(3);
        q3.setCurrentPosAll(0);
        q3.setQueryShowCount(66);
        q3.setType(TopQueryRecommendation.TypeEnum.OFFER);
        q3.setText("VENTA LW25 (ЧЕРНЫЙ)");
        q3.setPositions(bids);

        var q4 = new TopQueryRecommendation();
        q4.setModelCount(13);
        q4.setAverageOfferPos(40);
        q4.setOfferShowCount(7);
        q4.setCurrentPosAll(0);
        q4.setQueryShowCount(119);
        q4.setType(TopQueryRecommendation.TypeEnum.OFFER);
        q4.setText("Venta LW15");
        q4.setPositions(bids);


        var rec = new CommonRecommendations();
        rec.setBid(0.73);
        rec.setDontPullUpBids(true);
        rec.setFeedId(1);
        rec.setOfferId("offer");
        rec.setMinBid(0.73);
        rec.setName("Мойка воздуха Venta Venta black/white, LW62 WiFi weiss");
        rec.setQueries(List.of(q1, q2, q3, q4));

        var expect = new GetPhraseResponse()
                .status("OK")
                .result(new GetPhraseResponseResult()
                        .topRecommendations(rec));
        assertEquals(expect, transformed);
    }

}
