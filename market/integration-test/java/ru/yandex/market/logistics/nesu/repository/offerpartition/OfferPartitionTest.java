package ru.yandex.market.logistics.nesu.repository.offerpartition;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

class OfferPartitionTest extends AbstractContextualTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    private void dropOfferTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS offer_partition.offer_1");
    }

    /**
     * Проверяем, что изначально в таблице нет секционированной таблицы.
     */
    @Test
    @DisplayName("Проверка первоначальной конфигурации")
    void getNonExistTable() {
        softly.assertThatThrownBy(() -> jdbcTemplate.execute("SELECT * FROM offer_partition.offer_1"))
            .isInstanceOf(BadSqlGrammarException.class)
            .hasMessage("StatementCallback; bad SQL grammar [SELECT * FROM offer_partition.offer_1];" +
                " nested exception is org.postgresql.util.PSQLException:" +
                " ERROR: relation \"offer_partition.offer_1\" does not exist\n" +
                "  Position: 15");
    }

    /**
     * Проверяем, что при добавлении нового сендера, создаётся пустая секционированная таблица.
     */
    @Test
    @DisplayName("Проверка создания таблицы")
    @DatabaseSetup("/repository/offer-partition/before/before.xml")
    void checkThatTableIsCreated() {
        jdbcTemplate.execute("INSERT INTO sender values (1, now(), 1, 'name', 1, null, 'ACTIVE')");

        softly.assertThatCode(() -> jdbcTemplate.execute("SELECT * FROM offer_partition.offer_1"))
            .doesNotThrowAnyException();

        checkNumberOfRecords(0);
    }

    /**
     * Проверяем, что при добавлении нового оффера в общую таблицу, запись также добавлется в секционированную таблицу.
     */
    @Test
    @DisplayName("Проверка добавления значений в новую таблицу")
    @DatabaseSetup("/repository/offer-partition/before/before.xml")
    void checkOfferInsert() {
        jdbcTemplate.execute("INSERT INTO sender values (1, now(), 1, 'name', 1, null, 'ACTIVE')");
        jdbcTemplate.execute("INSERT INTO feed values (1, now(), now(), 1, 1, 'ACTIVE', null)");

        jdbcTemplate.execute("INSERT INTO offer values (1, 1, 1, now(), 'external_id', 'name', null, 1, '1', null)");

        checkNumberOfRecords(1);
    }

    private void checkNumberOfRecords(int expectedNumberOfRecords) {
        Integer actualNumberOfRecords =
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM offer_partition.offer_1", Integer.class);

        softly.assertThat(actualNumberOfRecords).isNotNull();
        softly.assertThat(actualNumberOfRecords).isEqualTo(expectedNumberOfRecords);
    }

}
