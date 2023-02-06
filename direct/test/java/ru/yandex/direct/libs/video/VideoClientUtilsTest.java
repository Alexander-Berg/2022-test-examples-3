package ru.yandex.direct.libs.video;


import java.util.List;

import org.junit.Test;

import ru.yandex.direct.libs.video.model.VideoBanner;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.libs.video.VideoClientUtils.buildUrlForQueryParam;
import static ru.yandex.direct.libs.video.VideoClientUtils.buildUrlsQueryParam;
import static ru.yandex.direct.libs.video.VideoClientUtils.getUniformUrl;

public class VideoClientUtilsTest {
    private static final String YOUTUBE_URL = "https://www.youtube.com/watch?v=0hCBBnZI2AU";
    private static final String YOUTUBE_URL_SECOND = "https://www.youtube.com/watch?v=61wTJhg7N5o";
    private static final String EFIR_URL = "https://yandex.ru/efir?stream_id=486e8c6ecba13d90b5f20372848a9082";
    private static final String EFIR_URL_SECOND = "https://yandex.ru/efir?stream_id=40e6fd6b7f91283c9beaa2d0ada317d1";
    private static final String EFIR_URL_WITH_ADDITIONAL_QUERY_PARAMS =
            "https://yandex.ru/efir?from=efir&from_block=ya_organic_results&stream_id=486e8c6ecba13d90b5f20372848a9082";
    private static final String EFIR_URL_WITHOUT_STREAM_ID = "https://yandex.ru/efir?from=efir&from_block=ya_organic_results";
    private static final String EFIR_URL_WITH_EMPTY_QUERY_PARAMETERS = "https://yandex.ru/efir?";
    private static final String EFIR_URL_WITHOUT_QUERY_PARAMETERS = "https://yandex.ru/efir";
    private static final String EFIR_URL_BY = "https://yandex.by/efir?stream_id=486e8c6ecba13d90b5f20372848a9082";
    private static final String EFIR_URL_BY_WITH_ADDITIONAL_PARAMS =
            "https://yandex.by/efir?from=efir&from_block=ya_organic_results&stream_id=486e8c6ecba13d90b5f20372848a9082";
    private static final String EFIR_URL_WWW = "https://www.yandex.ru/efir?stream_id=486e8c6ecba13d90b5f20372848a9082";
    private static final String EFIR_URL_WWW_WITH_ADDITIONAL_PARAMS =
            "https://www.yandex.ru/efir?from=efir&from_block=ya_organic_results&stream_id=486e8c6ecba13d90b5f20372848a9082";
    private static final String FRONTEND_VH_URL = "frontend.vh.yandex.ru/player/15247200752434461202";

    @Test
    public void buildUrlsQueryParam_OneVideoUrl_UrlsConcatenatedProperly() {
        assertThat(buildUrlsQueryParam(List.of(YOUTUBE_URL)))
                .isEqualTo("(url:https://www.youtube.com/watch?v=0hCBBnZI2AU)");
    }

    @Test
    public void buildUrlsQueryParam_OneEfirVideoUrl_UrlsConcatenatedProperly() {
        assertThat(buildUrlsQueryParam(List.of(EFIR_URL)))
                .isEqualTo("(uuid:486e8c6ecba13d90b5f20372848a9082)");
    }

    @Test
    public void buildUrlsQueryParam_SeveralVideoUrls_UrlsConcatenatedProperly() {
        assertThat(buildUrlsQueryParam(
                List.of(YOUTUBE_URL, EFIR_URL, YOUTUBE_URL_SECOND, EFIR_URL_SECOND)))
                .isEqualTo("(url:https://www.youtube.com/watch?v=0hCBBnZI2AU) | "
                        + "(uuid:486e8c6ecba13d90b5f20372848a9082) | "
                        + "(url:https://www.youtube.com/watch?v=61wTJhg7N5o) | "
                        + "(uuid:40e6fd6b7f91283c9beaa2d0ada317d1)");
    }

    @Test
    public void buildUrlForQueryParam_NotEfirUrl_UnchangedUrlReturned() {
        assertThat(buildUrlForQueryParam(YOUTUBE_URL)).isEqualTo("(url:https://www.youtube.com/watch?v=0hCBBnZI2AU)");
    }

    @Test
    public void buildUrlForQueryParam_EfirUrl_StreamIdReturned() {
        assertThat(buildUrlForQueryParam(EFIR_URL)).isEqualTo("(uuid:486e8c6ecba13d90b5f20372848a9082)");
    }

    @Test
    public void buildUrlForQueryParam_EfirUrlBy_StreamIdReturned() {
        assertThat(buildUrlForQueryParam(EFIR_URL_BY)).isEqualTo("(uuid:486e8c6ecba13d90b5f20372848a9082)");
    }

    @Test
    public void buildUrlForQueryParam_EfirUrlWww_StreamIdReturned() {
        assertThat(buildUrlForQueryParam(EFIR_URL_WWW)).isEqualTo("(uuid:486e8c6ecba13d90b5f20372848a9082)");
    }

    @Test
    public void buildUrlForQueryParam_EfirUrlWithOtherQueryParameters_StreamIdReturned() {
        assertThat(buildUrlForQueryParam(EFIR_URL_WITH_ADDITIONAL_QUERY_PARAMS))
                .isEqualTo("(uuid:486e8c6ecba13d90b5f20372848a9082)");
    }

    @Test
    public void buildUrlForQueryParam_EfirUrlWithoutStreamId_UnchangedUrlReturned() {
        assertThat(buildUrlForQueryParam(EFIR_URL_WITHOUT_STREAM_ID))
                .isEqualTo("(url:https://yandex.ru/efir?from=efir&from_block=ya_organic_results)");
    }

    @Test
    public void buildUrlForQueryParam_EfirUrlWithEmptyQueryParameters_UnchangedUrlReturned() {
        assertThat(buildUrlForQueryParam(EFIR_URL_WITH_EMPTY_QUERY_PARAMETERS))
                .isEqualTo("(url:https://yandex.ru/efir?)");
    }

    @Test
    public void buildUrlForQueryParam_EfirUrlWithoutQueryParameters_UnchangedUrlReturned() {
        assertThat(buildUrlForQueryParam(EFIR_URL_WITHOUT_QUERY_PARAMETERS))
                .isEqualTo("(url:https://yandex.ru/efir)");
    }

    @Test
    public void getUniformUrl_NotEfirUrl_UnchangedUrlReturned() {
        VideoBanner youtubeBanner = new VideoBanner()
                .setUrl("www.youtube.com/watch?v=0hCBBnZI2AU")
                .setVisibleUrl("http://www.youtube.com/watch?v=0hCBBnZI2AU");
        assertThat(getUniformUrl(youtubeBanner)).isEqualTo("www.youtube.com/watch?v=0hCBBnZI2AU");
    }

    @Test
    public void getUniformUrl_EfirUrl_UniformEfirUrlReturned() {
        VideoBanner efirBanner = new VideoBanner()
                .setUrl(FRONTEND_VH_URL)
                .setVisibleUrl(EFIR_URL_WITH_ADDITIONAL_QUERY_PARAMS);
        assertThat(getUniformUrl(efirBanner))
                .isEqualTo(EFIR_URL);
    }

    @Test
    public void getUniformUrl_EfirUrlBy_UniformEfirUrlReturned() {
        VideoBanner efirBanner = new VideoBanner()
                .setUrl(FRONTEND_VH_URL)
                .setVisibleUrl(EFIR_URL_BY_WITH_ADDITIONAL_PARAMS);
        assertThat(getUniformUrl(efirBanner))
                .isEqualTo(EFIR_URL_BY);
    }

    @Test
    public void getUniformUrl_EfirUrlWww_UniformEfirUrlReturned() {
        VideoBanner efirBanner = new VideoBanner()
                .setUrl(FRONTEND_VH_URL)
                .setVisibleUrl(EFIR_URL_WWW_WITH_ADDITIONAL_PARAMS);
        assertThat(getUniformUrl(efirBanner))
                .isEqualTo(EFIR_URL_WWW);
    }

    @Test
    public void getUniformUrl_EfirUrlWithoutStreamId_NotUniformEfirUrlReturned() {
        VideoBanner efirBanner = new VideoBanner()
                .setUrl(FRONTEND_VH_URL)
                .setVisibleUrl(EFIR_URL_WITHOUT_STREAM_ID);
        assertThat(getUniformUrl(efirBanner))
                .isEqualTo(EFIR_URL_WITHOUT_STREAM_ID);
    }

    @Test
    public void getUniformUrl_EfirUrlWithEmptyQueryParameters_NotUniformEfirUrlReturned() {
        VideoBanner efirBanner = new VideoBanner()
                .setUrl(FRONTEND_VH_URL)
                .setVisibleUrl(EFIR_URL_WITH_EMPTY_QUERY_PARAMETERS);
        assertThat(getUniformUrl(efirBanner))
                .isEqualTo(EFIR_URL_WITH_EMPTY_QUERY_PARAMETERS);
    }

    @Test
    public void getUniformUrl_EfirUrlWithoutQueryParameters_NotUniformEfirUrlReturned() {
        VideoBanner efirBanner = new VideoBanner()
                .setUrl(FRONTEND_VH_URL)
                .setVisibleUrl(EFIR_URL_WITHOUT_QUERY_PARAMETERS);
        assertThat(getUniformUrl(efirBanner))
                .isEqualTo(EFIR_URL_WITHOUT_QUERY_PARAMETERS);
    }

    @Test
    public void getUniformUrl_BannerWithoutVisibleUrl_UnchangedUrlReturned() {
        VideoBanner efirBanner = new VideoBanner()
                .setUrl(FRONTEND_VH_URL);
        assertThat(getUniformUrl(efirBanner))
                .isEqualTo(FRONTEND_VH_URL);
    }
}
