package ru.yandex.direct.core.service.urlchecker;

import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.direct.core.configuration.CoreConfiguration;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.UrlUtils.urlDomainToAscii;

/**
 * Перед локальным запуском поднять туннель
 * <pre>
 * ssh -L localhost:8166:zora-online.yandex.net:8166 ppcdev1
 * </pre>
 */
@Ignore("Для запуска вручную. Нужен запущенный докер и, возможно, что-то ещё.")
@WebAppConfiguration
@RunWith(JUnitParamsRunner.class)
@ContextConfiguration(classes = CoreConfiguration.class)
public class RedirectCheckerManualTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private RedirectChecker redirectChecker;

    public static List<String> validUrls() {
        return asList(
                "http://httpstat.us/200",
                "http://httpstat.us/404",
                "https://redirect.appmetrica.yandex.com/serve/745636406096908737"
        );
    }

    @Test
    @Parameters(method = "validUrls")
    public void checkValidUrl_success(String url) {
        GetRedirectChainResult result = redirectChecker.getRedirectChain(createUrlToCheck(url));
        assertThat(result.isSuccessful()).isTrue();
    }

    public static List<String> invalidUrls() {
        return singletonList(
                "https://redirect.appmetrica.yandex.com/serve/745636406096908737_01"
        );
    }

    @Test
    @Parameters(method = "invalidUrls")
    public void checkValidUrl_fail_onInvalidUrl(String url) {
        GetRedirectChainResult result = redirectChecker.getRedirectChain(createUrlToCheck(url));
        assertThat(result.isSuccessful()).isFalse();
    }

    /**
     * Copy-paste of BannerUrlCheckService#createUrlToCheck(String)
     */
    private UrlToCheck createUrlToCheck(String url) {
        return new UrlToCheck()
                .withUrl(urlDomainToAscii(url))
                .withRedirectsLimit(5)
                .withTimeout(7000L);
    }
}
