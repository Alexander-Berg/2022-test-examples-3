package ru.yandex.market.logistics.lom.client;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.lom.model.filter.ExistingOrderSearchFilter;

class OrderCheckExistingClientTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Проверка наличия заказов")
    void searchOrders() {
        prepareMockRequest(
            HttpMethod.PUT,
            "/orders/existing",
            "request/order/existing.json",
            "response/order/existing.json"
        );
        Map<Long, Boolean> result = lomClient.checkOrdersExisting(
            ExistingOrderSearchFilter.builder()
                .createdFrom(Instant.parse("2021-05-20T17:00:00Z"))
                .partnerIds(List.of(1L, 2L, 3L, 4L, 5L))
                .build()
        );
        Map<Long, Boolean> expected = Map.of(
            1L, false,
            2L, true,
            3L, true,
            4L, false,
            5L, true
        );

        softly.assertThat(result).isEqualTo(expected);
    }
}
