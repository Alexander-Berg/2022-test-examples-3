package ru.yandex.market.vendors.analytics.tms.jobs.offer;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.tms.BalanceFunctionalTest;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "OfferVendors.common.before.csv")
class ActualizeVendorDatasourcesExecutorTest extends BalanceFunctionalTest {

    @Autowired
    private ActualizeVendorDatasourcesExecutor actualizeVendorDatasourcesExecutor;

    @Test
    @DbUnitDataSet(after = "ActualizeVendorDatasourcesExecutorTest.actualize.after.csv")
    void actualize() {
        mockVendorApiServer.expect(once(), requestTo(vendorDatasourcesUrl(List.of(1L, 2L, 3L, 4L, 5L))))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(loadFromFile("ActualizeVendorDatasourcesExecutorTest.cs-billing.response.json"))
                );
        actualizeVendorDatasourcesExecutor.doJob(null);
    }
}