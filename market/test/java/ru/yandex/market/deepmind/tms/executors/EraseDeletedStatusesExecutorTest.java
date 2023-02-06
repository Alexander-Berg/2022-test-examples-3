package ru.yandex.market.deepmind.tms.executors;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.db.monitoring.DbMonitoring;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusDeletedRepository;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.tms.executors.EraseDeletedStatusesExecutor.DAYS_TO_WAIT_BEFORE_ERASURE;
import static ru.yandex.market.deepmind.tms.executors.EraseDeletedStatusesExecutor.MonitoringExecutor.MINUTES_TO_WAIT;
import static ru.yandex.market.deepmind.tms.executors.EraseDeletedStatusesExecutor.TABLE_NAME;

public class EraseDeletedStatusesExecutorTest extends DeepmindBaseDbTestClass {
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private SskuStatusDeletedRepository sskuStatusDeletedRepository;
    @Autowired
    private DbMonitoring deepmindDbMonitoring;

    private EraseDeletedStatusesExecutor executor;
    private EraseDeletedStatusesExecutor.MonitoringExecutor monitoringExecutor;

    @Before
    public void setUp() throws Exception {
        executor = new EraseDeletedStatusesExecutor(
            namedParameterJdbcTemplate, sskuStatusDeletedRepository
        );

        monitoringExecutor = new EraseDeletedStatusesExecutor.MonitoringExecutor(
            deepmindDbMonitoring.getOrCreateUnit("unit"),
            sskuStatusDeletedRepository
        );
    }

    @Test
    public void newDeletedStatusRemainUntouched() {
        //arrange
        var newKey = new ServiceOfferKey(1, "1");
        sskuStatusDeletedRepository.addByKeys(newKey);

        //act
        executor.execute();

        //assert
        assertThat(sskuStatusDeletedRepository.findAllKeys())
            .containsExactly(newKey);
    }

    @Test
    public void oldDeletedStatusAreBeingErased() {
        //arrange
        var oldKey = new ServiceOfferKey(1, "1");
        sskuStatusDeletedRepository.addByKeys(oldKey);

        ageStatuses();

        var newKey = new ServiceOfferKey(2, "2");
        sskuStatusDeletedRepository.addByKeys(newKey);

        //act
        executor.execute();

        //assert
        assertThat(sskuStatusDeletedRepository.findAllKeys())
            .doesNotContain(oldKey)
            .containsExactly(newKey);
    }

    private void ageStatuses() {
        namedParameterJdbcTemplate.update(
            format(
                "update %s set deleted_ts = :past_ts", TABLE_NAME
            ),
            new MapSqlParameterSource()
                .addValue(
                    "past_ts",
                    Timestamp.from(Instant.now().minus(DAYS_TO_WAIT_BEFORE_ERASURE + 1, ChronoUnit.DAYS))
                )
        );
    }

    @Test
    public void notDeletedMonitoringDoesNotWarnAheadOfTime() {
        //arrange
        var key = new ServiceOfferKey(1, "1");
        sskuStatusDeletedRepository.addByKeys(key);

        //act
        executeWithNowTime(Instant.now().plus(1, ChronoUnit.MINUTES));

        //assert
        assertThat(deepmindDbMonitoring.fetchTotalResult().getStatus())
            .isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void notDeletedMonitoringWarnInTime() {
        //arrange
        var key1 = new ServiceOfferKey(1, "1");
        var key2 = new ServiceOfferKey(2, "2");
        sskuStatusDeletedRepository.addByKeys(key1, key2);

        //act
        executeWithNowTime(Instant.now()
            .plus(DAYS_TO_WAIT_BEFORE_ERASURE, ChronoUnit.DAYS)
            .plus(MINUTES_TO_WAIT + 5, ChronoUnit.MINUTES)
        );

        //assert
        assertThat(deepmindDbMonitoring.fetchTotalResult().getStatus()).isEqualTo(MonitoringStatus.WARNING);
        assertThat(deepmindDbMonitoring.fetchTotalResult().getMessage())
            .contains("deleted ssku_statuses not erased in 45 mins (supplier_id, ssku): (1,'1'),(2,'2')");
    }

    @Test
    public void notDeletedReturnsOnly3Examples() {
        //arrange
        var key1 = new ServiceOfferKey(1, "1");
        var key2 = new ServiceOfferKey(2, "2");
        var key3 = new ServiceOfferKey(3, "3");
        var key4 = new ServiceOfferKey(4, "4");
        sskuStatusDeletedRepository.addByKeys(key1, key2, key3, key4);

        //act
        executeWithNowTime(Instant.now()
            .plus(DAYS_TO_WAIT_BEFORE_ERASURE, ChronoUnit.DAYS)
            .plus(MINUTES_TO_WAIT + 5, ChronoUnit.MINUTES)
        );

        //assert
        assertThat(deepmindDbMonitoring.fetchTotalResult().getStatus()).isEqualTo(MonitoringStatus.WARNING);
        assertThat(deepmindDbMonitoring.fetchTotalResult().getMessage())
            .contains("deleted ssku_statuses not erased in 45 mins (supplier_id, ssku): (1,'1'),(2,'2'),(3,'3')...+1");
    }

    private void executeWithNowTime(Instant checkTime) {
        try {
            monitoringExecutor.setClock(Clock.fixed(checkTime, ZoneOffset.UTC));
            monitoringExecutor.execute();
        } finally {
            monitoringExecutor.setClock(Clock.systemDefaultZone());
        }
    }
}
