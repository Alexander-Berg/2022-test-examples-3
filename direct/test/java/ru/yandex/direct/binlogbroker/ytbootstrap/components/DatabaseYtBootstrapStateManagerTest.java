package ru.yandex.direct.binlogbroker.ytbootstrap.components;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ParametersAreNonnullByDefault
public class DatabaseYtBootstrapStateManagerTest {
    private static final String JUNK_SOURCE = "junk:0";
    private static final String TABLE_NAME = "test";
    private static final BinlogEvent EVENT_TEMPLATE = new BinlogEvent()
            .withSource(JUNK_SOURCE)
            .withDb("ppc")
            .withGtid("0000:1111")
            .withOperation(Operation.INSERT)
            .withQueryIndex(0)
            .withServerUuid("0000")
            .withTable(TABLE_NAME)
            .withTransactionId(0L)
            .withUtcTimestamp(LocalDateTime.now());


    private List<BinlogEvent> createBatch(Map<String, Object> primaryKey) {
        return ImmutableList.of(
                BinlogEvent.fromTemplate(EVENT_TEMPLATE)
                        .withRows(ImmutableList.of(
                                new BinlogEvent.Row()
                                        .withRowIndex(0)
                                        .withPrimaryKey(primaryKey)
                                        .withAfter(Map.of())
                                        .withBefore(Map.of())
                                )
                        )
                        .validate()
        );
    }


    /**
     * проверяем корректность сохранения и последующего чтения различных значений в primary key
     */
    @Test
    public void saveAndLoadState() {
        final DatabaseYtBootstrapStateManager stateManager =
                new DatabaseYtBootstrapStateManager(JUNK_SOURCE, 10, 10);

        final Map<String, Object> primaryKey = Map.of(
                "id", 10,
                "string", "text",
                "timestamp", LocalDateTime.now(),
                "date", LocalDate.now(),
                "bid", BigInteger.ONE.shiftRight(40)
        );

        stateManager.batchQueued(createBatch(primaryKey));
        stateManager.saveState();
        final DatabaseYtBootstrapState loadedState = DatabaseYtBootstrapStateManager.loadState(JUNK_SOURCE);
        assertNotNull(loadedState);
        final DatabaseYtBootstrapState.TableState tableState = loadedState.tables.get(TABLE_NAME);
        assertEquals(primaryKey, tableState.lastReadPrimaryKey);
    }
}
