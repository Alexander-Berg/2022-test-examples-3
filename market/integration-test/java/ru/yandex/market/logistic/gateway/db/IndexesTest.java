package ru.yandex.market.logistic.gateway.db;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import io.github.mfvanek.pg.connection.PgConnectionImpl;
import io.github.mfvanek.pg.index.maintenance.IndexMaintenanceOnHostImpl;
import io.github.mfvanek.pg.index.maintenance.IndexesMaintenanceOnHost;
import io.github.mfvanek.pg.model.index.Index;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IndexesTest extends AbstractIntegrationTest {

    private static final String DELIMITER = ", ";

    @Autowired
    private DataSource ds;
    private IndexesMaintenanceOnHost indexMaintenance;

    @Before
    public void beforeEach() {
        this.indexMaintenance = new IndexMaintenanceOnHostImpl(PgConnectionImpl.ofPrimary(ds));
    }

    @Test
    public void foreignKeysAreCoveredWithIndex() {
        var fks = indexMaintenance.getForeignKeysNotCoveredWithIndex();
        assertIsEmpty(
            fks,
            "Не все FK имеют индекс: %s",
            fk -> String.join(".", fk.getTableName(), fk.getConstraintName())
        );
    }

    @Test
    public void duplicatedIndexesTest() {
        var duplicates = indexMaintenance.getDuplicatedIndexes();
        assertIsEmpty(duplicates, "Нашлись дубликаты индексов: %s", duplicate -> duplicate.getIndexNames().toString());
    }

    @Test
    public void intersectedIndexesTest() {
        var intersections = indexMaintenance.getIntersectedIndexes();
        assertIsEmpty(
            intersections,
            "Есть пересечения в индексах: %s",
            intersection -> intersection.getDuplicatedIndexes().stream()
                .map(Index::getIndexName)
                .collect(Collectors.joining(DELIMITER, intersection.getTableName() + ": {", "}"))
        );
    }

    @Test
    public void invalidIndexesTest() {
        var invalidIndexes = indexMaintenance.getInvalidIndexes();
        assertIsEmpty(invalidIndexes, "Найдены невалидные индексы: %s", this::getIndexFullName);
    }

    private <T> void assertIsEmpty(
        Collection<T> collection,
        String failedAssertionMessage,
        Function<T, String> mapping
    ) {
        assertNotNull(collection);
        String elements = collection.stream()
            .map(elem -> mapping.apply(elem))
            .collect(Collectors.joining(DELIMITER));
        assertTrue(collection.isEmpty(), String.format(failedAssertionMessage, elements));
    }

    private String getIndexFullName(Index index) {
        return String.join(".", index.getTableName(), index.getIndexName());
    }
}
