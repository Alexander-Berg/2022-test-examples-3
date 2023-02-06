package ru.yandex.market.ff4shops.client;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import ru.yandex.market.ff4shops.api.model.ConfirmOutboundDto;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@DisplayName("Подтверждение отправки в клиенте")
public class ConfirmOutboundClientTest extends AbstractClientTest {
    @Test
    void confirmOutbound() {
        mock.expect(requestTo(uri + "/partner/outbounds/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(OK).contentType(APPLICATION_JSON)
                );

        Assertions.assertThatCode(
                () -> client.confirmOutbound(new ConfirmOutboundDto())
        ).doesNotThrowAnyException();
    }
}
