package ru.yandex.market.logistics.nesu.client;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.nesu.client.enums.PageSize;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentLabelRequest;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

@DisplayName("Генерация ярлыков заказов в клиенте")
public class GeneratePartnerShipmentLabelsClientTest extends AbstractClientTest {

    @Test
    @DisplayName("Успех")
    void success() {
        byte[] content = {1, 2, 3};
        mock.expect(requestTo(startsWith(uri + "/internal/partner/shipments/labels")))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(queryParam("userId", "100"))
            .andExpect(queryParam("shopId", "500"))
            .andExpect(jsonRequestContent("request/shipments/generate_labels.json"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(content)
            );

        softly.assertThat(client.generateLabels(
            100,
            500,
            PartnerShipmentLabelRequest.builder()
                .shipmentIds(List.of(200L, 220L))
                .orderIds(List.of(300L, 330L))
                .pageSize(PageSize.A6)
                .build()
        )).isEqualTo(content);
    }

}
