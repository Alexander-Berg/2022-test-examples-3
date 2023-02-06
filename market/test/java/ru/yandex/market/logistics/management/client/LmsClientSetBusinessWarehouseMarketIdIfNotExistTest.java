package ru.yandex.market.logistics.management.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.marketId.MarketIdDto;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

@DisplayName("Обновление marketId бизнес-склада")
class LmsClientSetBusinessWarehouseMarketIdIfNotExistTest extends AbstractClientTest {
    @Test
    void success() {
        mockServer.expect(requestTo(uri + "/externalApi/business-warehouse/2/market-id"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/marketId/set_market_id_request.json"), true))
            .andRespond(withStatus(OK));
        softly.assertThatCode(
            () -> client.setBusinessWarehouseMarketId(2L, MarketIdDto.of(300L))
        )
            .doesNotThrowAnyException();
    }
}
