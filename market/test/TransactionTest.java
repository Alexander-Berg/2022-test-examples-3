package ru.yandex.market.jmf.logic.def.test;

import java.sql.ResultSet;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.tx.TxService;

/**
 * @see ru.yandex.market.jmf.db.impl.JmfJdbcTemplate
 */
@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
public class TransactionTest {

    @Inject
    TxService txService;
    @Inject
    JdbcTemplate jdbcTemplate;
    @Inject
    DbService dbService;

    @Test
    public void checkTx() {
        txService.runInNewTx(() -> {
            jdbcTemplate.query("select * from tbl_entitySimple", (ResultSet rs, int rowNum) -> null);
            dbService.createQuery("from entitySimple").list();
        });
    }

    @Test
    public void checkReadOnlyTx() {
        txService.runInReadOnlyTx(() -> {
            jdbcTemplate.query("select * from tbl_entitySimple", (ResultSet rs, int rowNum) -> null);
            dbService.createQuery("from entitySimple").list();
        });
    }

    @Test
    public void checkReadOnlyManyTx() {
        txService.runInReadOnlyTx(() -> {
            jdbcTemplate.query("select * from tbl_entitySimple", (ResultSet rs, int rowNum) -> null);
            dbService.createQuery("from entitySimple").list();
        });
        txService.runInReadOnlyTx(() -> {
            jdbcTemplate.query("select * from tbl_entitySimple", (ResultSet rs, int rowNum) -> null);
            dbService.createQuery("from entitySimple").list();
        });
    }

    @Test
    public void checkReadOnlyManyQueryTx() {
        txService.runInReadOnlyTx(() -> {
            jdbcTemplate.query("select * from tbl_entitySimple", (ResultSet rs, int rowNum) -> null);
            dbService.createQuery("from entitySimple").list();
            jdbcTemplate.query("select * from tbl_entitySimple", (ResultSet rs, int rowNum) -> null);
            dbService.createQuery("from entitySimple").list();
        });
    }

    @Test
    public void checkReadOnlyNoBdTx() {
        txService.runInReadOnlyTx(() ->
                jdbcTemplate.query("select * from tbl_entitySimple", (ResultSet rs, int rowNum) -> null)
        );
    }
}
