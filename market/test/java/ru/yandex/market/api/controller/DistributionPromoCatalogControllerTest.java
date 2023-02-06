package ru.yandex.market.api.controller;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.XmlUtil;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.util.httpclient.clients.AffiliatePromoTestClient;
import ru.yandex.market.api.util.httpclient.clients.ClckTestClient;

import static org.junit.Assert.assertEquals;

public class DistributionPromoCatalogControllerTest extends BaseTest {
    // a value from content-api/make/download-test-cache/clients.csv
    private static final String CLID = "8888888";
    private static final String SECRET = "GGSj03H0ztvdd9yrs7o84gWjYy7Sjv";

    private static final String THIS_PATH = "V3/affiliate/promo/catalog";

    @Inject
    AffiliatePromoTestClient affiliatePromoTestClient;
    @Inject
    ClckTestClient clckTestClient;

    @Test
    public void test() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", SECRET);
        String expectedResponseFileName = "affiliate_promo_catalog_response.json";

        PageInfo pageInfoToMatch = new PageInfo();
        pageInfoToMatch.setCount(15);
        pageInfoToMatch.setPage(1);

        affiliatePromoTestClient.getCatalogPromosFull(pageInfoToMatch, ImmutableList.of(12), expectedResponseFileName);
        clckTestClient.clck("https://market.yandex.ru/special/cheapest-as-gift-2-3-landing?shopPromoId=%2317208&clid=8888888&pp=941&utm_term=promo_cat&utm_campaign=8888888&utm_medium=link&distr_type=7&mclid=1003&utm_source=partner_network", "http://ya.cc/shrturl1");
        clckTestClient.clck("https://market.yandex.ru/special/LETO10?clid=8888888&pp=941&utm_term=promo_cat&utm_campaign=8888888&utm_medium=link&distr_type=7&mclid=1003&utm_source=partner_network", "http://ya.cc/shrturl2");

        ResponseEntity<String> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + "/" + THIS_PATH + "?clid=" + CLID
                        + "&page=1&count=15&categories=12",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        XmlUtil.assertEqual(
                DistributionPromoCatalogControllerTest.class,
                "affiliate_promo_catalog_expected_result.xml",
                responseEntity.getBody());
    }

}