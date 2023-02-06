package ru.yandex.market.common.test.jdbc;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitRefreshMatViews;

/**
 * Тест проверяет работу аннотации {@link ru.yandex.market.common.test.db.DbUnitRefreshMatViews}.
 * В классе 2 теста, которые должны проверять, что материализованные вьюхи будут отрефрешнуты перед тестом.
 */
public class PostgresRefreshMatviewTest extends BasePostgresTest {

    @Before
    public void setUp() {
        // assert mat view is empty before each test
        List<String> values = jdbcTemplate.queryForList("select number_letter from common_test.number_letters", String.class);
        Assertions.assertThat(values).isEmpty();
    }

    @After
    public void tearDown() {
        // manually truncate tables. Manually, because we want to test DbUnitRefreshMatViews in isolation
        jdbcTemplate.execute("truncate common_test.numbers, common_test.letters");
    }

    @Test
    @DbUnitRefreshMatViews
    public void test1() {
        jdbcTemplate.update("insert into common_test.numbers values (1)");
        jdbcTemplate.update("insert into common_test.letters values ('a')");

        jdbcTemplate.execute("refresh materialized view common_test.number_letters;");

        List<String> values = jdbcTemplate.queryForList("select number_letter from common_test.number_letters", String.class);
        Assertions.assertThat(values).containsExactlyInAnyOrder("1a");
    }

    @Test
    @DbUnitRefreshMatViews
    public void test2() {
        jdbcTemplate.update("insert into common_test.numbers values (2)");
        jdbcTemplate.update("insert into common_test.letters values ('b')");

        jdbcTemplate.execute("refresh materialized view common_test.number_letters;");

        List<String> values = jdbcTemplate.queryForList("select number_letter from common_test.number_letters", String.class);
        Assertions.assertThat(values).containsExactlyInAnyOrder("2b");
    }
}
