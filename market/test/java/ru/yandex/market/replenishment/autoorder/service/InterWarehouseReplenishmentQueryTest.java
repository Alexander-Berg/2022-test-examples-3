package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.replenishment.autoorder.config.yql.YqlQueryTest;
import ru.yandex.market.replenishment.autoorder.repository.postgres.InterWarehouseReplenishmentRepository;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.InterWarehouseReplenishmentQueryLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.YtResultSetSaver;
import ru.yandex.market.replenishment.autoorder.service.yt.watchable.YtInterWarehouseReplenishmentWatchable;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;
import ru.yandex.market.yql_query_service.service.QueryService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
    TimeService.class,
})
public class InterWarehouseReplenishmentQueryTest extends YqlQueryTest {

    @Qualifier("yqlJdbcTemplate")
    @Autowired
    private JdbcTemplate yqlJdbcTemplate;
    @Autowired
    private YtResultSetSaver ytResultSetSaver;
    @Autowired
    private QueryService queryService;
    @Autowired
    private TimeService timeService;
    @Autowired
    private YtInterWarehouseReplenishmentWatchable watchable;
    @Autowired
    private DbYtTableWatchLogService watchLogService;

    @Test
    public void testReplenishmentQuery() {
        when(timeService.getNowDate()).thenReturn(LocalDate.of(2020, 11, 23));

        final InterWarehouseReplenishmentQueryLoader interWarehouseReplenishmentQueryLoader =
            new InterWarehouseReplenishmentQueryLoader(
                yqlJdbcTemplate,
                ytResultSetSaver,
                queryService,
                timeService,
                watchable,
                watchLogService,
                Mockito.mock(InterWarehouseReplenishmentRepository.class)
            );

        String query = interWarehouseReplenishmentQueryLoader.getQuery(
            "//home/market/production/replenishment/order_planning/2020-11-23/intermediate/inter_wh_movements"
        );

        assertEquals(TestUtils.readResource("/queries/expected_old_interwarehouse_replenishments.yt.sql"), query);
    }
}
