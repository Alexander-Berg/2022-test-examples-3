package ru.yandex.direct.core.service.storeurlchecker;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.SoftAssertions;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.zora.ZoraService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.stub.TvmIntegrationTestStub;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;
import ru.yandex.direct.zorafetcher.ZoraFetcher;
import ru.yandex.direct.zorafetcher.ZoraFetcherSettings;
import ru.yandex.direct.zorafetcher.ZoraGoRequestCreator;
import ru.yandex.direct.zorafetcher.ZoraOnlineRequestCreator;

import static java.util.Arrays.asList;
import static ru.yandex.direct.zorafetcher.ZoraOnlineResponse.OK_200_STRING;
import static ru.yandex.direct.zorafetcher.ZoraResponse.X_YANDEX_ORIG_HTTP_CODE;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class StoreUrlInstantCheckerTest {
    private final static Logger logger = LoggerFactory.getLogger(StoreUrlInstantCheckerTest.class);
    private static final String VALID_URL = "https://play.google.com/store/apps/details?id=valid.app.id";
    private static final String INVALID_URL = "https://play.google.com/store/apps/details?id=wrong.app.id";

    private AsyncHttpClient asyncHttpClient;
    private MockWebServer mockZora;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private StoreUrlInstantChecker storeUrlInstantChecker;

    @Before
    public void before() throws IOException {
        TvmIntegration tvmIntegration = new TvmIntegrationTestStub(TvmService.DUMMY);

        mockZora = new MockWebServer();
        mockZora.setDispatcher(createDispatcher());
        mockZora.start();

        asyncHttpClient = new DefaultAsyncHttpClient();

        var zoraOnlineRequestCreator = new ZoraOnlineRequestCreator(mockZora.getHostName(), mockZora.getPort(), "some-source-name");
        var zoraGoRequestCreator = new ZoraGoRequestCreator(mockZora.getHostName(), mockZora.getPort(), "some-source-name");

        var settings = ZoraFetcherSettings.builder().build();
        storeUrlInstantChecker = new StoreUrlInstantChecker(new ZoraService(
                new ZoraFetcher(asyncHttpClient, asyncHttpClient, settings),
                tvmIntegration,
                ppcPropertiesSupport,
                zoraGoRequestCreator,
                zoraOnlineRequestCreator,
                settings
        ));
    }

    @After
    public void after() throws IOException {
        try {
            mockZora.shutdown();
        } catch (Exception e) {
            logger.warn("cannot shut down mockWebServer", e);
        }
        asyncHttpClient.close();
    }

    @Test
    public void isStoreUrlReachable_CheckResult() {
        Map<String, Boolean> result = storeUrlInstantChecker.isStoreUrlReachable(asList(VALID_URL, INVALID_URL));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).hasSize(2);
            softly.assertThat(result).containsOnlyKeys(VALID_URL, INVALID_URL);
            softly.assertThat(result.get(VALID_URL)).isTrue();
            softly.assertThat(result.get(INVALID_URL)).isFalse();
        });
    }

    @Nonnull
    private Dispatcher createDispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest req) {
                if (req.getPath().equals(httpsToHttp(VALID_URL))) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader(X_YANDEX_ORIG_HTTP_CODE, OK_200_STRING);
                } else if (req.getPath().equals(httpsToHttp(INVALID_URL))) {
                    return new MockResponse().setResponseCode(404);
                }
                return new MockResponse().setResponseCode(500);
            }

            private String httpsToHttp(String url) {
                return url.replace("https://", "http://");
            }
        };
    }
}
