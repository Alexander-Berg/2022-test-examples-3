package ru.yandex.market.logistics.nesu.base;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.missingParameter;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.nullParameter;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

public abstract class AbstractGetWithdrawIntervalsTest extends AbstractContextualTest {

    private static final LocalDate DEFAULT_LOCAL_DATE = LocalDate.of(2019, 6, 18);
    private static final Map<String, String> QUERY_PARAMETERS = Map.of(
        "partnerId", "1",
        "date", DEFAULT_LOCAL_DATE.toString()
    );

    @Autowired
    private LMSClient lmsClient;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса")
    void validateRequest(String missingParameter, String type) throws Exception {
        Map<String, String> queryParameters = new HashMap<>(QUERY_PARAMETERS);
        queryParameters.put(missingParameter, "");

        getWithdrawIntervals(queryParameters)
            .andExpect(status().isBadRequest())
            .andExpect(nullParameter(missingParameter, type));
    }

    @Nonnull
    private static Stream<Arguments> validateRequest() {
        return Stream.of(
            Arguments.of("partnerId", "Long"),
            Arguments.of("date", "LocalDate")
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("withdrawIntervalsParametersProvider")
    @DisplayName("Успешное получение интервалов отгрузки при заборе")
    void getWithdrawIntervals(String filename, List<ScheduleDayResponse> schedule) throws Exception {
        when(lmsClient.getPartner(1L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder().id(1L).build()));
        mockPartnerIntakeSchedule(schedule);

        getWithdrawIntervals()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/shipmentintervals/response/" + filename));
    }

    @Nonnull
    private static Stream<Arguments> withdrawIntervalsParametersProvider() {
        return Stream.of(
            ImmutablePair.of(
                //Из ручки LMS нам приходят уже отфильтрованные значения.
                "get_filtered_shipments_intervals.json",
                List.of(
                    new ScheduleDayResponse(1L, 2, LocalTime.of(12, 0), LocalTime.of(20, 0)),
                    new ScheduleDayResponse(2L, 2, LocalTime.of(20, 0), LocalTime.of(23, 0))
                )
            ),
            ImmutablePair.of(
                "empty_array.json",
                List.of()
            )
        ).map(pair -> Arguments.of(pair.left, pair.right));
    }

    @Test
    @DisplayName("Получение пустого интервала заборов")
    void getNonExistWarehouseWithdrawIntervals() throws Exception {
        when(lmsClient.getPartner(1L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder().id(1L).build()));
        mockPartnerIntakeSchedule(List.of());

        getWithdrawIntervals()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/shipmentintervals/response/empty_array.json"));
    }

    @Test
    @DisplayName("Получение интервала заборов собственной СД")
    void getOwnDeliveryWithdrawIntervals() throws Exception {
        when(lmsClient.getPartner(1L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder().id(1L).partnerType(PartnerType.OWN_DELIVERY).build()));

        getWithdrawIntervals()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/shipmentintervals/response/own_delivery_withdraws.json"));

        verify(lmsClient, never()).getPartnerIntakeSchedule(any(), any());
    }

    @Test
    @DisplayName("Получение интервалов отгрузок при заборе, не заполняя все обязательные поля")
    void getWithdrawIntervalsByInvalidRequest() throws Exception {
        mockMvc.perform(getWithdrawIntervalsRequest().param("partnerId", "1"))
            .andExpect(status().isBadRequest())
            .andExpect(missingParameter("date", "LocalDate"));
    }

    @Test
    @DisplayName("Получение интервалов отгрузок при заборе, используя неправильный формат даты")
    void getWithdrawIntervalsByInvalidLocalDateFormat() throws Exception {
        mockMvc.perform(getWithdrawIntervalsRequest()
                .param("partnerId", "1")
                .param("date", "2019/6/18")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/api/shipmentintervals/response/wrong_date_format.json"));
    }

    private void mockPartnerIntakeSchedule(List<ScheduleDayResponse> schedule) {
        when(lmsClient.getPartnerIntakeSchedule(1L, DEFAULT_LOCAL_DATE))
            .thenReturn(schedule);
    }

    @Nonnull
    private ResultActions getWithdrawIntervals() throws Exception {
        return getWithdrawIntervals(QUERY_PARAMETERS);
    }

    @Nonnull
    private ResultActions getWithdrawIntervals(Map<String, String> queryParameters) throws Exception {
        return mockMvc.perform(getWithdrawIntervalsRequest().params(toParams(queryParameters)));
    }

    @Nonnull
    protected abstract MockHttpServletRequestBuilder getWithdrawIntervalsRequest();
}
