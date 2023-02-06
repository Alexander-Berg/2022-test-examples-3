package ru.yandex.market.ff4shops.client;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.ff4shops.api.model.StatusDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Получение истории заказа")
public class GetOrderHistoryClientTest extends AbstractClientTest {
    @Test
    void success() {
        mock.expect(requestTo(startsWith(uri + "/getOrderHistory/100")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(OK)
                    .body(extractFileContent("response/get_history.json"))
                    .contentType(APPLICATION_JSON)
            );

        List<StatusDto> response = client.getOrderHistory(100);
        assertThat(response).usingRecursiveFieldByFieldElementComparator().containsExactly(
            new StatusDto("ORDER_CREATED_FF", 1580691906000L, null),
            new StatusDto("ORDER_CREATED_BUT_NOT_APPROVED_FF", 1577923445000L, null)
        );
    }
}
