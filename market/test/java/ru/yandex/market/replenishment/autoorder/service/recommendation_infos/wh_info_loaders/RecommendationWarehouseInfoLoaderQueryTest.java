package ru.yandex.market.replenishment.autoorder.service.recommendation_infos.wh_info_loaders;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.replenishment.autoorder.config.yql.YqlQueryTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.YtTableWatchLog;
import ru.yandex.market.replenishment.autoorder.service.TimeService;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.wh_info.RecommendationWarehouseInfoByDemandLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.wh_info.RecommendationWarehouseInfoLoader1p;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static ru.yandex.market.replenishment.autoorder.model.entity.postgres.Environment.YT_POOL;

@ContextConfiguration(classes = {
    RecommendationWarehouseInfoLoader1p.class
})
public class RecommendationWarehouseInfoLoaderQueryTest extends YqlQueryTest {

    @Autowired
    EnvironmentService environmentService;

    @Autowired
    private TimeService timeService;

    @Autowired
    private RecommendationWarehouseInfoByDemandLoader recommendationWarehouseInfoLoader;

    @Test
    public void testQuery() {
        when(environmentService.getStringWithDefault(YT_POOL, "default")).thenReturn("default");
        TestUtils.mockTimeService(timeService, LocalDateTime.of(2021, 9, 17, 0, 0, 0));
        YtTableWatchLog event = new YtTableWatchLog();
        event.setTablePath("//home/market/production/replenishment/order_planning/2021-09-16/outputs/recommendations");

        assertEquals(
            TestUtils.readResource("/queries/expected_recommendation_warehouse_infos.yt.sql"),
            recommendationWarehouseInfoLoader.getQuery(event));
    }
}
