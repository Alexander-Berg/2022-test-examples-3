package ru.yandex.market.logistics.nesu.client;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.nesu.client.enums.DropoffDisablingRequestStatus;
import ru.yandex.market.logistics.nesu.client.model.dropoff.DropoffDisablingRequestCreatedDto;
import ru.yandex.market.logistics.nesu.client.model.dropoff.DropoffDisablingRequestDto;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

@DisplayName("Создание заявки на отключение дропоффа")
class DisableDropoffClientTest extends AbstractClientTest {

    private static final DropoffDisablingRequestDto REQUEST = DropoffDisablingRequestDto.builder()
        .logisticPointId(11L)
        .reason("UNPROFITABLE")
        .startClosingDate(LocalDateTime.of(2022, 1, 17, 17, 0))
        .closingDate(LocalDateTime.of(2022, 1, 22, 17, 0))
        .build();

    private static final DropoffDisablingRequestCreatedDto RESPONSE = DropoffDisablingRequestCreatedDto.builder()
        .id(1L)
        .logisticPointId(11L)
        .reason("UNPROFITABLE")
        .status(DropoffDisablingRequestStatus.SCHEDULED)
        .startClosingDate(LocalDateTime.of(2022, 1, 17, 17, 0))
        .closingDate(LocalDateTime.of(2022, 1, 22, 17, 0))
        .build();

    @Test
    @DisplayName("Успех")
    void success() {
        mock.expect(requestTo(startsWith(uri + "/internal/dropoff-disabling")))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonRequestContent("request/dropoff/dropoff_disabling_request.json"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/dropoff/dropoff_disabling_response.json"))
            );

        softly.assertThat(client.createDropoffDisablingRequest(REQUEST))
            .isEqualTo(RESPONSE)
            .usingRecursiveComparison();
    }

}
