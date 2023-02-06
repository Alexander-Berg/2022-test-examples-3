package ru.yandex.market.live;


import java.util.List;

import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.live.model.LiveStreamingData;
import ru.yandex.market.live.model.LiveStreamingPresenter;
import ru.yandex.market.live.model.LiveStreamingPreview;
import ru.yandex.market.pers.test.http.HttpClientMockUtils;

import static org.mockito.Mockito.mock;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withQueryParam;

class LiveStreamingTarantinoClientTest {
    private HttpClient httpClient = mock(HttpClient.class);
    private LiveStreamingTarantinoClient tarantinoClient =
            new LiveStreamingTarantinoClient("http://target:90",
                    new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient)));

    @Test
    void getLiveStreamingInfoForBlogger() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/live/tarantino_blogger_response.json",
                withQueryParam("page_id", 1234));
        LiveStreamingData liveStreamingData = tarantinoClient.getLiveStreamingInfo(1234);
        LiveStreamingPresenter presenter = liveStreamingData.getPresenter();
        Assertions.assertEquals("Алексей Йоку", presenter.getName());
        Assertions.assertEquals("Ведущий, креативный продюсер, фотограф-документалист", presenter.getDescription());
        Assertions.assertEquals("//avatars.mds.yandex.net/get-marketcms/1779479/img-a6c17ed3-c558-4466-8cce-d136bec61e93.png/optimize", presenter.getImage().getUrl());
        Assertions.assertEquals(List.of(649703010L, 536106094L, 662892201L),
                liveStreamingData.getSkuList());
    }

    @Test
    void getScheduledLiveStreams() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/live/scheduled_streams.json");
        List<LiveStreamingPreview> liveStreamingPreviews = tarantinoClient.getScheduledLiveStreams();
        Assertions.assertEquals(2, liveStreamingPreviews.size());
    }

}
