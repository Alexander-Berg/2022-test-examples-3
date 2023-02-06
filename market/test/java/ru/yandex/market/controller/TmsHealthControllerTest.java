package ru.yandex.market.controller;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.shop.FunctionalTest;

public class TmsHealthControllerTest extends FunctionalTest {
    @Test
    void testJobStatus() {
        ResponseEntity<String> resp = FunctionalTestHelper.get(getBaseUrl() + "/api/jobStatus");
        int statusCodeValue = resp.getStatusCodeValue();
        Assert.assertEquals(200, statusCodeValue);
        Assert.assertNotNull(resp.getBody());
        Assert.assertTrue(resp.getBody().matches("[012];<(OK|WARN|CRIT)>.*"));
    }
}
