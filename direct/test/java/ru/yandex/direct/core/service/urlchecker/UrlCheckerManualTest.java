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
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.UrlUtils.urlDomainToAscii;

/**
 * Перед локальным запуском поднять туннель
 * <pre>
 * ssh -L localhost:8166:zora-online.yandex.net:8166 ppcdev1
 * </pre>
 */
@Ignore("Для запуска вручную. Нужен запущенный докер и, возможно, что-то ещё.")
@CoreTest
@RunWith(JUnitParamsRunner.class)
public class UrlCheckerManualTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    UrlChecker urlChecker;

    public static List<String> validUrls() {
        return asList(
                // Примеры из https://st.yandex-team.ru/DIRECT-99074 "Улучшить работу с Zora"
                "http://dom-remont.moscow",
                "http://stock.audi-warshavka.ru",
                "http://www.vhakasiyu.ru/tours/krasnoyarsk/splav-po-kazyru-porogi-gorod-solntsa/",
                "http://httpstat.us/200?sleep=6000",
                "http://httpstat.us/200?sleep=6630", // 7000 (наш timeout) - 10% - 300ms (roundtrip)
                "https://tdarmada.ru/parts/iveco/#42103392#"
        );
    }

    @Test
    @Parameters(method = "validUrls")
    public void checkValidUrl_success(String url) {
        UrlCheckResult result = urlChecker.isUrlReachable(createUrlToCheck(url));
        assertThat(result.getResult()).isTrue();
    }

    public static List<String> invalidUrls() {
        return asList(
                "http://httpstat.us/403",
                "http://httpstat.us/404",
                "http://httpstat.us/503",
                "http://no-such-site-should-exist.org",
                "http://httpstat.us/200?sleep=7000"
        );
    }

    @Test
    @Parameters(method = "invalidUrls")
    public void checkValidUrl_fail_onInvalidUrl(String url) {
        UrlCheckResult result = urlChecker.isUrlReachable(createUrlToCheck(url));
        assertThat(result.getResult()).isFalse();
    }

    /**
     * Copy-paste of BannerUrlCheckService#createUrlToCheck(java.lang.String)
     */
    private UrlToCheck createUrlToCheck(String url) {
        return new UrlToCheck()
                .withUrl(urlDomainToAscii(url))
                .withRedirectsLimit(5)
                .withTimeout(7000L);
    }

}
