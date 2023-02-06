package ru.yandex.market.ff4shops.client;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.ff4shops.api.model.OutboundStatusDto;
import ru.yandex.market.ff4shops.api.model.SearchOutboundsFilter;
import ru.yandex.market.ff4shops.api.model.StatusDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class GetOutboundStatusClientTest extends AbstractClientTest {
    @Test
    @DisplayName("Успешное получение статусов отправок")
    void success() {
        mock.expect(requestTo(startsWith(uri + "/partner/outbounds/status")))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(
                        withStatus(OK)
                                .body(extractFileContent("response/get_outbound_status_response.json"))
                                .contentType(APPLICATION_JSON)
                );

        List<OutboundStatusDto> outbounds = client.getOutboundStatus(
                new SearchOutboundsFilter().setOutboundYandexIds(List.of("1", "2", "3"))
        );
        assertThat(outbounds).usingRecursiveFieldByFieldElementComparator().containsExactly(
                new OutboundStatusDto("2", "100", new StatusDto("TRANSFERRED", 1616008656000L, null)),
                new OutboundStatusDto("3", "200", new StatusDto("CREATED", 1616008656000L, null))
        );
    }
}
