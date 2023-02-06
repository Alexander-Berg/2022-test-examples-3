package ru.yandex.direct.core.service.urlchecker;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.zora.ZoraService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.zorafetcher.ZoraFetcher;
import ru.yandex.direct.zorafetcher.ZoraFetcherSettings;
import ru.yandex.direct.zorafetcher.ZoraGoRequestCreator;
import ru.yandex.direct.zorafetcher.ZoraOnlineRequestCreator;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.core.service.urlchecker.UrlToCheckUtils.createUrlToCheck;
import static ru.yandex.direct.zorafetcher.ZoraResponse.X_YANDEX_HTTP_CODE;
import static ru.yandex.direct.zorafetcher.ZoraResponse.X_YANDEX_ORIG_HTTP_CODE;
import static ru.yandex.direct.zorafetcher.ZoraResponse.X_YANDEX_STATUS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UrlCheckerTest {
    private final static Logger logger = LoggerFactory.getLogger(UrlCheckerTest.class);

    @Autowired
    private ZoraFetcher zoraFetcher;
    @Autowired
    private TvmIntegration tvmIntegration;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private ZoraFetcherSettings zoraFetcherSettings;

    private MockWebServer mockZora;

    private MockWebServerDispatcher mockZoraDispatcher;

    private UrlChecker urlChecker;

    @Before
    public void before() throws IOException {
        mockZoraDispatcher = spy(new MockWebServerDispatcher());
        mockZora = new MockWebServer();
        mockZora.setDispatcher(mockZoraDispatcher);
        mockZora.start();

        ZoraOnlineRequestCreator zoraOnlineRequestCreator =
                new ZoraOnlineRequestCreator(mockZora.getHostName(), mockZora.getPort(), "zora-source-name");
        ZoraGoRequestCreator zoraGoRequestCreator =
                new ZoraGoRequestCreator(mockZora.getHostName(), mockZora.getPort(), "zora-source-name");
        urlChecker = spy(new UrlChecker(
                new ZoraService(
                        zoraFetcher,
                        tvmIntegration,
                        ppcPropertiesSupport,
                        zoraGoRequestCreator,
                        zoraOnlineRequestCreator,
                        zoraFetcherSettings
                )
        ));
    }

    @After
    public void tearDown() {
        try {
            mockZora.shutdown();
        } catch (Exception e) {
            logger.warn("cannot shut down mockWebServer", e);
        }
    }

    @Test
    public void isUrlReachable_200_Success() {
        mockZoraDispatcher.respond("http://200/", new MockResponse()
                .setHeader(X_YANDEX_ORIG_HTTP_CODE, "200")
                .setResponseCode(200));

        UrlCheckResult result = urlChecker.isUrlReachable(createUrlToCheck("http://200/"));
        assertSuccessResult(result);
    }

    @Test
    public void isUrlReachable_400_DefectHttp() {
        mockZoraDispatcher.respond("http://400/", new MockResponse()
                .setHeader(X_YANDEX_ORIG_HTTP_CODE, "400")
                .setResponseCode(400));

        UrlCheckResult result = urlChecker.isUrlReachable(createUrlToCheck("http://400/"));
        assertFailureResult(result, UrlCheckResult.Error.HTTP_ERROR);
    }

    @Test
    public void isUrlReachable_ZoraError_DefectHttp() {
        mockZoraDispatcher.respond("http://zora_error/", new MockResponse()
                .setHeader("X-Yandex-Http-Code", "1006")
                .setHeader("X-Yandex-Status", "unavailable"));

        UrlCheckResult result = urlChecker.isUrlReachable(createUrlToCheck("http://zora_error/"));
        assertFailureResult(result, UrlCheckResult.Error.HTTP_ERROR);
    }

    @Test
    public void isUrlReachable_ZoraTimeout_Success() {
        mockZoraDispatcher.respond("http://zora_timeout/", new MockResponse()
                .setHeader(X_YANDEX_ORIG_HTTP_CODE, "200")
                .setResponseCode(200)
                .setBodyDelay(2000L, TimeUnit.MILLISECONDS));

        UrlCheckResult result = urlChecker.isUrlReachable(createUrlToCheck("http://zora_timeout/"));
        assertSuccessResult(result);
    }

    @Test
    public void isUrlReachable_TargetTimeout_DefectTimeout() {
        mockZoraDispatcher.respond("http://target_timeout/", new MockResponse()
                .setHeader(X_YANDEX_ORIG_HTTP_CODE, "200")
                .setHeader("X-Yandex-Http-Code", "1031")
                .setHeader("X-Yandex-Status", "timeout"));

        UrlCheckResult result = urlChecker.isUrlReachable(createUrlToCheck("http://target_timeout/"));
        assertFailureResult(result, UrlCheckResult.Error.TIMEOUT);
        assertFalse(result.getResult());
    }

    @Test
    public void isUrlReachable_TooManyRedirects_DefectTooManyRedirects() {
        mockZoraDispatcher.redirectChain(ImmutableList.of("http://redir_1/", "http://redir_2/", "http://redir_3/"),
                new MockResponse()
                        .setHeader(X_YANDEX_ORIG_HTTP_CODE, "200")
                        .setResponseCode(200));

        UrlCheckResult result = urlChecker.isUrlReachable(createUrlToCheck("http://redir_1/"));
        assertFailureResult(result, UrlCheckResult.Error.TOO_MANY_REDIRECTS);
    }

    @Test
    public void isUrlReachable_AbsoluteRedirectLocation_Success() throws InterruptedException {
        mockZoraDispatcher.redirectChain(ImmutableList.of("http://redir_1/", "http://redir_2/"),
                new MockResponse()
                        .setHeader(X_YANDEX_ORIG_HTTP_CODE, "200")
                        .setResponseCode(200));

        UrlCheckResult result = urlChecker.isUrlReachable(createUrlToCheck("http://redir_1/"));
        assertSuccessResult(result);
        assertRequestedUrl(1, "http://redir_2/");
    }

    @Test
    public void isUrlReachable_RelativeRedirectLocation_Success() throws InterruptedException {
        mockZoraDispatcher.redirectChain(ImmutableList.of("http://redir_1/", "/some_page"),
                new MockResponse()
                        .setHeader(X_YANDEX_ORIG_HTTP_CODE, "200")
                        .setResponseCode(200));

        UrlCheckResult result = urlChecker.isUrlReachable(createUrlToCheck("http://redir_1/"));
        assertSuccessResult(result);
        assertRequestedUrl(1, "http://redir_1/some_page");
    }

    @Test
    public void isUrlReachable_ZoraInternalError_Success() {
        mockZoraDispatcher.respond("http://zora_internal_error/",
                new MockResponse()
                        .setHeader(X_YANDEX_ORIG_HTTP_CODE, "200")
                        .setHeader(X_YANDEX_STATUS, "internal error")
                        .setResponseCode(500));

        UrlCheckResult result = urlChecker.isUrlReachable(createUrlToCheck("http://zora_internal_error/"));
        assertSuccessResult(result);
    }

    @Test
    public void isUrlReachable_ZoraError_Success() {
        mockZoraDispatcher.respond("http://zora_error/",
                new MockResponse()
                        .setHeader(X_YANDEX_ORIG_HTTP_CODE, "200")
                        .setHeader(X_YANDEX_HTTP_CODE, "1032")
                        .setResponseCode(200));

        UrlCheckResult result = urlChecker.isUrlReachable(createUrlToCheck("http://zora_error/"));
        assertSuccessResult(result);
    }

    private void assertSuccessResult(UrlCheckResult actualResult) {
        assertThat(actualResult.getResult(), is(true));
    }

    private void assertFailureResult(UrlCheckResult actualResult, UrlCheckResult.Error expectedError) {
        assertThat(actualResult.getResult(), is(false));
        assertThat(actualResult.getError(), is(expectedError));
    }

    private void assertRequestedUrl(int requestIndex, String expectedUrl) throws InterruptedException {
        ArgumentCaptor<RecordedRequest> argument = ArgumentCaptor.forClass(RecordedRequest.class);
        verify(mockZoraDispatcher, atLeast(requestIndex + 1)).dispatch(argument.capture());
        assertThat(argument.getAllValues().get(requestIndex).getPath(), is(expectedUrl));
    }

}
