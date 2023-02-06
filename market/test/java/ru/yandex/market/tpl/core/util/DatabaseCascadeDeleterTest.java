package ru.yandex.market.tpl.core.util;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.service.DatabaseCascadeDeleter;

import static org.junit.jupiter.api.Assertions.assertEquals;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class DatabaseCascadeDeleterTest {
    private final DatabaseCascadeDeleter databaseCascadeDeleter;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("CREATE TABLE table_z (id bigint primary key)");
        jdbcTemplate.update("CREATE TABLE table_a (id bigint primary key, city text, t_z bigint references " +
                "table_z(id))");
        jdbcTemplate.update("CREATE TABLE table_b (id bigint primary key, t_a bigint references table_a(id) not null)");
        jdbcTemplate.update("CREATE TABLE table_c (id bigint primary key, t_b bigint references table_b(id) not null)");
        jdbcTemplate.update("INSERT INTO table_z (id) VALUES (1)");
        jdbcTemplate.update("INSERT INTO table_a (id, city, t_z) VALUES (1, 'Moscow', 1), (2, 'Ufa', 1)");
        jdbcTemplate.update("INSERT INTO table_b (id, t_a) VALUES (1, 1), (2, 2)");
        jdbcTemplate.update("INSERT INTO table_c (id, t_b) VALUES (1, 1), (2, 2)");
    }

    @Test
    void delete() {
        assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM table_z", Long.class));
        assertEquals(2, jdbcTemplate.queryForObject("SELECT count(*) FROM table_a", Long.class));
        assertEquals(2, jdbcTemplate.queryForObject("SELECT count(*) FROM table_b", Long.class));
        assertEquals(2, jdbcTemplate.queryForObject("SELECT count(*) FROM table_c", Long.class));

        Map<String, List<Object>> resultMap = databaseCascadeDeleter.delete("table_a", "city", List.of("Moscow"));

        assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM table_z", Long.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM table_a", Long.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM table_b", Long.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM table_c", Long.class));
        var expectedResult = Map.of("table_a", List.of(1L), "table_b", List.of(1L), "table_c", List.of(1L));
        assertEquals(expectedResult, resultMap);
    }
}
