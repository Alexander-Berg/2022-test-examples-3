package ru.yandex.market.ff4shops.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.ff4shops.api.model.DebugStatus;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Получение данных об отладке стоков")
public class GetDebugStocksClientTest extends AbstractClientTest {

    @Test
    @DisplayName("Успешное получние статуса отладки стоков")
    void getDebugStocks() {
        mock.expect(requestTo(startsWith(uri + "/stocks/debug/status?supplier_id=1")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .body(extractFileContent("response/get_debug_stocks.json"))
                                .contentType(MediaType.APPLICATION_JSON)
                );

        DebugStatus status = client.getDebugStockStatus(1);
        Assertions.assertEquals(DebugStatus.NO_STOCKS, status);
    }
}
