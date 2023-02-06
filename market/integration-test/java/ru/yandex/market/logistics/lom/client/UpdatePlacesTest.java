package ru.yandex.market.logistics.lom.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestPayloadDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderPlacesRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Обновление данных о грузоместах заказа")
class UpdatePlacesTest extends AbstractClientTest {
    @Autowired
    private LomClient lomClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Отправить запрос на обновление данных получателя заказа")
    void updatePlaces() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/1/updatePlaces"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(extractFileContent("request/order/update/places/request.json"), true))
            .andRespond(withSuccess(
                extractFileContent("response/order/update_places.json"),
                MediaType.APPLICATION_JSON
            ));

        ChangeOrderRequestDto result = lomClient.updatePlaces(1, createRequest());
        softly.assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(createExpectedResponse());
    }

    @Nonnull
    private UpdateOrderPlacesRequestDto createRequest() {
        return UpdateOrderPlacesRequestDto.builder()
            .waybillSegmentId(10L)
            .places(List.of(
                UpdateOrderPlacesRequestDto.Place.builder()
                    .externalId("externalId")
                    .dimensions(
                        KorobyteDto.builder()
                            .length(1)
                            .width(2)
                            .height(3)
                            .weightGross(new BigDecimal("4"))
                            .build()
                    )
                    .build()
            ))
            .build();
    }

    @Nonnull
    private ChangeOrderRequestDto createExpectedResponse() {
        return ChangeOrderRequestDto.builder()
            .id(1L)
            .requestType(ChangeOrderRequestType.UPDATE_PLACES)
            .status(ChangeOrderRequestStatus.PROCESSING)
            .reason(null)
            .payloads(Set.of(createPayload()))
            .waybillSegmentId(10L)
            .build();
    }

    @Nonnull
    private ChangeOrderRequestPayloadDto createPayload() {
        return ChangeOrderRequestPayloadDto.builder()
            .status(ChangeOrderRequestStatus.INFO_RECEIVED)
            .payload(objectMapper.convertValue(createRequest(), JsonNode.class))
            .build();
    }
}
