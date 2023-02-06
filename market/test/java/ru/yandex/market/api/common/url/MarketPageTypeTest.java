package ru.yandex.market.api.common.url;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MarketPageTypeTest {

    @Test
    public void shouldReturnMainPageType() {
        String url = "https://market.yandex.ru/";
        assertEquals(MarketPageType.MARKET_MAIN, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void shouldReturnProductPageType() {
        String url = "https://market.yandex.ru/product--robot-pylesos-xiaomi-mi-robot-vacuum-cleaner/14260832";
        assertEquals(MarketPageType.MARKET_PRODUCT, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void shouldReturnCategoryPageType() {
        String url = "https://market.yandex.ru/catalog--elektronika/54440";
        assertEquals(MarketPageType.MARKET_CATEGORY, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void shouldReturnProductCategoryListPageType() {
        String url = "https://market.yandex.ru/catalog--umnye-chasy-i-braslety/56034/list?hid=10498025";
        assertEquals(MarketPageType.MARKET_PRODUCT_CATEGORY_LIST, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void shouldReturnSearchResultPageType() throws UnsupportedEncodingException {
        String url = "https://market.yandex.ru/search?text="
                + URLEncoder.encode("аккумуляторы makita", StandardCharsets.UTF_8.name())
                + "&cvredirect=0&track=redirbarup&local-offers-first=0";
        assertEquals(MarketPageType.MARKET_SEARCH_RESULT, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void shouldReturnPromoLandingageType() {
        String url = "https://market.yandex.ru/promo/eldorado";
        assertEquals(MarketPageType.MARKET_PROMO_LANDING, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void shouldReturnMarketJournalPageType() {
        String url = "https://market.yandex.ru/journal/overview/kapsulnaja-kofemashina-nespresso-vertuo-plus-d";
        assertEquals(MarketPageType.MARKET_MARKET_JOURNAL, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void shouldReturnBrandPageType() {
        String url = "https://market.yandex.ru/brands--acuvue/10714190";
        assertEquals(MarketPageType.MARKET_BRAND, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void shouldReturnPokupkiMainPageType() {
        String url = "https://pokupki.market.yandex.ru";
        assertEquals(MarketPageType.POKUPKI_MAIN, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void shouldReturnPokupkiProductPageType() {
        String url = "https://pokupki.market.yandex.ru/product/smartfon-apple-iphone-x-64gb-seryi-kosmos-mqac2ru-a/100210864686";
        assertEquals(MarketPageType.POKUPKI_PRODUCT, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void shouldReturnPokupkiProductCategoryType() {
        String url = "https://pokupki.market.yandex.ru/catalog/elektronika/80155?hid=198119";
        assertEquals(MarketPageType.POKUPKI_CATEGORY, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void shouldReturnPokupkiProductCategoryListType() {
        String url = "https://pokupki.market.yandex.ru/catalog/smartfony-i-mobilnye-telefony/80542/list?hid=91491";
        assertEquals(MarketPageType.POKUPKI_PRODUCT_CATEGORY_LIST, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void shouldReturnPokupkiPromoPageType() {
        String url = "https://pokupki.market.yandex.ru/special/bonusy";
        assertEquals(MarketPageType.POKUPKI_PROMO, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void shouldReturnPokupkiBonusPageType() {
        String url = "https://pokupki.market.yandex.ru/search?bonusId=322948492";
        assertEquals(MarketPageType.POKUPKI_BONUS, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void shouldReturnPokupkiSearchPageType() throws UnsupportedEncodingException {
        String url = "https://pokupki.market.yandex.ru/search?cvredirect=2&text="
                + URLEncoder.encode("аккумуляторы makita", StandardCharsets.UTF_8.name());
        assertEquals(MarketPageType.POKUPKI_SEARCH_RESULT, MarketPageType.getUrlPageType(url));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptIllegalUrl() {
        String url = "https://mail.yandex.ru/";
        MarketPageType.getUrlPageType(url);
    }

    @Test
    public void emptyPathWithParams() {
        String url = "https://market.yandex.ru?clid=123";
        assertEquals(MarketPageType.MARKET_MAIN, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void emptyPathWithParams2() {
        String url = "https://market.yandex.ru/?clid=123";
        assertEquals(MarketPageType.MARKET_MAIN, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void testDomainKz() {
        String url = "http://market.yandex.kz/brands--acuvue/10714190";
        assertEquals(MarketPageType.MARKET_BRAND, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void mobileOffer() {
        String url = "https://m.market.yandex.ru/offer/8SSP3bRhzvgTv8PEV-jU1w";
        assertEquals(MarketPageType.MARKET_PRODUCT_OFFER, MarketPageType.getUrlPageType(url));
    }

    @Test
    public void mobileOfferBy() {
        String url = "https://m.market.yandex.by/offer/8SSP3bRhzvgTv8PEV-jU1w";
        assertEquals(MarketPageType.MARKET_PRODUCT_OFFER, MarketPageType.getUrlPageType(url));
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongUrl() {
        String url = "https://supermarket.yandex.ru/offer/8SSP3bRhzvgTv8PEV-jU1w";
        MarketPageType.getUrlPageType(url);
    }
}
