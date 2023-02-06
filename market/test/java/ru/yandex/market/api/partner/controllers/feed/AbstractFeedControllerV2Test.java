package ru.yandex.market.api.partner.controllers.feed;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.offers.searcher.ChunkedOffersSearcher;
import ru.yandex.market.common.report.model.SearchResults;

@ParametersAreNonnullByDefault
abstract class AbstractFeedControllerV2Test extends FunctionalTest {

    @Autowired
    private ChunkedOffersSearcher chunkedOffersSearcher;

    @BeforeEach
    public void simulateImportGenerations() {
        SearchResults result = new SearchResults();
        Mockito.when(chunkedOffersSearcher.asyncOffersRegardlessOfRegionChunked(Mockito.anyLong(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(new Pair<>(result, Collections.emptyList())));
    }

    @Nonnull
    ResponseEntity<String> requestCampaignFeed(long campaignId, long feedId, Format format) throws URISyntaxException {
        return requestCampaignFeed(campaignId, feedId, format, Collections.emptyMap());
    }

    @Nonnull
    ResponseEntity<String> requestCampaignFeed(
            long campaignId,
            long feedId,
            Format format,
            Map<String, String> queryParams) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(
                new UriTemplate("{base}/campaigns/{campaignId}/feeds/{feedId}.{format}")
                        .expand(ImmutableMap.<String, String>builder()
                                .put("base", urlBasePrefix)
                                .put("campaignId", String.valueOf(campaignId))
                                .put("feedId", String.valueOf(feedId))
                                .put("format", format.formatName())
                                .build()));
        queryParams.forEach(uriBuilder::addParameter);

        return FunctionalTestHelper.makeRequest(uriBuilder.build(), HttpMethod.GET, format);
    }

    @Nonnull
    ResponseEntity<String> requestCampaignFeeds(long campaignId, Format format) throws URISyntaxException {
        return requestCampaignFeeds(campaignId, format, Collections.emptyMap());
    }

    @Nonnull
    ResponseEntity<String> requestCampaignFeeds(
            long campaignId,
            Format format,
            Map<String, String> queryParams) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(
                new UriTemplate("{base}/campaigns/{campaignId}/feeds.{format}")
                        .expand(ImmutableMap.<String, String>builder()
                                .put("base", urlBasePrefix)
                                .put("campaignId", String.valueOf(campaignId))
                                .put("format", format.formatName())
                                .build()));
        queryParams.forEach(uriBuilder::addParameter);

        return FunctionalTestHelper.makeRequest(uriBuilder.build(), HttpMethod.GET, format);

    }
}
