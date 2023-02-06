package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.replenishment.autoorder.config.yql.YqlQueryTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.YtTableWatchLog;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.not_grouped_recommendations.NotGrouped1PLoader;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;
import ru.yandex.market.yql_query_service.service.QueryService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        TimeService.class,
        NotGrouped1PLoader.class
})
public class ReplenishmentQueryTest extends YqlQueryTest {

    @Autowired
    QueryService query;

    @Autowired
    TimeService timeService;

    @Autowired
    EnvironmentService environmentService;

    @Autowired
    NotGrouped1PLoader loader;

    @Test
    public void testNotGroupedQuery() {
        when(timeService.getNowDateTime()).thenReturn(LocalDateTime.of(2020, 8, 4, 0, 0));
        Mockito.when(environmentService.getStringWithDefault(Mockito.any(), Mockito.any())).thenReturn("default");

        YtTableWatchLog ytTableWatchLog = new YtTableWatchLog();
        ytTableWatchLog.setTablePath("//home/market/production/replenishment/order_planning/2020-08-04/outputs/recommendations");
        String query = loader.getQuery(ytTableWatchLog);
        assertEquals(TestUtils.readResource("/queries/expected_not_grouped_recommendations.yt.sql"), query);
    }
}
