package ru.yandex.market.abo.core.archive;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;

/**
 * @author artemmz
 * @date 19.07.17.
 */
public class ArchiveCoreDataTest extends EmptyTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void archiveCoreData() {
        jdbcTemplate.queryForObject("SELECT archive_core_data()", Object.class);
    }
}
