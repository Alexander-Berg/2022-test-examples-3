package ru.yandex.market.core.history;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.history.HistoryService.Record;

@DbUnitDataSet(before = "HistoryServiceTest.before.csv", after = "HistoryServiceTest.csv")
class HistoryServiceTest extends FunctionalTest {

    private static final String ENTITY_NAME = "entity-name";
    private static final String ENTITY_SNAPSHOT = "entity-snapshot";

    private static final long ACTION_ID = 1L;
    private static final long ENTITY_ID = 2L;
    private static final long DATASOURCE_ID = 3L;


    @Autowired
    private HistoryService historyService;


    @Test
    void testAddRecord() {
        final Collection<Record> records = createRecords();
        for (final Record record : records) {
            historyService.addRecord(record);
        }
    }

    @Test
    void testAddRecords() {
        final Collection<Record> records = createRecords();
        historyService.addRecords(records);
    }


    private Collection<Record> createRecords() {
        final SimpleEntityFinder entityFinder = createEntityFinder();

        return Arrays.asList(
                createRecord(() -> historyService.buildCreateRecord(entityFinder)),
                createRecord(() -> historyService.buildUpdateRecord(entityFinder)),
                createRecord(() -> historyService.buildDeleteRecord(entityFinder))
        );
    }

    private Record createRecord(final Supplier<Record.Builder> recordBuilder) {
        final Record.Builder builder = recordBuilder.get();
        return builder.setActionID(ACTION_ID)
                .setEntityID(ENTITY_ID)
                .setDatasourceID(DATASOURCE_ID)
                .build();
    }

    private SimpleEntityFinder createEntityFinder() {
        return new SimpleEntityFinder(ENTITY_NAME, ENTITY_SNAPSHOT);
    }

}
