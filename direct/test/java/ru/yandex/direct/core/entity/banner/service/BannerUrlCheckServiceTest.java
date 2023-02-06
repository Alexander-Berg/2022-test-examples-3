package ru.yandex.direct.core.entity.banner.service;

import java.io.IOException;

import com.google.common.collect.ImmutableList;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService;
import ru.yandex.direct.core.entity.banner.type.href.BannersUrlHelper;
import ru.yandex.direct.core.entity.trustedredirects.repository.TrustedRedirectsRepository;
import ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService;
import ru.yandex.direct.core.entity.zora.ZoraService;
import ru.yandex.direct.core.service.urlchecker.GetRedirectChainResult;
import ru.yandex.direct.core.service.urlchecker.RedirectCheckResult;
import ru.yandex.direct.core.service.urlchecker.RedirectChecker;
import ru.yandex.direct.core.service.urlchecker.UrlCheckResult;
import ru.yandex.direct.core.service.urlchecker.UrlChecker;
import ru.yandex.direct.core.service.urlchecker.UrlToCheck;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.zorafetcher.ZoraFetcher;
import ru.yandex.direct.zorafetcher.ZoraFetcherSettings;
import ru.yandex.direct.zorafetcher.ZoraGoRequestCreator;
import ru.yandex.direct.zorafetcher.ZoraOnlineRequestCreator;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.core.service.urlchecker.UrlCheckResult.Error.HTTP_ERROR;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.emptyPath;
import static ru.yandex.direct.zorafetcher.ZoraResponse.X_YANDEX_ORIG_HTTP_CODE;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerUrlCheckServiceTest {
    private final static Logger logger = LoggerFactory.getLogger(BannerUrlCheckServiceTest.class);

    @Autowired
    private TrustedRedirectsRepository trustedRedirectsRepository;
    @Autowired
    private ZoraFetcher zoraFetcher;
    @Autowired
    private TvmIntegration tvmIntegration;
    @Autowired
    protected Steps steps;
    @Autowired
    private BannersUrlHelper bannersUrlHelper;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private ZoraFetcherSettings zoraFetcherSettings;

    private MockWebServer mockZora;

    private UrlChecker urlChecker;

    private RedirectChecker redirectChecker;

    private BannerUrlCheckService bannerUrlCheckService;

    @Before
    public void before() throws IOException {
        mockZora = new MockWebServer();
        mockZora.setDispatcher(dispatcher());
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
                )));

        // trustedRedirectsService использует ленивый кеш, который сбрасывается только при переинициализации бина.
        // так как контекст шарится между тестами создаем бин руками сами.
        TrustedRedirectsService trustedRedirectsService = new TrustedRedirectsService(trustedRedirectsRepository);
        redirectChecker = spy(new RedirectChecker(trustedRedirectsService, bannersUrlHelper,
                new ZoraService(
                        zoraFetcher,
                        tvmIntegration,
                        ppcPropertiesSupport,
                        zoraGoRequestCreator,
                        zoraOnlineRequestCreator,
                        zoraFetcherSettings
                )));
        bannerUrlCheckService =
                new BannerUrlCheckService(ppcPropertiesSupport, urlChecker, redirectChecker, trustedRedirectsService);

        steps.trustedRedirectSteps().addValidCounters();
    }

    /**
     * В тестах этого класса достаточно эмулировать Zora, которая успешно обрабатывает любой URL
     */
    private Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse()
                        .setHeader(X_YANDEX_ORIG_HTTP_CODE, "200")
                        .setResponseCode(200);
            }
        };
    }

    @After
    public void after() throws IOException {
        try {
            mockZora.shutdown();
        } catch (Exception e) {
            logger.warn("cannot shut down mockWebServer", e);
        }
        steps.trustedRedirectSteps().deleteTrusted();
    }

    @Test
    public void checkUrl_RegularLink_UrlCheckerCalledWithCorrectUrl() {
        bannerUrlCheckService.checkUrls(singletonList("http://ya.ru/"));
        verify(urlChecker).isUrlReachable(createUrlToCheck("http://ya.ru/", 5));
    }

    @Test
    public void checkUrl_CyrillicLink_UrlCheckerCalledWithCorrectUrl() {
        bannerUrlCheckService.checkUrls(singletonList("http://яндекс.рф/"));
        verify(urlChecker).isUrlReachable(createUrlToCheck("http://xn--d1acpjx3f.xn--p1ai/", 5));
    }

    @Test
    public void checkUrl_UglyLink_UrlCheckerCalledWithCorrectUrl() {
        bannerUrlCheckService.checkUrls(singletonList("    http://ya.ru/?par=‒!`  "));
        verify(urlChecker).isUrlReachable(createUrlToCheck("http://ya.ru/?par=-!'", 5));
    }

    @Test
    public void checkUrl_LinkWithParameters_UrlCheckerCalledWithCorrectUrl() {
        bannerUrlCheckService
                .checkUrls(singletonList("http://ya.ru/?par1={source}&par2={region_id}&par3={device_type}"));
        verify(urlChecker).isUrlReachable(createUrlToCheck("http://ya.ru/?par1=test&par2=123&par3=desktop", 5));
    }

    @Test
    public void checkUrl_NullUrl_ValidationError() {
        MassResult<UrlCheckResult> result = bannerUrlCheckService.checkUrls(singletonList(null));
        assertValidationFailResult(result.get(0), notNull());
    }

    @Test
    public void checkUrl_EmptyUrl_ValidationError() {
        MassResult<UrlCheckResult> result = bannerUrlCheckService.checkUrls(singletonList(""));
        assertValidationFailResult(result.get(0), notEmptyString());
    }

    @Test
    public void checkUrl_WrongUrl_ValidationError() {
        MassResult<UrlCheckResult> result = bannerUrlCheckService.checkUrls(singletonList("http://."));
        assertValidationFailResult(result.get(0), invalidValue());
    }

    @Test
    public void checkUrl_TrustedRedirect_UrlCheckerCalledWithIncreasedRedirectsLimit() {
        bannerUrlCheckService.checkUrls(singletonList("http://yandex.ru/"));
        verify(urlChecker).isUrlReachable(createUrlToCheck("http://yandex.ru/", 9));
    }

    @Test
    public void checkUrls_DifferentResults() {
        String successUrl = "http://200.ru/";
        String failUrl = "http://400.ru/";

        doReturn(new UrlCheckResult(true, null))
                .when(urlChecker).isUrlReachable(argThat(url -> url.getUrl().equals(successUrl)));
        doReturn(new UrlCheckResult(false, HTTP_ERROR))
                .when(urlChecker).isUrlReachable(argThat(url -> url.getUrl().equals(failUrl)));

        MassResult<UrlCheckResult> result =
                bannerUrlCheckService.checkUrls(ImmutableList.of("", successUrl, failUrl));
        assertValidationFailResult(result.get(0), notEmptyString());
        assertSuccessResult(result.get(1));
        assertFailureResult(result.get(2), HTTP_ERROR);
    }

    @Test
    public void getRedirect() {
        RedirectCheckResult result = bannerUrlCheckService.getRedirect("http://yandex.ru/");
        assertThat(result.getRedirectDomain(), is("yandex.ru"));
    }

    @Test
    public void getRedirect_CyrillicUrl() {
        RedirectCheckResult result =
                bannerUrlCheckService.getRedirect("http://тестовый-домен.рф/some/path/");
        assertThat(result.getRedirectDomain(), is("тестовый-домен.рф"));
    }

    @Test
    public void getRedirect_PunycodeUrl() {
        RedirectCheckResult result =
                bannerUrlCheckService.getRedirect("http://xn----ctbhbdvxjce1akc2k.xn--p1ai/some/path/");
        assertThat(result.getRedirectDomain(), is("тестовый-домен.рф"));
    }

    @Test
    public void getRedirects_FailUrl() {
        String failUrl = "http://400.ru/";
        doReturn(GetRedirectChainResult.createFailResult(singletonList(failUrl)))
                .when(redirectChecker).getRedirectChain(argThat(url -> url.getUrl().equals(failUrl)));

        RedirectCheckResult result = bannerUrlCheckService.getRedirect(failUrl);
        assertThat(result.isSuccessful(), is(false));

    }

    @Test
    public void getRedirects_InvalidLastUrlInRedirectChain() {
        String firstUrl = "http://yandex.ru/";
        doReturn(GetRedirectChainResult.createSuccessResult(asList(firstUrl, null)))
                .when(redirectChecker).getRedirectChain(argThat(url -> url.getUrl().equals(firstUrl)));

        RedirectCheckResult result = bannerUrlCheckService.getRedirect(firstUrl);
        assertThat(result.isSuccessful(), is(false));
    }

    @Test
    public void getRedirects_ThreeUrlsInRedirectChain() {
        String firstUrl = "http://first-url.ru/";
        String secondUrl = "http://second-url.ru/";
        String thirdUrl = "http://third-url.ru/";
        doReturn(GetRedirectChainResult.createSuccessResult(asList(firstUrl, secondUrl, thirdUrl)))
                .when(redirectChecker).getRedirectChain(argThat(url -> url.getUrl().equals(firstUrl)));

        RedirectCheckResult result = bannerUrlCheckService.getRedirect(firstUrl);
        assertThat(result.getRedirectDomain(), is("third-url.ru"));
    }

    @Test
    public void getRedirects_whenGetRedirectUrlThrowException() {
        String href = "http://throw-exception.ru/";
        doThrow(new RuntimeException())
                .when(redirectChecker).getRedirectChain(argThat(url -> url.getUrl().equals(href)));

        RedirectCheckResult result = bannerUrlCheckService.getRedirect(href);
        assertThat(result.isSuccessful(), is(false));
    }

    @Test
    public void getRedirects_whenCreateUrlToCheckThrowException() {
        String href = "invalid-href±";
        RedirectCheckResult result = bannerUrlCheckService.getRedirect(href);

        verify(redirectChecker, never()).getRedirectChain(argThat(url -> url.getUrl().equals(href)));
        assertThat(result.isSuccessful(), is(false));
    }

    private UrlToCheck createUrlToCheck(String url, int redirectsLimit) {
        return new UrlToCheck()
                .withUrl(url)
                .withTimeout(20_000L)
                .withRedirectsLimit(redirectsLimit)
                .withTrustRedirectFromAnyDomain(false);
    }

    private void assertSuccessResult(Result<UrlCheckResult> actualResult) {
        assertThat(actualResult.getResult().getResult(), is(true));
    }

    private void assertFailureResult(
            Result<UrlCheckResult> actualResult,
            @SuppressWarnings("SameParameterValue") UrlCheckResult.Error expectedError) {
        assertThat(actualResult.getResult().getResult(), is(false));
        assertThat(actualResult.getResult().getError(), is(expectedError));
    }

    private void assertValidationFailResult(Result<UrlCheckResult> actualResult, Defect expectedDefect) {
        assertThat(actualResult.getResult(), nullValue());
        assertThat(actualResult.getErrors().get(0), validationError(emptyPath(), expectedDefect));
    }
}
