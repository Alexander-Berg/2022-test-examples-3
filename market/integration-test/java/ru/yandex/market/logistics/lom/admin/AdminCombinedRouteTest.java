package ru.yandex.market.logistics.lom.admin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.entity.combinator.embedded.CombinedRoute;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Получение информации о комбинированном маршруте")
@DatabaseSetup("/controller/admin/combined/before/prepare.xml")
class AdminCombinedRouteTest extends AbstractCombinedRouteTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск маршрутов")
    void search(
        @SuppressWarnings("unused") String displayName,
        Map<String, List<String>> filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/routes/waybill").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                Map.of(),
                "controller/admin/combined/all.json"
            ),
            Arguments.of(
                "С указанием размера страницы",
                Map.of("size", List.of("3")),
                "controller/admin/combined/segments_1_2_3.json"
            ),
            Arguments.of(
                "По внешнему идентификатору",
                Map.of("externalId", List.of("order_1_from_partner_id_2")),
                "controller/admin/combined/segments_2_3.json"
            ),
            Arguments.of(
                "По идентификатору заказа",
                Map.of("orderId", List.of("3")),
                "controller/admin/combined/segments_7_8_9_10.json"
            ),
            Arguments.of(
                "По имени партнера",
                Map.of("partnerName", List.of("CDEK")),
                "controller/admin/combined/segments_3_5.json"
            ),
            Arguments.of(
                "По типу партнера",
                Map.of("partnerType", List.of("DELIVERY")),
                "controller/admin/combined/segments_2_4_5_7_9_10.json"
            ),
            Arguments.of(
                "По типу сегмента",
                Map.of("segmentType", List.of("SORTING_CENTER")),
                "controller/admin/combined/segments_1_2_6_8_9.json"
            ),
            Arguments.of(
                "По статусу сегмента СЦ",
                Map.of("segmentStatus", List.of("SORTING_CENTER_AT_START")),
                "controller/admin/combined/segment_8_9.json"
            ),
            Arguments.of(
                "По статусу сегмента СД",
                Map.of("segmentStatus", List.of("DELIVERY_AT_START")),
                "controller/admin/combined/segment_10.json"
            ),
            Arguments.of(
                "По дате отгрузки",
                Map.of("shipmentDate", List.of("2020-06-26")),
                "controller/admin/combined/segments_7_8_9_10.json"
            ),
            Arguments.of(
                "По дате и времени отгрузки",
                Map.of(
                    "fromShipmentDateTime", List.of("2020-06-26 16:38"),
                    "toShipmentDateTime", List.of("2020-06-26 16:40")
                ),
                "controller/admin/combined/segments_7_8_9_10.json"
            ),
            Arguments.of(
                "По идентификатору отгрузки",
                Map.of("shipmentId", List.of("1")),
                "controller/admin/combined/segments_1_2_3.json"
            ),
            Arguments.of(
                "По типу отгрузки",
                Map.of("shipmentType", List.of("WITHDRAW")),
                "controller/admin/combined/segments_5_7.json"
            ),
            Arguments.of(
                "По идентификатору из трекера",
                Map.of("trackerId", List.of("1244")),
                "controller/admin/combined/segment_10.json"
            )
        );
    }

    @Test
    @DisplayName("Поиск детальной информации по маршруту")
    void detailRouteWaybill() throws Exception {
        mockMvc.perform(get("/admin/routes/waybill/5"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/combined/segment_5_detail.json"));
    }

    @Test
    @DisplayName("Сегмент не найден")
    void detailRouteWaybillNotFound() throws Exception {
        mockMvc.perform(get("/admin/routes/waybill/15"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAYBILL_SEGMENT] with id [15]"));
    }

    @Test
    @DisplayName("Комбинированный маршрут не найден")
    void detailRouteNotFound() throws Exception {
        mockMvc.perform(get("/admin/routes/15"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [COMBINED_ROUTE] for order with id [15]"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение графа")
    @DatabaseSetup(
        value = "/controller/admin/combined/before/order_with_route_uuid.xml",
        type = DatabaseOperation.REFRESH
    )
    void getRouteGraphSuccess() {
        List<CombinedRoute> combinedRoutes = List.of(
            combinedRoute(3L, EXISTING_UUID),
            combinedRoute(5L, UUID.randomUUID())
        );

        insertAllIntoTable(routeHistoryTable, combinedRoutes, routeHistoryConverter::mapToItem);

        mockMvc.perform(get("/admin/routes/3/graph"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/combined/route_3_graph.json"));

        verify(newRepository).getRouteByUuid(EXISTING_UUID);
    }
}
