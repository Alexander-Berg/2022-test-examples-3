package ru.yandex.market.common.test.jdbc;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;

public class PostgresEnumTest extends BasePostgresTest {

    @Test
    @DbUnitDataSet(before = "PostgesEnumTest.before.csv")
    public void test() {
        List<String> numberTypes = jdbcTemplate.queryForList("select number_type from common_test.numbers", String.class);
        Assertions.assertThat(numberTypes).containsExactlyInAnyOrder("INT", "DOUBLE");

        List<String> letterTypes = jdbcTemplate.queryForList("select letter_type from common_test.letters", String.class);
        Assertions.assertThat(letterTypes).containsExactlyInAnyOrder("CAPITAL", "LOWERCASE");
    }
}
