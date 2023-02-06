package ru.yandex.market.logistics.nesu.client;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.nesu.client.enums.SenderStatus;
import ru.yandex.market.logistics.nesu.client.model.ContactDto;
import ru.yandex.market.logistics.nesu.client.model.PhoneDto;
import ru.yandex.market.logistics.nesu.client.model.SenderDto;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class GetSendersClientTest extends AbstractClientTest {
    private static final SenderDto SENDER_1 = SenderDto.builder()
        .id(1L)
        .shopId(1L)
        .name("test-sender-name")
        .status(SenderStatus.ACTIVE)
        .siteUrl("www.test-sender-name.com")
        .created(LocalDateTime.of(2020, 1, 1, 16, 0, 0))
        .contact(ContactDto.builder()
            .id(1L)
            .emails(List.of("test-email@test-sender-name.com"))
            .firstName("test-first-name")
            .lastName("test-last-name")
            .phone(PhoneDto.builder().phoneNumber("9999999999").build())
            .build())
        .build();

    private static final SenderDto SENDER_2 = SenderDto.builder()
        .id(2L)
        .shopId(1L)
        .name("test-sender-name-2")
        .status(SenderStatus.DELETED)
        .siteUrl("www.test-sender-name-2.com")
        .created(LocalDateTime.of(2020, 1, 2, 16, 0, 0))
        .contact(ContactDto.builder()
            .id(2L)
            .emails(List.of("test-email@test-sender-name.com"))
            .firstName("test-first-name")
            .lastName("test-last-name")
            .phone(PhoneDto.builder().phoneNumber("9999999999").build())
            .build())
        .build();

    @Test
    void getSenders() {
        mock.expect(requestTo(uri + "/internal/senders"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(OK).body(extractFileContent("response/senders_response.json")).contentType(APPLICATION_JSON)
            );

        List<SenderDto> senders = client.getSenders();

        softly.assertThat(senders)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(SENDER_1, SENDER_2);
    }

    @Test
    void getSender() {
        mock.expect(requestTo(uri + "/internal/senders/1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(OK).body(extractFileContent("response/sender_response.json")).contentType(APPLICATION_JSON)
            );

        SenderDto sender = client.getSender(1);
        softly.assertThat(sender).usingRecursiveComparison().isEqualTo(SENDER_1);
    }

}
