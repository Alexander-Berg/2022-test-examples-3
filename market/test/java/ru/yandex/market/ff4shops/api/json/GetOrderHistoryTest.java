package ru.yandex.market.ff4shops.api.json;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.OrderHistoryUtil;
import ru.yandex.market.ff4shops.util.FF4ShopsUrlBuilder;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;

import static ru.yandex.market.ff4shops.config.FunctionalTest.TUPLE_PARAMETERIZED_DISPLAY_NAME;

@DbUnitDataSet(before = "Environment.useYandexIdInsteadExternalId.before.csv")
public class GetOrderHistoryTest extends AbstractJsonControllerFunctionalTest {
    @Autowired
    private CheckouterAPI checkouterAPI;

    private CheckouterOrderHistoryEventsApi historyEventsApi;

    @BeforeEach
    void setup() {
        historyEventsApi = OrderHistoryUtil.prepareMock(checkouterAPI);
    }

    @Test
    @DisplayName("Использовать yandexId вместо partnerId для походов в чекаутер")
    void successUseYandexIdInsteadPartnerId() {
        mockHistoryEvents();

        assertGetOrderHistory("ru/yandex/market/ff4shops/api/json/get_order_history_success.json");
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("eventsArguments")
    @DisplayName("История удаления товаров из заказа")
    void removedItems(@SuppressWarnings("unused") String caseName, List<OrderHistoryEvent> events) {
        OrderHistoryUtil.mockHistoryEvents(historyEventsApi, events, OrderHistoryUtil.YANDEX_ID);
        assertGetOrderHistory(
            "ru/yandex/market/ff4shops/api/json/get_order_history_with_114.json"
        );
    }

    @Test
    @DisplayName("Невалидный запрос")
    void invalidOrderId() {
        assertGetOrderHistory("ru/yandex/market/ff4shops/api/json/get_order_history_invalid_response.json");
    }

    @Nonnull
    private static Stream<Arguments> eventsArguments() {
        return Stream.of(
            Arguments.of(
                "Прямой порядок событий из чекаутера",
                OrderHistoryUtil.ORDER_HISTORY_EVENTS
            ),
            Arguments.of(
                "Обратный порядок событий из чекаутера",
                Lists.reverse(OrderHistoryUtil.ORDER_HISTORY_EVENTS)
            )
        );
    }

    private void assertGetOrderHistory(String responsePath) {
        String response = FunctionalTestHelper.postForEntity(
                FF4ShopsUrlBuilder.getOrderHistory(randomServerPort, OrderHistoryUtil.YANDEX_ID),
                null,
                FunctionalTestHelper.jsonHeaders()
            )
            .getBody();

        assertResponseBody(response, responsePath);
    }

    private void mockHistoryEvents() {
        OrderHistoryUtil.mockHistoryEvents(
            historyEventsApi,
            List.of(
                OrderHistoryUtil.eventSubstatusUpdated(
                    1,
                    OrderHistoryUtil.YANDEX_ID,
                    LocalDateTime.of(2020, 1, 2, 3, 4, 5),
                    OrderSubstatus.STARTED
                ),
                OrderHistoryUtil.eventSubstatusUpdated(
                    2,
                    OrderHistoryUtil.YANDEX_ID,
                    LocalDateTime.of(2020, 2, 3, 4, 5, 6),
                    OrderSubstatus.PACKAGING
                )
            ),
            OrderHistoryUtil.YANDEX_ID
        );
    }
}
