package ru.yandex.market.logistics.lms.client.controller.yt;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.NestedServletException;

import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.logistics.lom.configuration.properties.LmsYtProperties;
import ru.yandex.market.logistics.lom.service.yt.dto.YtLogisticsPoint;
import ru.yandex.market.logistics.lom.service.yt.dto.YtLogisticsPointsAggModel;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPhoneByLogisticsPointId;
import ru.yandex.market.logistics.lom.service.yt.dto.YtScheduleDays;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Получение логистических точек")
class LmsLomYtControllerGetLogisticsPointTest extends LmsLomYtControllerAbstractTest {

    private static final String GET_LOGISTICS_POINT_PATH = "/lms/test-yt/logistics-point/get/%d";
    private static final String GET_LOGISTICS_POINT_BY_FILTER_PATH = "/lms/test-yt/logistics-point/get-by-filter";
    private static final String GET_LOGISTICS_POINT_QUERY_PREFIX =
        "* FROM [//home/2022-03-02T08:05:24Z/logistics_point_dyn] WHERE id IN (";
    private static final String GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX =
        "* FROM [//home/2022-03-02T08:05:24Z/logistics_point_phones_dyn] WHERE id IN (";
    private static final String GET_LOGISTICS_POINT_SCHEDULE_QUERY_PREFIX =
        "* FROM [//home/2022-03-02T08:05:24Z/schedule_days_dyn] WHERE schedule_id IN (";
    private static final String GET_LOGISTICS_POINT_AGG_QUERY_PREFIX =
        "* FROM [//home/2022-03-02T08:05:24Z/logistics_points_agg_dyn] WHERE partner_id IN (";

    private static final Long LOGISTICS_POINT_ID = 1L;
    private static final Long SCHEDULE_ID = 50L;

    private static final Set<Long> LOGISTICS_POINT_IDS = Set.of(1L, 2L, 3L, 4L);
    private static final Set<Long> PARTNER_IDS = Set.of(100L, 300L);
    private static final Set<Long> SCHEDULE_IDS = Set.of(10L, 20L, 30L, 40L);
    private static final LogisticsPointFilter FILTER_WITH_IDS =
        LogisticsPointFilter.newBuilder().ids(LOGISTICS_POINT_IDS).build();
    private static final LogisticsPointFilter FILTER =
        LogisticsPointFilter.newBuilder().partnerIds(PARTNER_IDS).type(PointType.WAREHOUSE).active(true).build();

    @Autowired
    private LmsYtProperties lmsYtProperties;

    @Autowired
    private YtTables ytTables;

    @Override
    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(ytTables, hahnYt);
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение лог точки")
    void successGetLogisticsPoint() {
        mockYtLogisticsPointResponse();

        getLogisticsPoint(LOGISTICS_POINT_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/yt/logistics_point.json"));

        verifyYtCalling(List.of(
            GET_LOGISTICS_POINT_QUERY_PREFIX,
            GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX,
            GET_LOGISTICS_POINT_SCHEDULE_QUERY_PREFIX
        ));
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение лог точки без расписаний и телефоов")
    void successGetLogisticsPointWithoutScheduleAndPhones() {
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            List.of(RedisFromYtMigrationTestUtils.buildPoint(LOGISTICS_POINT_ID).setScheduleId(Optional.empty())),
            GET_LOGISTICS_POINT_QUERY_PREFIX
        );
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            List.of(),
            GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX
        );

        getLogisticsPoint(LOGISTICS_POINT_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/yt/logistics_point_without_schedule_and_phones.json"));

        verifyYtCalling(List.of(GET_LOGISTICS_POINT_QUERY_PREFIX, GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX));
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение лог точки с пустым расписанием")
    void successGetLogisticsPointWithEmptySchedule() {
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            List.of(ytLogisticsPoint(LOGISTICS_POINT_ID, SCHEDULE_ID)),
            GET_LOGISTICS_POINT_QUERY_PREFIX
        );
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            List.of(ytPhoneByLogisticsPointId(LOGISTICS_POINT_ID)),
            GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX
        );
        YtUtils.mockSelectRowsFromYtQueryStartsWith(ytTables, List.of(), GET_LOGISTICS_POINT_SCHEDULE_QUERY_PREFIX);

        getLogisticsPoint(LOGISTICS_POINT_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/yt/logistics_point_with_empty_schedule.json"));

        verifyYtCalling(List.of(
            GET_LOGISTICS_POINT_QUERY_PREFIX,
            GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX,
            GET_LOGISTICS_POINT_SCHEDULE_QUERY_PREFIX
        ));
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение лог точек по фильтру с id")
    void successGetLogisticsPointsByFilterWithId() {
        mockYtLogisticsPointsResponse();

        getLogisticsPointsByFilter(FILTER_WITH_IDS)
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/yt/logistics_points.json", false));

        verifyYtCalling(List.of(
            GET_LOGISTICS_POINT_QUERY_PREFIX,
            GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX,
            GET_LOGISTICS_POINT_SCHEDULE_QUERY_PREFIX
        ));
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение лог точек по фильтру")
    void successGetLogisticsPointsByFilter() {
        mockYtLogisticsPointsResponse();

        getLogisticsPointsByFilter(FILTER)
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/yt/logistics_points_by_filter.json", false));

        verifyYtCalling(List.of(
            GET_LOGISTICS_POINT_AGG_QUERY_PREFIX,
            GET_LOGISTICS_POINT_QUERY_PREFIX,
            GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX,
            GET_LOGISTICS_POINT_SCHEDULE_QUERY_PREFIX
        ));
    }

    @Test
    @SneakyThrows
    @DisplayName("Точка не найдена")
    void pointNotFound() {
        YtUtils.mockSelectRowsFromYtQueryStartsWith(ytTables, List.of(), GET_LOGISTICS_POINT_QUERY_PREFIX);

        getLogisticsPoint(LOGISTICS_POINT_ID)
            .andExpect(status().isOk())
            .andExpect(content().string("null"));

        verifyYtCalling(List.of(GET_LOGISTICS_POINT_QUERY_PREFIX));
    }

    @Test
    @SneakyThrows
    @DisplayName("Точки по фильтру с id не найдены")
    void pointsByFilterWithIdsNotFound() {
        YtUtils.mockSelectRowsFromYtQueryStartsWith(ytTables, List.of(), GET_LOGISTICS_POINT_QUERY_PREFIX);

        getLogisticsPointsByFilter(FILTER_WITH_IDS)
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));

        verifyYtCalling(List.of(GET_LOGISTICS_POINT_QUERY_PREFIX));
    }

    @Test
    @SneakyThrows
    @DisplayName("Точки по пустому фильтру с id не найдены")
    void pointsByEmptyFilterWithIdsNotFound() {
        getLogisticsPointsByFilter(LogisticsPointFilter.newBuilder().ids(Set.of()).build())
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));
        verifyYtCalling(List.of());
    }

    @Test
    @SneakyThrows
    @DisplayName("Точки по фильтру не найдены")
    void pointsByFilterNotFound() {
        YtUtils.mockSelectRowsFromYtQueryStartsWith(ytTables, List.of(), GET_LOGISTICS_POINT_AGG_QUERY_PREFIX);

        getLogisticsPointsByFilter(FILTER)
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));

        verifyYtCalling(List.of(GET_LOGISTICS_POINT_AGG_QUERY_PREFIX));
    }

    @Test
    @SneakyThrows
    @DisplayName("Точки по пустому фильтру не найдены")
    void pointsByEmptyFilterNotFound() {
        getLogisticsPointsByFilter(
            LogisticsPointFilter.newBuilder()
                .partnerIds(Set.of())
                .type(PointType.WAREHOUSE)
                .active(true)
                .build()
        )
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));
        verifyYtCalling(List.of());
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка в yt при получении лог точки")
    void ytErrorWhileGettingLogisticsPoint() {
        YtUtils.mockExceptionCallingYtQueryStartsWith(
            ytTables,
            GET_LOGISTICS_POINT_QUERY_PREFIX,
            new RuntimeException("Some yt exception")
        );

        softly.assertThatCode(() -> getLogisticsPoint(LOGISTICS_POINT_ID))
            .isInstanceOf(NestedServletException.class)
            .hasMessage("Request processing failed; nested exception is java.lang.RuntimeException: Some yt exception");

        verifyYtCalling(List.of(GET_LOGISTICS_POINT_QUERY_PREFIX));
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка в yt при получении лог точки по фильтру с id")
    void ytErrorWhileGettingLogisticsPointByFilterWithIds() {
        YtUtils.mockExceptionCallingYtQueryStartsWith(
            ytTables,
            GET_LOGISTICS_POINT_QUERY_PREFIX,
            new RuntimeException("Some yt exception")
        );

        softly.assertThatCode(() -> getLogisticsPointsByFilter(FILTER_WITH_IDS))
            .isInstanceOf(NestedServletException.class)
            .hasMessage("Request processing failed; nested exception is java.lang.RuntimeException: Some yt exception");

        verifyYtCalling(List.of(GET_LOGISTICS_POINT_QUERY_PREFIX));
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка в yt при получении лог точки по фильтру")
    void ytErrorWhileGettingLogisticsPointByFilter() {
        YtUtils.mockExceptionCallingYtQueryStartsWith(
            ytTables,
            GET_LOGISTICS_POINT_AGG_QUERY_PREFIX,
            new RuntimeException("Some yt exception")
        );

        softly.assertThatCode(() -> getLogisticsPointsByFilter(FILTER))
            .isInstanceOf(NestedServletException.class)
            .hasMessage("Request processing failed; nested exception is java.lang.RuntimeException: Some yt exception");

        verifyYtCalling(List.of(GET_LOGISTICS_POINT_AGG_QUERY_PREFIX));
    }

    private void verifyYtCalling(List<String> queriesToVerify) {
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        queriesToVerify.forEach(query -> YtUtils.verifySelectRowsInteractionsQueryStartsWith(ytTables, query));
        verify(hahnYt, times(queriesToVerify.size() + 1)).tables();
    }

    @Nonnull
    @SneakyThrows
    private ResultActions getLogisticsPoint(Long logisticsPointId) {
        return mockMvc.perform(
            get(String.format(GET_LOGISTICS_POINT_PATH, logisticsPointId))
        );
    }

    @Nonnull
    @SneakyThrows
    private ResultActions getLogisticsPointsByFilter(LogisticsPointFilter filter) {
        return mockMvc.perform(
            post(GET_LOGISTICS_POINT_BY_FILTER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter))
        );
    }

    private void mockYtLogisticsPointResponse() {
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            List.of(ytLogisticsPoint(LOGISTICS_POINT_ID, SCHEDULE_ID)),
            GET_LOGISTICS_POINT_QUERY_PREFIX
        );
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            List.of(ytPhoneByLogisticsPointId(LOGISTICS_POINT_ID)),
            GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX
        );
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            List.of(ytScheduleDays(SCHEDULE_ID)),
            GET_LOGISTICS_POINT_SCHEDULE_QUERY_PREFIX
        );
    }

    private void mockYtLogisticsPointsResponse() {
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            ytLogisticsPointsAggModelList(PARTNER_IDS),
            GET_LOGISTICS_POINT_AGG_QUERY_PREFIX
        );
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            ytLogisticsPointList(LOGISTICS_POINT_IDS),
            GET_LOGISTICS_POINT_QUERY_PREFIX
        );
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            ytPhoneByLogisticsPointIdList(LOGISTICS_POINT_IDS),
            GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX
        );
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            ytScheduleDaysList(SCHEDULE_IDS),
            GET_LOGISTICS_POINT_SCHEDULE_QUERY_PREFIX
        );
    }

    @Nonnull
    private List<YtLogisticsPointsAggModel> ytLogisticsPointsAggModelList(Set<Long> partnerIds) {
        return partnerIds.stream()
            .map(partnerId -> RedisFromYtMigrationTestUtils.buildPointAgg(
                partnerId,
                List.of(partnerId / 100, partnerId / 100 + 1)
            ))
            .collect(Collectors.toList());
    }

    @Nonnull
    private List<YtLogisticsPoint> ytLogisticsPointList(Set<Long> logisticsPointIds) {
        return logisticsPointIds.stream()
            .map(logisticsPointId -> ytLogisticsPoint(logisticsPointId, 10 * logisticsPointId))
            .collect(Collectors.toList());
    }

    @Nonnull
    private List<YtPhoneByLogisticsPointId> ytPhoneByLogisticsPointIdList(Set<Long> logisticsPointIds) {
        return logisticsPointIds.stream()
            .map(this::ytPhoneByLogisticsPointId)
            .collect(Collectors.toList());
    }

    @Nonnull
    private List<YtScheduleDays> ytScheduleDaysList(Set<Long> scheduleIds) {
        return scheduleIds.stream()
            .map(this::ytScheduleDays)
            .collect(Collectors.toList());
    }

    @Nonnull
    private YtLogisticsPoint ytLogisticsPoint(Long logisticsPointId, Long scheduleId) {
        return RedisFromYtMigrationTestUtils.buildPoint(logisticsPointId).setScheduleId(Optional.of(scheduleId));
    }

    @Nonnull
    private YtScheduleDays ytScheduleDays(Long scheduleId) {
        return RedisFromYtMigrationTestUtils.buildSchedule(scheduleId);
    }

    @Nonnull
    private YtPhoneByLogisticsPointId ytPhoneByLogisticsPointId(Long logisticsPointId) {
        return RedisFromYtMigrationTestUtils.buildPhone(logisticsPointId);
    }
}
