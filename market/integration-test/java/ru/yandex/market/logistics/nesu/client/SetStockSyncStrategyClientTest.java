package ru.yandex.market.logistics.nesu.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class SetStockSyncStrategyClientTest extends AbstractClientTest {

    @Test
    void setStockSyncStrategy() {
        mock.expect(requestTo(startsWith(uri + "/internal/partner/1/set-stock-sync-strategy")))
            .andExpect(queryParam("shopId", "2"))
            .andExpect(queryParam("isFeedStrategy", "true"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(OK));

        client.setStockSyncStrategy(1, 2, true);
    }
}
