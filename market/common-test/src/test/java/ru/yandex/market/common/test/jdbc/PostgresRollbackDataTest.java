package ru.yandex.market.common.test.jdbc;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTruncatePolicy;
import ru.yandex.market.common.test.db.TruncateType;

/**
 * Тест проверяет работу аннотации {@link DbUnitTruncatePolicy} в симбиозе с {@link Transactional}.
 */
@Transactional
@DbUnitTruncatePolicy(truncateType = TruncateType.NOT_TRUNCATE)
public class PostgresRollbackDataTest extends BasePostgresTest {

    @Test
    @DbUnitDataSet(after = "PostgresRollbackDataTest.test1.after.csv")
    public void test1() {
        jdbcTemplate.update("insert into common_test.numbers values (1)");

        List<Integer> values = jdbcTemplate.queryForList("select number from common_test.numbers", Integer.class);
        Assertions.assertThat(values).containsExactlyInAnyOrder(1);
    }

    @Test
    @DbUnitDataSet(after = "PostgresRollbackDataTest.test2.after.csv")
    public void test2() {
        jdbcTemplate.update("insert into common_test.numbers values (2)");

        List<Integer> values = jdbcTemplate.queryForList("select number from common_test.numbers", Integer.class);
        Assertions.assertThat(values).containsExactlyInAnyOrder(2);
    }
}
