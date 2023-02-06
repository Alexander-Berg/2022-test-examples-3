package ru.yandex.market.logistics.lms.client.controller.redis;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistics.lom.service.redis.AbstractRedisTest;
import ru.yandex.market.logistics.lom.service.redis.util.RedisKeys;
import ru.yandex.market.logistics.lom.service.yt.dto.YtLogisticsPoint;
import ru.yandex.market.logistics.lom.service.yt.dto.YtLogisticsPointsAggModel;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPhoneByLogisticsPointId;
import ru.yandex.market.logistics.lom.service.yt.dto.YtScheduleDays;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildPhone;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildPoint;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildPointAgg;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildSchedule;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DisplayName("Ручки для тестирования работы редиса с методами логистических точек")
class LmsLomRedisControllerLogisticsPointTest extends AbstractRedisTest {

    private static final String LOGISTICS_POINT_ID = "1";
    private static final String GET_LOGISTICS_POINT_BY_ID_PATH = "/lms/test-redis/logistics-point/get/"
        + LOGISTICS_POINT_ID;
    private static final String GET_LOGISTICS_POINTS_BY_FILTER_PATH = "/lms/test-redis/logistics-point/get-by-filter";

    @Override
    @AfterEach
    public void tearDown() {
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        super.tearDown();
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение логистической точки: точка существует")
    void getLogisticsPointSuccess() {
        long logisticsPointId = Long.parseLong(LOGISTICS_POINT_ID);
        doReturn(redisObjectConverter.serializeToString(buildPoint(logisticsPointId)))
            .when(clientJedis).hget(getLogisticsPointsHashTableName(), LOGISTICS_POINT_ID);
        doReturn(redisObjectConverter.serializeToString(buildPhone(logisticsPointId)))
            .when(clientJedis).hget(getPhonesHashTableName(), LOGISTICS_POINT_ID);
        doReturn(redisObjectConverter.serializeToString(buildSchedule(logisticsPointId)))
            .when(clientJedis).hget(getScheduleHashTableName(), LOGISTICS_POINT_ID);

        mockMvc.perform(get(GET_LOGISTICS_POINT_BY_ID_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/logistics_point_by_id_response.json"));

        verify(clientJedis).hget(getLogisticsPointsHashTableName(), LOGISTICS_POINT_ID);
        verify(clientJedis).hget(getPhonesHashTableName(), LOGISTICS_POINT_ID);
        verify(clientJedis).hget(getScheduleHashTableName(), LOGISTICS_POINT_ID);
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение логистической точки: ошибка редиса")
    void getLogisticsPointError() {
        doThrow(new RuntimeException())
            .when(clientJedis).hget(getLogisticsPointsHashTableName(), LOGISTICS_POINT_ID);

        softly.assertThatCode(() -> mockMvc.perform(get(GET_LOGISTICS_POINT_BY_ID_PATH)))
            .isInstanceOf(NestedServletException.class)
            .hasMessage(
                "Request processing failed; nested exception is java.lang.RuntimeException: "
                    + "java.lang.RuntimeException: Connection retries to redis limit exceeded"
            );

        verifyGetLogisticsPoint();
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение логистической точки: точки не существует")
    void getLogisticsPointNoPointExists() {
        mockMvc.perform(get(GET_LOGISTICS_POINT_BY_ID_PATH))
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        verifyGetLogisticsPoint();
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение логистических точек по фильтру")
    void getPointsByFilter() {
        Set<Long> partnerIds = Set.of(1L, 2L, 3L);
        String[] partnerKeysRequest = {
            "1:WAREHOUSE:true",
            "2:WAREHOUSE:true",
            "3:WAREHOUSE:true"
        };
        String[] pointsRequest = {"101", "111", "102", "112", "103", "113"};
        LogisticsPointFilter filter = LogisticsPointFilter.newBuilder()
            .partnerIds(partnerIds)
            .active(true)
            .type(PointType.WAREHOUSE)
            .build();
        doReturn(
            partnerIds.stream()
                .map(id -> redisObjectConverter.serializeToString(buildPointAgg(id, List.of(100 + id, 110 + id))))
                .collect(Collectors.toList())
        )
            .when(clientJedis).hmget(eq(getPointsAggHashTableName()), any());
        doReturn(Arrays.stream(pointsRequest)
            .map(id -> redisObjectConverter.serializeToString(buildPoint(Long.parseLong(id))))
            .collect(Collectors.toList())
        )
            .when(clientJedis).hmget(eq(getLogisticsPointsHashTableName()), any());

        mockMvc.perform(
                post(GET_LOGISTICS_POINTS_BY_FILTER_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(redisObjectConverter.serializeToString(filter))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/points_by_filter_response.json", false));

        verify(clientJedis).hmget(eq(getPointsAggHashTableName()), any());
        verify(clientJedis).hmget(eq(getLogisticsPointsHashTableName()), any());
        verify(clientJedis).hmget(eq(getPhonesHashTableName()), any());
        verify(clientJedis).hmget(eq(getScheduleHashTableName()), any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение логистических точек по фильтру: точки не найдены")
    void getPointsByFilterNoPointsFound() {
        LogisticsPointFilter filter = LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(1L))
            .active(true)
            .type(PointType.WAREHOUSE)
            .build();

        mockMvc.perform(
                post(GET_LOGISTICS_POINTS_BY_FILTER_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(redisObjectConverter.serializeToString(filter))
            )
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));

        verify(clientJedis).hmget(getPointsAggHashTableName(), "1:WAREHOUSE:true");
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение логистических точек с пустым фильтром")
    void getPointsByEmptyFilter() {
        LogisticsPointFilter filter = LogisticsPointFilter.newBuilder().build();

        softly.assertThatCode(
                () -> mockMvc.perform(
                    post(GET_LOGISTICS_POINTS_BY_FILTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(redisObjectConverter.serializeToString(filter))
                )
            )
            .isInstanceOf(NestedServletException.class)
            .hasMessage("Request processing failed; nested exception is java.lang.IllegalArgumentException: "
                + "Fields 'partnerIds', 'type' and 'active' in filter must not be null");

    }

    private void verifyGetLogisticsPoint() {
        verify(clientJedis).hget(getLogisticsPointsHashTableName(), LOGISTICS_POINT_ID);
    }

    @Nonnull
    private String getLogisticsPointsHashTableName() {
        return RedisKeys.getHashTableFromYtName(YtLogisticsPoint.class, RedisKeys.REDIS_DEFAULT_VERSION);
    }

    @Nonnull
    private String getPhonesHashTableName() {
        return RedisKeys.getHashTableFromYtName(YtPhoneByLogisticsPointId.class, RedisKeys.REDIS_DEFAULT_VERSION);
    }

    @Nonnull
    private String getScheduleHashTableName() {
        return RedisKeys.getHashTableFromYtName(YtScheduleDays.class, RedisKeys.REDIS_DEFAULT_VERSION);
    }

    @Nonnull
    private String getPointsAggHashTableName() {
        return RedisKeys.getHashTableFromYtName(YtLogisticsPointsAggModel.class, RedisKeys.REDIS_DEFAULT_VERSION);
    }
}
