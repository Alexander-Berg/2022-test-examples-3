package ru.yandex.market.delivery.transport_manager.db;

import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import io.github.mfvanek.pg.connection.PgConnectionImpl;
import io.github.mfvanek.pg.index.maintenance.IndexMaintenanceOnHostImpl;
import io.github.mfvanek.pg.index.maintenance.IndexesMaintenanceOnHost;
import io.github.mfvanek.pg.model.index.ForeignKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

public class DbHealthTest extends AbstractContextualTest {

    @Autowired
    private DataSource dataSource;

    private IndexesMaintenanceOnHost indexMaintenance;

    private static final Set<ForeignKey> FOREIGN_KEYS_EXCLUDED_FROM_CHECK = Set.of();

    @BeforeEach
    public void beforeEach() {
        this.indexMaintenance = new IndexMaintenanceOnHostImpl(PgConnectionImpl.ofPrimary(dataSource));
    }

    @Test
    @DisplayName("Нет ли битых индексов")
    public void invalidIndexesTest() {
        var invalidIndexes = indexMaintenance.getInvalidIndexes();
        softly.assertThat(invalidIndexes).isEmpty();
    }

    @Test
    @DisplayName("Нет ли внешних ключей без индексов")
    public void indexForForeignKeys() {
        var fks = indexMaintenance.getForeignKeysNotCoveredWithIndex();
        removeExcluded(fks);
        softly.assertThat(fks).isEmpty();
    }

    @Test
    @DisplayName("Нет ли дубликатов индексов")
    public void duplicatedIndexesTest() {
        var duplicates = indexMaintenance.getDuplicatedIndexes();
        softly.assertThat(duplicates).isEmpty();
    }

    private void removeExcluded(List<ForeignKey> keys) {
        keys.removeIf(FOREIGN_KEYS_EXCLUDED_FROM_CHECK::contains);
    }
}
