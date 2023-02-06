package ru.yandex.market.mboc.integration.test;

import java.util.Collections;
import java.util.Objects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.mboc.integration.test.config.HttpIntegrationTestConfig.CommonTestParameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProxyHandlesTest extends BaseHttpIntegrationTestClass {

    private String cookie;

    @Autowired
    private CommonTestParameters config;

    @Before
    public void setUp() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String pass = System.getenv("robot_mbo_dev_pass");
        assertNotNull(pass);
        assertFalse(pass.isBlank());
        HttpEntity<String> entity = new HttpEntity<>("login=robot-mbo-dev&passwd=" + pass, headers);
        RestTemplate rest = new RestTemplate();
        final ResponseEntity<String> bb = rest.exchange("https://passport.yandex-team.ru/auth",
            HttpMethod.POST, entity, String.class);
        cookie = String.join(";", Objects.requireNonNull(bb.getHeaders().get("Set-Cookie")));
    }

    @Test
    public void testPromos() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", cookie);
        HttpEntity<String> entity = new HttpEntity<>("", headers);
        final ResponseEntity<String> exchange = new RestTemplate().exchange(config.getIntTestHandlesHost() +
            "/promo-management-api/v1/promos/meta", HttpMethod.GET, entity, String.class);
        assertEquals(HttpStatus.OK, exchange.getStatusCode());
        assertNotNull(exchange.getBody());
        assertTrue(exchange.getBody().contains("allowedOfferFilters"));
    }

    @Ignore
    @Test
    public void testCustomsCommCodes() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", cookie);
        HttpEntity<String> entity = new HttpEntity<>("", headers);
        final ResponseEntity<String> exchange = new RestTemplate().exchange(config.getIntTestHandlesHost() +
            "/mdm-api/customs-comm-codes/list", HttpMethod.GET, entity, String.class);
        assertEquals(HttpStatus.OK, exchange.getStatusCode());
        assertNotNull(exchange.getBody());
        assertTrue(exchange.getBody().contains("<parentId>"));
        assertTrue(exchange.getBody().contains("<code>"));
    }

    @Ignore
    @Test
    public void testAutoorder() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", cookie);
        HttpEntity<String> entity = new HttpEntity<>("", headers);
        final ResponseEntity<String> exchange = new RestTemplate().exchange(config.getIntTestHandlesHost() +
            "/autoorder/warehouses", HttpMethod.GET, entity, String.class);
        assertEquals(HttpStatus.OK, exchange.getStatusCode());
        assertNotNull(exchange.getBody());
        assertTrue(exchange.getBody().contains("logisticPointId"));
    }
}
