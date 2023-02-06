package ru.yandex.market.logistics.lom.admin;

import java.util.List;
import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.entity.combinator.embedded.CombinedRoute;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Тесты ручки получения детальной информации о маршруте из ydb")
class AdminCombinedRouteGetRouteTest extends AbstractCombinedRouteTest {

    @Test
    @SneakyThrows
    @DisplayName("У заказа есть routeUuid, получаем из нового репозитория в ydb")
    @DatabaseSetup("/controller/admin/combined/before/order_with_route_uuid.xml")
    void getRouteSuccessCases() {
        List<CombinedRoute> combinedRoutes = List.of(
            combinedRoute(3L, EXISTING_UUID),
            combinedRoute(5L, UUID.randomUUID())
        );

        insertAllIntoTable(routeHistoryTable, combinedRoutes, routeHistoryConverter::mapToItem);

        mockMvc.perform(get("/admin/routes/3"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/combined/route_3_detail.json"));

        verify(newRepository).getRouteByUuid(EXISTING_UUID);
    }

    @Test
    @DisplayName("Ошибка поиска детальной информации по комбинированному маршруту")
    void detailRouteNotFound() throws Exception {
        mockMvc.perform(get("/admin/routes/4"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [COMBINED_ROUTE] for order with id [4]"));
    }
}
