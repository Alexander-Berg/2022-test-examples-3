package ru.yandex.market.logistics.lom.client;

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
import ru.yandex.market.logistics.lom.model.dto.OrderContactDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderRecipientRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.ContactType;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Обновление данных получателя заказа")
class UpdateOrderRecipientTest extends AbstractClientTest {
    @Autowired
    private LomClient lomClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Отправить запрос на обновление данных получателя заказа")
    void updateOrderRecipient() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/updateRecipient"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(extractFileContent("request/order/update/recipient/request.json"), true))
            .andRespond(withSuccess(
                extractFileContent("response/order/update_recipient.json"),
                MediaType.APPLICATION_JSON
            ));

        ChangeOrderRequestDto result = lomClient.updateOrderRecipient(createRequest());
        softly.assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(createExpectedResponse());
    }

    @Nonnull
    private UpdateOrderRecipientRequestDto createRequest() {
        return UpdateOrderRecipientRequestDto.builder()
            .checkouterRequestId(123L)
            .barcode("barcode")
            .email("email")
            .personalEmailId("personal-email-id")
            .contact(
                OrderContactDto.builder()
                    .comment("comment")
                    .contactType(ContactType.RECIPIENT)
                    .extension("extension")
                    .firstName("First name")
                    .lastName("Last name")
                    .middleName("Middle name")
                    .phone("phone")
                    .personalFullnameId("personal-fullname-id")
                    .personalPhoneId("personal-phone-id")
                    .build()
            )
            .build();
    }

    @Nonnull
    private ChangeOrderRequestDto createExpectedResponse() {
        return ChangeOrderRequestDto.builder()
            .id(1L)
            .requestType(ChangeOrderRequestType.RECIPIENT)
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
