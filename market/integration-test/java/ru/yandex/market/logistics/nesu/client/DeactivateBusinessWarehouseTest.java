package ru.yandex.market.logistics.nesu.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class DeactivateBusinessWarehouseTest extends AbstractClientTest {
    @Test
    @DisplayName("Деактивация бизнес-склада")
    void success() {
        mock.expect(requestTo(startsWith(uri + "/internal/business-warehouse/2/deactivate")))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(HttpStatus.OK));

        softly.assertThatCode(() -> client.deactivateBusinessWarehouse(2L)).doesNotThrowAnyException();
    }
}
