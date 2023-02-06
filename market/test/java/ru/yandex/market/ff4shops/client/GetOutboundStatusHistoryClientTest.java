package ru.yandex.market.ff4shops.client;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.ff4shops.api.model.OutboundStatusHistoryDto;
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

public class GetOutboundStatusHistoryClientTest extends AbstractClientTest {
    @Test
    @DisplayName("Успешное получение истории статусов отправок")
    void success() {
        mock.expect(requestTo(startsWith(uri + "/partner/outbounds/statusHistory")))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(
                        withStatus(OK)
                                .body(extractFileContent("response/getOutboundStatusHistoryResponse.json"))
                                .contentType(APPLICATION_JSON)
                );

        List<OutboundStatusHistoryDto> outbounds = client.getOutboundStatusHistory(
                new SearchOutboundsFilter().setOutboundYandexIds(List.of("1", "2", "3"))
        );
        assertThat(outbounds).usingRecursiveFieldByFieldElementComparator().containsExactly(
                new OutboundStatusHistoryDto(
                        "2",
                        "100",
                        List.of(
                                new StatusDto("CREATED", 1615895975000L, null),
                                new StatusDto("ASSEMBLED", 1616008655000L, null),
                                new StatusDto("TRANSFERRED", 1616008656000L, null)
                        )
                ),
                new OutboundStatusHistoryDto(
                        "3",
                        "200",
                        List.of(new StatusDto("ASSEMBLED", 1616008655000L, null))
                )
        );
    }
}
