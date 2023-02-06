package ru.yandex.market.logistics.management.client.async.putReferenceWarehouses;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.client.async.AbstractClientTest;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class PutReferenceWarehousesTest extends AbstractClientTest {
    @Test
    void successRequest() {
        mockServer.expect(requestTo(uri + "/lgw_callback/put_reference_warehouses_success"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json("{ \"partnerId\": 1, \"warehouseIds\": [1] }"))
            .andRespond(withStatus(OK));

        lmsAsyncClient.putReferenceWarehouseSuccess(1L, Collections.singletonList(1L));
    }

    @Test
    void errorRequest() {
        mockServer.expect(requestTo(uri + "/lgw_callback/put_reference_warehouses_error"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json("{ \"partnerId\": 1, \"warehouseIds\": [1] }"))
            .andRespond(withStatus(OK));

        lmsAsyncClient.putReferenceWarehouseError(1L, Collections.singletonList(1L), null, null);
    }
}
