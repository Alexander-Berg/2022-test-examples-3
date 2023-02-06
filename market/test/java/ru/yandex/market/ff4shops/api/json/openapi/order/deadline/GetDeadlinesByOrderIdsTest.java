package ru.yandex.market.ff4shops.api.json.openapi.order.deadline;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.json.openapi.AbstractOpenApiTest;

import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.ff4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.ff4shops.client.ResponseSpecBuilders.validatedWith;

@ParametersAreNonnullByDefault
@DisplayName("Получение дедлайнов сборки заказов")
@DbUnitDataSet(before = "GetDeadlinesByOrderIdsTest.before.csv")
class GetDeadlinesByOrderIdsTest extends AbstractOpenApiTest {
    @Test
    @DisplayName("Успешное получение соответствия между идентификаторами заказов и дедлайнами их сборки")
    void getDeadlines() {
        getDeadlines(
            "ru/yandex/market/ff4shops/api/json/openapi/order/deadline/getDeadlines.success.json",
            111L,
            222L,
            333L
        );
    }

    @Test
    @DisplayName("Запрос на получение соответствия по несуществующим идентификаторам заказов")
    void getDeadlinesForNonexistingOrders() {
        getDeadlines(
            "ru/yandex/market/ff4shops/api/json/openapi/order/deadline/getDeadlines.noDeadlinesFound.json",
            10000L
        );
    }

    @Test
    @DisplayName("Получение пустого ответа при пустом списке идентификаторов")
    void getDeadlinesWithEmptyRequest() {
        getDeadlines("ru/yandex/market/ff4shops/api/json/openapi/order/deadline/getDeadlines.noDeadlinesFound.json");
    }

    private void getDeadlines(String expectedBodyFilePath, Object... orderIds) {
        var responseBody = apiClient.orderReadyToShipDeadline()
            .getDeadlinesByOrderIds()
            .orderIdsQuery(orderIds)
            .execute(validatedWith(shouldBeCode(SC_OK)));

        assertResponseBody(responseBody.asPrettyString(), expectedBodyFilePath);
    }
}
