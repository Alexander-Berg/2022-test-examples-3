package ru.yandex.market.logistics.lom.client;

import java.time.LocalDate;
import java.time.LocalTime;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.model.dto.UpdateOrderDeliveryDateRequestDto;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class UpdateOrderDeliveryDateTest extends AbstractClientTest {
    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Отправить запрос на обновление даты доставки заказа")
    void updateOrderDeliveryDate() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/updateDeliveryDate"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(extractFileContent("request/order/update/deliverydate/request.json"), true))
            .andRespond(withSuccess());
        lomClient.updateOrderDeliveryDate(createRequest());
    }

    @Nonnull
    UpdateOrderDeliveryDateRequestDto createRequest() {
        return UpdateOrderDeliveryDateRequestDto
            .builder()
            .barcode("LOinttest-1")
            .dateMin(LocalDate.of(2020, 10, 8))
            .dateMax(LocalDate.of(2020, 10, 9))
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(18, 0))
            .changeRequestExternalId(123456L)
            .build();
    }
}
