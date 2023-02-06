package ru.yandex.market.delivery.tracker.db;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import io.github.mfvanek.pg.connection.PgConnectionImpl;
import io.github.mfvanek.pg.index.maintenance.IndexMaintenanceOnHostImpl;
import io.github.mfvanek.pg.index.maintenance.IndexesMaintenanceOnHost;
import io.github.mfvanek.pg.model.PgContext;
import io.github.mfvanek.pg.model.index.Index;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.delivery.tracker.AbstractContextualTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IndexesTest extends AbstractContextualTest {

    private static final String DELIMITER = ", ";
    private static final List<String> EXCLUDE_INTERSECTIONS = Lists.newArrayList("qrtz_triggers_pkey");

    @Autowired
    private DataSource ds;
    @Value("${spring.liquibase.default-schema}")
    private String schema;
    private IndexesMaintenanceOnHost indexMaintenance;
    private PgContext pgContext;

    @BeforeEach
    public void before() {
        this.indexMaintenance = new IndexMaintenanceOnHostImpl(PgConnectionImpl.ofPrimary(ds));
        this.pgContext = PgContext.of(schema);
    }

    @Test
    public void foreignKeysAreCoveredWithIndex() {
        var fks = indexMaintenance.getForeignKeysNotCoveredWithIndex(pgContext);
        assertIsEmpty(
            fks,
            "Не все FK имеют индекс: %s",
            fk -> String.join(".", fk.getTableName(), fk.getConstraintName())
        );
    }

    @Test
    public void duplicatedIndexesTest() {
        var duplicates = indexMaintenance.getDuplicatedIndexes(pgContext);
        assertIsEmpty(duplicates, "Нашлись дубликаты индексов: %s", duplicate -> duplicate.getIndexNames().toString());
    }

    @Test
    @Disabled("Оно точно нужно?")
    public void intersectedIndexesTest() {
        var intersections = indexMaintenance.getIntersectedIndexes(pgContext).stream()
            .filter(it -> it.getDuplicatedIndexes().stream()
                .noneMatch(idx -> EXCLUDE_INTERSECTIONS.contains(idx.getIndexName())))
            .collect(Collectors.toList());
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
        var invalidIndexes = indexMaintenance.getInvalidIndexes(pgContext);
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
