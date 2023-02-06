package ru.yandex.market.logistics.lom.client;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.model.dto.ChangeOrderToOnDemandRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class ChangeOrderToOnDemandTest extends AbstractClientTest {
    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Отправить запрос на изменение")
    void changeOrderToOnDemand() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/changeToOnDemand"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(extractFileContent("request/order/update/change_to_ondemand/request.json"), true))
            .andRespond(withSuccess());
        softly.assertThatCode(() -> lomClient.changeOrderToOnDemand(createRequest())).doesNotThrowAnyException();
    }

    @Nonnull
    private ChangeOrderToOnDemandRequestDto createRequest() {
        return ChangeOrderToOnDemandRequestDto
            .builder()
            .barcode("LOinttest-1")
            .reason(ChangeOrderRequestReason.SHIPPING_DELAYED)
            .segmentId(1L)
            .build();
    }
}
