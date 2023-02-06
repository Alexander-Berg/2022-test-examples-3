package ru.yandex.direct.core.entity.banner.service;

import java.io.IOException;

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
import ru.yandex.direct.core.service.urlchecker.RedirectChecker;
import ru.yandex.direct.core.service.urlchecker.UrlCheckResult;
import ru.yandex.direct.core.service.urlchecker.UrlChecker;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.zorafetcher.ZoraFetcher;
import ru.yandex.direct.zorafetcher.ZoraFetcherSettings;
import ru.yandex.direct.zorafetcher.ZoraGoRequestCreator;
import ru.yandex.direct.zorafetcher.ZoraOnlineRequestCreator;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.zorafetcher.ZoraRequestCreator.X_YANDEX_USE_HTTPS;
import static ru.yandex.direct.zorafetcher.ZoraResponse.X_YANDEX_ORIG_HTTP_CODE;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerUrlCheckRedirectServiceTest {
    private final static Logger logger = LoggerFactory.getLogger(BannerUrlCheckRedirectServiceTest.class);

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
        UrlChecker urlChecker = spy(new UrlChecker(
                new ZoraService(
                        zoraFetcher,
                        tvmIntegration,
                        ppcPropertiesSupport,
                        zoraGoRequestCreator,
                        zoraOnlineRequestCreator,
                        zoraFetcherSettings
                )
        ));

        TrustedRedirectsService trustedRedirectsService = new TrustedRedirectsService(trustedRedirectsRepository);
        RedirectChecker redirectChecker = spy(new RedirectChecker(trustedRedirectsService, bannersUrlHelper,
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

    private Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath().equals("http://ya.ru/initial")
                        && request.getHeader(X_YANDEX_USE_HTTPS) != null) {
                    return new MockResponse()
                            .setHeader(X_YANDEX_ORIG_HTTP_CODE, "301")
                            .setHeader("Location", "/redirect")
                            .setResponseCode(301);
                } else if (request.getPath().equals("http://ya.ru/redirect")
                        && request.getHeader(X_YANDEX_USE_HTTPS) != null) {
                    return new MockResponse()
                            .setHeader(X_YANDEX_ORIG_HTTP_CODE, "200")
                            .setResponseCode(200);
                } else {
                    return new MockResponse()
                            .setHeader(X_YANDEX_ORIG_HTTP_CODE, "500")
                            .setResponseCode(500);
                }
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
    @Description("Тест на относительный редирект с https")
    public void checkUrl_RelativeRedirect() {
        MassResult<UrlCheckResult> result = bannerUrlCheckService.checkUrls(singletonList("https://ya.ru/initial"));
        assertNull(result.getResult().get(0).getResult().getError());
    }
}
