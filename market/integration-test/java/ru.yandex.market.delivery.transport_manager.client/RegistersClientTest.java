package ru.yandex.market.delivery.transport_manager.client;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.model.dto.RegisterOrdersCountDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterOrdersCountRequestDto;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class RegistersClientTest extends AbstractClientTest {

    @Autowired
    private TransportManagerClient transportManagerClient;

    @DisplayName("Получение количества заказов в реестрах")
    @Test
    void getOrdersCount() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/registers/ordersCount"))
            .andRespond(
                withSuccess(
                    extractFileContent("response/register/get_orders_count.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        List<RegisterOrdersCountDto> actual = transportManagerClient.getOrdersCount(
            new RegisterOrdersCountRequestDto(
                List.of(1L, 2L)
            )
        );

        List<RegisterOrdersCountDto> expected = List.of(
            new RegisterOrdersCountDto(1L, 2L),
            new RegisterOrdersCountDto(2L, 2L)
        );

        softly.assertThat(actual).usingRecursiveFieldByFieldElementComparator().isEqualTo(expected);
    }
}
