package ru.yandex.market.api.common.url;

import javax.servlet.http.HttpServletRequest;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.domain.v2.redirect.parameters.SuggestUrlUserParams;
import ru.yandex.market.api.domain.v2.redirect.parameters.UrlUserParams;
import ru.yandex.market.api.integration.UnitTestBase;

/**
 * @author dimkarp93
 */
public class UrlUserParamsParserTest extends UnitTestBase {
    private final UrlControllerHelper.UrlUserParamsParser parser = new UrlControllerHelper.UrlUserParamsParser();

    private final Matcher<Iterable<? extends String>> defaultBlacklistMatcher = Matchers.containsInAnyOrder(
            "utm_source",
            "utm_medium",
            "utm_campaign",
            "utm_term",
            "utm_content"
    );

    @Test
    public void testUrl() {
        HttpServletRequest request = MockRequestBuilder.start()
                .param("url", "http://abc.def")
                .build();

        UrlUserParams params = parser.get(request);
        Assert.assertEquals("http://abc.def", params.getUrl());
        Assert.assertThat(params.getBlacklist(), defaultBlacklistMatcher);
    }

    @Test
    public void testSuggestUrl() {
        HttpServletRequest request = MockRequestBuilder.start()
                .param("url", "http://abc.def")
                .param("suggest_text", "xyz")
                .build();


        UrlUserParams params = parser.get(request);
        Assert.assertTrue(params instanceof SuggestUrlUserParams);
        SuggestUrlUserParams suggestUrlUserParams = (SuggestUrlUserParams) params;

        Assert.assertEquals("http://abc.def", params.getUrl());
        Assert.assertEquals("xyz", suggestUrlUserParams.getText());
        Assert.assertThat(params.getBlacklist(), defaultBlacklistMatcher);
    }

    @Test
    public void testBlacklistSingle() {
        HttpServletRequest request = MockRequestBuilder.start()
                .param("url", "http://abc.def")
                .param("blacklist", "123")
                .build();

        UrlUserParams params = parser.get(request);
        Assert.assertEquals("http://abc.def", params.getUrl());
        Assert.assertThat(params.getBlacklist(), Matchers.contains("123"));
    }

    @Test
    public void testBlacklistList() {
        HttpServletRequest request = MockRequestBuilder.start()
                .param("url", "http://abc.def")
                .param("blacklist", "def,xyz,%2Cd")
                .build();

        UrlUserParams params = parser.get(request);
        Assert.assertEquals("http://abc.def", params.getUrl());
        Assert.assertThat(params.getBlacklist(), Matchers.containsInAnyOrder("def", "xyz", "%2Cd"));
    }

    @Test
    public void testBlacklistDefault() {
        HttpServletRequest request = MockRequestBuilder.start()
                .param("url", "http://abc.def")
                .param("blacklist", "")
                .build();

        UrlUserParams params = parser.get(request);
        Assert.assertEquals("http://abc.def", params.getUrl());

        Assert.assertThat(params.getBlacklist(), defaultBlacklistMatcher);
    }

}
