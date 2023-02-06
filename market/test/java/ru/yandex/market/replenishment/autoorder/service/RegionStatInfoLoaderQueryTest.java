package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.replenishment.autoorder.config.yql.YqlQueryTest;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;
import ru.yandex.market.yql_query_service.service.QueryService;

import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = {
        TimeService.class,
        RegionStatInfoLoader.class
})
public class RegionStatInfoLoaderQueryTest extends YqlQueryTest {

    @Autowired
    QueryService queryService;

    @Autowired
    RegionStatInfoLoader regionStatInfoLoader;

    @Test
    public void testRegionStatInfoGetQuery() {
        String query = regionStatInfoLoader.getQuery(
                "//home/market/production/mstat/dictionaries/stock_sku/1d/2020-08-11",
                LocalDate.of(2020, 8, 11)
        );
        assertEquals(TestUtils.readResource("/queries/expected_region_stat_info.yt.sql"), query);
    }
}
