package ru.yandex.market.common.test.jdbc;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;

public class PostgresTest extends BasePostgresTest {

    @Test
    @DbUnitDataSet(before = "PostgresTest.simple.csv")
    public void testBeforeData() {
        List<Integer> numbers = jdbcTemplate.queryForList("select number from common_test.numbers", Integer.class);
        Assertions.assertThat(numbers).containsExactlyInAnyOrder(1, 2);

        List<String> letters = jdbcTemplate.queryForList("select letter from common_test.letters", String.class);
        Assertions.assertThat(letters).containsExactlyInAnyOrder("A", "a");
    }

    @Test
    @DbUnitDataSet(after = "PostgresTest.simple.csv")
    public void testAfterData() {
        jdbcTemplate.update("insert into common_test.numbers (number) values (1)");
        jdbcTemplate.update("insert into common_test.numbers (number) values (2)");

        jdbcTemplate.update("insert into common_test.letters (letter) values ('A')");
        jdbcTemplate.update("insert into common_test.letters (letter) values ('a')");
    }

    @Test
    @DbUnitDataSet(after = "PostgresTest.new_table.csv")
    public void testCreateTableAfter() {
        jdbcTemplate.execute("create table common_test.new_table (number int not null)");
        jdbcTemplate.update("insert into common_test.new_table (number) values (1)");
        jdbcTemplate.update("insert into common_test.new_table (number) values (2)");
    }

    /**
     * Тест такой же как предыдущий, только на этот раз есть before.
     */
    @Test
    @DbUnitDataSet(before = "PostgresTest.simple.csv", after = "PostgresTest.new_table2.csv")
    public void testCreateTableAfter2() {
        jdbcTemplate.execute("create table common_test.new_table2 (number int not null)");
        jdbcTemplate.update("insert into common_test.new_table2 (number) values (1)");
        jdbcTemplate.update("insert into common_test.new_table2 (number) values (2)");
    }
}
