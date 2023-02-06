package ru.yandex.market.logistics.nesu.controller.shipment;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.api.model.Dimensions;
import ru.yandex.market.logistics.nesu.client.model.error.ResourceType;
import ru.yandex.market.logistics.nesu.dto.shipments.GetWithdrawCostRequest;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.utils.MatcherUtils;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.model.dto.WithdrawPriceListItemDto;
import ru.yandex.market.logistics.tarifficator.model.dto.WithdrawPriceListItemSearchDto;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
class GetWithdrawCostTest extends AbstractContextualTest {
    private static final BigDecimal WITHDRAW_COST = BigDecimal.valueOf(210);

    @Autowired
    private TarifficatorClient tarifficatorClient;

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(tarifficatorClient, lmsClient);
    }

    @Test
    @DisplayName("Успешное получение стоимости забора")
    void getWithdrawCostSuccess() throws Exception {
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(
            Optional.of(defaultLogisticPointResponse().locationZoneId(1L).build())
        );
        mockTarifficatorClient();
        getWithdrawCost(defaultRequest().build())
            .andExpect(status().isOk())
            .andExpect(content().string(WITHDRAW_COST.toString()));
        verifyLmsClient();
        verifyTarifficatorClient();
    }

    @Test
    @DisplayName("Получение стоимости забора — склад не найден")
    void getWithdrawCostWarehouseNotFound() throws Exception {
        getWithdrawCost(defaultRequest().build())
            .andExpect(status().isNotFound())
            .andExpect(MatcherUtils.resourceNotFoundMatcher(ResourceType.WAREHOUSE, Set.of(1L)));
        verifyLmsClient();
    }

    @Test
    @DisplayName("Получение стоимости забора — склад не привязан к зоне")
    void getWithdrawCostLocationZoneNotFound() throws Exception {
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(
            Optional.of(defaultLogisticPointResponse().build())
        );
        getWithdrawCost(defaultRequest().build())
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/shipment/response/warehouse_not_linked.json"));
        verifyLmsClient();
    }

    @Test
    @DisplayName("Получение стоимости забора — тариф не найден")
    void getWithdrawCostTariffNotFound() throws Exception {
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(
            Optional.of(defaultLogisticPointResponse().locationZoneId(1L).build())
        );

        getWithdrawCost(defaultRequest().build())
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/shipment/response/tariff_does_not_exist.json"));
        verifyLmsClient();
        verifyTarifficatorClient();
    }

    @DisplayName("Тесты валидации запроса")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("invalidRequests")
    void validateDto(String displayName, GetWithdrawCostRequest request, ValidationErrorData error) throws Exception {
        getWithdrawCost(request)
            .andExpect(MatcherUtils.validationErrorMatcher(error))
            .andExpect(status().isBadRequest());
    }

    @Nonnull
    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
            Arguments.of(
                "Не указан идентификатор склада",
                defaultRequest().warehouseId(null).build(),
                fieldError("warehouseId", "must not be null", "getWithdrawCostRequest", "NotNull")
            ),
            Arguments.of(
                "Не указаны габариты",
                defaultRequest().dimensions(null).build(),
                fieldError("dimensions", "must not be null", "getWithdrawCostRequest", "NotNull")
            ),
            Arguments.of(
                "Не указана длина",
                defaultRequest().dimensions(defaultDimensions().setLength(null)).build(),
                fieldError("dimensions.length", "must not be null", "getWithdrawCostRequest", "NotNull")
            ),
            Arguments.of(
                "Не указана высота",
                defaultRequest().dimensions(defaultDimensions().setHeight(null)).build(),
                fieldError("dimensions.height", "must not be null", "getWithdrawCostRequest", "NotNull")
            ),
            Arguments.of(
                "Не указана ширина",
                defaultRequest().dimensions(defaultDimensions().setWidth(null)).build(),
                fieldError("dimensions.width", "must not be null", "getWithdrawCostRequest", "NotNull")
            ),
            Arguments.of(
                "Не указана масса",
                defaultRequest().dimensions(defaultDimensions().setWeight(null)).build(),
                fieldError("dimensions.weight", "must not be null", "getWithdrawCostRequest", "NotNull")
            ),
            Arguments.of(
                "Указана нулевая длина",
                defaultRequest().dimensions(defaultDimensions().setLength(0)).build(),
                fieldError("dimensions.length", "must be greater than 0", "getWithdrawCostRequest", "Positive")
            ),
            Arguments.of(
                "Указана нулевая высота",
                defaultRequest().dimensions(defaultDimensions().setHeight(0)).build(),
                fieldError("dimensions.height", "must be greater than 0", "getWithdrawCostRequest", "Positive")
            ),
            Arguments.of(
                "Указана нулевая ширина",
                defaultRequest().dimensions(defaultDimensions().setWidth(0)).build(),
                fieldError("dimensions.width", "must be greater than 0", "getWithdrawCostRequest", "Positive")
            ),
            Arguments.of(
                "Указана нулевая масса",
                defaultRequest().dimensions(defaultDimensions().setWeight(BigDecimal.ZERO)).build(),
                fieldError("dimensions.weight", "must be greater than 0", "getWithdrawCostRequest", "Positive")
            ),
            Arguments.of(
                "Указана слишком большая длина",
                defaultRequest().dimensions(defaultDimensions().setLength(100500)).build(),
                fieldError(
                    "dimensions.length",
                    "must be less than or equal to 500",
                    "getWithdrawCostRequest",
                    "Max",
                    Map.of("value", 500)
                )
            ),
            Arguments.of(
                "Указана слишком большая высота",
                defaultRequest().dimensions(defaultDimensions().setHeight(100500)).build(),
                fieldError("dimensions.height",
                    "must be less than or equal to 500",
                    "getWithdrawCostRequest",
                    "Max",
                    Map.of("value", 500)
                )
            ),
            Arguments.of(
                "Указана слишком большая ширина",
                defaultRequest().dimensions(defaultDimensions().setWidth(100500)).build(),
                fieldError(
                    "dimensions.width",
                    "must be less than or equal to 500",
                    "getWithdrawCostRequest",
                    "Max",
                    Map.of("value", 500)
                )
            ),
            Arguments.of(
                "Указана слишком большая масса",
                defaultRequest().dimensions(defaultDimensions().setWeight(BigDecimal.valueOf(100500))).build(),
                fieldError(
                    "dimensions.weight",
                    "must be less than or equal to 1500",
                    "getWithdrawCostRequest",
                    "Max",
                    Map.of("value", 1500)
                )
            )
        );
    }

    @Nonnull
    private static GetWithdrawCostRequest.GetWithdrawCostRequestBuilder defaultRequest() {
        return GetWithdrawCostRequest.builder()
            .warehouseId(1L)
            .dimensions(defaultDimensions());
    }

    @Nonnull
    private static Dimensions defaultDimensions() {
        return new Dimensions()
            .setWidth(100)
            .setHeight(100)
            .setLength(50)
            .setWeight(BigDecimal.valueOf(0.5));
    }

    @Nonnull
    private ResultActions getWithdrawCost(GetWithdrawCostRequest requestBody) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/back-office/shipments/cost/withdraw", requestBody));
    }

    private void mockTarifficatorClient() {
        when(tarifficatorClient.searchWithdrawPriceListItemDto(safeRefEq(new WithdrawPriceListItemSearchDto()
            .setLocationZoneId(1L)
            .setVolume(BigDecimal.valueOf(0.5))
        )))
            .thenReturn(Optional.of(new WithdrawPriceListItemDto().setCost(WITHDRAW_COST)));
    }

    @Nonnull
    private LogisticsPointResponse.LogisticsPointResponseBuilder defaultLogisticPointResponse() {
        return LmsFactory.createLogisticsPointResponseBuilder(1L,  null, null, PointType.WAREHOUSE);
    }

    private void verifyLmsClient() {
        verify(lmsClient).getLogisticsPoint(1L);
    }

    private void verifyTarifficatorClient() {
        verify(tarifficatorClient).searchWithdrawPriceListItemDto(safeRefEq(new WithdrawPriceListItemSearchDto()
            .setLocationZoneId(1L)
            .setVolume(BigDecimal.valueOf(0.5))
        ));
    }
}
