package ru.yandex.market.tms;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;

public class TmsHealthControllerTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "TmsHealthControllerTest.before.csv")
    void testJobStatus() {
        ResponseEntity<String> resp = FunctionalTestHelper.get(getBaseUrl() + "/api/jobStatus");
        int statusCodeValue = resp.getStatusCodeValue();
        Assert.assertEquals(200, statusCodeValue);
        Assert.assertNotNull(resp.getBody());
        Assert.assertTrue(resp.getBody().matches("[012];<(OK|WARN|CRIT)>.*"));
    }
}
