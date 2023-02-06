package ru.yandex.market.logistics.lom.controller.order;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение информации о заказе")
class OrderGetTest extends AbstractContextualTest {

    @Test
    @DisplayName("Заполненный заказ")
    @DatabaseSetup("/controller/order/before/get_order.xml")
    void success() throws Exception {
        getOrder()
            .andExpect(status().isOk())
            .andExpect(orderJson("controller/order/response/get_order.json"));
    }

    @Test
    @DisplayName("Несуществующий заказ")
    void nonExisting() throws Exception {
        getOrder()
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/order/response/order_not_found.json"));
    }

    @Test
    @DisplayName("Не заполненный заказ")
    @DatabaseSetup("/controller/order/before/get_draft_order.xml")
    void draft() throws Exception {
        getOrder()
            .andExpect(status().isOk())
            .andExpect(orderJson("controller/order/response/get_draft_order.json"));
    }

    @Test
    @DisplayName("Заказ с признаком наличия ярлыков")
    @DatabaseSetup("/controller/order/before/get_order_with_label.xml")
    void orderLabels() throws Exception {
        getOrder()
            .andExpect(status().isOk())
            .andExpect(jsonPath("hasLabels").value(Boolean.TRUE));
    }

    @Test
    @DisplayName("Заказ с новой структурой товарных мест")
    @DatabaseSetup("/controller/order/before/order_with_new_places.xml")
    void newPlaces() throws Exception {
        getOrder()
            .andExpect(status().isOk())
            .andExpect(orderJson("controller/order/response/get_order_places.json"));
    }

    @Test
    @DisplayName("Заказ с опциональными частями")
    @DatabaseSetup("/controller/order/before/get_order.xml")
    void optionalParts() throws Exception {
        mockMvc.perform(
            get("/orders/1")
                .param(
                    "optionalParts",
                    "CHANGE_REQUESTS",
                    "CANCELLATION_REQUESTS",
                    "GLOBAL_STATUSES_HISTORY",
                    "UPDATE_RECIPIENT_ENABLED"
                )
        )
            .andExpect(status().isOk())
            .andExpect(orderJson("controller/order/response/get_order_with_optional_parts.json"));
    }

    @Test
    @DisplayName("Десериализация JSON-полей заказа в разных форматах")
    @DatabaseSetup("/controller/order/before/order_json_fields_deserialization.xml")
    void intervalAsArrayInJson() throws Exception {
        getOrder()
            .andExpect(status().isOk())
            .andExpect(jsonPath("waybill[0].shipment.locationFrom.inboundInterval.from").value("10:00:00"))
            .andExpect(jsonPath("waybill[0].shipment.locationFrom.inboundInterval.to").value("18:05:30"))
            .andExpect(jsonPath("waybill[0].shipment.locationTo.inboundInterval.from").value("10:00:00"))
            .andExpect(jsonPath("waybill[0].shipment.locationTo.inboundInterval.to").value("18:20:10"));
    }

    @Nonnull
    private ResultActions getOrder() throws Exception {
        return OrderTestUtil.getOrder(mockMvc, ORDER_ID);
    }

    @Nonnull
    private ResultMatcher orderJson(String expectedPath) {
        return jsonContent(expectedPath, "created", "updated");
    }
}
