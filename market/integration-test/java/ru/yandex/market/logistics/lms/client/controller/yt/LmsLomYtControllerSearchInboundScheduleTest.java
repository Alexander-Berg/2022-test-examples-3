package ru.yandex.market.logistics.lms.client.controller.yt;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.request.schedule.LogisticSegmentInboundScheduleFilter;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Получение расписаний заборов по фильтру")
class LmsLomYtControllerSearchInboundScheduleTest extends LmsLomYtControllerAbstractTest {

    private static final String SEARCH_INBOUND_SCHEDULE_PATH =
        "/lms/test-yt/inbound-schedule/get-by-filter";

    public static final String INBOUND_SCHEDULE_QUERY =
        "schedules FROM [//home/2022-03-02T08:05:24Z/inbound_schedule_dyn] "
            + "WHERE partner_from = 1 AND partner_to = 2 AND delivery_type = 'COURIER'";

    public static final String SCHEDULE_DAYS_QUERY =
        "* FROM [//home/2022-03-02T08:05:24Z/schedule_days_dyn] WHERE schedule_id IN (1,2,3)";

    private static final LogisticSegmentInboundScheduleFilter FILTER = new LogisticSegmentInboundScheduleFilter()
        .setFromPartnerId(1L)
        .setToPartnerId(2L)
        .setDeliveryType(DeliveryType.COURIER);

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение расписаний")
    void successGetSchedules() {
        mockYtResponse();

        getInboundSchedules(FILTER)
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/yt/inbound_schedule.json", false));

        verifyFullYtCalling(3);
    }

    @Test
    @SneakyThrows
    @DisplayName("Расписания не найдены")
    void schedulesNotFound() {
        getInboundSchedules(FILTER)
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/yt/inbound_schedule_empty.json"));

        verifyYtCalling(2);
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка в yt")
    void ytErrorWhileGettingSchedules() {
        YtUtils.mockExceptionCallingYt(
            ytTables,
            INBOUND_SCHEDULE_QUERY,
            new RuntimeException("Some yt exception")
        );

        softly.assertThatCode(
                () -> getInboundSchedules(FILTER)
            )
            .isInstanceOf(NestedServletException.class)
            .hasMessage("Request processing failed; nested exception is java.lang.RuntimeException: Some yt exception");

        verifyYtCalling(2);
    }

    private void verifyYtCalling(int interactions) {
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractions(
            ytTables,
            INBOUND_SCHEDULE_QUERY
        );
        verify(hahnYt, times(interactions)).tables();
        verifyNoMoreInteractions(ytTables, hahnYt);
    }

    private void verifyFullYtCalling(int interactions) {
        YtUtils.verifySelectRowsInteractions(
            ytTables,
            SCHEDULE_DAYS_QUERY
        );
        verifyYtCalling(interactions);
    }

    @Nonnull
    @SneakyThrows
    private ResultActions getInboundSchedules(LogisticSegmentInboundScheduleFilter filter) {
        return mockMvc.perform(
            post(SEARCH_INBOUND_SCHEDULE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter))
        );
    }

    private void mockYtResponse() {
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            getInboundSchedulesIds(),
            INBOUND_SCHEDULE_QUERY
        );
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            getScheduleDays(),
            SCHEDULE_DAYS_QUERY
        );
    }

    @Nonnull
    private List<Long> getInboundSchedulesIds() {
        return List.of(1L, 2L, 3L);
    }

    @Nonnull
    private Set<ScheduleDayResponse> getScheduleDays() {
        return Set.of(
            new ScheduleDayResponse(
                1L,
                1,
                LocalTime.of(12, 0),
                LocalTime.of(21, 0),
                true
            ),
            new ScheduleDayResponse(
                2L,
                2,
                LocalTime.of(12, 0),
                LocalTime.of(21, 0),
                false
            )
        );
    }

    @AfterEach
    @Override
    void tearDown() {
    }
}
