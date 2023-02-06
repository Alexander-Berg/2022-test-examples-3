package ru.yandex.market.logistics.lom.controller.route;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.entity.combinator.embedded.CombinedRoute;
import ru.yandex.market.logistics.lom.repository.ydb.OrderCombinedRouteHistoryYdbRepository;
import ru.yandex.market.logistics.lom.repository.ydb.description.OrderCombinedRouteHistoryTableDescription;
import ru.yandex.market.logistics.lom.utils.ydb.converter.OrderCombinedRouteHistoryYdbConverter;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DatabaseSetup("/route/order_for_combined_route.xml")
@DisplayName("Получение комбинированного маршрута")
class CombinedRouteControllerTest extends AbstractContextualYdbTest {

    private static final UUID EXISTING_UUID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID EXISTING_UUID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private OrderCombinedRouteHistoryTableDescription routeHistoryTable;

    @Autowired
    private OrderCombinedRouteHistoryYdbConverter routeHistoryConverter;

    @Autowired
    private OrderCombinedRouteHistoryYdbRepository newRepository;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(routeHistoryTable);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(newRepository);
    }

    @Test
    @SneakyThrows
    @DisplayName("Получить комбинированный маршрут по штрихкоду заказа")
    void getCombinedRouteByOrderBarcode() {
        prepareDataInYdb();

        mockMvc.perform(
            put("/routes/findOne")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"barcode\":\"1002-LOinttest-2\"}")
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/route/response/get_route.json"));

        verify(newRepository).getRouteByUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Не найден комбинированный маршрут по штрихкоду заказа")
    void getCombinedRouteByOrderBarcodeNotFound() {
        mockMvc.perform(
            put("/routes/findOne")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"barcode\":\"1002-LOinttest-3\"}")
        )
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [COMBINED_ROUTE] with id [barcode=1002-LOinttest-3]"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получить комбинированный маршрут по его идентификатору")
    void getCombinedRouteByRouteUuid() {
        prepareDataInYdb();

        mockMvc.perform(
            get("/routes/by-uuid/" + EXISTING_UUID_1)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/route/response/get_route.json"));

        verify(newRepository).getRouteByUuid(EXISTING_UUID_1);
    }

    @Test
    @SneakyThrows
    @DisplayName("Получить комбинированный маршрут с заполненным расписанием по его идентификатору")
    void getCombinedRouteWithScheduleByRouteUuid() {
        prepareDataInYdb("route/combined_route_with_schedule.json");

        mockMvc.perform(
                get("/routes/by-uuid/" + EXISTING_UUID_1)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/route/response/get_route_with_schedule.json"));

        verify(newRepository).getRouteByUuid(EXISTING_UUID_1);
    }

    @Test
    @SneakyThrows
    @DisplayName("Не найден комбинированный маршрут с заданным идентификатором")
    void getCombinedRouteByRouteUuidNotFound() {
        mockMvc.perform(
            get("/routes/by-uuid/" + EXISTING_UUID_1)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [COMBINED_ROUTE] with id [00000000-0000-0000-0000-000000000001]"));

        verify(newRepository).getRouteByUuid(EXISTING_UUID_1);
    }

    @Test
    @SneakyThrows
    @DisplayName("Запрос на получение маршрута с невалидным идентификатором")
    void getCombinedRouteWithInvalidUuid() {
        mockMvc.perform(
            get("/routes/by-uuid/invalid-uuid")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Invalid UUID string: invalid-uuid"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получить комбинированные маршруты по их идентификаторам")
    void getCombinedRoutesByRouteUuids() {
        prepareDataInYdb();

        mockMvc.perform(
            put("/routes/by-uuids")
                .content(extractFileContent("controller/route/request/get_routes.json"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/route/response/get_routes.json"));

        verify(newRepository).getRoutesByUuidIn(Set.of(EXISTING_UUID_1, EXISTING_UUID_2));
    }

    @Test
    @SneakyThrows
    @DisplayName("Запрос на получение маршрута с невалидным идентификатором")
    void getCombinedRoutesWithInvalidUuids() {
        mockMvc.perform(
            put("/routes/by-uuids")
                .content(extractFileContent("controller/route/request/get_routes_invalid_uuid.json"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Invalid UUID string: invalidUuid"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получить комбинированный маршрут по ID заказа: успех")
    void getCombinedRouteByOrderId() {
        prepareDataInYdb();

        mockMvc.perform(get("/routes/by-order-id/1001"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/route/response/get_route.json"));

        verify(newRepository).getRouteByUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получить комбинированный маршрут по ID заказа: маршрут не найден")
    void getCombinedRouteByOrderIdNotFound() {
        mockMvc.perform(get("/routes/by-order-id/1002"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [COMBINED_ROUTE] with id [order id=1002]"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получить комбинированный маршрут по ID заказа: заказ не найден")
    void getCombinedRouteByOrderIdOrderNotFound() {
        mockMvc.perform(get("/routes/by-order-id/1003"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER] with id [1003]"));
    }


    @SneakyThrows
    private void prepareDataInYdb() {
        prepareDataInYdb("route/combined_route.json");
    }

    @SneakyThrows
    private void prepareDataInYdb(String routePath) {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(
                createCombinedRoute(1001L, EXISTING_UUID_1, routePath),
                createCombinedRoute(1002L, UUID.randomUUID(), routePath)
            ),
            routeHistoryConverter::mapToItem
        );
    }

    @Nonnull
    @SneakyThrows
    private CombinedRoute createCombinedRoute(Long orderId, UUID routeUuid, String routePath) {
        return new CombinedRoute()
            .setOrderId(orderId)
            .setSourceRoute(objectMapper.readTree(extractFileContent(routePath)))
            .setRouteUuid(routeUuid);
    }
}
