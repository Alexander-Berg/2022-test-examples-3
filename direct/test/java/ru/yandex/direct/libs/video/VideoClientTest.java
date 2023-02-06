package ru.yandex.direct.libs.video;

import java.time.Duration;
import java.util.List;

import one.util.streamex.StreamEx;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.libs.video.model.VideoBanner;

import static org.assertj.core.api.Assertions.assertThat;

public class VideoClientTest {
    private static final List<String> testUrls = List.of("https://www.youtube.com/watch?v=0hCBBnZI2AU",
            "https://www.youtube.com/watch?v=61wTJhg7N5o",
            "https://www.youtube.com/watch?v=-9VU96DHzbA");
    private static final VideoClientConfig VIDEO_CLIENT_CONFIG = new VideoClientConfig(
            "https://yandex.ru/video/search", 1, Duration.ofSeconds(15), Duration.ofSeconds(60), 1);

    @Test
    @Ignore("Ходит в реальную систему")
    public void smokeTest() {
        VideoClient client = new VideoClient(new DefaultAsyncHttpClient(), VIDEO_CLIENT_CONFIG);
        List<VideoBanner> result = client.getMeta(testUrls,
                "correlation", 1L, null, "client");
        assertThat(result).isNotEmpty();
    }

    @Test
    @Ignore("Ходит в реальную систему")
    public void severalUrlsSentToClient_dataIsReceivedInTheSameOrder() {
        VideoClient client = new VideoClient(new DefaultAsyncHttpClient(), VIDEO_CLIENT_CONFIG);
        List<VideoBanner> result = client.getMeta(testUrls,
                "correlation", 1L, null, "client");

        assertThat(StreamEx.of(result).map(VideoBanner::getUrl).toList())
                .isEqualTo(StreamEx.of(testUrls).map(url -> url.substring("https://".length())).toList());
    }

    @Test
    @Ignore("Ходит в реальную систему")
    public void severalUrlsOneIsInvalid_dataIsReceived() {
        VideoClient client = new VideoClient(new DefaultAsyncHttpClient(), VIDEO_CLIENT_CONFIG);
        List<String> modifiedTestUrls = List.of(testUrls.get(0) + "1", testUrls.get(1), testUrls.get(2));
        List<VideoBanner> result = client.getMeta(modifiedTestUrls,
                "correlation", 1L, null, "client");

        assertThat(result.get(0)).isNull();
    }
}
