package ru.yandex.market.logistics.lms.client.redis;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lms.client.utils.LmsLomClientLogUtils;
import ru.yandex.market.logistics.lms.client.utils.LmsLomClientsChainTestCases;
import ru.yandex.market.logistics.lom.entity.InternalVariable;
import ru.yandex.market.logistics.lom.entity.enums.InternalVariableType;
import ru.yandex.market.logistics.lom.lms.model.logging.enums.LmsLomLoggingCode;
import ru.yandex.market.logistics.lom.repository.InternalVariableRepository;
import ru.yandex.market.logistics.lom.service.redis.util.RedisKeys;
import ru.yandex.market.logistics.lom.service.yt.dto.YtInboundSchedule;
import ru.yandex.market.logistics.lom.service.yt.dto.YtScheduleDays;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.request.schedule.LogisticSegmentInboundScheduleFilter;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildInboundSchedule;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildSchedule;

@ParametersAreNonnullByDefault
@DisplayName("Получение расписаний заборов из логистических сегментов")
class LmsLomLightClientGetInboundScheduleTest extends LmsLomLightClientAbstractTest {

    private static final String INBOUND_SCHEDULE_TABLE_NAME = RedisKeys.getHashTableFromYtName(
        YtInboundSchedule.class,
        REDIS_ACTUAL_VERSION
    );
    private static final String SCHEDULE_DAYS_TABLE_NAME = RedisKeys.getHashTableFromYtName(
        YtScheduleDays.class,
        REDIS_ACTUAL_VERSION
    );

    public static final String INBOUND_SCHEDULE_QUERY =
        "schedules FROM [//home/2022-03-02T08:05:24Z/inbound_schedule_dyn] "
            + "WHERE partner_from = 1 AND partner_to = 2 AND delivery_type = 'COURIER'";

    public static final String SCHEDULE_DAYS_QUERY =
        "* FROM [//home/2022-03-02T08:05:24Z/schedule_days_dyn] WHERE schedule_id IN (321)";

    @Autowired
    private InternalVariableRepository internalVariableRepository;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @SneakyThrows
    @DisplayName("Все флаги включены, идем в redis")
    void searchInboundSchedule(String name, List<Long> scheduleIds, List<Long> scheduleDayIds) {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        String inboundScheduleHash = "1:2:COURIER";
        List<String> expectedScheduleDays = new ArrayList<>();
        for (Long scheduleId : scheduleIds) {
            if (scheduleId == null) {
                expectedScheduleDays.add(null);
            } else {
                expectedScheduleDays.add(objectMapper.writeValueAsString(buildSchedule(scheduleId)));
            }
        }

        doReturn(objectMapper.writeValueAsString(buildInboundSchedule(1L)))
            .when(clientJedis).hget(eq(INBOUND_SCHEDULE_TABLE_NAME), eq(inboundScheduleHash));
        doReturn(expectedScheduleDays)
            .when(clientJedis).hmget(eq(SCHEDULE_DAYS_TABLE_NAME), any());

        softly.assertThat(lmsLomLightClient.searchInboundSchedule(buildFilter()))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(buildScheduleDays(scheduleDayIds));

        verify(clientJedis).get(eq(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY));
        verify(clientJedis).hget(eq(INBOUND_SCHEDULE_TABLE_NAME), eq(inboundScheduleHash));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(clientJedis).hmget(eq(SCHEDULE_DAYS_TABLE_NAME), captor.capture());
        softly.assertThat(captor.getAllValues()).containsExactlyInAnyOrderElementsOf(List.of("1", "3"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @SneakyThrows
    @DisplayName("Нет расписаний")
    void inboundSchedulesNotFound(
        @SuppressWarnings("unused") String name,
        @Nullable YtInboundSchedule schedule
    ) {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        String inboundScheduleHash = "1:2:COURIER";
        LogisticSegmentInboundScheduleFilter filter = buildFilter();

        doReturn(schedule == null ? null : objectMapper.writeValueAsString(schedule))
            .when(clientJedis).hget(eq(INBOUND_SCHEDULE_TABLE_NAME), eq(inboundScheduleHash));

        lmsLomLightClient.searchInboundSchedule(filter);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(LmsLomClientLogUtils.getEntityNotFoundLog(
                LmsLomLoggingCode.LMS_LOM_REDIS,
                "searchInboundSchedule fromPartnerId = 1, toPartnerId = 2, deliveryType = COURIER"
            ));

        verify(clientJedis).get(eq(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY));
        verify(clientJedis).hget(eq(INBOUND_SCHEDULE_TABLE_NAME), eq(inboundScheduleHash));
        verify(lmsClient).searchInboundSchedule(filter);
    }

    @Test
    @SneakyThrows
    @DisplayName("Все расписания пустые")
    void allSchedulesAreEmpty() {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        String inboundScheduleHash = "1:2:COURIER";
        LogisticSegmentInboundScheduleFilter filter = buildFilter();

        List<String> expectedScheduleDays = new ArrayList<>();
        expectedScheduleDays.add(null);
        expectedScheduleDays.add(null);

        doReturn(objectMapper.writeValueAsString(buildInboundSchedule(1L)))
            .when(clientJedis).hget(eq(INBOUND_SCHEDULE_TABLE_NAME), eq(inboundScheduleHash));
        doReturn(expectedScheduleDays)
            .when(clientJedis).hmget(eq(SCHEDULE_DAYS_TABLE_NAME), any());

        lmsLomLightClient.searchInboundSchedule(filter);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(LmsLomClientLogUtils.getEntityNotFoundLog(
                LmsLomLoggingCode.LMS_LOM_REDIS,
                "searchInboundSchedule fromPartnerId = 1, toPartnerId = 2, deliveryType = COURIER"
            ));

        verify(clientJedis).get(eq(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY));
        verify(clientJedis).hget(eq(INBOUND_SCHEDULE_TABLE_NAME), eq(inboundScheduleHash));
        verify(lmsClient).searchInboundSchedule(filter);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(clientJedis).hmget(eq(SCHEDULE_DAYS_TABLE_NAME), captor.capture());
        softly.assertThat(captor.getAllValues()).containsExactlyInAnyOrderElementsOf(List.of("1", "3"));
    }

    @Test
    @DisplayName("Не проставлены партнеры в фильтре, идем в лмс")
    void emptyFilter() {
        LogisticSegmentInboundScheduleFilter filter = new LogisticSegmentInboundScheduleFilter();
        lmsLomLightClient.searchInboundSchedule(filter);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Fields 'fromPartnerId' and 'toPartnerId' in filter must not be null");
        verify(lmsClient).searchInboundSchedule(eq(filter));
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

        String inboundScheduleRedisId = "1:2:COURIER";
        String scheduleDaysRedisId = "321";

        List<ScheduleDayResponse> expectedDays = buildExpectedDays();

        doReturn(redisObjectConverter.serializeToString(buildYtInboundSchedule()))
            .when(clientJedis).hget(eq(INBOUND_SCHEDULE_TABLE_NAME), eq(inboundScheduleRedisId));

        if (dataExistsInRedis) {
            doReturn(List.of(redisObjectConverter.serializeToString(buildYtScheduleDays())))
                .when(clientJedis).hmget(eq(SCHEDULE_DAYS_TABLE_NAME), eq(scheduleDaysRedisId));
        }

        YtUtils.mockSelectRowsFromYt(
            ytTables,
            List.of(321L),
            INBOUND_SCHEDULE_QUERY
        );

        if (dataExistsInYt) {
            YtUtils.mockSelectRowsFromYt(
                ytTables,
                new HashSet<>(expectedDays),
                SCHEDULE_DAYS_QUERY
            );
        }

        var filter = buildFilter();

        if (dataExistsInLms) {
            doReturn(expectedDays).when(lmsClient).searchInboundSchedule(filter);
        }

        List<ScheduleDayResponse> actualDays = lmsLomLightClient.searchInboundSchedule(filter);

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(clientJedis).hget(eq(INBOUND_SCHEDULE_TABLE_NAME), eq(inboundScheduleRedisId));
        verify(clientJedis).hmget(eq(SCHEDULE_DAYS_TABLE_NAME), eq(scheduleDaysRedisId));

        if (!dataExistsInRedis) {
            if (fetchingFromYtEnabled) {
                YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
                YtUtils.verifySelectRowsInteractions(
                    ytTables,
                    INBOUND_SCHEDULE_QUERY
                );
                YtUtils.verifySelectRowsInteractions(
                    ytTables,
                    SCHEDULE_DAYS_QUERY
                );
            }

            if (!fetchingFromYtEnabled || !dataExistsInYt) {
                verify(lmsClient).searchInboundSchedule(eq(filter));
            }
        }

        if (
            dataExistsInRedis
                || (fetchingFromYtEnabled && dataExistsInYt)
                || dataExistsInLms
        ) {
            softly.assertThat(actualDays)
                .containsExactlyInAnyOrderElementsOf(expectedDays);
        } else {
            softly.assertThat(actualDays).isEmpty();
        }
    }

    @NotNull
    private List<ScheduleDayResponse> buildExpectedDays() {
        return List.of(
            new ScheduleDayResponse(
                1L,
                1,
                LocalTime.of(12, 0),
                LocalTime.of(21, 0),
                false
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

    @Nonnull
    @SuppressWarnings("unused")
    private static Stream<Arguments> getDataFromDifferentSources() {
        return LmsLomClientsChainTestCases.TEST_CASES.stream()
            .map(it -> it.getArguments("Получение расписаний услуги 'Забор'"));
    }

    @NotNull
    private YtInboundSchedule buildYtInboundSchedule() {
        return new YtInboundSchedule()
            .setPartnerTo(1L)
            .setPartnerFrom(2L)
            .setDeliveryType(Optional.of(DeliveryType.COURIER.getName()))
            .setSchedules(Optional.of(buildSchedulesJson()));
    }

    @NotNull
    private String buildSchedulesJson() {
        return "{\"schedules\":[321]}";
    }

    @NotNull
    private YtScheduleDays buildYtScheduleDays() {
        return new YtScheduleDays()
            .setId(321L)
            .setScheduleDays(buildScheduleDaysJson());
    }

    @Nonnull
    private static String buildScheduleDaysJson() {
        return "{\"schedule_days\":["
            + "{\"day\":" + 1 + ","
            + "\"id\":" + 1 + ","
            + "\"is_main\":false,"
            + "\"time_from\":\"12:00:00.000000\","
            + "\"time_to\":\"21:00:00.000000\""
            + "},"
            + "{\"day\":" + 2 + ","
            + "\"id\":" + 2 + ","
            + "\"is_main\":false,"
            + "\"time_from\":\"12:00:00.000000\","
            + "\"time_to\":\"21:00:00.000000\""
            + "}"
            + "]}";
    }

    private void setCheckingDataInYt(boolean fetchingFromYtEnabled) {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.GET_INBOUND_SCHEDULE_ENABLED)
                .setValue(Boolean.toString(fetchingFromYtEnabled))
        );
    }

    @Nonnull
    private static Stream<Arguments> searchInboundSchedule() {
        List<Long> scheduleDayIdsWithOneNull = new ArrayList<>();
        scheduleDayIdsWithOneNull.add(1L);
        scheduleDayIdsWithOneNull.add(null);

        List<Long> scheduleDayIdsWithAllNulls = new ArrayList<>();
        scheduleDayIdsWithOneNull.add(null);
        scheduleDayIdsWithOneNull.add(null);

        return Stream.of(
            Arguments.of(
                "Все расписания заполнены",
                List.of(1L, 3L),
                List.of(1L, 2L, 3L, 4L)
            ),
            Arguments.of(
                "Одно из расписаний пустое",
                scheduleDayIdsWithOneNull,
                List.of(1L, 2L)
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> inboundSchedulesNotFound() {
        return Stream.of(
            Arguments.of(
                "Пустой список расписаний",
                buildInboundSchedule(1L).setSchedules(Optional.empty())
            ),
            Arguments.of(
                "Нет записи в редисе",
                null
            )
        );
    }

    @Nonnull
    private LogisticSegmentInboundScheduleFilter buildFilter() {
        return new LogisticSegmentInboundScheduleFilter()
            .setFromPartnerId(1L)
            .setToPartnerId(2L)
            .setDeliveryType(DeliveryType.COURIER);
    }

    @Nonnull
    private List<ScheduleDayResponse> buildScheduleDays(List<Long> ids) {
        return ids.stream().map(
            id -> new ScheduleDayResponse(
                37465940 + id,
                id.intValue(),
                LocalTime.of(12, 00),
                LocalTime.of(21, 00),
                false
            )
        ).collect(Collectors.toList());
    }
}
