package ru.yandex.direct.core.service.urlchecker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.SoftAssertions;
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
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;
import ru.yandex.direct.zorafetcher.ZoraFetcher;
import ru.yandex.direct.zorafetcher.ZoraFetcherSettings;
import ru.yandex.direct.zorafetcher.ZoraGoRequestCreator;
import ru.yandex.direct.zorafetcher.ZoraOnlineRequestCreator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.core.service.urlchecker.UrlToCheckUtils.createUrlToCheck;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UrlCheckerFixBrokenEncodingTest {

    static {
        // Нужно для инициализации okhttp3.internal.Internal.instance, который используется в
        // okhttp3.mockwebserver.MockResponse.addHeaderLenient
        new OkHttpClient.Builder().build();
    }

    private final static Logger logger = LoggerFactory.getLogger(UrlCheckerFixBrokenEncodingTest.class);

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private MockWebServer server;
    private UrlChecker urlChecker;
    private HashMap<String, MockResponse> responseByRequest = new HashMap<>();
    private List<String> requestedUrls;

    @Before
    public void setUp() throws IOException {
        requestedUrls = new ArrayList<>();
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String url = request.getPath();
                requestedUrls.add(url);
                MockResponse response = responseByRequest.get(url);
                if (response == null) {
                    throw new RuntimeException("UNEXPECTED REQUEST: " + url);
                }
                return response;
            }
        });
        server.start();

        DefaultAsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();
        TvmIntegration tvmIntegration = mock(TvmIntegration.class);
        doReturn("ticket").when(tvmIntegration).getTicket(any(TvmService.class));
        String zoraHost = server.getHostName();
        int zoraPort = server.getPort();
        String zoraSourceName = "direct_userproxy";
        ZoraGoRequestCreator zoraGoRequestCreator = new ZoraGoRequestCreator(zoraHost, zoraPort, zoraSourceName);
        ZoraOnlineRequestCreator zoraOnlineRequestCreator = new ZoraOnlineRequestCreator(zoraHost, zoraPort, zoraSourceName);
        ZoraFetcherSettings settings = ZoraFetcherSettings.builder().build();

        urlChecker = spy(new UrlChecker(
                new ZoraService(
                        new ZoraFetcher(asyncHttpClient, asyncHttpClient, settings),
                        tvmIntegration,
                        ppcPropertiesSupport,
                        zoraGoRequestCreator,
                        zoraOnlineRequestCreator,
                        settings
                )));
    }

    @After
    public void tearDown() {
        try {
            server.shutdown();
        } catch (Exception e) {
            logger.warn("cannot shut down mockWebServer", e);
        }
    }

    @Test
    public void isUrlReachable_fixBrokenEncoding_success() {
        String startUrl = "http://test.host/";
        String notEncodedUrl = "http://test.host/каско/";
        String encodedUrl = "http://test.host/%D0%BA%D0%B0%D1%81%D0%BA%D0%BE/";

        responseByRequest.put(startUrl,
                new MockResponse()
                        .setResponseCode(301)
                        .addHeader("X-Yandex-Orig-Http-Code", "301")
                        .addHeaderLenient("Location", notEncodedUrl));
        responseByRequest.put(encodedUrl,
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("X-Yandex-Orig-Http-Code", "200"));

        UrlToCheck urlToCheck = createUrlToCheck(startUrl);
        UrlCheckResult result = urlChecker.isUrlReachable(urlToCheck);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getError()).as("error").isNull();
            soft.assertThat(result.getResult()).as("result").isTrue();
            soft.assertThat(requestedUrls).as("requestedUrls").containsExactly(startUrl, encodedUrl);
        });
    }

}
