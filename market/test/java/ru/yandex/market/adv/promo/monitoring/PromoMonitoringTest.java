package ru.yandex.market.adv.promo.monitoring;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;

public class PromoMonitoringTest extends FunctionalTest {
    @Test
    void testJobStatus() {
        ResponseEntity<String> resp = FunctionalTestHelper.get(
                baseUrl() + "/monitoring/jobStatus/importPartnerPromosExecutor");
        int statusCodeValue = resp.getStatusCodeValue();
        Assertions.assertEquals(200, statusCodeValue);
        Assertions.assertNotNull(resp.getBody());
        Assertions.assertTrue(resp.getBody().matches("[012];<(OK|WARN|CRIT)>.*"));
    }
}
