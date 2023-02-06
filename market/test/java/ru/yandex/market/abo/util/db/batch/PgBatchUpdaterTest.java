package ru.yandex.market.abo.util.db.batch;

import java.util.List;

import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.abo.util.db.batch.EntityPreparedStatementSetter.longSetter;
import static ru.yandex.market.abo.util.db.batch.EntityPreparedStatementSetter.stringSetter;

/**
 * @author komarovns
 */
class PgBatchUpdaterTest extends EmptyTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void init() {
        jdbcTemplate.update("CREATE TABLE updater_test (id bigint PRIMARY KEY, val text)");
    }

    @ParameterizedTest
    @CsvSource({"true, 22", "false, 2"})
    void testBatchUpdater(boolean updateOnConflict, String conflictEntityValue) {
        var updater = new PgBatchUpdater<>(jdbcTemplate, PgBatchUpdaterConfig.<Entity>builder()
                .tableName("updater_test")
                .keyColumns("id")
                .column("id", longSetter(e -> e.id))
                .column("val", stringSetter(e -> e.value))
                .build()
        );

        jdbcTemplate.update("" +
                "INSERT INTO updater_test (id, val) " +
                "VALUES (1, '1')," +
                "       (2, '2')");
        var updateBatch = List.of(
                new Entity(2, "22"),
                new Entity(3, "33")
        );

        if (updateOnConflict) {
            updater.insertOrUpdate(updateBatch);
        } else {
            updater.insertWithoutUpdate(updateBatch);
        }

        var dbState = jdbcTemplate.query("SELECT id, val FROM updater_test",
                (rs, rowNum) -> new Entity(rs.getLong("id"), rs.getString("val"))
        );
        assertEquals(List.of(
                new Entity(1, "1"),
                new Entity(2, conflictEntityValue),
                new Entity(3, "33")
        ), dbState);
    }

    @Test
    void testValidationError() {
        var config = PgBatchUpdaterConfig.<Entity>builder()
                .tableName("updater_test")
                .keyColumns("id")
                .column("id", longSetter(e -> e.id))
                .build();
        assertThrows(IllegalArgumentException.class, () -> new PgBatchUpdater<>(jdbcTemplate, config));
    }

    @Test
    void testExcludedColumns() {
        var updater = new PgBatchUpdater<>(jdbcTemplate, PgBatchUpdaterConfig.<Entity>builder()
                .tableName("updater_test")
                .keyColumns("id")
                .excludedColumns("val", "not_existing_column")
                .column("id", longSetter(e -> e.id))
                .build());

        var entities = List.of(new Entity(0, "1"));
        updater.insertOrUpdate(entities);
        updater.insertOrUpdate(entities);

        var dbState = jdbcTemplate.query("SELECT id, val FROM updater_test",
                (rs, rowNum) -> new Entity(rs.getLong("id"), rs.getString("val"))
        );
        assertEquals(List.of(new Entity(0, null)), dbState);
    }

    @Test
    void testExcludedColumnsValidationError() {
        var config = PgBatchUpdaterConfig.<Entity>builder()
                .tableName("updater_test")
                .keyColumns("id")
                .excludedColumns("val")
                .column("id", longSetter(e -> e.id))
                .column("val", stringSetter(e -> e.value))
                .build();
        assertThrows(IllegalArgumentException.class, () -> new PgBatchUpdater<>(jdbcTemplate, config));
    }

    @Test
    void testCustomColumnValue() {
        var updater = new PgBatchUpdater<>(jdbcTemplate, PgBatchUpdaterConfig.<Entity>builder()
                .tableName("updater_test")
                .keyColumns("id")
                .column("id", longSetter(e -> e.id))
                .column("val", s -> "'" + s.value + "'")
                .build()
        );

        var entity = new Entity(1, "2");
        updater.insertOrUpdate(List.of(entity));
        var dbState = jdbcTemplate.query("SELECT id, val FROM updater_test",
                (rs, rowNum) -> new Entity(rs.getLong("id"), rs.getString("val"))
        );
        assertEquals(List.of(entity), dbState);
    }

    @Value
    private static class Entity {
        long id;
        String value;
    }
}
