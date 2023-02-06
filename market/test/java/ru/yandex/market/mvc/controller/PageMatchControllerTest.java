package ru.yandex.market.mvc.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.security.config.FunctionalTest;

class PageMatchControllerTest extends FunctionalTest {
    private static final String PAGEMATCH_ROW_TEMPLATE = "%s\t%s\t%s";

    @Autowired
    private String baseUrl;

    @Test
    void testPagematch() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/pagematch");
        String expected = String.format(PAGEMATCH_ROW_TEMPLATE, "pagematch", "/pagematch", "java-sec-http-proxy");
        Assertions.assertTrue(response.getBody().contains(expected));
    }
}
