package ru.yandex.market.wms.common.spring;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.wms.common.model.enums.DatabaseSchema;
import ru.yandex.market.wms.common.spring.service.CounterService;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;


public abstract class BaseIntegrationTest extends BaseTest {

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private CounterService counterService;

    @BeforeEach
    public void init() {
        runInNewTx(() -> {
            setupNCounter(DatabaseSchema.WMWHSE1);
            setupNCounter(DatabaseSchema.ENTERPRISE);
        });
        counterService.invalidateCache();
    }

    protected void runInNewTx(Runnable action) {
        new TransactionTemplate(txManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW))
                .executeWithoutResult(status -> action.run());
    }

    private void setupNCounter(DatabaseSchema schema) {
        jdbc.execute(String.format("delete from %s.NCOUNTER", schema.getName()));

        jdbc.execute(String.format("" +
                "insert into %s.NCOUNTER (KEYNAME, KEYCOUNT, EDITDATE) values " +
                "  ('CARTONID', 100, '2020-01-01 00:00:00')," +
                "  ('TASKDETAILKEY', 200, '2020-01-01 00:00:00')," +
                "  ('ITRNKEY', 300, '2020-01-01 00:00:00')," +
                "  ('ITRNSERIALKEY', 400, '2020-01-01 00:00:00')," +
                "  ('RPT_YMCRT02', 500, '2020-01-01 00:00:00')," +
                "  ('PICKDETAILKEY', 600, '2020-01-01 00:00:00')," +
                "  ('LOT', 700, '2020-01-01 00:00:00')," +
                "  ('SERIALTRANSKEY', 800, '2020-01-01 00:00:00')," +
                "  ('LOTXIDHEADER', 900, '2020-01-01 00:00:00')," +
                "  ('INVENTORYHOLDKEY', 1000, '2020-01-01 00:00:00')," +
                "  ('HOLDTRNKEY', 1000, '2020-01-01 00:00:00')," +
                "  ('HOLDTRNGROUP', 1000, '2020-01-01 00:00:00')," +
                "  ('TRAILERKEY', 1100, '2020-01-01 00:00:00')," +
                "  ('ORDER', 39465, '2020-01-01 00:00:00')," +
                "  ('ANOMALY_CONTAINER', 1, '2020-01-01 00:00:00')," +
                "  ('LOADPLANNING', 39465, '2020-01-01 00:00:00')," +
                "  ('PALLETID', 1, '2020-01-01 00:00:00')," +
                "  ('UNIT_ID_LINK_ID', 7100, '2020-01-01 00:00:00')", schema.getName()));
    }
}
