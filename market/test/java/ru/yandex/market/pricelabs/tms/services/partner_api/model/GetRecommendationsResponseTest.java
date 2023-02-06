package ru.yandex.market.pricelabs.tms.services.partner_api.model;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.misc.Utils;

@Slf4j
class GetRecommendationsResponseTest {

    @Test
    void testParseResponse() {
        var response = Utils.fromJsonResource("tms/services/partner_api/model/recommendations.json",
                GetRecommendationsResponse.class);
        log.info("Response: {}", response);

        var recs = new Recommendations();
        recs.setMinBid(1.09);
        recs.setBid(1.33);
        recs.setDontPullUpBids(true);
        recs.setOfferId("j100097");
        recs.setFeedId(2102L);

        var rec = new Recommendation();
        rec.setPosRecommendations(List.of());
        rec.setError(RecommendationBlockError.UNKNOWN);
        recs.setMarketSearch(rec);

        Assertions.assertEquals(new GetRecommendationsResponse(List.of(recs)), response);
    }
}
