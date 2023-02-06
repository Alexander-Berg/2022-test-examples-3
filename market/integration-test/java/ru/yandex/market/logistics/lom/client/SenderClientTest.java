package ru.yandex.market.logistics.lom.client;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.model.dto.SenderSearchFilterDto;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class SenderClientTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Получить сендеров, имеющих заказы в одном из переданных статусов")
    void getOrderById() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/senders/search"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(extractFileContent("request/senders/filter-having-orders-with-status.json")))
            .andRespond(
                withSuccess(
                    extractFileContent("response/senders/filter-having-orders-with-status.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        List<Long> actual = lomClient.searchSenders(
            SenderSearchFilterDto.builder()
                .senderIds(Set.of(11L))
                .platformClient(PlatformClient.YANDEX_DELIVERY)
                .haveWaybillSegmentStatuses(Set.of(SegmentStatus.IN))
                .build()
        );
        List<Long> expected = List.of(11L);
        softly.assertThat(actual).isEqualTo(expected);
    }
}
