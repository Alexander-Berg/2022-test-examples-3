package ru.yandex.direct.grid.schemagen.sqlgen;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import ru.yandex.direct.mysql.ytsync.common.keys.PivotKeys;
import ru.yandex.direct.mysql.ytsync.task.builders.JooqTaskBuilder;
import ru.yandex.direct.mysql.ytsync.task.builders.SyncTableConnections;
import ru.yandex.direct.mysql.ytsync.task.builders.SyncTask;
import ru.yandex.direct.mysql.ytsync.task.config.DirectYtSyncConfig;
import ru.yandex.direct.mysql.ytsync.task.provider.TaskProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_ARC;
import static ru.yandex.direct.dbschema.ppc.Tables.PHRASES;

public class TableSqlGeneratorTest {
    private static final PivotKeys PIVOT_KEYS = PivotKeys.signedHashPartitions(64);
    private static final SyncTableConnections TABLE_CONNECTIONS_ONE =
            new SyncTableConnections(BIDS_ARC.getName(), BIDS_ARC.CID.getName());
    private static final SyncTask TASK_ONE =
            new JooqTaskBuilder(TABLE_CONNECTIONS_ONE, DirectYtSyncConfig::bidsPath, PIVOT_KEYS)
                    .expressionKey("cid_hash", "int64(farm_hash(cid))")
                    .longKey(BIDS_ARC.CID)
                    .shardKey("__shard__")
                    .field(BIDS_ARC.PHRASE_ID)
                    .field(BIDS_ARC.PHRASE)
                    .moneyField(BIDS_ARC.PRICE)
                    .moneyField(BIDS_ARC.PRICE_CONTEXT)
                    .field(BIDS_ARC.SHOWS_FORECAST)
                    .build();

    private static final SyncTableConnections TABLE_CONNECTIONS_TWO =
            new SyncTableConnections(PHRASES.getName(), PHRASES.PID.getName());
    private static final SyncTask TASK_TWO =
            new JooqTaskBuilder(TABLE_CONNECTIONS_TWO, DirectYtSyncConfig::phrasesPath, PIVOT_KEYS)
                    .expressionKey("__hash__", String.format("int64(farm_hash(%s))", PHRASES.CID.getName()))
                    .longKey(PHRASES.CID)
                    .longKey(PHRASES.PID, 2)
                    .shardKey("__shard__", 1)
                    .enumField(PHRASES.ADGROUP_TYPE)
                    .field(PHRASES.GROUP_NAME)
                    .enumField(PHRASES.STATUS_MODERATE)
                    .enumField(PHRASES.STATUS_BS_SYNCED)
                    .enumField(PHRASES.STATUS_AUTOBUDGET_SHOW)
                    .enumField(PHRASES.STATUS_SHOWS_FORECAST)
                    .enumField(PHRASES.STATUS_POST_MODERATE)
                    .build();

    @Test
    public void testCreateSchema() {
        DirectYtSyncConfig config = mock(DirectYtSyncConfig.class);
        doReturn("//home/tmp/tables/one")
                .when(config).bidsPath();
        doReturn("//home/tmp/tables/two")
                .when(config).phrasesPath();
        TableSqlGenerator sqlGenerator =
                new TableSqlGenerator(Arrays.asList(new TaskProvider(TASK_ONE, 10L), new TaskProvider(TASK_TWO, 10L)));
        Map<String, String> schemas = sqlGenerator.getSchemas("ut", config);

        assertThat(schemas)
                .containsOnly(
                        entry("onetable_ut", "CREATE TABLE onetable_ut (\n"
                                + "  cid_hash bigint(20),\n"
                                + "  cid bigint(20),\n"
                                + "  __shard__ bigint(20),\n"
                                + "  PhraseID bigint(20) unsigned,\n"
                                + "  phrase text,\n"
                                + "  price bigint(20),\n"
                                + "  price_context bigint(20),\n"
                                + "  showsForecast bigint(20)\n"
                                + ");"),
                        entry("twotable_ut", "CREATE TABLE twotable_ut (\n"
                                + "  __hash__ bigint(20),\n"
                                + "  cid bigint(20),\n"
                                + "  pid bigint(20),\n"
                                + "  __shard__ bigint(20),\n"
                                + "  adgroup_type text,\n"
                                + "  group_name text,\n"
                                + "  statusModerate text,\n"
                                + "  statusBsSynced text,\n"
                                + "  statusAutobudgetShow text,\n"
                                + "  statusShowsForecast text,\n"
                                + "  statusPostModerate text\n"
                                + ");")
                );
    }
}
