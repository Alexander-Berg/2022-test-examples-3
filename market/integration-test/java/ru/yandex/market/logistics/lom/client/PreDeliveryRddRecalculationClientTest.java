package ru.yandex.market.logistics.lom.client;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestPayloadDto;
import ru.yandex.market.logistics.lom.model.dto.PreDeliveryRecalculateRouteDatesRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

@DisplayName("Тест клиента на создание заявки на пересчёт дат по сегментам заказа в Комбинаторе")
public class PreDeliveryRddRecalculationClientTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Создать заявку на пересчёт дат по сегментам заказа в Комбинаторе")
    void testRecalculateRouteDates() throws IOException {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/preDeliveryRddRecalculation"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonRequestContent("request/order/pre_delivery_rdd_recalculation.json"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/order/pre_delivery_rdd_recalculation.json"))
            );

        PreDeliveryRecalculateRouteDatesRequestDto requestDto = PreDeliveryRecalculateRouteDatesRequestDto.builder()
            .orderId(123L)
            .isRddDay(true)
            .build();

        ChangeOrderRequestDto expectedChangeOrderRequestDto = ChangeOrderRequestDto.builder()
            .id(1L)
            .requestType(ChangeOrderRequestType.RECALCULATE_ROUTE_DATES)
            .status(ChangeOrderRequestStatus.CREATED)
            .reason(ChangeOrderRequestReason.PRE_DELIVERY_ROUTE_RECALCULATION)
            .created(Instant.parse("2022-01-27T18:30:00.000000Z"))
            .updated(Instant.parse("2022-01-27T18:30:00.000000Z"))
            .payloads(Set.of(
                ChangeOrderRequestPayloadDto.builder()
                    .status(ChangeOrderRequestStatus.CREATED)
                    .payload(objectMapper.readTree(
                        extractFileContent("request/order/pre_delivery_recalculate_route_dates_payload.json")
                    ))
                    .build()
            ))
            .waybillSegmentId(10L)
            .build();

        softly.assertThat(lomClient.preDeliveryRddRecalculation(requestDto))
            .as("Asserting that the change order request in response is parsed correctly")
            .isEqualTo(expectedChangeOrderRequestDto);
    }
}
