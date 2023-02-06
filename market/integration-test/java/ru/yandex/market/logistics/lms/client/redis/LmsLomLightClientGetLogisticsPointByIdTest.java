package ru.yandex.market.logistics.lms.client.redis;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildPhone;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildPoint;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildSchedule;

@DisplayName("Получение логистической точки по идентификатору")
class LmsLomLightClientGetLogisticsPointByIdTest extends LmsLomLightClientAbstractTest {

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

    private static final Long LOGISTICS_POINT_ID = 1L;
    private static final Long SCHEDULE_ID = 50L;

    private static final String GET_LOGISTICS_POINT_QUERY_PREFIX =
        "* FROM [//home/2022-03-02T08:05:24Z/logistics_point_dyn] WHERE id IN (";
    private static final String GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX =
        "* FROM [//home/2022-03-02T08:05:24Z/logistics_point_phones_dyn] WHERE id IN (";
    private static final String GET_LOGISTICS_POINT_SCHEDULE_QUERY_PREFIX =
        "* FROM [//home/2022-03-02T08:05:24Z/schedule_days_dyn] WHERE schedule_id IN (";

    private static final Long POINT_ID = 1L;

    @Autowired
    private LogisticsPointYtToLmsConverter logisticsPointYtToLmsConverter;

    @Autowired
    private InternalVariableRepository internalVariableRepository;

    @Test
    @DisplayName("Точка в redis не найдена, пишем в лог, идем в лмс")
    void pointNotFoundInRedis() {
        long pointId = 1L;
        lmsLomLightClient.getLogisticsPoint(pointId);

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(clientJedis).hget(
            RedisKeys.getHashTableFromYtName(YtLogisticsPoint.class, REDIS_ACTUAL_VERSION),
            String.valueOf(pointId)
        );
        verify(lmsClient).getLogisticsPoint(pointId);
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(LmsLomClientLogUtils.getEntityNotFoundLog(
                LmsLomLoggingCode.LMS_LOM_REDIS,
                "getLogisticsPointById id = 1"
            ));
    }

    @Test
    @DisplayName("Точка найдена в redis")
    void pointFoundInRedis() {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        doReturn(convertToString(buildPoint(POINT_ID)))
            .when(clientJedis).hget(POINTS_TABLE_NAME, String.valueOf(POINT_ID));
        doReturn(convertToString(buildPhone(POINT_ID)))
            .when(clientJedis).hget(PHONES_TABLE_NAME, String.valueOf(POINT_ID));
        doReturn(convertToString(buildSchedule(POINT_ID)))
            .when(clientJedis).hget(SCHEDULE_TABLE_NAME, String.valueOf(POINT_ID));

        YtLogisticsPoint expectedYtPoint = buildPoint(POINT_ID)
            .setScheduleDays(buildSchedule(POINT_ID))
            .setPhones(buildPhone(POINT_ID));

        LogisticsPointLightModel expectedModel = logisticsPointYtToLmsConverter.convert(expectedYtPoint);

        softly.assertThat(lmsLomLightClient.getLogisticsPoint(POINT_ID))
            .get()
            .usingRecursiveComparison()
            .isEqualTo(expectedModel);

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(clientJedis).hget(POINTS_TABLE_NAME, String.valueOf(POINT_ID));
        verify(clientJedis).hget(PHONES_TABLE_NAME, String.valueOf(POINT_ID));
        verify(clientJedis).hget(SCHEDULE_TABLE_NAME, String.valueOf(POINT_ID));
    }

    @Test
    @DisplayName("У точки нет расписания")
    void scheduleIdIsNull() {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        doReturn(convertToString(buildPoint(POINT_ID).setScheduleId(Optional.empty())))
            .when(clientJedis).hget(POINTS_TABLE_NAME, String.valueOf(POINT_ID));
        doReturn(convertToString(buildPhone(POINT_ID)))
            .when(clientJedis).hget(PHONES_TABLE_NAME, String.valueOf(POINT_ID));

        YtLogisticsPoint expectedYtPoint = buildPoint(POINT_ID)
            .setPhones(buildPhone(POINT_ID));

        LogisticsPointLightModel expectedModel = logisticsPointYtToLmsConverter.convert(expectedYtPoint);

        softly.assertThat(lmsLomLightClient.getLogisticsPoint(POINT_ID))
            .get()
            .usingRecursiveComparison()
            .isEqualTo(expectedModel);

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(clientJedis).hget(POINTS_TABLE_NAME, String.valueOf(POINT_ID));
        verify(clientJedis).hget(PHONES_TABLE_NAME, String.valueOf(POINT_ID));
    }

    @Test
    @DisplayName("У точки пустое расписание")
    void scheduleIsEmpty() {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        doReturn(convertToString(buildPoint(POINT_ID)))
            .when(clientJedis).hget(POINTS_TABLE_NAME, String.valueOf(POINT_ID));
        doReturn(convertToString(buildPhone(POINT_ID)))
            .when(clientJedis).hget(PHONES_TABLE_NAME, String.valueOf(POINT_ID));
        doReturn(null)
            .when(clientJedis).hget(SCHEDULE_TABLE_NAME, String.valueOf(POINT_ID));

        YtLogisticsPoint expectedYtPoint = buildPoint(POINT_ID)
            .setPhones(buildPhone(POINT_ID))
            .setScheduleDays(buildSchedule(POINT_ID).setScheduleDays(null));

        LogisticsPointLightModel expectedModel = logisticsPointYtToLmsConverter.convert(expectedYtPoint);

        softly.assertThat(lmsLomLightClient.getLogisticsPoint(POINT_ID))
            .get()
            .usingRecursiveComparison()
            .isEqualTo(expectedModel);

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(clientJedis).hget(POINTS_TABLE_NAME, String.valueOf(POINT_ID));
        verify(clientJedis).hget(PHONES_TABLE_NAME, String.valueOf(POINT_ID));
        verify(clientJedis).hget(SCHEDULE_TABLE_NAME, String.valueOf(POINT_ID));
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

        LogisticsPointLightModel logisticsPointLightModel =
            logisticsPointYtToLmsConverter.convert(ytLogisticsPointCollected(LOGISTICS_POINT_ID, SCHEDULE_ID));

        if (dataExistsInRedis) {
            doReturn(convertToString(ytLogisticsPoint(LOGISTICS_POINT_ID, SCHEDULE_ID)))
                .when(clientJedis).hget(POINTS_TABLE_NAME, String.valueOf(LOGISTICS_POINT_ID));

            doReturn(convertToString(ytPhoneByLogisticsPointId(LOGISTICS_POINT_ID)))
                .when(clientJedis).hget(PHONES_TABLE_NAME, String.valueOf(LOGISTICS_POINT_ID));

            doReturn(convertToString(ytScheduleDays(SCHEDULE_ID)))
                .when(clientJedis).hget(SCHEDULE_TABLE_NAME, String.valueOf(SCHEDULE_ID));
        }

        if (dataExistsInYt) {
            mockYtLogisticsPointResponse();
        }

        if (dataExistsInLms) {
            doReturn(Optional.of(logisticsPointLightModel)).when(lmsClient)
                .getLogisticsPoint(LOGISTICS_POINT_ID);
        }

        Optional<LogisticsPointLightModel> result = lmsLomLightClient.getLogisticsPoint(LOGISTICS_POINT_ID);

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(clientJedis).hget(POINTS_TABLE_NAME, String.valueOf(POINT_ID));

        if (!dataExistsInRedis) {
            if (fetchingFromYtEnabled) {
                YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
                YtUtils.verifySelectRowsInteractionsQueryStartsWith(ytTables, GET_LOGISTICS_POINT_QUERY_PREFIX);

                if (dataExistsInYt) {
                    YtUtils.verifySelectRowsInteractionsQueryStartsWith(
                        ytTables,
                        GET_LOGISTICS_POINT_PHONES_QUERY_PREFIX
                    );
                    YtUtils.verifySelectRowsInteractionsQueryStartsWith(
                        ytTables,
                        GET_LOGISTICS_POINT_SCHEDULE_QUERY_PREFIX
                    );
                    verify(hahnYt, times(4)).tables();
                } else {
                    verify(hahnYt, times(2)).tables();
                }
            }

            if (!fetchingFromYtEnabled || !dataExistsInYt) {
                verify(lmsClient).getLogisticsPoint(any());
            }
        } else {
            verify(clientJedis).hget(PHONES_TABLE_NAME, String.valueOf(POINT_ID));
            verify(clientJedis).hget(SCHEDULE_TABLE_NAME, String.valueOf(SCHEDULE_ID));
        }

        if (dataExistsInRedis || (fetchingFromYtEnabled && dataExistsInYt) || dataExistsInLms) {
            softly.assertThat(result).isNotEmpty();
            softly.assertThat(result.get())
                .usingRecursiveComparison()
                .isEqualTo(logisticsPointLightModel);
        } else {
            softly.assertThat(result)
                .isEmpty();
        }
    }

    @Nonnull
    private static Stream<Arguments> getDataFromDifferentSources() {
        return LmsLomClientsChainTestCases.TEST_CASES.stream()
            .map(it -> it.getArguments("Получение логистической точки по идентификатору"));
    }

    private void setCheckingDataInYt(boolean isCheckEnabled) {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.GET_POINT_BY_ID_FROM_YT_ENABLED)
                .setValue(Boolean.toString(isCheckEnabled))
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

    @Nonnull
    private YtLogisticsPoint ytLogisticsPointCollected(Long logisticsPointId, Long scheduleId) {
        YtLogisticsPoint ytLogisticsPoint = ytLogisticsPoint(logisticsPointId, scheduleId);
        ytLogisticsPoint.setScheduleDays(ytScheduleDays(scheduleId));
        ytLogisticsPoint.setPhones(ytPhoneByLogisticsPointId(logisticsPointId));
        return ytLogisticsPoint;
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
