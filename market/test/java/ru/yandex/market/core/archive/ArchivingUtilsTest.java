package ru.yandex.market.core.archive;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Тесты для {@link ArchivingUtils}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class ArchivingUtilsTest extends ArchivingFunctionalTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Достаем null number")
    @DbUnitDataSet(before = "csv/ArchivingUtils.null_number.before.csv")
    void testNullNumber() {
        List<Object> actual = jdbcTemplate.query("select * from sch04.table03 where id = 1",
                (rs, c) -> ArchivingUtils.extractKeyValue(rs, 2));
        assertNull(actual.get(0));
    }

}
