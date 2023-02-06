package ru.yandex.market.mbo.gwt.client.models;

import com.google.gwt.regexp.shared.RegExp;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author ayratgdl
 * @date 03.03.17
 */
public class MarketDomainRegExpTest {
    private static final String MARKET_DOMAIN = "market.yandex.ru";
    private static final String CONTENT_PREVIEW_DOMAIN = "market.content-preview.yandex.ru";

    @Test
    public void replaceAllMarketDomainTest() {
        replaceDomainTest(MarketDomainRegExp.ALL_MARKET, MARKET_DOMAIN);
        replaceDomainTest(MarketDomainRegExp.ALL_MARKET, CONTENT_PREVIEW_DOMAIN);
    }

    @Test
    public void replaceMarketDomainTest() {
        replaceDomainTest(MarketDomainRegExp.MARKET, MARKET_DOMAIN);

        assertEquals(CONTENT_PREVIEW_DOMAIN, MarketDomainRegExp.MARKET.replace(CONTENT_PREVIEW_DOMAIN, "/"));
    }

    @Test
    public void replaceContentPreviewDomainTest() {
        replaceDomainTest(MarketDomainRegExp.CONTENT_PREVIEW, CONTENT_PREVIEW_DOMAIN);

        assertEquals(MARKET_DOMAIN, MarketDomainRegExp.CONTENT_PREVIEW.replace(MARKET_DOMAIN, "/"));
    }

    private void replaceDomainTest(RegExp domainRegExp, String domain) {
        assertEquals("/", domainRegExp.replace(domain, "/"));
        assertEquals("/", domainRegExp.replace(domain + "/", "/"));
        assertEquals("/abc", domainRegExp.replace(domain + "/abc", "/"));
        assertEquals("/?a=b&c=d", domainRegExp.replace("http://" + domain + "?a=b&c=d", "/"));
        assertEquals("/", domainRegExp.replace("https://" + domain, "/"));

        String otherSite = "other.domain?from=" + domain;
        assertEquals(otherSite, domainRegExp.replace(otherSite, "/"));
    }
}
