package ru.yandex.market.api.controller;

import java.util.Collections;

import javax.inject.Inject;

import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.XmlUtil;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.httpclient.clients.ClckTestClient;

import static org.junit.Assert.assertEquals;

@WithContext
@WithMocks
public class PartnerUrlControllerTest extends BaseTest {

    private static final String PARTNER_LINK_PATH = "/v3/affiliate/partner/link/create?";
    private static final String PARTNER_LINK_BATCH_PATH = "/v3/affiliate/partner/link/batchCreate?";

    public static final String FORMAT = "&format=xml";

    @Inject
    ClckTestClient clckTestClient;

    @Test
    public void shouldTransformValidUrl() {
        clckTestClient.clck(
                "https://market.yandex.ru?clid=12331&vid=123&pp=900&mclid=1003&distr_type=7",
                "ya.cc/short_url"
        );

        ResponseEntity<String> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + PARTNER_LINK_PATH + "url=https://market.yandex.ru&clid=12331&vid=123" + FORMAT,
                HttpMethod.GET,
                new HttpEntity<>(Collections.emptyList()),
                String.class
        );
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        XmlUtil.assertEqual(
                PartnerUrlController.class,
                "v3/partner_url/partner_url_ok.xml",
                responseEntity.getBody(),
                attr -> !"time".equals(attr.getName())
                        && !("id".equals(attr.getName()) && "context".equals(attr.getOwnerElement().getNodeName()))
        );

    }

    @Test
    public void batchCreateUrl() {
        clckTestClient.clck(
                "https://market.yandex.ru?clid=12331&vid=123&pp=900&mclid=1003&distr_type=7",
                "ya.cc/short_url1"
        );
        clckTestClient.clck(
                "https://market.yandex.ru/product--konvektor-nobo-ntl4s-05/53345803?clid=12331&vid=123&pp=900&mclid=1003&distr_type=7",
                "ya.cc/short_url2"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + PARTNER_LINK_BATCH_PATH + "clid=12331&vid=123&needShortLinks=true" + FORMAT,
                HttpMethod.POST,
                new HttpEntity<>("{\"urls\": [\"https://market.yandex.ru\", \"https://market.yandex.ru/product--konvektor-nobo-ntl4s-05/53345803\"]}", headers),
                String.class
        );
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        XmlUtil.assertEqual(
                PartnerUrlController.class,
                "v3/partner_url/partner_url_batch_ok.xml",
                responseEntity.getBody(),
                attr -> !"time".equals(attr.getName())
                        && !("id".equals(attr.getName()) && "context".equals(attr.getOwnerElement().getNodeName()))
        );

    }

}
