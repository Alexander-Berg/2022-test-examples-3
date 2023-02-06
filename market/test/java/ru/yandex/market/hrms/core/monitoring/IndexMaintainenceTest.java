package ru.yandex.market.hrms.core.monitoring;

import java.util.List;

import javax.sql.DataSource;

import io.github.mfvanek.pg.connection.PgConnectionImpl;
import io.github.mfvanek.pg.index.maintenance.IndexMaintenance;
import io.github.mfvanek.pg.index.maintenance.IndexMaintenanceImpl;
import io.github.mfvanek.pg.model.DuplicatedIndexes;
import io.github.mfvanek.pg.model.ForeignKey;
import io.github.mfvanek.pg.model.Index;
import io.github.mfvanek.pg.model.IndexWithNulls;
import io.github.mfvanek.pg.model.Table;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.hrms.core.AbstractCoreTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class IndexMaintainenceTest extends AbstractCoreTest {
    private IndexMaintenance indexMaintenance;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    public void setUp() {
        this.indexMaintenance = new IndexMaintenanceImpl(PgConnectionImpl.ofMaster(dataSource));
    }

    @Test
    public void getInvalidIndexesShouldReturnNothing() {
        final List<Index> invalidIndexes = indexMaintenance.getInvalidIndexes();

        assertNotNull(invalidIndexes);
        assertThat(invalidIndexes, empty());
    }

    @Test
    public void getDuplicatedIndexesShouldReturnNothing() {
        final List<DuplicatedIndexes> duplicatedIndexes = indexMaintenance.getDuplicatedIndexes();

        assertNotNull(duplicatedIndexes);
        assertThat(duplicatedIndexes, empty());
    }

    @Test
    public void getIntersectedIndexesShouldReturnNothing() {
        final List<DuplicatedIndexes> intersectedIndexes = indexMaintenance.getIntersectedIndexes();

        assertNotNull(intersectedIndexes);
        //  Не должно быть пересекающихся индексов!
        assertThat(intersectedIndexes, empty());
    }

    @Test
    public void getForeignKeysNotCoveredWithIndexShouldReturnNothing() {
        final List<ForeignKey> foreignKeys = indexMaintenance.getForeignKeysNotCoveredWithIndex();

        assertNotNull(foreignKeys);
        // Не должно быть внешних ключей, не покрытых индексами!
        assertThat(foreignKeys, empty());
    }

    @Test
    public void getTablesWithoutPrimaryKeyShouldReturnSeveralRows() {
        final List<Table> tables = indexMaintenance.getTablesWithoutPrimaryKey();

        assertNotNull(tables);
        // Не должно появляться новых таблиц без первичных ключей!
        assertThat(tables, hasSize(1));
        assertThat(tables, everyItem(Matchers.hasProperty("tableName", is("databasechangelog"))));
    }

    @Test
    public void getIndexesWithNullValuesShouldReturnNothing() {
        final List<IndexWithNulls> indexesWithNulls = indexMaintenance.getIndexesWithNullValues();

        assertNotNull(indexesWithNulls);
        assertThat(indexesWithNulls, empty());
    }
}
