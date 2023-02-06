package ru.yandex.market.ff4shops.client;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.ff4shops.api.model.OutboundDto;
import ru.yandex.market.ff4shops.api.model.OutboundFileDto;
import ru.yandex.market.ff4shops.api.model.SearchOutboundsFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Получение данных отправок в клиенте")
public class SearchOutboundsClientTest extends AbstractClientTest {
    private static final OutboundDto OUTBOUND1 = new OutboundDto()
            .setCreated(Instant.parse("2020-01-01T15:30:00Z"))
            .setConfirmed(Instant.parse("2020-02-03T09:36:43Z"))
            .setIntervalTo(Instant.parse("2020-01-04T15:30:00Z"))
            .setIntervalFrom(Instant.parse("2020-01-04T11:30:00Z"))
            .setId(1)
            .setYandexId("1")
            .setOrderIds(List.of(1L, 2L, 3L));

    private static final OutboundDto OUTBOUND2 = new OutboundDto()
            .setCreated(Instant.parse("2020-01-02T16:00:00Z"))
            .setConfirmed(Instant.parse("2020-01-03T23:48:17Z"))
            .setId(2)
            .setYandexId("2")
            .setOrderIds(List.of(4L, 5L, 6L))
            .setIntervalTo(Instant.parse("2020-01-04T14:30:00Z"))
            .setIntervalFrom(Instant.parse("2020-01-04T12:30:00Z"))
            .setFiles(List.of(
                    new OutboundFileDto()
                            .setId(10L)
                            .setName("test-file")
                            .setUrl("test-url")
            ));

    @Test
    @DisplayName("Успешное получение: 3-й отправки нет в БД")
    void getOutboundsSuccess() {
        mock.expect(requestTo(startsWith(uri + "/partner/outbounds/search")))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(
                        withStatus(OK)
                                .body(extractFileContent("response/get_outbounds_success.json"))
                                .contentType(APPLICATION_JSON)
                );

        List<OutboundDto> outbounds = client.searchOutbounds(
                new SearchOutboundsFilter().setOutboundYandexIds(List.of("1", "2", "3"))
        );
        assertThat(outbounds).usingRecursiveFieldByFieldElementComparator().containsExactly(OUTBOUND1, OUTBOUND2);
    }
}
