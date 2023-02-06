package ru.yandex.direct.bannerstorage.client;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.JUnitSoftAssertions;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.bannerstorage.client.model.Creative;
import ru.yandex.direct.bannerstorage.client.model.Item;
import ru.yandex.direct.bannerstorage.client.model.ItemsRequest;
import ru.yandex.direct.utils.JsonUtils;

public class BannerStorageClientTest {

    private final static Logger logger = LoggerFactory.getLogger(BannerStorageClientTest.class);

    static final String TOKEN = "token";
    static final String FILE_TOKEN = "FILE_TOKEN";

    private MockWebServer mockWebServer;

    public AsyncHttpClient asyncHttpClient;

    @Rule
    public JUnitSoftAssertions softAssertions = new JUnitSoftAssertions();

    RealBannerStorageClient bannerStorageClient;

    BannerStorageClientConfiguration config;

    @Test
    public void createCreativeTest() throws IOException {
        init(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getMethod()).isEqualTo("POST");
                softAssertions.assertThat(request.getPath()).isEqualTo("/creatives/?include=");
                softAssertions.assertThat(request.getHeader("Authorization")).isEqualTo("OAuth " + config.getToken());
                return new MockResponse().setBody(request.getBody()).setResponseCode(201);
            }
        });
        Creative creative = new Creative();
        bannerStorageClient.createCreative(creative, Locale.ENGLISH);
    }

    @Test
    public void addTnsArticlesTest() throws IOException {
        Creative creative = new Creative().withId(3);
        init(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getMethod()).isEqualTo("PUT");
                softAssertions.assertThat(request.getPath())
                        .isEqualTo("/creatives/" + creative.getId() + "/tnsarticles");
                return new MockResponse().setBody(request.getBody());
            }
        });
        bannerStorageClient
                .addTnsArticles(creative.getId(), new ItemsRequest().withItems(List.of(new Item().withId(1))));
    }

    @Test
    public void addTnsBrandsTest() throws IOException {
        Creative creative = new Creative().withId(3);
        init(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getMethod()).isEqualTo("PUT");
                softAssertions.assertThat(request.getPath()).isEqualTo("/creatives/" + creative.getId() + "/tnsbrands");
                return new MockResponse().setBody(request.getBody());
            }
        });
        bannerStorageClient.addTnsBrands(creative.getId(), new ItemsRequest().withItems(List.of(new Item().withId(1))));
    }

    @Test
    public void requestModerationTest() throws IOException {
        Creative creative = new Creative().withId(3);
        init(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getMethod()).isEqualTo("POST");
                softAssertions.assertThat(request.getPath())
                        .isEqualTo("/creatives/" + creative.getId() + "/requestModeration");
                return new MockResponse().setBody(JsonUtils.toJson(creative)).setResponseCode(201);
            }
        });
        bannerStorageClient.requestModeration(creative.getId());
    }

    @Test
    public void tokenFile() {
        String filePath = this.getClass().getClassLoader().getResource("file_with_token").getPath();
        BannerStorageClientConfiguration configuration =
                new BannerStorageClientConfiguration("https://bs-api.ru", "", filePath);
        softAssertions.assertThat(configuration.getToken()).isEqualTo(FILE_TOKEN);
    }

    protected String url() {
        return "http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort();
    }

    private void init(Dispatcher dispatcher) throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher);
        mockWebServer.start();

        asyncHttpClient = new DefaultAsyncHttpClient();

        config = new BannerStorageClientConfiguration(url(), TOKEN, null);

        bannerStorageClient = new RealBannerStorageClient(config, asyncHttpClient);
    }

    @After
    public void tearDown() {
        try {
            if (mockWebServer != null) {
                mockWebServer.shutdown();
            }
        } catch (Exception e) {
            logger.warn("cannot shut down mockWebServer", e);
        }
    }
}
