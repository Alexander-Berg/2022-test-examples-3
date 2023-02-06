package ru.yandex.direct.turbopages.client;

import java.util.Map;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Collections.singletonList;

public class TurbopagesClientTest extends TurbopagesClientTestBase{

    private static final String PREFIX_URL = "/check_urls?is_listing=true";
    private static final String ORIGINAL_URL = "https://testing.url.ru";
    private static final String TURBO_URL = "https://yandex.ru/turbo?text=listing" + ORIGINAL_URL;

    @Autowired
    TestingConfiguration testingConfiguration;

    @Autowired
    public ParallelFetcherFactory parallelFetcherFactory;

    @Test
    public void testSuccessfulRequest() {
        Map<String, String> result = turbopagesClient.checkUrls(singletonList(ORIGINAL_URL));
        softAssertions.assertThat(result).hasSize(1);
        softAssertions.assertThat(result.get(ORIGINAL_URL)).isEqualTo(TURBO_URL);
    }

    @Override
    protected Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getPath()).startsWith(PREFIX_URL);
                return new MockResponse().setBody(JsonUtils
                        .toJson(singletonList(new CheckUrlsResponse().withOriginalUrl(ORIGINAL_URL).withTurboUrl(TURBO_URL))));
            }
        };
    }
}
