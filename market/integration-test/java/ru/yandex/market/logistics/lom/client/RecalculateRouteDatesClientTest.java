package ru.yandex.market.logistics.lom.client;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestPayloadDto;
import ru.yandex.market.logistics.lom.model.dto.RecalculateRouteDatesRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

@DisplayName("Тест клиента на создание заявки на пересчёт дат по сегментам заказа в Комбинаторе")
public class RecalculateRouteDatesClientTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Создать заявку на пересчёт дат по сегментам заказа в Комбинаторе")
    void testRecalculateRouteDates() throws IOException {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/recalculateRouteDates"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonRequestContent("request/order/recalculate_route_dates.json"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/order/recalculate_route_dates.json"))
            );

        RecalculateRouteDatesRequestDto requestDto = RecalculateRouteDatesRequestDto.builder()
            .startDateTime(OffsetDateTime.parse("2020-11-01T16:00:00+03:00"))
            .segmentId(1L)
            .segmentStatus(SegmentStatus.IN)
            .build();

        ChangeOrderRequestDto expectedChangeOrderRequestDto = ChangeOrderRequestDto.builder()
            .id(1L)
            .requestType(ChangeOrderRequestType.RECALCULATE_ROUTE_DATES)
            .status(ChangeOrderRequestStatus.PROCESSING)
            .reason(ChangeOrderRequestReason.SHIPPING_DELAYED)
            .created(Instant.parse("2020-10-30T02:30:00.000000Z"))
            .updated(Instant.parse("2020-10-30T02:30:00.000000Z"))
            .payloads(Set.of(
                ChangeOrderRequestPayloadDto.builder()
                    .status(ChangeOrderRequestStatus.PROCESSING)
                    .payload(objectMapper.readTree(
                        extractFileContent("request/order/recalculate_route_dates_payload.json")
                    ))
                    .build()
            ))
            .waybillSegmentId(10L)
            .build();

        softly.assertThat(lomClient.recalculateRouteDates(requestDto))
            .as("Asserting that the change order request in response is parsed correctly")
            .isEqualTo(expectedChangeOrderRequestDto);
    }
}
