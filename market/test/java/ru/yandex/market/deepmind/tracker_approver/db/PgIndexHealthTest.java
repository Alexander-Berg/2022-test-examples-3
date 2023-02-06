package ru.yandex.market.deepmind.tracker_approver.db;

import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import io.github.mfvanek.pg.connection.PgConnection;
import io.github.mfvanek.pg.connection.PgConnectionImpl;
import io.github.mfvanek.pg.index.maintenance.IndexMaintenanceOnHostImpl;
import io.github.mfvanek.pg.index.maintenance.IndexesMaintenanceOnHost;
import io.github.mfvanek.pg.model.PgContext;
import io.github.mfvanek.pg.model.index.DuplicatedIndexes;
import io.github.mfvanek.pg.model.index.ForeignKey;
import io.github.mfvanek.pg.model.index.Index;
import io.github.mfvanek.pg.model.index.IndexWithNulls;
import io.github.mfvanek.pg.model.table.Table;
import io.github.mfvanek.pg.table.maintenance.TablesMaintenanceOnHost;
import io.github.mfvanek.pg.table.maintenance.TablesMaintenanceOnHostImpl;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.deepmind.tracker_approver.BaseTrackerApproverTest;

/**
 * Tests of health of postgres db. Don't add new exceptions without exceptional reason.
 * <p>
 * See https://ivvakhrushev.at.yandex-team.ru/1
 */
public class PgIndexHealthTest extends BaseTrackerApproverTest {

    @Resource
    protected DataSource dataSource;

    @Value("${tracker_approver.schema}")
    private String schemaStr;

    protected IndexesMaintenanceOnHost indexMaintenance;
    protected TablesMaintenanceOnHost tablesMaintenance;
    protected PgContext schema;

    @Before
    public void setUp() {
        PgConnection pgConnection = PgConnectionImpl.ofPrimary(dataSource);
        this.indexMaintenance = new IndexMaintenanceOnHostImpl(pgConnection);
        this.tablesMaintenance = new TablesMaintenanceOnHostImpl(pgConnection);
        this.schema = PgContext.of(schemaStr);
    }

    @Test
    public void getInvalidIndexesShouldReturnNothing() {
        List<Index> invalidIndexes = indexMaintenance.getInvalidIndexes(schema);

        Assertions.assertThat(invalidIndexes).isEmpty();
    }

    @Test
    public void getDuplicatedIndexesShouldReturnNothing() {
        List<DuplicatedIndexes> duplicatedIndexes = indexMaintenance.getDuplicatedIndexes(schema);

        Assertions.assertThat(duplicatedIndexes).isEmpty();
    }

    @Test
    public void getIntersectedIndexesShouldReturnNothing() {
        List<DuplicatedIndexes> intersectedIndexes = indexMaintenance.getIntersectedIndexes(schema);

        Assertions.assertThat(intersectedIndexes).isEmpty();
    }

    @Test
    public void getForeignKeysNotCoveredWithIndexShouldReturnNothing() {
        List<ForeignKey> foreignKeys = indexMaintenance.getForeignKeysNotCoveredWithIndex(schema);

        Assertions.assertThat(foreignKeys).isEmpty();
    }

    @Test
    public void getTablesWithoutPrimaryKeyShouldReturnNothing() {
        List<Table> tables = tablesMaintenance.getTablesWithoutPrimaryKey(schema);
        Assertions.assertThat(tables).isEmpty();
    }

    @Test
    public void getIndexesWithNullValuesShouldReturnNothing() {
        List<IndexWithNulls> indexesWithNulls = indexMaintenance.getIndexesWithNullValues(schema);

        Assertions.assertThat(indexesWithNulls).isEmpty();
    }
}

