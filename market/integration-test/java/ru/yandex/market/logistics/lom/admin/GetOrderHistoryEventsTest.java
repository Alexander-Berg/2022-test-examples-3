package ru.yandex.market.logistics.lom.admin;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.entity.OrderHistoryEvent;
import ru.yandex.market.logistics.lom.repository.ydb.description.OrderHistoryEventTableDescription;
import ru.yandex.market.logistics.lom.utils.ydb.converter.OrderHistoryEventYdbConverter;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение списка событий изменения заказов")
@DatabaseSetup("/controller/admin/order/history/before/orders.xml")
class GetOrderHistoryEventsTest extends AbstractContextualYdbTest {

    @Autowired
    private OrderHistoryEventYdbConverter converter;

    @Autowired
    private OrderHistoryEventTableDescription oheTable;

    @Override
    @Nonnull
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(oheTable);
    }

    @Test
    @DisplayName("Получение страницы событий изменения заказов из YDB без указания id заказа")
    void getOrderHistoryEventsFromYdb() throws Exception {
        insertAllIntoTable(oheTable, orderHistoryEvents(), converter::mapToItem);
        mockMvc.perform(get("/admin/orders/history-events").param("sort", "id,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/order/history/response/all.json"));
    }

    @Test
    @DisplayName("Получение страницы событий изменения заказов с указанием id заказа дополнительно из YDB")
    void getOrderHistoryEventsByIdFromYdbAdditional() throws Exception {
        insertAllIntoTable(oheTable, orderHistoryEvents(), converter::mapToItem);
        mockMvc.perform(get("/admin/orders/history-events").param("orderId", "1").param("sort", "id,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/order/history/response/for_order_id_1.json"));
    }

    @Test
    @DisplayName("Получение страницы событий изменения заказов дополнительно из YDB - сортировка не разрешена")
    void getOrderHistoryEventsByIdFromYdbAdditionalSortIsNotAllowed() throws Exception {
        mockMvc.perform(
                get("/admin/orders/history-events")
                    .param("orderId", "1")
                    .param("sort", "created,asc", "id,asc")
            )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Properties [created] are not allowed for sorting"));
    }

    @Test
    @DisplayName("Получение страницы событий изменения заказов с указанием id несуществующего заказа")
    void getOrderHistoryEventsOrderNotFound() throws Exception {
        mockMvc.perform(get("/admin/orders/history-events").param("orderId", "10"))
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/admin/order/history/response/order_not_found.json"));
    }

    @Test
    @DisplayName("Получение отдельного события изменения заказа")
    void getOrderHistoryEventDetailsFromYdb() throws Exception {
        insertAllIntoTable(oheTable, orderHistoryEvents(), converter::mapToItem);
        mockMvc.perform(get("/admin/orders/history-events/5"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/order/history/response/event_5.json"));
    }

    @Nonnull
    private OrderHistoryEvent orderHistoryEvent(long id, long orderId) throws IOException {
        return new OrderHistoryEvent()
            .setId(id)
            .setOrderId(orderId)
            .setCreated(Instant.parse("2018-04-01T15:00:00Z"))
            .setDiff(objectMapper.readValue(
                extractFileContent("controller/admin/order/history/before/diff.json"),
                JsonNode.class
            ));
    }

    @Nonnull
    private List<OrderHistoryEvent> orderHistoryEvents() throws IOException {
        return List.of(
            orderHistoryEvent(5, 1),
            orderHistoryEvent(6, 1),
            orderHistoryEvent(7, 2)
        );
    }
}
