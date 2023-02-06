package ru.yandex.market.logistics.nesu.client;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@DisplayName("Генерация транспортной накладной в клиенте")
public class GeneratePartnerShipmentTransportationWaybillClientTest extends AbstractClientTest {

    @Test
    @DisplayName("Успех")
    void success() {
        byte[] content = {1, 2, 3};
        mock.expect(requestTo(startsWith(uri + "/internal/partner/shipments/1000/transportation-waybill")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("userId", "100"))
            .andExpect(queryParam("shopId", "500"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(content)
            );

        softly.assertThat(client.generateTransportationWaybill(100, 500, 1000)).isEqualTo(content);
    }

    @Test
    @DisplayName("Успех, список shopId")
    void successWithShopIds() {
        byte[] content = {1, 2, 3};
        mock.expect(requestTo(startsWith(uri + "/internal/partner/shipments/1000/transportation-waybill")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("userId", "100"))
            .andExpect(queryParam("shopIds",  String.valueOf(100L), String.valueOf(200L), String.valueOf(300L)))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(content)
            );

        softly.assertThat(client.generateTransportationWaybill(100, Set.of(100L, 200L, 300L), 1000))
            .isEqualTo(content);
    }

    @Test
    @DisplayName("Bad Request")
    void error() {
        mock.expect(requestTo(startsWith(uri + "/internal/partner/shipments/1000/transportation-waybill")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("userId", "100"))
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
            );
        Assertions.assertThrows(
            HttpTemplateException.class,
            () -> client.generateTransportationWaybill(100, Set.of(), 1000));
    }
}
