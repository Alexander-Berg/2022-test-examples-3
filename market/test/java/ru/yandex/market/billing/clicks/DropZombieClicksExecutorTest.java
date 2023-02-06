package ru.yandex.market.billing.clicks;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.msapi.logbroker.ChunkStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Тесты для {@link DropZombieClicksExecutor}
 */
@ParametersAreNonnullByDefault
public class DropZombieClicksExecutorTest extends FunctionalTest {

    private DropZombieClicksExecutor executor;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    void setUp() {
        executor = new DropZombieClicksExecutor(
                namedParameterJdbcTemplate,
                new ChunkMetadataRepository(
                        "wuser.chunks_state",
                        "wuser.stashed_records",
                        "wuser.s_chunks_state",
                        namedParameterJdbcTemplate
                ),
                environmentService,
                Clock.fixed(
                        DateTimes.toInstantAtDefaultTz(2021, 4, 8, 10, 0, 0),
                        ZoneId.systemDefault()
                )
        );
    }

    @Test
    @DbUnitDataSet(before = "DropZombieClicksExecutor.before.csv")
    void testExecutor() {
        environmentService.setValue("mbi.billing.do.run.drop.zombie.clicks", "true");
        executor.doJob(null);

        List<Long> savedTransactions = namedParameterJdbcTemplate.queryForList(""+
                        "select trans_id " +
                        "from wuser.chunks_state " +
                        "where status = :suspendedStatus",
                new MapSqlParameterSource("suspendedStatus", ChunkStatus.SUSPENDED.name()),
                Long.class
        );
        assertThat(savedTransactions, containsInAnyOrder(1L, 2L, 9L));
    }
}
