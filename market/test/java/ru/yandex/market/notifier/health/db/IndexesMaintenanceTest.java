package ru.yandex.market.notifier.health.db;

import io.github.mfvanek.pg.connection.PgConnectionImpl;
import io.github.mfvanek.pg.index.maintenance.IndexMaintenance;
import io.github.mfvanek.pg.index.maintenance.IndexMaintenanceImpl;
import io.github.mfvanek.pg.model.DuplicatedIndexes;
import io.github.mfvanek.pg.model.ForeignKey;
import io.github.mfvanek.pg.model.Index;
import io.github.mfvanek.pg.model.IndexWithNulls;
import io.github.mfvanek.pg.model.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class IndexesMaintenanceTest extends AbstractServicesTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private IndexMaintenance indexMaintenance;

    @BeforeEach
    public void setUp() {
        this.indexMaintenance = new IndexMaintenanceImpl(PgConnectionImpl.ofMaster(getDatasource()));
    }

    @Test
    public void checkPostgresVersion() {
        final String pgVersion = jdbcTemplate.queryForObject("select version();", String.class);
        assertThat(pgVersion, startsWith("PostgreSQL 11.6"));
    }

    @Test
    public void getInvalidIndexesShouldReturnNothing() {
        final List<Index> invalidIndexes = indexMaintenance.getInvalidIndexes();

        assertNotNull(invalidIndexes);
        assertEquals(0, invalidIndexes.size());
    }

    @Test
    public void getDuplicatedIndexesShouldReturnNothing() {
        final List<DuplicatedIndexes> duplicatedIndexes = indexMaintenance.getDuplicatedIndexes();

        assertNotNull(duplicatedIndexes);
        assertEquals(0, duplicatedIndexes.size());
    }

    @Test
    public void getIntersectedIndexesShouldReturnNothing() {
        final List<DuplicatedIndexes> intersectedIndexes = indexMaintenance.getIntersectedIndexes();

        assertNotNull(intersectedIndexes);
        assertEquals(0, intersectedIndexes.size());
    }

    @Test
    public void getForeignKeysNotCoveredWithIndexShouldReturnNothing() {
        final List<ForeignKey> foreignKeys = indexMaintenance.getForeignKeysNotCoveredWithIndex();

        assertNotNull(foreignKeys);
        // Не должно быть внешних ключей, не покрытых индексами!
        assertEquals(0, foreignKeys.size());
    }

    @Test
    public void getTablesWithoutPrimaryKeyShouldReturnNothing() {
        final List<Table> tables = indexMaintenance.getTablesWithoutPrimaryKey();

        assertNotNull(tables);
        // Не должно появляться новых таблиц без первичных ключей!
        assertEquals(1, tables.size());
        assertEquals("databasechangelog", tables.get(0).getTableName());
    }

    @Test
    public void getIndexesWithNullValuesShouldReturnNothing() {
        final List<IndexWithNulls> indexesWithNulls = indexMaintenance.getIndexesWithNullValues();

        assertNotNull(indexesWithNulls);
        assertEquals(0, indexesWithNulls.size());
    }
}
