package ru.yandex.market.pers.feedback.liquibase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import io.github.mfvanek.pg.connection.PgConnectionImpl;
import io.github.mfvanek.pg.index.maintenance.IndexMaintenance;
import io.github.mfvanek.pg.index.maintenance.IndexMaintenanceImpl;
import io.github.mfvanek.pg.model.DuplicatedIndexes;
import io.github.mfvanek.pg.model.ForeignKey;
import io.github.mfvanek.pg.model.Index;
import io.github.mfvanek.pg.model.IndexWithNulls;
import io.github.mfvanek.pg.model.Table;
import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmbeddedPostgresTest {

    public static final String PG_VERSION = "PostgreSQL 12";

    // Миграции могут падать в тестах из-за Аркадии
    @RegisterExtension
    static final PreparedDbExtension embeddedPostgres = EmbeddedPostgresExtension.preparedDatabase(
            LiquibasePreparer.forClasspathLocation("db/changelog/db.changelog-master.xml"));

    private IndexMaintenance indexMaintenance;

    @BeforeEach
    void setUp() {
        this.indexMaintenance = new IndexMaintenanceImpl(PgConnectionImpl.ofMaster(embeddedPostgres.getTestDatabase()));
    }

    @Test
    void checkPostgresVersion() throws SQLException {
        try (Connection c = embeddedPostgres.getTestDatabase().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("select version()")) {
            rs.next();
            final String pgVersion = rs.getString(1);
            assertTrue(pgVersion.contains(PG_VERSION));
        }
    }

    @Test
    void getInvalidIndexesShouldReturnNothing() {
        final List<Index> invalidIndexes = indexMaintenance.getInvalidIndexes();
        assertNotNull(invalidIndexes);
        assertEquals(0, invalidIndexes.size());
    }

    @Test
    void getDuplicatedIndexesShouldReturnNothing() {
        final List<DuplicatedIndexes> duplicatedIndexes = indexMaintenance.getDuplicatedIndexes();
        assertNotNull(duplicatedIndexes);
        assertEquals(0, duplicatedIndexes.size());
    }

    @Test
    void getIntersectedIndexesShouldReturnNothing() {
        final List<DuplicatedIndexes> intersectedIndexes = indexMaintenance.getIntersectedIndexes();
        assertNotNull(intersectedIndexes);
        assertEquals(0, intersectedIndexes.size());
    }

    @Test
    void getForeignKeysNotCoveredWithIndexShouldReturnNothing() {
        final List<ForeignKey> foreignKeys = indexMaintenance.getForeignKeysNotCoveredWithIndex();
        assertNotNull(foreignKeys);
        assertEquals(0, foreignKeys.size());
    }

    @Test
    void getTablesWithoutPrimaryKeyShouldReturnOneRowForLiquibase() {
        final List<Table> tables = indexMaintenance.getTablesWithoutPrimaryKey();
        assertNotNull(tables);
        assertEquals(1, tables.size());
        assertEquals("databasechangelog", tables.get(0).getTableName());
    }

    @Test
    @Disabled
    void getIndexesWithNullValuesShouldReturnNothing() {
        final List<IndexWithNulls> indexesWithNulls = indexMaintenance.getIndexesWithNullValues();
        assertNotNull(indexesWithNulls);
        assertEquals(0, indexesWithNulls.size());
    }
}
