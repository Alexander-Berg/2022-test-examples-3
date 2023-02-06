package ru.yandex.market.api.partner.controllers.feed;

import java.net.URISyntaxException;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriTemplate;

import ru.yandex.common.util.StringUtils;
import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.core.IndexerApiClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Date: 07.09.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class FeedControllerV2Test extends FunctionalTest {

    @Autowired
    @Qualifier("samovarLogbrokerService")
    private LogbrokerService samovarLogbrokerService;

    @Autowired
    private LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher;

    @Autowired
    private IndexerApiClient indexerApiClient;

    @DisplayName("Принудительное обновление информации по фидам через самовар")
    @DbUnitDataSet(before = {"FeedControllerV2Test.triggerFeedRefresh.before.csv"})
    @ParameterizedTest(name = "feedId = {0}, campaignId = {1}")
    @CsvSource({
            "1,10897,test.login,test.password",
            "4,10897,,",
            "5,10898,,",
            "12,11113,,",
            "15,11115,,",
            "16,11200,,"
    })
    void triggerFeedRefresh_testCorrectShopFeedForSamovar_invokeLogbroker(String feedId,
                                                                          String campaignId,
                                                                          String login,
                                                                          String password) throws URISyntaxException {
        ResponseEntity<String> responseEntity = refreshCampaignFeed(campaignId, feedId);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        var argumentCaptor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        verify(feedProcessorUpdateLogbrokerEventPublisher, times(1))
                .publishEvent(argumentCaptor.capture());
        assertEquals(1, argumentCaptor.getAllValues().size());

        FeedProcessorUpdateRequestEvent samovarEvent = argumentCaptor.getValue();
        assertTrue(samovarEvent.toString().contains("test.feed.url"));

        var eventLogin = samovarEvent.getPayload().getFeed().getLogin();
        var eventPassword = samovarEvent.getPayload().getFeed().getPassword();
        if (login == null) {
            assertEquals(StringUtils.EMPTY, eventLogin);
            assertEquals(StringUtils.EMPTY, eventPassword);
        } else {
            assertEquals(login, eventLogin);
            assertEquals(password, eventPassword);
        }
    }

    @DisplayName("Невозможность принудительного обновления информации по фидам через самовар")
    @DbUnitDataSet(before = {"FeedControllerV2Test.triggerFeedRefresh.before.csv"})
    @ParameterizedTest(name = "feedId = {0}, campaignId = {1}")
    @CsvSource({
            "2,10897",
            "3,10897",
            "6,10899",
            "7,10900"
    })
    void triggerFeedRefresh_testWrongShopFeedForSamovar_threeTimeInvokeLogbroker(String feedId, String campaignId)
            throws URISyntaxException {
        ResponseEntity<String> responseEntity = refreshCampaignFeed(campaignId, feedId);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        verify(feedProcessorUpdateLogbrokerEventPublisher, never()).publishEvent(any());
    }

    @SuppressWarnings("SameParameterValue")
    @Nonnull
    ResponseEntity<String> refreshCampaignFeed(String campaignId, String feedId) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(
                new UriTemplate("{base}/campaigns/{campaignId}/feeds/{feedId}/refresh")
                        .expand(ImmutableMap.<String, String>builder()
                                .put("base", urlBasePrefix)
                                .put("campaignId", campaignId)
                                .put("feedId", feedId)
                                .build()));

        return FunctionalTestHelper.makeRequest(uriBuilder.build(), HttpMethod.POST, Format.XML);
    }
}
