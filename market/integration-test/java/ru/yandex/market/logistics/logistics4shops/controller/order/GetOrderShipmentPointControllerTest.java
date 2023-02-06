package ru.yandex.market.logistics.logistics4shops.controller.order;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.logistics4shops.client.api.OrderApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ApiError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.LogisticPointInfo;
import ru.yandex.market.logistics.logistics4shops.controller.logisticinfo.AbstractGetLogisticPointControllerTest;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Тесты на получение информации о точке отгрузки заказа")
@DatabaseSetup("/controller/order/getshipmentpoint/before/get_order_shipment_point_before.xml")
@ParametersAreNonnullByDefault
class GetOrderShipmentPointControllerTest extends AbstractGetLogisticPointControllerTest {
    private static final Long LOGISTICS_POINT_ID = 400100L;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Получение точки отгрузки")
    @MethodSource("pointConversion")
    void getShipmentPointTest(
        @SuppressWarnings("unused") String displayName,
        LogisticsPointResponse pointResponse,
        LogisticPointInfo expected
    ) throws Exception {
        try (var mockGetPoint = mockGetLogisticsPoint(LOGISTICS_POINT_ID, pointResponse)) {
            LogisticPointInfo result = apiOperation("100100", 200100L).executeAs(validatedWith(shouldBeCode(SC_OK)));
            softly.assertThat(result).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("Получение точки отгрузки: заказ не найден")
    void getShipmentPointOrderNotFound() {
        ApiError error = apiOperation("100101", 200100L)
            .execute(validatedWith(shouldBeCode(HttpStatus.SC_NOT_FOUND)))
            .as(ApiError.class);

        softly.assertThat(error.getMessage()).isEqualTo("Failed to find [ORDER] with id [100101]");
    }

    @Test
    @DisplayName("Получение точки отгрузки: id точки в заказе отсутствует")
    void getShipmentPointIdIsNull() {
        ApiError error = apiOperation("100102", 200102L)
            .execute(validatedWith(shouldBeCode(HttpStatus.SC_NOT_FOUND)))
            .as(ApiError.class);

        softly.assertThat(error.getMessage()).isEqualTo("Unknown shipment logistics point for order 100102");
    }

    @Test
    @DisplayName("Получение точки отгрузки: точка не найдена в LMS")
    void getShipmentPointNotFoundInLms() {
        ApiError error = apiOperation("100100", 200100L)
            .execute(validatedWith(shouldBeCode(HttpStatus.SC_NOT_FOUND)))
            .as(ApiError.class);

        softly.assertThat(error.getMessage()).isEqualTo("Failed to find [LOGISTICS_POINT] with id [400100]");

        verifyGetLogisticsPoint(LOGISTICS_POINT_ID);
    }

    @Nonnull
    private OrderApi.GetOrderShipmentPointOper apiOperation(String orderId, long mbiPartnerId) {
        return apiClient.order()
            .getOrderShipmentPoint()
            .orderIdPath(orderId)
            .mbiPartnerIdQuery(mbiPartnerId);
    }
}
