package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.replenishment.autoorder.config.yql.YqlQueryTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Environment;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.YtTableWatchLog;
import ru.yandex.market.replenishment.autoorder.repository.postgres.EnvironmentRepository;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.region_info.RecommendationRegionInfoLoader1p;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;
import ru.yandex.market.yql_query_service.service.QueryService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static ru.yandex.market.replenishment.autoorder.model.entity.postgres.Environment.YT_POOL;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
        RecommendationRegionInfoLoader1p.class,
        EnvironmentService.class
})
@ActiveProfiles("unittest")
@TestPropertySource(locations = "classpath:functional-test.properties")
public class RecommendationRegionInfo1pLoaderQueryTest extends YqlQueryTest {

    @Autowired
    QueryService queryService;

    @Autowired
    RecommendationRegionInfoLoader1p loader;

    @Autowired
    EnvironmentRepository environmentRepository;

    @Autowired
    TimeService timeService;

    @Autowired
    EnvironmentService environmentService;

    @Test
    public void testRegionStatInfoGetQuery() {
        when(timeService.getNowDate()).thenReturn(LocalDate.of(2021, 9, 17));
        Environment environment = new Environment();
        environment.setValue("default");
        when(environmentRepository.findById(YT_POOL)).thenReturn(Optional.of(environment));
        when(environmentService.getStringWithDefault(Mockito.anyString(), Mockito.anyString())).thenReturn("default");
        YtTableWatchLog event = new YtTableWatchLog();
        event.setTablePath("//home/market/production/replenishment/order_planning/2021-09-16/outputs/recommendations");
        String query = loader.getQuery(event);
        assertEquals(TestUtils.readResource("/queries/expected_recommendation_region_infos.yt.sql"), query);
    }
}
