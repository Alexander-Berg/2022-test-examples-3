package ru.yandex.market.logistics.nesu.client;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentConfirmRequest;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

@DisplayName("Подтверждение отгрузки магазина в клиенте")
class ConfirmPartnerShipmentClientTest extends AbstractClientTest {

    @Test
    @DisplayName("Успех")
    void success() {
        mock.expect(requestTo(startsWith(uri + "/internal/partner/shipments/200/confirm")))
            .andExpect(method(HttpMethod.POST))
            .andExpect(queryParam("userId", "100"))
            .andExpect(queryParam("shopId", "500"))
            .andExpect(jsonRequestContent("request/shipments/confirm.json"))
            .andRespond(withStatus(HttpStatus.OK));

        softly.assertThatCode(() -> client.confirmShipment(
            100,
            500,
            200,
            shipmentConfirmRequest()
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Успех, список shopId")
    void successWithShopIds() {
        mock.expect(requestTo(startsWith(uri + "/internal/partner/shipments/200/confirm")))
            .andExpect(method(HttpMethod.POST))
            .andExpect(queryParam("userId", "100"))
            .andExpect(queryParam("shopIds", String.valueOf(100L), String.valueOf(200L), String.valueOf(300L)))
            .andExpect(jsonRequestContent("request/shipments/confirm.json"))
            .andRespond(withStatus(HttpStatus.OK));

        softly.assertThatCode(() -> client.confirmShipment(
            100,
            Set.of(100L, 200L, 300L),
            200,
            shipmentConfirmRequest()
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Bad Request")
    void error() {
        mock.expect(requestTo(startsWith(uri + "/internal/partner/shipments/200/confirm")))
            .andExpect(method(HttpMethod.POST))
            .andExpect(queryParam("userId", "100"))
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
            );
        Assertions.assertThrows(
            HttpTemplateException.class,
            () -> client.confirmShipment(100, Set.of(), 200, shipmentConfirmRequest()));
    }

    @Nonnull
    private PartnerShipmentConfirmRequest shipmentConfirmRequest() {
        return PartnerShipmentConfirmRequest.builder()
            .externalId("shipment-external-id")
            .orderIds(List.of(400L, 440L))
            .excludedOrderIds(List.of(410L, 450L))
            .build();
    }
}
