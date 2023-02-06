package ru.yandex.market.api.partner.controllers.feed;

import java.net.URISyntaxException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriTemplate;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;

@ParametersAreNonnullByDefault
abstract class AbstractFeedLogControllerV2Test extends FunctionalTest {

    @Nonnull
    ResponseEntity<String> requestCampaignFeedIndexLogs(
            long campaignId,
            long feedId,
            Format format,
            Map<String, String> queryParams) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(
                new UriTemplate("{base}/campaigns/{campaignId}/feeds/{feedId}/index-logs.{format}")
                        .expand(ImmutableMap.<String, String>builder()
                                .put("base", urlBasePrefix)
                                .put("campaignId", String.valueOf(campaignId))
                                .put("feedId", String.valueOf(feedId))
                                .put("format", format.formatName())
                                .build()));
        queryParams.forEach(uriBuilder::addParameter);

        return FunctionalTestHelper.makeRequest(uriBuilder.build(), HttpMethod.GET, format);
    }

}
