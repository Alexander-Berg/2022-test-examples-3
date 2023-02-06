package ru.yandex.market.logistics.lms.client.yt;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.lms.converter.LogisticsPointYtToLmsConverter;
import ru.yandex.market.logistics.lom.lms.model.LogisticsPointLightModel;
import ru.yandex.market.logistics.lom.service.yt.dto.YtLogisticsPoint;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPhoneByLogisticsPointId;
import ru.yandex.market.logistics.lom.service.yt.dto.YtScheduleDays;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("Получение лог точек по идентификатору")
class LmsLomYtGetLogisticsPointTest extends LmsLomYtAbstractTest {
    private static final String GET_LOGISTICS_POINT_QUERY_PREFIX =
        "* FROM [//home/2022-03-02T08:05:24Z/logistics_point_dyn] WHERE id IN (";
    private static final String GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX =
        "* FROM [//home/2022-03-02T08:05:24Z/logistics_point_phones_dyn] WHERE id IN (";
    private static final String GET_LOGISTICS_POINT_SCHEDULE_QUERY_PREFIX =
        "* FROM [//home/2022-03-02T08:05:24Z/schedule_days_dyn] WHERE schedule_id IN (";

    private static final Long LOGISTICS_POINT_ID = 1L;
    private static final Long SCHEDULE_ID = 50L;

    private static final Set<Long> LOGISTICS_POINT_IDS = Set.of(1L, 2L, 3L, 4L);
    private static final Set<Long> SCHEDULE_IDS = Set.of(10L, 20L, 30L, 40L);
    private static final LogisticsPointFilter FILTER =
        LogisticsPointFilter.newBuilder().ids(LOGISTICS_POINT_IDS).build();

    @Autowired
    private LogisticsPointYtToLmsConverter converter;

    @Test
    @DisplayName("Успешное получение лог точки по id из yt")
    @DatabaseSetup("/lms/client/yt/get_point_by_id_from_yt_enabled.xml")
    void successGetLogisticsPointById() {
        mockYtLogisticsPointResponse();

        softly.assertThat(lmsLomYtClient.getLogisticsPoint(LOGISTICS_POINT_ID))
            .usingRecursiveComparison()
            .isEqualTo(Optional.of(logisticsPointLightModel(LOGISTICS_POINT_ID, SCHEDULE_ID)));

        verifyYtCalling(List.of(
            GET_LOGISTICS_POINT_QUERY_PREFIX,
            GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX,
            GET_LOGISTICS_POINT_SCHEDULE_QUERY_PREFIX
        ));
    }

    @Test
    @DisplayName("Флаг получения лог точки по id выключен, клиент не идет в yt")
    void goingToYtForLogisticsPointDisabled() {
        mockYtLogisticsPointResponse();

        softly.assertThat(lmsLomYtClient.getLogisticsPoint(LOGISTICS_POINT_ID))
            .isEmpty();
    }

    @Test
    @DisplayName("Успешное получение лог точки из yt по фильтру")
    @DatabaseSetup("/lms/client/yt/get_point_by_filter_from_yt_enabled.xml")
    void successGetLogisticsPointByFilter() {
        mockYtLogisticsPointsResponse();

        softly.assertThat(lmsLomYtClient.getLogisticsPoints(FILTER))
            .usingRecursiveComparison()
            .isEqualTo(logisticsPointLightModels(LOGISTICS_POINT_IDS));

        verifyYtCalling(List.of(
            GET_LOGISTICS_POINT_QUERY_PREFIX,
            GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX,
            GET_LOGISTICS_POINT_SCHEDULE_QUERY_PREFIX
        ));
    }

    @Test
    @DisplayName("Получение лог точек из yt по фильтру с идентификаторами: не все точки найдены")
    @DatabaseSetup("/lms/client/yt/get_point_by_filter_from_yt_enabled.xml")
    void successGetLogisticsPointByFilterFoundLessThanNeeded() {
        mockYtLogisticsPointsResponseSomePointsNotFound();

        softly.assertThat(lmsLomYtClient.getLogisticsPoints(FILTER))
            .isEmpty();

        verifyYtCalling(List.of(GET_LOGISTICS_POINT_QUERY_PREFIX));
    }

    @Test
    @DisplayName("Флаг получения лог точек по фильтру выключен, клиент не идет в yt")
    void goingToYtForLogisticsPointByFilterDisabled() {
        mockYtLogisticsPointsResponse();

        softly.assertThat(lmsLomYtClient.getLogisticsPoints(FILTER))
            .isEmpty();
    }

    private void verifyYtCalling(List<String> queriesToVerify) {
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        queriesToVerify.forEach(query -> YtUtils.verifySelectRowsInteractionsQueryStartsWith(ytTables, query));
        verify(hahnYt, times(queriesToVerify.size() + 1)).tables();
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

    private void mockYtLogisticsPointsResponseSomePointsNotFound() {
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            ytLogisticsPointList(Set.of(1L, 2L)),
            GET_LOGISTICS_POINT_QUERY_PREFIX
        );
    }

    private void mockYtLogisticsPointsResponse() {
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
    private List<LogisticsPointLightModel> logisticsPointLightModels(Set<Long> logisticsPointIds) {
        return logisticsPointIds.stream().map(id -> logisticsPointLightModel(id, 10 * id)).collect(Collectors.toList());
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
    private LogisticsPointLightModel logisticsPointLightModel(Long logisticsPointId, Long scheduleId) {
        YtLogisticsPoint ytLogisticsPoint = ytLogisticsPoint(logisticsPointId, scheduleId);
        ytLogisticsPoint.setScheduleDays(ytScheduleDays(scheduleId));
        ytLogisticsPoint.setPhones(ytPhoneByLogisticsPointId(logisticsPointId));

        return converter.convert(ytLogisticsPoint);
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
