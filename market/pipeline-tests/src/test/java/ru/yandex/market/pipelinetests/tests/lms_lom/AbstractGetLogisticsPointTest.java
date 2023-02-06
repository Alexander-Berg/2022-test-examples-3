package ru.yandex.market.pipelinetests.tests.lms_lom;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.pipelinetests.tests.lms_lom.utils.LogisticsPointCompareUtils;

import static toolkit.FileUtil.bodyStringFromFile;
import static toolkit.Mapper.mapLmsResponse;

@Epic("Lms Lom Redis")
@Tag("LmsLomRedisSyncTest")
public abstract class AbstractGetLogisticsPointTest extends AbstractLmsLomTest {

    private static final LogisticsPointResponse EXPECTED_LMS_FILLED_POINT = mapLmsResponse(
        bodyStringFromFile("lms_lom_redis/lms_response/logistics_point/filled.json"),
        LogisticsPointResponse.class
    );

    private static final LogisticsPointResponse EXPECTED_LMS_EMPTY_POINT = mapLmsResponse(
        bodyStringFromFile("lms_lom_redis/lms_response/logistics_point/empty.json"),
        LogisticsPointResponse.class
    );

    private static final LogisticsPointResponse EXPECTED_LMS_EMPTY_SCHEDULE_POINT = mapLmsResponse(
        bodyStringFromFile("lms_lom_redis/lms_response/logistics_point/empty_schedule.json"),
        LogisticsPointResponse.class
    );

    private static final LogisticsPointResponse EXPECTED_LMS_FILLED_LOGISTICS_POINT = mapLmsResponse(
        bodyStringFromFile("lms_lom_redis/lms_response/logistics_point/filled.json"),
        LogisticsPointResponse.class
    );
    private static final LogisticsPointResponse EXPECTED_LMS_EMPTY_LOGISTICS_POINT = mapLmsResponse(
        bodyStringFromFile("lms_lom_redis/lms_response/logistics_point/empty.json"),
        LogisticsPointResponse.class
    );

    @Test
    @DisplayName("Логистическая точка со всеми заполненными данными в лмс соответствует ожидаемым данным")
    void checkPreConditionsForFilledLogisticsPoint() {
        LogisticsPointResponse lmsLogisticsPoint = LMS_STEPS.getLogisticsPoint(FILLED_LOGISTICS_POINT_ID);
        LogisticsPointCompareUtils.comparePoints(softly, EXPECTED_LMS_FILLED_LOGISTICS_POINT, lmsLogisticsPoint);
    }

    @Test
    @DisplayName("Логистическая точка с заполненными только обязательными данными в лмс соответствует ожидаемым данным")
    void checkPreConditionsForRequiredFieldsOnlyLogisticsPoint() {
        LogisticsPointResponse lmsLogisticsPoint = LMS_STEPS.getLogisticsPoint(EMPTY_LOGISTICS_POINT_ID);
        LogisticsPointCompareUtils.comparePoints(softly, EXPECTED_LMS_EMPTY_POINT, lmsLogisticsPoint);
    }

    @Test
    @DisplayName("Список логистических точек в лмс соответствует ожидаемым данным")
    void checkPreConditionForLogisticsPointList() {
        LogisticsPointFilter filter = LogisticsPointFilter.newBuilder()
            .ids(Set.of(FILLED_LOGISTICS_POINT_ID, EMPTY_LOGISTICS_POINT_ID, LOGISTICS_POINT_WITH_EMPTY_SCHEDULE_ID))
            .build();

        List<LogisticsPointResponse> lmsPoints = LMS_STEPS.getLogisticsPoints(filter);
        LogisticsPointCompareUtils.comparePointLists(
            softly,
            new ArrayList<>(List.of(
                EXPECTED_LMS_FILLED_POINT,
                EXPECTED_LMS_EMPTY_POINT,
                EXPECTED_LMS_EMPTY_SCHEDULE_POINT
            )),
            lmsPoints
        );
    }

    public abstract void getLogisticsPointAllFieldsFilled();

    public abstract void getLogisticsPointWithRequiredFieldsOnly();

    public abstract void getLogisticPointsByFilterWithIds();

    public abstract void getLogisticPointsByFilterWithPartnerIds();
}
