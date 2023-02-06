package ru.yandex.market.checkout.checkouter.monitoring.db.maintenance;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

import ru.yandex.market.checkout.application.AbstractWebTestBase;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class IndexesMaintenanceTest extends AbstractWebTestBase {

    private IndexMaintenance indexMaintenance;

    @BeforeEach
    public void setUp() {
        this.indexMaintenance = new IndexMaintenanceImpl(PgConnectionImpl.ofMaster(masterDatasource));
    }

    @Test
    public void checkPostgresVersion() {
        final String pgVersion = masterJdbcTemplate.queryForObject("select version();", String.class);
        assertThat(pgVersion, startsWith("PostgreSQL 12.8"));
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
    public void getIntersectedIndexesShouldReturnOneRow() {
        final List<DuplicatedIndexes> intersectedIndexes = indexMaintenance.getIntersectedIndexes();

        assertNotNull(intersectedIndexes);
        //  Не должно быть пересекающихся индексов!
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
    public void getTablesWithoutPrimaryKeyShouldReturnSeveralRows() {
        final List<Table> tables = indexMaintenance.getTablesWithoutPrimaryKey();

        assertNotNull(tables);
        // Не должно появляться новых таблиц без первичных ключей!
        assertEquals(3, tables.size());
        final List<String> tableNames = tables.stream()
                .map(Table::getTableName)
                .collect(toList());
        assertThat(tableNames, containsInAnyOrder(
                "delivery_track_checkpoint_history",
                "delivery_track_history",
                "databasechangelog"));
    }

    // TODO колонка buyer_id в orders удаляется в две тиреации:
    // 1) Удаление NOT NULL ограничения от колонки и игнорирование колонки в жуке
    // 2) Удалление самой колонки
    // Поэтому между первой и второй итерацией становится на один NullValue индекс больше
    // После окончания воторой итерации кол-во NullValue индексов снова будет равно 6 !!!Исправить обратно в тесте!!!
    // не забыть удалить i_orders_buyer_id в последнем ассерте
    @Test
    public void getIndexesWithNullValuesShouldReturnSeveralRows() {
        final List<IndexWithNulls> indexesWithNulls = indexMaintenance.getIndexesWithNullValues();

        assertNotNull(indexesWithNulls);
        assertEquals(7, indexesWithNulls.size());
        final Set<String> indexes = indexesWithNulls.stream()
                .map(Index::getIndexName)
                .collect(Collectors.toSet());
        assertThat(indexes, containsInAnyOrder(
                "i_order_event_eid",
                "i_delivery_track_checkpoint_raw_status_date",
                "i_queued_calls_processing_by",
                "i_queued_calls_processed_at",
                "i_order_buyer_normalized_phone", // удалить в MARKETCHECKOUT-27095
                "i_order_buyer_email", // удалить в MARKETCHECKOUT-27943
                "i_orders_buyer_id" // удалить в MARKETCHECKOUT-11519
        ));
    }
}
