package ru.yandex.market.logistics.lms.client.redis;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lms.client.utils.LmsLomClientLogUtils;
import ru.yandex.market.logistics.lms.client.utils.LmsLomClientsChainTestCases;
import ru.yandex.market.logistics.lom.entity.InternalVariable;
import ru.yandex.market.logistics.lom.entity.enums.InternalVariableType;
import ru.yandex.market.logistics.lom.lms.converter.LogisticsPointYtToLmsConverter;
import ru.yandex.market.logistics.lom.lms.model.LogisticsPointLightModel;
import ru.yandex.market.logistics.lom.lms.model.logging.enums.LmsLomLoggingCode;
import ru.yandex.market.logistics.lom.repository.InternalVariableRepository;
import ru.yandex.market.logistics.lom.service.redis.util.RedisKeys;
import ru.yandex.market.logistics.lom.service.yt.dto.YtLogisticsPoint;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPhoneByLogisticsPointId;
import ru.yandex.market.logistics.lom.service.yt.dto.YtScheduleDays;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildPhone;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildPoint;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildSchedule;

@DisplayName("Получение логистических точек по идентификаторам")
@ParametersAreNonnullByDefault
class LmsLomLightClientGetLogisticsPointsByIdsTest extends LmsLomLightClientAbstractTest {
    private static final String POINTS_TABLE_NAME = RedisKeys.getHashTableFromYtName(
        YtLogisticsPoint.class,
        REDIS_ACTUAL_VERSION
    );
    private static final String PHONES_TABLE_NAME = RedisKeys.getHashTableFromYtName(
        YtPhoneByLogisticsPointId.class,
        REDIS_ACTUAL_VERSION
    );
    private static final String SCHEDULE_TABLE_NAME = RedisKeys.getHashTableFromYtName(
        YtScheduleDays.class,
        REDIS_ACTUAL_VERSION
    );
    private static final Set<Long> POINT_IDS = Set.of(101L, 102L, 103L);
    private static final Set<Long> SCHEDULE_IDS = Set.of(1010L, 1020L, 1030L);
    private static final Set<String> STRING_POINT_IDS = Set.of("101", "102", "103");
    private static final Set<String> STRING_SCHEDULE_IDS = Set.of("1010", "1020", "1030");

    private static final String GET_LOGISTICS_POINT_QUERY_PREFIX =
        "* FROM [//home/2022-03-02T08:05:24Z/logistics_point_dyn] WHERE id IN (";
    private static final String GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX =
        "* FROM [//home/2022-03-02T08:05:24Z/logistics_point_phones_dyn] WHERE id IN (";
    private static final String GET_LOGISTICS_POINT_SCHEDULE_QUERY_PREFIX =
        "* FROM [//home/2022-03-02T08:05:24Z/schedule_days_dyn] WHERE schedule_id IN (";

    @Autowired
    private LogisticsPointYtToLmsConverter logisticsPointYtToLmsConverter;

    @Autowired
    private InternalVariableRepository internalVariableRepository;

    @Test
    @SneakyThrows
    @DisplayName("Точки в редисе не найдены, идём в лмс")
    void pointsNotFoundInRedis() {
        LogisticsPointFilter filter = getFilterWithPointIds(Set.of(5L));
        lmsLomLightClient.getLogisticsPointsByIds(Set.of(5L));

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        Set<String> request = Set.of("5");
        verifyMultiGetArguments(POINTS_TABLE_NAME, request);

        verify(lmsClient).getLogisticsPoints(filter);
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(LmsLomClientLogUtils.getEntityNotFoundLog(
                LmsLomLoggingCode.LMS_LOM_REDIS,
                "getLogisticsPointsByFilter ids = [5], partnerIds = null, type = null, active = null"
            ));
    }

    @Test
    @SneakyThrows
    @DisplayName("Не все точки в редисе найдены, идём в лмс")
    void filterByPointIdsNotAll() {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        mockPoints(POINT_IDS);

        Set<Long> ids = Set.of(101L, 102L, 103L, 5L);
        LogisticsPointFilter filter = getFilterWithPointIds(ids);
        lmsLomLightClient.getLogisticsPointsByIds(ids);

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        Set<String> request = Set.of("101", "102", "103", "5");
        verifyMultiGetArguments(POINTS_TABLE_NAME, request);

        verify(lmsClient).getLogisticsPoints(filter);
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(LmsLomClientLogUtils.getEntityNotFoundLog(
                    LmsLomLoggingCode.LMS_LOM_REDIS,
                    "getLogisticsPointsByFilter ids = [5, 101, 102, 103], partnerIds = null, type = null, active = null"
                )
            );
    }

    @Test
    @SneakyThrows
    @DisplayName("Идем в redis, фильтр с id точек")
    void filterByPointIds() {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        mockPoints(POINT_IDS);

        List<LogisticsPointLightModel> expectedModels = POINT_IDS.stream()
            .map(id -> buildPoint(id).setPhones(buildPhone(id)).setScheduleDays(buildSchedule(id)))
            .map(logisticsPointYtToLmsConverter::convert)
            .collect(Collectors.toList());

        softly.assertThat(lmsLomLightClient.getLogisticsPointsByIds(POINT_IDS))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expectedModels);

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verifyPoints(STRING_POINT_IDS);
    }

    @Test
    @SneakyThrows
    @DisplayName("У точек нет расписаний")
    void scheduleIdsAreNull() {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        doReturn(POINT_IDS.stream()
            .map(id -> convertToString(buildPoint(id).setScheduleId(Optional.empty())))
            .collect(Collectors.toList())
        ).when(clientJedis).hmget(eq(POINTS_TABLE_NAME), any());
        doReturn(POINT_IDS.stream()
            .map(id -> convertToString(buildPhone(id)))
            .collect(Collectors.toList())
        ).when(clientJedis).hmget(eq(PHONES_TABLE_NAME), any());

        List<LogisticsPointLightModel> expectedModels = POINT_IDS.stream()
            .map(id -> buildPoint(id).setPhones(buildPhone(id)))
            .map(logisticsPointYtToLmsConverter::convert)
            .collect(Collectors.toList());

        softly.assertThat(lmsLomLightClient.getLogisticsPointsByIds(POINT_IDS))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expectedModels);

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verifyMultiGetArguments(POINTS_TABLE_NAME, STRING_POINT_IDS);
        verifyMultiGetArguments(PHONES_TABLE_NAME, STRING_POINT_IDS);
    }

    @Test
    @SneakyThrows
    @DisplayName("У точек пустые расписания")
    void schedulesAreEmpty() {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        doReturn(POINT_IDS.stream()
            .map(id -> convertToString(buildPoint(id))).collect(Collectors.toList())
        ).when(clientJedis).hmget(eq(POINTS_TABLE_NAME), any());
        doReturn(POINT_IDS.stream()
            .map(id -> convertToString(buildPhone(id)))
            .collect(Collectors.toList())
        ).when(clientJedis).hmget(eq(PHONES_TABLE_NAME), any());
        doReturn(POINT_IDS.stream()
            .map(id -> convertToString(buildSchedule(id).setScheduleDays(null)))
            .collect(Collectors.toList())
        ).when(clientJedis).hmget(eq(SCHEDULE_TABLE_NAME), any());

        List<LogisticsPointLightModel> expectedModels = POINT_IDS.stream()
            .map(id -> buildPoint(id)
                .setPhones(buildPhone(id))
                .setScheduleDays(buildSchedule(id).setScheduleDays(null))
            )
            .map(logisticsPointYtToLmsConverter::convert)
            .collect(Collectors.toList());

        softly.assertThat(lmsLomLightClient.getLogisticsPointsByIds(POINT_IDS))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expectedModels);

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verifyPoints(STRING_POINT_IDS);
    }

    @Test
    @DisplayName("Пустой сет идентификаторов")
    void emptyFilter() {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        lmsLomLightClient.getLogisticsPointsByIds(Set.of());
    }

    private void mockPoints(Set<Long> pointIds) {
        doReturn(pointIds.stream()
            .map(id -> convertToString(buildPoint(id)))
            .collect(Collectors.toList())
        ).when(clientJedis).hmget(eq(POINTS_TABLE_NAME), any());
        doReturn(pointIds.stream()
            .map(id -> convertToString(buildPhone(id)))
            .collect(Collectors.toList())
        ).when(clientJedis).hmget(eq(PHONES_TABLE_NAME), any());
        doReturn(pointIds.stream()
            .map(id -> convertToString(buildSchedule(id)))
            .collect(Collectors.toList())
        ).when(clientJedis).hmget(eq(SCHEDULE_TABLE_NAME), any());
    }

    private void verifyPoints(Set<String> multiGetRequest) {
        verifyMultiGetArguments(POINTS_TABLE_NAME, multiGetRequest);
        verifyMultiGetArguments(PHONES_TABLE_NAME, multiGetRequest);
        verifyMultiGetArguments(SCHEDULE_TABLE_NAME, multiGetRequest);
    }

    @Nonnull
    private static LogisticsPointFilter getFilterWithPointIds(Set<Long> ids) {
        return LogisticsPointFilter.newBuilder()
            .ids(ids)
            .build();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Достаем данные из разных источников (Redis, YT, LMS)")
    void getDataFromDifferentSources(
        @SuppressWarnings("unused") String displayName,
        boolean dataExistsInRedis,
        boolean dataExistsInYt,
        boolean fetchingFromYtEnabled,
        boolean dataExistsInLms
    ) {
        setCheckingDataInYt(fetchingFromYtEnabled);
        mockExistingData(dataExistsInRedis, dataExistsInYt, dataExistsInLms);

        List<LogisticsPointLightModel> result = lmsLomLightClient.getLogisticsPointsByIds(POINT_IDS);

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verifyMultiGetArguments(POINTS_TABLE_NAME, STRING_POINT_IDS);

        if (!dataExistsInRedis) {
            if (fetchingFromYtEnabled) {
                if (dataExistsInYt) {
                    verifyYtCalling(List.of(
                        GET_LOGISTICS_POINT_QUERY_PREFIX,
                        GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX,
                        GET_LOGISTICS_POINT_SCHEDULE_QUERY_PREFIX
                    ));

                } else {
                    verifyYtCalling(List.of(GET_LOGISTICS_POINT_QUERY_PREFIX));
                }
            }

            if (!fetchingFromYtEnabled || !dataExistsInYt) {
                verify(lmsClient).getLogisticsPoints(any());
            }
        } else {
            verifyMultiGetArguments(PHONES_TABLE_NAME, STRING_POINT_IDS);
            verifyMultiGetArguments(SCHEDULE_TABLE_NAME, STRING_SCHEDULE_IDS);
        }

        if (dataExistsInRedis || (fetchingFromYtEnabled && dataExistsInYt) || dataExistsInLms) {
            softly.assertThat(result)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .isEqualTo(logisticsPointLightModels(POINT_IDS));
        } else {
            softly.assertThat(result)
                .isEmpty();
        }
    }

    @Nonnull
    private static Stream<Arguments> getDataFromDifferentSources() {
        return LmsLomClientsChainTestCases.TEST_CASES.stream()
            .map(it -> it.getArguments("Получение логистических точек по идентификаторам"));
    }

    private void verifyYtCalling(List<String> queries) {
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        queries.forEach(query ->
            YtUtils.verifySelectRowsInteractionsQueryStartsWith(
                ytTables,
                query
            )
        );

        verify(hahnYt, times(queries.size() + 1)).tables();
    }

    private void mockExistingData(boolean dataExistsInRedis, boolean dataExistsInYt, boolean dataExistsInLms) {
        if (dataExistsInRedis) {
            doReturn(ytLogisticsPointList(POINT_IDS).stream().map(this::convertToString).collect(Collectors.toList()))
                .when(clientJedis).hmget(eq(POINTS_TABLE_NAME), any());

            doReturn(
                ytPhoneByLogisticsPointIdList(POINT_IDS).stream()
                    .map(this::convertToString)
                    .collect(Collectors.toList())
            )
                .when(clientJedis).hmget(eq(PHONES_TABLE_NAME), any());

            doReturn(ytScheduleDaysList(SCHEDULE_IDS).stream().map(this::convertToString).collect(Collectors.toList()))
                .when(clientJedis).hmget(eq(SCHEDULE_TABLE_NAME), any());
        }

        if (dataExistsInYt) {
            mockYtLogisticsPointsResponse();
        }

        if (dataExistsInLms) {
            doReturn(logisticsPointLightModels(POINT_IDS)).when(lmsClient)
                .getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(POINT_IDS).build());
        }
    }

    private void setCheckingDataInYt(boolean isCheckEnabled) {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.GET_POINT_BY_FILTER_FROM_YT_ENABLED)
                .setValue(Boolean.toString(isCheckEnabled))
        );
    }

    private void mockYtLogisticsPointsResponse() {
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            ytLogisticsPointList(POINT_IDS),
            GET_LOGISTICS_POINT_QUERY_PREFIX
        );
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            ytPhoneByLogisticsPointIdList(POINT_IDS),
            GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX
        );
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            ytScheduleDaysList(POINT_IDS.stream().map(pointId -> pointId * 10).collect(Collectors.toSet())),
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

        return logisticsPointYtToLmsConverter.convert(ytLogisticsPoint);
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
