package ru.yandex.market.logistics.lom.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestPayloadDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateLastMilePayload;
import ru.yandex.market.logistics.lom.model.dto.UpdateLastMileRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Обновление данных последней мили")
class UpdateLastMileTest extends AbstractClientTest {
    @Autowired
    private LomClient lomClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Отправить запрос на обновление данных последней мили (адрес или/и дата/интервалы доставки)")
    void updateOrderLastMile() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/updateLastMile"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(extractFileContent("request/order/update/lastmile/request.json"), true))
            .andRespond(withSuccess(
                extractFileContent("response/order/update_lastmile.json"),
                MediaType.APPLICATION_JSON
            ));

        ChangeOrderRequestDto result = lomClient.updateLastMile(createRequest());
        softly.assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(createExpectedResponse());
    }

    @Nonnull
    private UpdateLastMileRequestDto createRequest() {
        return UpdateLastMileRequestDto.builder()
            .barcode("barcode")
            .deliveryType(DeliveryType.COURIER)
            .route(JsonNodeFactory.instance.objectNode())
            .payload(createResponsePayload())
            .build();
    }

    @Nonnull
    private AddressDto createAddress() {
        return AddressDto.builder()
            .country("country")
            .federalDistrict("federalDistrict")
            .region("region")
            .locality("locality")
            .subRegion("subRegion")
            .settlement("settlement")
            .district("district")
            .street("street")
            .house("house")
            .building("building")
            .housing("housing")
            .room("room")
            .zipCode("zipCode")
            .porch("porch")
            .floor(1)
            .metro("metro")
            .latitude(BigDecimal.ZERO)
            .longitude(BigDecimal.ONE)
            .geoId(123)
            .intercom("intercom")
            .build();
    }

    @Nonnull
    private ChangeOrderRequestDto createExpectedResponse() {
        return ChangeOrderRequestDto.builder()
            .id(1L)
            .requestType(ChangeOrderRequestType.LAST_MILE)
            .status(ChangeOrderRequestStatus.PROCESSING)
            .reason(null)
            .payloads(Set.of(createPayload()))
            .waybillSegmentId(10L)
            .build();
    }

    @Nonnull
    private UpdateLastMilePayload createResponsePayload() {
        return UpdateLastMilePayload.builder()
            .address(createAddress())
            .comment("Комментарий")
            .checkouterChangeRequestId(12345L)
            .dateMin(LocalDate.of(2020, 10, 8))
            .dateMax(LocalDate.of(2020, 10, 9))
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(18, 0))
            .build();
    }

    @Nonnull
    private ChangeOrderRequestPayloadDto createPayload() {
        return ChangeOrderRequestPayloadDto.builder()
            .status(ChangeOrderRequestStatus.INFO_RECEIVED)
            .payload(objectMapper.convertValue(createResponsePayload(), JsonNode.class))
            .build();
    }
}
