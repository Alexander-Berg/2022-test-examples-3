package ru.yandex.direct.web.entity.banner.service;

import java.util.List;

import junitparams.JUnitParamsRunner;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.asynchttp.Result;
import ru.yandex.direct.core.entity.zora.ZoraService;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.banner.model.WebCheckUrlResponse;
import ru.yandex.direct.web.entity.banner.model.WebUrlCheckResult;
import ru.yandex.direct.web.validation.model.WebDefect;
import ru.yandex.direct.zorafetcher.ZoraResponse;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

@DirectWebTest
@RunWith(JUnitParamsRunner.class)
public class WebBannerUrlCheckServiceTest {
    private TestContextManager testContextManager = new TestContextManager(WebBannerUrlCheckServiceTest.class);

    @Autowired
    private WebBannerUrlCheckService webBannerUrlCheckService;

    @Autowired
    private ZoraService zoraService;

    @Before
    public void setUp() throws Exception {
        testContextManager.prepareTestInstance(this);
        var zoraEmptyResponse = zoraEmptyResponse();
        Mockito.doReturn(zoraEmptyResponse)
                .when(zoraService)
                .fetchByUrl(anyString(), anyBoolean());
    }

    @Test
    @junitparams.Parameters(method = "positiveCases")
    public void domainIsCorrect(String url, String expectedDomain) {
        WebCheckUrlResponse response = (WebCheckUrlResponse) webBannerUrlCheckService.checkUrls(List.of(url));
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(response.getValidationErrors()).isEmpty();
        sa.assertThat(response.getCheckResults())
                .singleElement()
                .extracting(WebUrlCheckResult::getDomain)
                .isEqualTo(expectedDomain);
        sa.assertAll();
    }

    @Test
    @junitparams.Parameters(method = "negativeCases")
    public void incorrectUrl(String url, String expectedError) {
        WebCheckUrlResponse response = (WebCheckUrlResponse) webBannerUrlCheckService.checkUrls(List.of(url));
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(response.getCheckResults()).containsExactly((WebUrlCheckResult) null);
        sa.assertThat(response.getValidationErrors())
                .singleElement()
                .extracting(WebDefect::getCode)
                .isEqualTo(expectedError);
        sa.assertAll();
    }


    Iterable<Object[]> positiveCases() {
        return asList(new Object[][]{
                {"https://yandex.ru", "yandex.ru"},
                {"https://yandex.ru/maps", "maps.yandex.ru"},
                {"https://yandex.ru/uslugi", "uslugi.yandex.ru"},
                {"http://google.com", "google.com"},
        });
    }

    Iterable<Object[]> negativeCases() {
        return asList(new Object[][]{
                {"https://yandeaps", "DefectIds.INVALID_VALUE"},
        });
    }

    private Result<ZoraResponse> zoraEmptyResponse() {
        var zoraResponse = Mockito.mock(ZoraResponse.class);
        Mockito.when(zoraResponse.isOk()).thenReturn(true);
        var result = new Result<ZoraResponse>(0);
        result.setSuccess(zoraResponse);
        return result;
    }
}
