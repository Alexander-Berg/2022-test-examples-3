package ru.yandex.market.ff4shops.client;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.ff4shops.api.model.GetOrdersStatusDto;
import ru.yandex.market.ff4shops.api.model.OrderStatusHistoryDto;
import ru.yandex.market.ff4shops.api.model.StatusDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Получение статусов заказов")
public class GetOrdersStatusesClientTest extends AbstractClientTest {
    @Test
    void success() {
        mock.expect(requestTo(startsWith(uri + "/getOrdersStatus")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(OK)
                    .body(extractFileContent("response/get_orders_status.json"))
                    .contentType(APPLICATION_JSON)
            );

        List<OrderStatusHistoryDto> response = client.getOrdersStatus(new GetOrdersStatusDto(List.of(1L, 2L, 3L)));
        assertThat(response).usingRecursiveFieldByFieldElementComparator().containsExactly(
            new OrderStatusHistoryDto(List.of(new StatusDto(
                "ORDER_READY_TO_BE_SEND_TO_SO_FF",
                1580691906000L,
                null
            )), 2),
            new OrderStatusHistoryDto(List.of(new StatusDto(
                "ORDER_CREATED_BUT_NOT_APPROVED_FF",
                1583287567000L,
                null
            )), 3),
            new OrderStatusHistoryDto(List.of(new StatusDto(
                "ORDER_ITEMS_AUTOMATICALLY_REMOVED_FF",
                1586056029000L,
                null
            )), 4),
            new OrderStatusHistoryDto(List.of(new StatusDto(
                "ORDER_READY_TO_BE_SEND_TO_SO_FF",
                1588738089000L,
                null
            )), 5)
        );
    }
}
