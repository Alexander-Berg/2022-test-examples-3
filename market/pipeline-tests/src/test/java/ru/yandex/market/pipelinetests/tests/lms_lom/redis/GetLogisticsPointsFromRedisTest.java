package ru.yandex.market.pipelinetests.tests.lms_lom.redis;

import java.util.List;

import io.qameta.allure.Epic;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.pipelinetests.tests.lms_lom.AbstractGetLogisticsPointTest;
import ru.yandex.market.pipelinetests.tests.lms_lom.utils.LogisticsPointCompareUtils;

@Epic("Lms Lom Redis")
@Tag("LmsLomRedisSyncTest")
@DisplayName("Синхронизация данных LMS в redis")
public class GetLogisticsPointsFromRedisTest extends AbstractGetLogisticsPointTest {

    @Test
    @Override
    @SneakyThrows
    @DisplayName("Получение логистической точки по идентификатору: заполнены все поля")
    public void getLogisticsPointAllFieldsFilled() {
        LogisticsPointResponse lmsLogisticsPoint = LMS_STEPS.getLogisticsPoint(FILLED_LOGISTICS_POINT_ID);
        LogisticsPointResponse redisLogisticsPoint =
            LOM_REDIS_STEPS.getLogisticsPointFromRedis(FILLED_LOGISTICS_POINT_ID);
        LogisticsPointCompareUtils.comparePoints(softly, lmsLogisticsPoint, redisLogisticsPoint);
    }

    @Test
    @Override
    @SneakyThrows
    @DisplayName("Получение логистической точки по идентификатору: заполнены только обязательные поля")
    public void getLogisticsPointWithRequiredFieldsOnly() {
        LogisticsPointResponse lmsLogisticsPoint = LMS_STEPS.getLogisticsPoint(EMPTY_LOGISTICS_POINT_ID);
        LogisticsPointResponse redisLogisticsPoint =
            LOM_REDIS_STEPS.getLogisticsPointFromRedis(EMPTY_LOGISTICS_POINT_ID);
        LogisticsPointCompareUtils.comparePoints(softly, lmsLogisticsPoint, redisLogisticsPoint);
    }

    @Test
    @Override
    @DisplayName("Поиск точек по фильтру с id точек")
    @SneakyThrows
    public void getLogisticPointsByFilterWithIds() {
        List<LogisticsPointResponse> lmsPoints = LMS_STEPS.getLogisticsPoints(LOGISTICS_POINTS_IDS_FILTER);
        List<LogisticsPointResponse> redisPoints =
            LOM_REDIS_STEPS.getLogisticsPointsFromRedis(LOGISTICS_POINTS_IDS_FILTER);
        LogisticsPointCompareUtils.comparePointLists(softly, lmsPoints, redisPoints);
    }

    @Test
    @Override
    @DisplayName("Поиск точек по фильтру с id партнеров")
    @SneakyThrows
    public void getLogisticPointsByFilterWithPartnerIds() {
        List<LogisticsPointResponse> lmsPoints = LMS_STEPS.getLogisticsPoints(LOGISTICS_POINTS_PARTNER_IDS_FILTER);
        List<LogisticsPointResponse> redisPoints =
            LOM_REDIS_STEPS.getLogisticsPointsFromRedis(LOGISTICS_POINTS_PARTNER_IDS_FILTER);
        LogisticsPointCompareUtils.comparePointLists(softly, lmsPoints, redisPoints);
    }
}
