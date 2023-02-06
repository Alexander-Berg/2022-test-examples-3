package ru.yandex.market.logistics.lom.client;

import java.util.Map;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.model.dto.CancellationOrderConfirmationDto;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

class CancellationRequestsClientTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Подтвердить заявку на отмену заказа")
    void confirmOrderRequests() {
        mock.expect(method(HttpMethod.POST))
            .andExpect(requestTo(uri + "/cancellation-requests/order/confirm"))
            .andExpect(jsonRequestContent("request/order/cancellationrequests/confirm_order_requests.json"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/order/cancellationrequests/confirm_order.json"))
            );

        Map<Long, String> result = lomClient.manuallyConfirmCancellationOrderRequest(
            CancellationOrderConfirmationDto.builder()
                .barcodes(ImmutableSet.of("barcode1", "barcode2"))
                .requestIds(ImmutableSet.of(1L, 2L))
                .reason("test reason")
                .build()
        );
        softly.assertThat(result).isEqualTo(Map.of(
            1L, "barcode1",
            2L, "barcode2"
        ));
    }
}
