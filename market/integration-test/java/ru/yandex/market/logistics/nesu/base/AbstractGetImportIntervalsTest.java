package ru.yandex.market.logistics.nesu.base;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.missingParameter;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.nullParameter;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

public abstract class AbstractGetImportIntervalsTest extends AbstractContextualTest {
    private static final Map<String, String> QUERY_PARAMETERS = Map.of(
        "warehouseId", "1",
        "date", "2019-06-18"
    );

    @Autowired
    private LMSClient lmsClient;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса")
    void validateRequest(String missingParameter, String type) throws Exception {
        Map<String, String> queryParameters = new HashMap<>(QUERY_PARAMETERS);
        queryParameters.put(missingParameter, "");

        getImportIntervals(queryParameters)
            .andExpect(status().isBadRequest())
            .andExpect(nullParameter(missingParameter, type));
    }

    @Nonnull
    private static Stream<Arguments> validateRequest() {
        return Stream.of(
            Arguments.of("warehouseId", "Long"),
            Arguments.of("date", "LocalDate")
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("importIntervalsParametersProvider")
    @DisplayName("Успешное получение интервалов отгрузки при самопривозе")
    void getImportIntervals(String filename, Set<ScheduleDayResponse> schedule) throws Exception {
        mockLogisticsPoint(createLogisticsPointResponse(schedule, PointType.WAREHOUSE));

        getImportIntervals()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/shipmentintervals/response/" + filename));

        verify(lmsClient).getLogisticsPoint(1L);
    }

    private static Stream<Arguments> importIntervalsParametersProvider() {
        return Stream.of(
            ImmutablePair.of(
                "get_filtered_shipments_intervals.json",
                Set.of(
                    new ScheduleDayResponse(1L, 2, LocalTime.of(12, 0), LocalTime.of(20, 0)),
                    new ScheduleDayResponse(2L, 2, LocalTime.of(20, 0), LocalTime.of(23, 0)),
                    new ScheduleDayResponse(3L, 1, LocalTime.of(20, 0), LocalTime.of(23, 0))
                )
            ),
            ImmutablePair.of(
                "get_sorted_shipment_intervals.json",
                Set.of(
                    new ScheduleDayResponse(1L, 2, LocalTime.of(12, 0), LocalTime.of(20, 0)),
                    new ScheduleDayResponse(2L, 2, LocalTime.of(8, 0), LocalTime.of(10, 0))
                )
            ),
            ImmutablePair.of(
                "empty_array.json",
                Set.of()
            )
        ).map(pair -> Arguments.of(pair.left, pair.right));
    }

    @Test
    @DisplayName("Получение списка интервалов отгрузок у несуществующего склада при самопривозе")
    void getNonExistWarehouseImportIntervals() throws Exception {
        mockLogisticsPoint(null);

        getImportIntervals()
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/api/shipmentintervals/response/not_found.json"));

        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Получение списка интервалов отгрузок у ПВЗ при самопривозе")
    void getPickupPointImportIntervals() throws Exception {
        mockLogisticsPoint(createLogisticsPointResponse(null, PointType.PICKUP_POINT));

        getImportIntervals()
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/api/shipmentintervals/response/not_found.json"));

        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Получение интервалов отгрузок при самопривозе, не заполняя все обязательные поля")
    void getImportIntervalsByInvalidRequest() throws Exception {
        mockMvc.perform(getImportIntervalsRequest()
                .param("warehouseId", "1")
            )
            .andExpect(status().isBadRequest())
            .andExpect(missingParameter("date", "LocalDate"));
    }

    @Test
    @DisplayName("Получение интервалов отгрузок при самопривозе, используя неправильный формат даты")
    void getImportIntervalsByInvalidLocalDateFormat() throws Exception {
        mockMvc.perform(getImportIntervalsRequest()
                .param("warehouseId", "1")
                .param("date", "2019/6/18")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/api/shipmentintervals/response/wrong_date_format.json"));
    }

    private void mockLogisticsPoint(LogisticsPointResponse logisticsPoint) {
        when(lmsClient.getLogisticsPoint(1L))
            .thenReturn(Optional.ofNullable(logisticsPoint));
    }

    @Nonnull
    private ResultActions getImportIntervals() throws Exception {
        return getImportIntervals(QUERY_PARAMETERS);
    }

    @Nonnull
    private ResultActions getImportIntervals(Map<String, String> queryParameters) throws Exception {
        return mockMvc.perform(getImportIntervalsRequest().params(toParams(queryParameters)));
    }

    @Nonnull
    protected abstract MockHttpServletRequestBuilder getImportIntervalsRequest();

    @Nonnull
    private LogisticsPointResponse createLogisticsPointResponse(Set<ScheduleDayResponse> schedule, PointType type) {
        return LogisticsPointResponse.newBuilder()
            .id(1L)
            .partnerId(1L)
            .type(type)
            .active(true)
            .schedule(schedule)
            .build();
    }
}
