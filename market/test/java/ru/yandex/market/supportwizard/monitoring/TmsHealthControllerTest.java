package ru.yandex.market.supportwizard.monitoring;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;

public class TmsHealthControllerTest extends BaseFunctionalTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    @DbUnitDataSet(before = "TmsHealthControllerTest.before.csv")
    void testJobStatus() {
        ResponseEntity<String> resp =
                testRestTemplate.getForEntity("/jobStatus", String.class);
        int statusCodeValue = resp.getStatusCodeValue();
        Assert.assertEquals(200, statusCodeValue);
        Assert.assertNotNull(resp.getBody());
        Assert.assertTrue(resp.getBody().startsWith("2;<CRIT>"));
    }
}
