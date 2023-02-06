package ru.yandex.market.util.feeddispatcher;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.report.indexer.model.OfferDetails;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.util.TestSerializationService;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;

@Component
public class FeedDispatcherConfigurer {
    private final WireMockServer feedDispatcherMock;
    private final TestSerializationService testSerializationService;

    public FeedDispatcherConfigurer(WireMockServer feedDispatcherMock,
                                    TestSerializationService testSerializationService) {
        this.feedDispatcherMock = feedDispatcherMock;
        this.testSerializationService = testSerializationService;
    }

    public void configureFeedDispatcher(List<Pair<FeedOfferId, OfferDetails>> offers) throws IOException {
        if (CollectionUtils.isEmpty(offers)) {
            return;
        }

        for (Pair<FeedOfferId, OfferDetails> offerPair : offers) {
            feedDispatcherMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/v2/smart/offer"))
                    .withQueryParam("feed_id", equalTo(String.valueOf(offerPair.getFirst().getFeedId())))
                    .withQueryParam("offer_id", equalTo(offerPair.getFirst().getId()))
                    .willReturn(ResponseDefinitionBuilder.responseDefinition()
                    .withBody(testSerializationService.serializeXml(offerPair.getSecond()))));
        }

    }
}
