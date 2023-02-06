package ru.yandex.market.api.server.sec.cors;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.util.Urls;

/**
 * @author dimkarp93
 */
public class CorsHelperTest {

    private static List<Pattern> patterns = Arrays.asList(
        CorsHelper.compilePattern("yandex\\.(ru|by|kz|ua|com|com\\.tr|com\\.ge|com\\.il|az|kg|lv|lt|md|tj|tm|fr|ee)"),
        CorsHelper.compilePattern("ya\\.(ru)"),
        CorsHelper.compilePattern("yandex-team\\.(ru)")
    );

    @Test
    public void yandexAllowed() {
        doAllowedTest(true, "https://yandex.ru");
        doAllowedTest(true, "https://yandex.by");
        doAllowedTest(true, "https://yandex.kz");
        doAllowedTest(true, "https://yandex.ua");
        doAllowedTest(true, "https://yandex.com");
        doAllowedTest(true, "https://yandex.com.tr");
        doAllowedTest(true, "https://yandex.com.ge");
        doAllowedTest(true, "https://yandex.com.il");
        doAllowedTest(true, "https://yandex.az");
        doAllowedTest(true, "https://yandex.kg");
        doAllowedTest(true, "https://yandex.lv");
        doAllowedTest(true, "https://yandex.lt");
        doAllowedTest(true, "https://yandex.md");
        doAllowedTest(true, "https://yandex.tj");
        doAllowedTest(true, "https://yandex.tm");
        doAllowedTest(true, "https://yandex.fr");
        doAllowedTest(true, "https://yandex.ee");

        doAllowedTest(true, "https://YANDEX.RU");
    }

    @Test
    public void yandexDisallowed() {
        doAllowedTest(false, "https://yandex.edu");
        doAllowedTest(false, "https://yandex.org");
    }

    @Test
    public void yaAllowed() {
        doAllowedTest(true, "https://ya.ru");

        doAllowedTest(true, "https://YA.RU");
    }

    @Test
    public void yaDisallowed() {
        doAllowedTest(false, "https://ya.com");
    }

    @Test
    public void yandexTeamAllowed() {
        doAllowedTest(true, "https://yandex-team.ru");

        doAllowedTest(true, "https://YANDEX-TEAM.RU");
    }

    @Test
    public void yandexTeamDisallowed() {
        doAllowedTest(false, "https://yandex-team.com");
    }


    @Test
    public void someYandexAllowed() {
        doAllowedTest(true, "https://some.yandex.ru");
        doAllowedTest(true, "https://some.yandex.by");
        doAllowedTest(true, "https://some.yandex.kz");
        doAllowedTest(true, "https://some.yandex.ua");
        doAllowedTest(true, "https://some.yandex.com");
        doAllowedTest(true, "https://some.yandex.com.tr");
        doAllowedTest(true, "https://some.yandex.com.ge");
        doAllowedTest(true, "https://some.yandex.com.il");
        doAllowedTest(true, "https://some.yandex.az");
        doAllowedTest(true, "https://some.yandex.kg");
        doAllowedTest(true, "https://some.yandex.lv");
        doAllowedTest(true, "https://some.yandex.lt");
        doAllowedTest(true, "https://some.yandex.md");
        doAllowedTest(true, "https://some.yandex.tj");
        doAllowedTest(true, "https://some.yandex.tm");
        doAllowedTest(true, "https://some.yandex.fr");
        doAllowedTest(true, "https://some.yandex.ee");

        doAllowedTest(true, "https://SOME.YANDEX.RU");
    }

    @Test
    public void someYandexDisallowed() {
        doAllowedTest(false, "https://some.yandex.edu");
        doAllowedTest(false, "https://some.yandex.org");
    }

    @Test
    public void someYaAllowed() {
        doAllowedTest(true, "https://some.ya.ru");

        doAllowedTest(true, "https://SOME.YA.RU");
    }

    @Test
    public void someYaDisallowed() {
        doAllowedTest(false, "https://some.ya.com");
    }

    @Test
    public void someYandexTeamAllowed() {
        doAllowedTest(true, "https://some.yandex-team.ru");

        doAllowedTest(true, "https://SOME.YANDEX-TEAM.RU");
    }

    @Test
    public void someYandexTeamDisallowed() {
        doAllowedTest(false, "https://some.yandex-team.com");
    }

    @Test
    public void prefixYandexDisallowed() {
        doAllowedTest(false, "https://yandexa.ru");
        doAllowedTest(false, "https://yandexa.by");
        doAllowedTest(false, "https://yandexa.kz");
        doAllowedTest(false, "https://yandexa.ua");
        doAllowedTest(false, "https://yandexa.com");
        doAllowedTest(false, "https://yandexa.com.tr");
        doAllowedTest(false, "https://yandexa.com.ge");
        doAllowedTest(false, "https://yandexa.com.il");
        doAllowedTest(false, "https://yandexa.az");
        doAllowedTest(false, "https://yandexa.kg");
        doAllowedTest(false, "https://yandexa.lv");
        doAllowedTest(false, "https://yandexa.lt");
        doAllowedTest(false, "https://yandexa.md");
        doAllowedTest(false, "https://yandexa.tj");
        doAllowedTest(false, "https://yandexa.tm");
        doAllowedTest(false, "https://yandexa.fr");
        doAllowedTest(false, "https://yandexa.ee");
    }

    @Test
    public void prefixYaDisallowed() {
        doAllowedTest(false, "https://yaa.ru");
    }

    @Test
    public void prefixYandexTeamDisallowed() {
        doAllowedTest(false, "https://yandex-teama.ru");
    }

    @Test
    public void suffixYandexDisallowed() {
        doAllowedTest(false, "https://ayandex.ru");
        doAllowedTest(false, "https://ayandex.by");
        doAllowedTest(false, "https://ayandex.kz");
        doAllowedTest(false, "https://ayandex.ua");
        doAllowedTest(false, "https://ayandex.com");
        doAllowedTest(false, "https://ayandex.com.tr");
        doAllowedTest(false, "https://ayandex.com.ge");
        doAllowedTest(false, "https://ayandex.com.il");
        doAllowedTest(false, "https://ayandex.az");
        doAllowedTest(false, "https://ayandex.kg");
        doAllowedTest(false, "https://ayandex.lv");
        doAllowedTest(false, "https://ayandex.lt");
        doAllowedTest(false, "https://ayandex.md");
        doAllowedTest(false, "https://ayandex.tj");
        doAllowedTest(false, "https://ayandex.tm");
        doAllowedTest(false, "https://ayandex.fr");
        doAllowedTest(false, "https://ayandex.ee");

    }

    @Test
    public void suffixYaDisallowed() {
        doAllowedTest(false, "https://aya.ru");
    }

    @Test
    public void suffixYandexTeamDisallowed() {
        doAllowedTest(false, "https://ayandex-team.ru");
    }


    @Test
    public void containsYandexDisallowed() {
        doAllowedTest(false, "https://ayandexa.ru");
        doAllowedTest(false, "https://ayandexa.by");
        doAllowedTest(false, "https://ayandexa.kz");
        doAllowedTest(false, "https://ayandexa.ua");
        doAllowedTest(false, "https://ayandexa.com");
        doAllowedTest(false, "https://ayandexa.com.tr");
        doAllowedTest(false, "https://ayandexa.com.ge");
        doAllowedTest(false, "https://ayandexa.com.il");
        doAllowedTest(false, "https://ayandexa.az");
        doAllowedTest(false, "https://ayandexa.kg");
        doAllowedTest(false, "https://ayandexa.lv");
        doAllowedTest(false, "https://ayandexa.lt");
        doAllowedTest(false, "https://ayandexa.md");
        doAllowedTest(false, "https://ayandexa.tj");
        doAllowedTest(false, "https://ayandexa.tm");
        doAllowedTest(false, "https://ayandexa.fr");
        doAllowedTest(false, "https://ayandexa.ee");
    }


    @Test
    public void containsYaDisallowed() {
        doAllowedTest(false, "https://ayaa.ru");
    }

    @Test
    public void containsYandexTeamDisallowed() {
        doAllowedTest(false, "https://ayandex-teama.ru");
    }

    @Test
    public void noHeaderDoNotExtract() {
        doExtractTest(ApiMatchers.emptyOptional(), MockRequestBuilder.start().build());
    }

    @Test
    public void headerIsEmptyDoNotExtract() {
        doExtractTest(ApiMatchers.emptyOptional(), request(""));
    }

    @Test
    public void originIsNotUriEmptyDoNotExtract() {
        doExtractTest(ApiMatchers.emptyOptional(), request("yandex.ru"));
    }

    @Test
    public void illegalOriginEmptyDoNotExtract() {
        doExtractTest(ApiMatchers.emptyOptional(), request("https://e1.ru"));
    }

    @Test
    public void extractLegalOrigin() {
        String origin = "https://yandex.ru";
        doExtractTest(ApiMatchers.optionalHasValue(origin), request(origin));
    }

    @Test
    public void extractLegalOriginWithPort() {
        String origin = "https://yandex.ru:123";
        doExtractTest(ApiMatchers.optionalHasValue(origin), request(origin));
    }

    private static void doAllowedTest(boolean isAllowed, String origin) {
        URI uri = Urls.toUri(origin);
        Assert.assertEquals(isAllowed, CorsHelper.isAllowedOrigin(uri, patterns));
    }

    private static void doExtractTest(Matcher<Optional<String>> matcher, HttpServletRequest request) {
        Assert.assertThat(CorsHelper.extractOrigin(request, patterns), matcher);
    }

    private static HttpServletRequest request(String origin) {
        return MockRequestBuilder.start()
            .header("Origin", origin)
            .build();
    }
}
