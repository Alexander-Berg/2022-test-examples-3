package ru.yandex.market.api.util.httpclient.sec;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.URIMatcher;

import java.net.URI;

/**
 * @author dimkarp93
 */
public class UriSanitizerTest extends UnitTestBase {
    @Test
    public void nullSafe() {
        Assert.assertNull(UriSanitizer.sanitize((String) null));
        Assert.assertNull(UriSanitizer.sanitize(""));
        Assert.assertNull(UriSanitizer.sanitize((URI) null));
    }

    @Test
    public void oauthSanitize() throws Exception {
        String uri = "https://api.ru:333/run?param1=2&oauth_token=123&param3=4";
        URI sanitized = UriSanitizer.sanitize(new URI(uri));

        Matcher<URI> expected = URIMatcher.uri(
            URIMatcher.hasQueryParams("oauth_token", "XXXXX"),
            URIMatcher.hasNoQueryParams("oauth_token", "123"),
            URIMatcher.hasNoQueryParams("sessionid"),
            URIMatcher.hasNoQueryParams("sber_id"),
            URIMatcher.host("api.ru"),
            URIMatcher.port(333),
            URIMatcher.path("/run"),
            URIMatcher.hasQueryParams("param1", "2"),
            URIMatcher.hasQueryParams("param3", "4")
        );

        Assert.assertThat(sanitized, expected);
    }

    @Test
    public void oauthSantizePartUrlWithSlash() {
        String uri = "/run?param1=2&oauth_token=123&param3=4";
        String sanitized = UriSanitizer.sanitize(uri);

        Matcher<String> expected = Matchers.allOf(
                Matchers.containsString("oauth_token=XXXXX"),
                Matchers.not(Matchers.containsString("oauth_token=123")),
                Matchers.not(Matchers.containsString("sessionid")),
                Matchers.not(Matchers.containsString("sber_id")),
                Matchers.containsString("/run"),
                Matchers.containsString("param1=2"),
                Matchers.containsString("param3=4")
        );

        Assert.assertThat(sanitized, expected);
    }

    @Test
    public void oauthSantizePartUrlWithoutSlash() {
        String uri = "run?param1=2&oauth_token=123&param3=4";
        String sanitized = UriSanitizer.sanitize(uri);

        Matcher<String> expected = Matchers.allOf(
                Matchers.containsString("oauth_token=XXXXX"),
                Matchers.not(Matchers.containsString("oauth_token=123")),
                Matchers.not(Matchers.containsString("sessionid")),
                Matchers.not(Matchers.containsString("sber_id")),
                Matchers.containsString("/run"),
                Matchers.containsString("param1=2"),
                Matchers.containsString("param3=4")
        );

        Assert.assertThat(sanitized, expected);
    }

    @Test
    public void oauthSantizePartUrlWithStrangeUrl() {
        String uri = "://api/run?param1=2&oauth_token=123&param3=4";
        String sanitized = UriSanitizer.sanitize(uri);

        Matcher<String> expected = Matchers.allOf(
                Matchers.containsString("oauth_token=XXXXX"),
                Matchers.not(Matchers.containsString("oauth_token=123")),
                Matchers.not(Matchers.containsString("sessionid")),
                Matchers.not(Matchers.containsString("sber_id")),
                Matchers.containsString("/run"),
                Matchers.containsString("param1=2"),
                Matchers.containsString("param3=4")
        );

        Assert.assertThat(sanitized, expected);
    }

    @Test
    public void oauthWithoutQuery() {
        String uri = "/run";
        String sanitized = UriSanitizer.sanitize(uri);

        Matcher<String> expected = Matchers.allOf(
                Matchers.not(Matchers.containsString("oauth_token=123")),
                Matchers.not(Matchers.containsString("sessionid")),
                Matchers.not(Matchers.containsString("sber_id")),
                Matchers.containsString("/run")
        );

        Assert.assertThat(sanitized, expected);
    }
}
