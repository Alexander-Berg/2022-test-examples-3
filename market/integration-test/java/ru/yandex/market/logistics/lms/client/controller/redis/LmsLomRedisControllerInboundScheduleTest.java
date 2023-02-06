package ru.yandex.market.logistics.lms.client.controller.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistics.lom.service.redis.AbstractRedisTest;
import ru.yandex.market.logistics.lom.service.redis.util.RedisKeys;
import ru.yandex.market.logistics.lom.service.yt.dto.YtInboundSchedule;
import ru.yandex.market.logistics.lom.service.yt.dto.YtScheduleDays;
import ru.yandex.market.logistics.management.entity.request.schedule.LogisticSegmentInboundScheduleFilter;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@ParametersAreNonnullByDefault
@DisplayName("Ручки для тестирования работы редиса с методами по получению расписаний заборов")
class LmsLomRedisControllerInboundScheduleTest extends AbstractRedisTest {

    private static final long PARTNER_FROM_ID = 1L;
    private static final long PARTNER_TO_ID = 1L;
    private static final DeliveryType DELIVERY_TYPE = DeliveryType.COURIER;

    private static final String GET_INBOUND_SCHEDULE_BY_FILTER_PATH =
        "/lms/test-redis/inbound-schedule/get-by-filter";

    @Test
    @SneakyThrows
    @DisplayName("Получение расписаний заборов по фильтру")
    void getInboundScheduleByFilter() {
        LogisticSegmentInboundScheduleFilter filter = new LogisticSegmentInboundScheduleFilter()
            .setFromPartnerId(PARTNER_FROM_ID)
            .setToPartnerId(PARTNER_TO_ID)
            .setDeliveryType(DELIVERY_TYPE);
        Set<Long> scheduleIds = Set.of(0L, 1L, 2L);
        doReturn(
            redisObjectConverter.serializeToString(buildYtInboundSchedule(
                PARTNER_FROM_ID,
                PARTNER_TO_ID,
                DELIVERY_TYPE,
                3
            ))
        )
            .when(clientJedis).hget(
                getInboundScheduleHashTableName(),
                String.format("%s:%s:%s", PARTNER_FROM_ID, PARTNER_TO_ID, DELIVERY_TYPE)
            );

        doReturn(
            buildYtScheduleDays(scheduleIds, 3).stream()
                .map(redisObjectConverter::serializeToString)
                .collect(Collectors.toList())
        )
            .when(clientJedis).hmget(eq(getScheduleDaysHashTableName()), any());

        performScheduleSearchByFilter(filter)
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "lms/client/controller/inbound_schedule_by_filter_response.json",
                false
            ));

        verify(clientJedis).hget(
            getInboundScheduleHashTableName(),
            String.format("%s:%s:%s", PARTNER_FROM_ID, PARTNER_TO_ID, DELIVERY_TYPE)
        );
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(clientJedis).hmget(
            eq(getScheduleDaysHashTableName()),
            argumentCaptor.capture()
        );
        softly.assertThat(argumentCaptor.getAllValues())
            .containsExactlyInAnyOrderElementsOf(
                scheduleIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList())
            );
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение пустых расписаний заборов по фильтру")
    void getEmptyInboundScheduleByFilter() {
        LogisticSegmentInboundScheduleFilter filter = new LogisticSegmentInboundScheduleFilter()
            .setFromPartnerId(PARTNER_FROM_ID)
            .setToPartnerId(PARTNER_TO_ID)
            .setDeliveryType(DELIVERY_TYPE);

        performScheduleSearchByFilter(filter)
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));

        verify(clientJedis).hget(
            getInboundScheduleHashTableName(),
            String.format("%s:%s:%s", PARTNER_FROM_ID, PARTNER_TO_ID, DELIVERY_TYPE)
        );
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение расписаний заборов по пустому фильтру")
    void getInboundScheduleByEmptyFilter() {
        LogisticSegmentInboundScheduleFilter filter = new LogisticSegmentInboundScheduleFilter();

        softly.assertThatCode(
                () -> performScheduleSearchByFilter(filter)
            )
            .isInstanceOf(NestedServletException.class)
            .hasMessage("Request processing failed; nested exception is java.lang.IllegalArgumentException: "
                + "Fields 'fromPartnerId' and 'toPartnerId' in filter must not be null");
    }

    @Nonnull
    @SneakyThrows
    private ResultActions performScheduleSearchByFilter(LogisticSegmentInboundScheduleFilter filter) {
        return mockMvc.perform(
            post(GET_INBOUND_SCHEDULE_BY_FILTER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(redisObjectConverter.serializeToString(filter))
        );
    }

    @Nonnull
    private List<YtScheduleDays> buildYtScheduleDays(Set<Long> scheduleIds, int schedulesSize) {
        return scheduleIds.stream()
            .map(scheduleId ->
                new YtScheduleDays()
                    .setId(scheduleId)
                    .setScheduleDays(buildScheduleDaysJson(scheduleId, schedulesSize))
            )
            .collect(Collectors.toList());
    }

    @Nonnull
    private String buildScheduleDaysJson(long scheduleId, long scheduleSize) {
        List<String> scheduleDays = new ArrayList<>();
        for (int i = 0; i < scheduleSize; i++) {
            scheduleDays.add(buildScheduleDay(scheduleId, i));
        }
        return "{\"schedule_days\":[" + String.join(",", scheduleDays) + "]}";
    }

    @Nonnull
    private static String buildScheduleDay(long scheduleDayId, long day) {
        return "{\"day\":" + day + "," +
            "\"id\":" + scheduleDayId + "," +
            "\"is_main\":true," +
            "\"time_from\":\"" + (10 + day) + ":00:00.000000\"," +
            "\"time_to\":\"20:00:00.000000\"" +
            "}";
    }

    @Nonnull
    private YtInboundSchedule buildYtInboundSchedule(
        long partnerFrom,
        long partnerTo,
        DeliveryType deliveryType,
        int schedulesAmount
    ) {
        YtInboundSchedule schedule = new YtInboundSchedule();

        schedule.setPartnerFrom(partnerFrom);
        schedule.setPartnerTo(partnerTo);
        schedule.setDeliveryType(Optional.of(deliveryType.name()));
        schedule.setSchedules(Optional.of(buildScheduleDayIdsJson(schedulesAmount)));

        return schedule;
    }

    @Nonnull
    private String buildScheduleDayIdsJson(int scheduleSize) {
        List<Long> scheduleIds = new ArrayList<>();
        for (int i = 0; i < scheduleSize; i++) {
            scheduleIds.add((long) i);
        }
        return "{\"schedules\":" + redisObjectConverter.serializeToString(scheduleIds) + "}";
    }

    @Nonnull
    private String getInboundScheduleHashTableName() {
        return RedisKeys.getHashTableFromYtName(YtInboundSchedule.class, RedisKeys.REDIS_DEFAULT_VERSION);
    }

    @Nonnull
    private String getScheduleDaysHashTableName() {
        return RedisKeys.getHashTableFromYtName(YtScheduleDays.class, RedisKeys.REDIS_DEFAULT_VERSION);
    }
}
