package ru.yandex.market.logistics.lom.controller.order;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.combinator.embedded.CombinedRoute;
import ru.yandex.market.logistics.lom.service.order.combinator.RouteRecalculationService;
import ru.yandex.market.logistics.lom.service.order.combinator.dto.RouteRecalculationParams;
import ru.yandex.market.logistics.lom.service.order.combinator.dto.RouteRecalculationResult;
import ru.yandex.market.logistics.lom.service.order.combinator.enums.RecalculationStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.lom.utils.TestUtils.validationErrorsJsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/order/before/set_up_orders_for_route_recalculation.xml")
@DisplayName("Тест пересчёта маршрута у заказов")
class RecalculateOrdersRouteTest extends AbstractContextualTest {

    @Autowired
    private RouteRecalculationService routeRecalculationService;

    @Captor
    private ArgumentCaptor<List<RouteRecalculationParams>> captor;

    @BeforeEach
    public void setUp() {
        clock.setFixed(Instant.parse("2022-03-10T15:00:00.00Z"), ZoneId.systemDefault());
    }

    @Test
    @DisplayName("recalculateOrdersRoute успешный пересчет нескольких заказов")
    void recalculateOrdersRoute() throws Exception {
        doReturn(List.of(
            createRouteRecalculationResult(1001, UUID.fromString("00000000-0000-0000-0000-000000000011")),
            createRouteRecalculationResult(2002, UUID.fromString("00000000-0000-0000-0000-000000000012"))
        ))
            .when(routeRecalculationService).recalculateRoutes(any());
        performRecalculateOrdersRoutes("controller/order/request/recalculate_orders_route.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/recalculated_orders_route.json",
                "recalculatedRouteDtoList[*].newRoute"
            ));
    }

    @Test
    @DisplayName("recalculateOrdersRoute Ошибка, в запросе в списке заказов есть null")
    void recalculateOrdersRouteRequestHasNulls() throws Exception {
        performRecalculateOrdersRoutes("controller/order/request/recalculate_orders_route_with_null.json")
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorsJsonContent("orderIds[0]", "must not be null"));
    }


    @Test
    @DisplayName("recalculateOrdersRoute Ошибка, в запросе дублирующиеся заказы")
    void recalculateOrdersRouteRequestHasDuplicates() throws Exception {
        performRecalculateOrdersRoutes("controller/order/request/recalculate_orders_route_with_duplicates.json")
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorsJsonContent("orderIds", "must only contain unique elements"));
    }

    @Test
    @DisplayName("recalculateOrdersRoute Ошибка, список заказов пустой")
    void recalculateOrdersRouteRequestIsEmpty() throws Exception {
        performRecalculateOrdersRoutes("controller/order/request/recalculate_orders_route_empty_list.json")
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorsJsonContent("orderIds", "must not be empty"));
    }

    @Nonnull
    @SneakyThrows
    protected RouteRecalculationResult createRouteRecalculationResult(long orderId, UUID routeUuid) {
        return new RouteRecalculationResult(
            orderId,
            RecalculationStatus.SUCCESS,
            new CombinedRoute()
                .setOrderId(orderId)
                .setRouteUuid(routeUuid)
                .setSourceRoute(objectMapper.readTree(extractFileContent(
                    "controller/order/combined/combined_route.json"
                )))
        );
    }

    @Nonnull
    private ResultActions performRecalculateOrdersRoutes(@Nonnull String expectedRequestBodyFilePath) throws Exception {
        return mockMvc.perform(
            put("/orders/recalculateOrdersRoute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(expectedRequestBodyFilePath))
        );
    }
}
