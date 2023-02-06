package ru.yandex.market.logistics.lom.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Получение истории изменения статусов заказа")
public class GetStatusHistoryTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получение истории статусов несуществующего заказа")
    void getNonExitOrderStatusHistory() throws Exception {
        mockMvc.perform(get("/admin/orders/history-events/statuses/")
            .param("orderId", "1")
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/order/response/order_not_found.json"));
    }

    @Test
    @DisplayName("Ошибка валидации")
    void getOrderStatusHistoryValidationError() throws Exception {
        mockMvc.perform(get("/admin/orders/history-events/statuses/"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent(
                "controller/order/response/validation_error_order_id_or_waybill_segment_id_must_be_not_null.json"
            ));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("getStatusHistoryArgument")
    @DatabaseSetup("/controller/admin/order/history/statuses/before/orders.xml")
    void getStatusHistoryByOrderIdSuccess(
        long orderId,
        String responsePath,
        @SuppressWarnings("unused") String displayName
    ) throws Exception {
        mockMvc.perform(get("/admin/orders/history-events/statuses/")
                .param("orderId", Long.toString(orderId))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Test
    @DisplayName("Получение истории статусов по идентификатору сегмента")
    @DatabaseSetup("/controller/admin/order/history/statuses/before/orders.xml")
    void getStatusHistoryByWaybillSegmentIdSuccess() throws Exception {
        mockMvc.perform(get("/admin/orders/history-events/statuses/")
            .param("waybillSegmentId", Long.toString(1))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/order/history/statuses/response/status_history_1.json"));
    }

    @Nonnull
    private static Stream<Arguments> getStatusHistoryArgument() {
        return Stream.of(
            Triple.of(
                1L,
                "controller/admin/order/history/statuses/response/status_history_all.json",
                "Полная история статусов"
            ),
            Triple.of(
                2L,
                "controller/admin/order/history/statuses/response/status_history_empty.json",
                "Пустая история статусов"
            )
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }
}
