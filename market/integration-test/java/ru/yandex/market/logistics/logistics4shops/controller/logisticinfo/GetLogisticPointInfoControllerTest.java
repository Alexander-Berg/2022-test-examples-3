package ru.yandex.market.logistics.logistics4shops.controller.logisticinfo;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.logistics4shops.client.api.LogisticInfoApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ApiError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.LogisticPointInfo;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Получение информации о логистической точке")
@ParametersAreNonnullByDefault
class GetLogisticPointInfoControllerTest extends AbstractGetLogisticPointControllerTest {
    private static final Long LOGISTICS_POINT_ID = 42L;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Получение точки")
    @MethodSource("pointConversion")
    void success(
        @SuppressWarnings("unused") String displayName,
        LogisticsPointResponse pointResponse,
        LogisticPointInfo expected
    ) throws Exception {
        try (var mockGetPoint = mockGetLogisticsPoint(LOGISTICS_POINT_ID, pointResponse)) {
            LogisticPointInfo result = apiOperation().executeAs(validatedWith(shouldBeCode(SC_OK)));
            softly.assertThat(result).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("Точка не найдена")
    void notFound() {
        ApiError error = apiOperation()
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(ApiError.class);

        softly.assertThat(error.getMessage()).isEqualTo("Failed to find [LOGISTICS_POINT] with id [42]");
        verifyGetLogisticsPoint(LOGISTICS_POINT_ID);
    }

    @Nonnull
    private LogisticInfoApi.GetLogisticPointInfoOper apiOperation() {
        return apiClient.logisticInfo()
            .getLogisticPointInfo()
            .logisticPointIdPath(LOGISTICS_POINT_ID);
    }
}
