package ru.yandex.market.logistics.nesu.api.order;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.market.logistics.nesu.api.auth.ApiAuthHolder;
import ru.yandex.market.logistics.nesu.base.order.AbstractUpdateOrderTest;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraft;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.defaultOrderDraft;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.orderDraftShipment;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointsFilter;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обновление черновика заказа в Open API")
@DatabaseSetup("/repository/settings/delivery_type_service_settings.xml")
@DatabaseSetup("/repository/order/database_order_prepare.xml")
class ApiUpdateOrderTest extends AbstractUpdateOrderTest {

    @Autowired
    private BlackboxService blackboxService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MbiApiClient mbiApiClient;

    private ApiAuthHolder authHolder;

    @BeforeEach
    void setupAuth() {
        authHolder = new ApiAuthHolder(blackboxService, objectMapper);

        authHolder.mockAccess(mbiApiClient, 1L);
    }

    @Test
    @DisplayName("Недоступный магазин")
    void noShopAccess() throws Exception {
        authHolder.mockNoAccess(mbiApiClient, 1L);

        updateOrder(defaultOrderDraft(), 42L)
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Failed to find [ORDER] with ids [42]\","
                + "\"resourceType\":\"ORDER\",\"identifiers\":[42]}"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Редактирование заказа с заменой склада")
    @DatabaseSetup(
        value = "/repository/delivery-options/api_warehouse_substitution.xml",
        type = DatabaseOperation.INSERT
    )
    void updateOrderDraftWithSubstitution(
        @SuppressWarnings("unused") String name,
        Long oldWarehouseId,
        Long newWarehouseId
    ) throws Exception {
        mockSuccess();
        mockAvailablePartners();

        doReturn(List.of(warehouseFrom(newWarehouseId), WAREHOUSE_TO, PICKUP_POINT)).when(lmsClient)
            .getLogisticsPoints(createLogisticsPointsFilter(Set.of(newWarehouseId, 4L, 101L), true));

        doReturn(Optional.of(warehouseFrom(newWarehouseId))).when(lmsClient).getLogisticsPoint(newWarehouseId);

        updateOrder(
            defaultOrderDraft().andThen(o -> o.setShipment(orderDraftShipment().setWarehouseFrom(oldWarehouseId))),
            ORDER_ID
        )
            .andExpect(status().isOk())
            .andExpect(content().string("42"));

        verify(lmsClient).getLogisticsPoints(createLogisticsPointsFilter(Set.of(newWarehouseId, 4L, 101L), true));
        verify(lmsClient).getLogisticsPoint(newWarehouseId);
        verifyUnifierClient();
    }

    @Nonnull
    private static Stream<Arguments> updateOrderDraftWithSubstitution() {
        return Stream.of(
            Arguments.of("Склад был заменен", 1L, 500L),
            Arguments.of("Нет замены для склада, есть по businessId", 2L, 2L),
            Arguments.of("Нет замены по businessId, есть по складу", 6L, 6L),
            Arguments.of("Нет замены по businessId, и складу", 7L, 7L)
        );
    }

    @Nonnull
    @Override
    protected ResultActions updateOrder(OrderDraft orderDraft, Long orderId) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/api/orders/" + orderId, orderDraft)
            .headers(authHolder.authHeaders()));
    }
}
