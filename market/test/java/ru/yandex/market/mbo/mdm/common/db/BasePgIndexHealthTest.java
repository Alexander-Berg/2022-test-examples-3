package ru.yandex.market.mbo.mdm.common.db;

import java.util.List;
import java.util.stream.Collectors;

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

import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

/**
 * Tests of health of postgres db. Don't add new exceptions without exceptional reason.
 * <p>
 * See https://ivvakhrushev.at.yandex-team.ru/1
 * Or https://st.yandex-team.ru/MBO-24340
 */
public abstract class BasePgIndexHealthTest extends MdmBaseDbTestClass {

    @Resource
    protected DataSource dataSource;

    protected IndexesMaintenanceOnHost indexMaintenance;
    protected TablesMaintenanceOnHost tablesMaintenance;
    protected List<PgContext> schemas;

    @Before
    public void setUp() {
        PgConnection pgConnection = PgConnectionImpl.ofPrimary(dataSource);
        this.indexMaintenance = new IndexMaintenanceOnHostImpl(pgConnection);
        this.tablesMaintenance = new TablesMaintenanceOnHostImpl(pgConnection);
        List<String> schemasToAnalyze = getSchemasToAnalyze();
        this.schemas = schemasToAnalyze.stream()
            .map(PgContext::of)
            .collect(Collectors.toList());
    }

    protected abstract List<String> getSchemasToAnalyze();

    @Test
    public void getInvalidIndexesShouldReturnNothing() {
        List<Index> invalidIndexes = getInvalidIndexes();

        Assertions.assertThat(invalidIndexes).isEmpty();
    }

    @Test
    public void getDuplicatedIndexesShouldReturnNothing() {
        List<DuplicatedIndexes> duplicatedIndexes = getDuplicatedIndexes();

        Assertions.assertThat(duplicatedIndexes).isEmpty();
    }

    @Test
    public void getIntersectedIndexesShouldReturnNothing() {
        List<DuplicatedIndexes> intersectedIndexes = getIntersectedIndexes();

        Assertions.assertThat(intersectedIndexes).isEmpty();
    }

    @Test
    public void getForeignKeysNotCoveredWithIndexShouldReturnNothing() {
        List<ForeignKey> foreignKeys = getForeignKeysNotCoveredWithIndex();

        Assertions.assertThat(foreignKeys).isEmpty();
    }

    @Test
    public void getTablesWithoutPrimaryKeyShouldReturnNothing() {
        List<Table> tables = getTablesWithoutPrimaryKey();

        Assertions.assertThat(tables).isEmpty();
    }

    @Test
    public void getIndexesWithNullValuesShouldReturnNothing() {
        List<IndexWithNulls> indexesWithNulls = getIndexesWithNullValues();

        Assertions.assertThat(indexesWithNulls).isEmpty();
    }

    protected List<Index> getInvalidIndexes() {
        return schemas.stream()
            .flatMap(schema -> indexMaintenance.getInvalidIndexes(schema).stream())
            .collect(Collectors.toList());
    }

    protected List<DuplicatedIndexes> getDuplicatedIndexes() {
        return schemas.stream()
            .flatMap(schema -> indexMaintenance.getDuplicatedIndexes(schema).stream())
            .collect(Collectors.toList());
    }

    protected List<DuplicatedIndexes> getIntersectedIndexes() {
        return schemas.stream()
            .flatMap(schema -> indexMaintenance.getIntersectedIndexes(schema).stream())
            .collect(Collectors.toList());
    }

    protected List<ForeignKey> getForeignKeysNotCoveredWithIndex() {
        return schemas.stream()
            .flatMap(schema -> indexMaintenance.getForeignKeysNotCoveredWithIndex(schema).stream())
            .collect(Collectors.toList());
    }

    protected List<Table> getTablesWithoutPrimaryKey() {
        return schemas.stream()
            .flatMap(schema -> tablesMaintenance.getTablesWithoutPrimaryKey(schema).stream())
            .collect(Collectors.toList());
    }

    protected List<IndexWithNulls> getIndexesWithNullValues() {
        return schemas.stream()
            .flatMap(schema -> indexMaintenance.getIndexesWithNullValues(schema).stream())
            .collect(Collectors.toList());
    }
}
