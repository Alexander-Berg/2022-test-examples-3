package ru.yandex.market.pers.author.tms.live.client;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.author.tms.live.client.model.zen.LiveDraftDto;
import ru.yandex.market.pers.author.tms.live.client.model.zen.ZenProduct;
import ru.yandex.market.pers.author.tms.live.client.model.zen.ZenStreamStatus;
import ru.yandex.market.pers.test.http.HttpClientMockUtils;

import static org.mockito.Mockito.mock;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.and;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withPath;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withQueryParam;

public class ZenClientTest {

    private static final String streamId = "1234";
    private static final String publisherId = "publisher-1234567";
    private static final String publicationId = "12c3f456cd7e891f11011c1a";

    private HttpClient httpClient = mock(HttpClient.class);
    private ZenClient zenClient =
            new ZenClient("http://target:90",
                    new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient)), "");

    public void liveStreamStatus(ZenStreamStatus expected) {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/live/zen/stream_status_" + expected.getValue() + ".json",
                and(
                        withPath("/internal-api/live-status"),
                        withQueryParam("streamId", streamId)));
        ZenStreamStatus zenStreamStatus = zenClient.liveStreamStatus(streamId);

        Assertions.assertEquals(expected, zenStreamStatus);
    }

    @Test
    public void liveStreamStatus() {
        Arrays.stream(ZenStreamStatus.values()).forEach(this::liveStreamStatus);
    }

    @Test
    public void liveStreamStatusIncorrect() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/live/zen/stream_status_incorrect.json",
                and(
                        withPath("/internal-api/live-status"),
                        withQueryParam("streamId", streamId)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> zenClient.liveStreamStatus(streamId));
    }


    @Test
    void createDraftLive() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/live/zen/draft_live.json",
                withPath("/internal-api/add-live"));

        LiveDraftDto draftLive = zenClient.createDraftLive(publisherId, "LIVE TITLE");
        Assertions.assertEquals(publicationId, draftLive.getPublicationId());
        Assertions.assertEquals(streamId, draftLive.getContent().getLiveMetaInfo().getLiveMetaIds().getStreamId());
    }

    @Test
    void startZenLiveStream() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/live/zen/stream_status_ok.json",
                and(
                        withPath("/internal-api/start-live-stream-and-publish"),
                        withQueryParam("publisherId", publisherId),
                        withQueryParam("publicationId", publicationId)));

        boolean success = zenClient.startZenLiveStream(publisherId, publicationId, "title", "description", "imageId");
        Assertions.assertTrue(success);
    }

    @Test
    void startZenLiveStreamNotOk() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/live/zen/stream_status_not_ok.json",
                and(
                        withPath("/internal-api/start-live-stream-and-publish"),
                        withQueryParam("publisherId", publisherId),
                        withQueryParam("publicationId", publicationId)));

        boolean success = zenClient.startZenLiveStream(publisherId, publicationId, "title", "description", "imageId");
        Assertions.assertFalse(success);
    }

    @Test
    void stopZenLiveStream() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/live/zen/stream_status_ok.json",
                and(
                        withPath("/internal-api/stop-live-stream"),
                        withQueryParam("publisherId", publisherId),
                        withQueryParam("publicationId", publicationId)));

        boolean success = zenClient.stopZenLiveStream(publisherId, publicationId);
        Assertions.assertTrue(success);
    }

    @Test
    void stopZenLiveStreamAlreadyFinished() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, 403, "/live/zen/stream_status_already_finished.json",
                and(
                        withPath("/internal-api/stop-live-stream"),
                        withQueryParam("publisherId", publisherId),
                        withQueryParam("publicationId", publicationId)));

        boolean success = zenClient.stopZenLiveStream(publisherId, publicationId);
        Assertions.assertTrue(success);
    }

    @Test
    void testAddSkus() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/live/zen/stream_status_ok.json",
                withPath("/internal-api/publisher/" + publisherId + "/publication/" +publicationId + "/products"));

        List<ZenProduct> products = List.of(new ZenProduct("title", "url", "market","logoUrl",123));
        boolean success = zenClient.addModelsForLive(publisherId, publicationId, products);
        Assertions.assertTrue(success);
    }
}
