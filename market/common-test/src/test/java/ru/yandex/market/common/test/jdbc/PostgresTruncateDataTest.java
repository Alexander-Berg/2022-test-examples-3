package ru.yandex.market.common.test.jdbc;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitTruncatePolicy;
import ru.yandex.market.common.test.db.TruncateType;

/**
 * Тест проверяет работу аннотации {@link DbUnitTruncatePolicy}.
 */
@DbUnitTruncatePolicy(truncateType = TruncateType.TRUNCATE)
public class PostgresTruncateDataTest extends BasePostgresTest {
    private static boolean PREPARE_DATA = false;

    @BeforeClass
    public static void beforeClass() {
        PREPARE_DATA = true;
    }

    @Before
    public void setUp() {
        // Если это первый запуск теста, то очищаем данные
        if (PREPARE_DATA) {
            jdbcTemplate.execute("truncate common_test.numbers, common_test.letters");
            PREPARE_DATA = false;
        } else {
            // если это второй и более запуск, то проверяем, что с предыдущего запуска остались значения
            List<Integer> values = jdbcTemplate.queryForList("select number from common_test.numbers", Integer.class);
            Assertions.assertThat(values).isEmpty();
        }
    }

    @Test
    public void test1() {
        jdbcTemplate.update("insert into common_test.numbers values (1)");

        List<Integer> values = jdbcTemplate.queryForList("select number from common_test.numbers", Integer.class);
        Assertions.assertThat(values).isNotEmpty();
    }

    @Test
    public void test2() {
        jdbcTemplate.update("insert into common_test.numbers values (2)");

        List<Integer> values = jdbcTemplate.queryForList("select number from common_test.numbers", Integer.class);
        Assertions.assertThat(values).isNotEmpty();
    }
}
