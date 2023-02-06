package ru.yandex.market.promoboss.postgres;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;

@DbUnitDataSet(
        before = "AuditTriggersTest.before.csv"
)
public class AuditTriggersTest extends AbstractDbUnitPostgresTest {
    @Test
    @DbUnitDataSet(
            before = "AuditTriggersTest.truncate.before.csv",
            after = "AuditTriggersTest.truncate.after.csv"
    )
    void truncate() {
        jdbcTemplate.execute("truncate promos cascade;");
    }

    @Test
    @DbUnitDataSet(
            after = "AuditTriggersTest.insert.after.csv"
    )
    void insert() {
        jdbcTemplate.execute("""
                insert into test_table_01(bigint_not_null,boolean_not_null,text_not_null)
                values(3, false, 'text3')
                """);
    }

    @Test
    @DbUnitDataSet(
            after = "AuditTriggersTest.update.after.csv"
    )
    void update() {
        jdbcTemplate.execute("""
                update test_table_01 set
                bigint_not_null = 11,
                bigint_null = 2,
                boolean_not_null = false,
                boolean_null = true,
                text_not_null = 'text011',
                text_null = 'text02'
                where bigint_not_null = 1
                """);
    }

    @Test
    @DbUnitDataSet(
            after = "AuditTriggersTest.update_same_values.after.csv"
    )
    void update_same_values() {
        jdbcTemplate.execute("""
                update test_table_01 set
                bigint_not_null = 1,
                bigint_null = null,
                boolean_not_null = true,
                boolean_null = null,
                text_not_null = 'text01',
                text_null = null
                where bigint_not_null = 1
                """);
    }

    @Test
    @DbUnitDataSet(
            after = "AuditTriggersTest.delete.after.csv"
    )
    void delete() {
        jdbcTemplate.execute("delete from test_table_01 where bigint_not_null = 1");
    }
}


