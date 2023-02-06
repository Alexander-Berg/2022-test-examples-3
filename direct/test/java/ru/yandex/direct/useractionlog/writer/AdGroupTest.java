package ru.yandex.direct.useractionlog.writer;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.jooq.TableField;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.direct.binlog.reader.EnrichedEvent;
import ru.yandex.direct.binlogclickhouse.schema.FieldValueList;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.db.config.DbConfig;
import ru.yandex.direct.dbschema.ppc.enums.PhrasesAdgroupType;
import ru.yandex.direct.dbschema.ppc.enums.PhrasesStatusautobudgetshow;
import ru.yandex.direct.dbschema.ppc.enums.PhrasesStatusbssynced;
import ru.yandex.direct.dbschema.ppc.enums.PhrasesStatusmoderate;
import ru.yandex.direct.dbschema.ppc.enums.PhrasesStatusshowsforecast;
import ru.yandex.direct.mysql.MySQLServerBuilder;
import ru.yandex.direct.tracing.data.DirectTraceInfo;
import ru.yandex.direct.useractionlog.AdGroupId;
import ru.yandex.direct.useractionlog.CampaignId;
import ru.yandex.direct.useractionlog.ClientId;
import ru.yandex.direct.useractionlog.db.StateReaderWriter;
import ru.yandex.direct.useractionlog.db.WriteActionLogTable;
import ru.yandex.direct.useractionlog.dict.DictDataCategory;
import ru.yandex.direct.useractionlog.dict.MemoryDictRepository;
import ru.yandex.direct.useractionlog.model.TestRowModel;
import ru.yandex.direct.useractionlog.schema.ActionLogRecord;
import ru.yandex.direct.useractionlog.schema.ObjectPath;
import ru.yandex.direct.useractionlog.schema.Operation;
import ru.yandex.direct.useractionlog.schema.RecordSource;
import ru.yandex.direct.useractionlog.writer.generator.RowProcessingDefaults;

import static ru.yandex.direct.dbschema.ppc.tables.Phrases.PHRASES;

@ParametersAreNonnullByDefault
public class AdGroupTest {
    private MemoryDictRepository dictRepository;
    private RecordSource recordSource;
    private ActionProcessor actionProcessor;

    @Before
    public void setUp() {
        dictRepository = new MemoryDictRepository();
        recordSource = RecordSource.makeDaemonRecordSource();
        DbConfig dbConfig = new DbConfig();
        dbConfig.setHosts(Collections.singletonList("ignored"));
        dbConfig.setPort(0);
        dbConfig.setUser("ignored");
        dbConfig.setPass("ignored");
        actionProcessor = new ActionProcessor.Builder()
                .withBinlogKeepAliveTimeout(Duration.ofSeconds(10))
                .withBinlogStateFetchingSemaphore(null)
                .withDirectConfig(DirectConfigFactory.getCachedConfig())
                .withDbConfig(dbConfig)
                .withDictRepository(dictRepository)
                .withBatchDuration(Duration.ofSeconds(1))
                .withEventBatchSize(100)
                .withInitialServerId(null)
                .withMaxBufferedEvents(100)
                .withReadWriteStateTable(Mockito.mock(StateReaderWriter.class))
                .withRowProcessingStrategy(RowProcessingDefaults.defaultRowToActionLog(recordSource))
                .withSchemaReplicaMysqlBuilder(Mockito.mock(MySQLServerBuilder.class))
                .withSchemaReplicaMysqlSemaphore(null)
                .withUntilGtidSet(null)
                .withWriteActionLogTable(Mockito.mock(WriteActionLogTable.class))
                .build();
    }

    /**
     * Проверяет, что если приходит {@link EnrichedEvent}, в котором изменяется название группы, то в итоговом
     * {@link ActionLogRecord#oldFields} будет присутствовать название из словаря.
     */
    @Test
    public void testAdGroupName() {
        LocalDateTime dateTime = LocalDateTime.now().withNano(0);
        long serverEventId = 123;
        EnrichedEvent event = BinlogFixtureGenerator.createUpdateEvent(dateTime, serverEventId, PHRASES,
                ImmutableList.<Pair<TableField, Serializable>>builder()
                        .add(Pair.of(PHRASES.CID, 456L))
                        .add(Pair.of(PHRASES.PID, 789))
                        .add(Pair.of(PHRASES.ADGROUP_TYPE, PhrasesAdgroupType.base.ordinal() + 1))
                        .build(),
                ImmutableList.<Pair<TableField, Serializable>>builder()
                        .add(Pair.of(PHRASES.CID, 456L))
                        .add(Pair.of(PHRASES.PID, 789))
                        .add(Pair.of(PHRASES.ADGROUP_TYPE, PhrasesAdgroupType.base.ordinal() + 1))
                        .add(Pair.of(PHRASES.GROUP_NAME, "New name"))
                        .build());
        dictRepository.addData(DictDataCategory.CAMPAIGN_PATH, 456L,
                new ObjectPath.CampaignPath(new ClientId(123), new CampaignId(456)));
        dictRepository.addData(DictDataCategory.ADGROUP_NAME, 789L, "Old name");

        actionProcessor.handleRows(actionProcessor.makeRowsFromEvents(ImmutableList.of(event)), true);
        List<ActionLogRecord> records = actionProcessor.makeBufferCopy();
        Assertions.assertThat(records.size()).isEqualTo(1);
        Assertions.assertThat(records.get(0)).isEqualToComparingFieldByFieldRecursively(
                ActionLogRecord.builder()
                        .withDateTime(dateTime)
                        .withPath(
                                new ObjectPath.AdGroupPath(new ClientId(123), new CampaignId(456), new AdGroupId(789)))
                        .withGtid(BinlogFixtureGenerator.SERVER_UUID + ":" + serverEventId)
                        .withQuerySerial(0)
                        .withRowSerial(0)
                        .withDirectTraceInfo(DirectTraceInfo.empty())
                        .withDb("ppc")
                        .withType(PHRASES.getName())
                        .withOperation(Operation.UPDATE)
                        .withOldFields(FieldValueList.zip(
                                ImmutableList.<String>builder()
                                        .add(PHRASES.GROUP_NAME.getName())
                                        .add(TestRowModel.getVersionField())
                                        .build(),
                                ImmutableList.<String>builder()
                                        .add("Old name")
                                        .add(TestRowModel.VERSION)
                                        .build()))
                        .withNewFields(FieldValueList.zip(
                                ImmutableList.<String>builder()
                                        .add(PHRASES.GROUP_NAME.getName())
                                        .add(TestRowModel.getVersionField())
                                        .build(),
                                ImmutableList.<String>builder()
                                        .add("New name")
                                        .add(TestRowModel.VERSION)
                                        .build()))
                        .withRecordSource(recordSource)
                        .build());
    }

    /**
     * Когда-то на таком изменении были падения на ровном месте
     */
    @Test
    public void testAdGroupNameRealCase() {
        LocalDateTime dateTime = LocalDateTime.now().withNano(0);
        long serverEventId = 123;
        EnrichedEvent event = BinlogFixtureGenerator.createUpdateEvent(dateTime, serverEventId, PHRASES,
                ImmutableList.<Pair<TableField, Serializable>>builder()
                        .add(Pair.of(PHRASES.PID, 1545926726))
                        .add(Pair.of(PHRASES.ADGROUP_TYPE, PhrasesAdgroupType.base.ordinal() + 1))
                        .add(Pair.of(PHRASES.BID, null))
                        .add(Pair.of(PHRASES.CONTEXT_ID, 1074137368))
                        .add(Pair.of(PHRASES.PRIORITY_ID, 1))
                        .add(Pair.of(PHRASES.STATUS_MODERATE, PhrasesStatusmoderate.Yes.ordinal() + 1))
                        .add(Pair.of(PHRASES.LAST_CHANGE,
                                java.sql.Timestamp.valueOf(LocalDateTime.of(2017, 3, 29, 9, 2, 47))))
                        .add(Pair.of(PHRASES.STATUS_BS_SYNCED, PhrasesStatusbssynced.Yes.ordinal() + 1))
                        .add(Pair.of(PHRASES.STATUS_AUTOBUDGET_SHOW, PhrasesStatusautobudgetshow.Yes.ordinal() + 1))
                        .add(Pair.of(PHRASES.STATUS_SHOWS_FORECAST, PhrasesStatusshowsforecast.Sending.ordinal() + 1))
                        .add(Pair.of(PHRASES.FORECAST_DATE,
                                java.sql.Timestamp.valueOf(LocalDateTime.of(2017, 6, 6, 9, 3, 51))))
                        .add(Pair.of(PHRASES.STATUS_POST_MODERATE, PhrasesStatusmoderate.No.ordinal() + 1))
                        .add(Pair.of(PHRASES.MW_ID, null))
                        .add(Pair.of(PHRASES.CID, 18803772L))
                        .add(Pair.of(PHRASES.IS_BS_RARELY_LOADED, 0))
                        .build(),
                ImmutableList.<Pair<TableField, Serializable>>builder()
                        .add(Pair.of(PHRASES.PID, 1545926726))
                        .add(Pair.of(PHRASES.ADGROUP_TYPE, PhrasesAdgroupType.base.ordinal() + 1))
                        .add(Pair.of(PHRASES.BID, null))
                        .add(Pair.of(PHRASES.CONTEXT_ID, 1074137368))
                        .add(Pair.of(PHRASES.PRIORITY_ID, 1))
                        .add(Pair.of(PHRASES.STATUS_MODERATE, PhrasesStatusmoderate.Yes.ordinal() + 1))
                        .add(Pair.of(PHRASES.LAST_CHANGE,
                                java.sql.Timestamp.valueOf(LocalDateTime.of(2017, 3, 29, 9, 2, 47))))
                        .add(Pair.of(PHRASES.STATUS_BS_SYNCED, PhrasesStatusbssynced.Yes.ordinal() + 1))
                        .add(Pair.of(PHRASES.STATUS_AUTOBUDGET_SHOW, PhrasesStatusautobudgetshow.Yes.ordinal() + 1))
                        .add(Pair.of(PHRASES.STATUS_SHOWS_FORECAST, PhrasesStatusshowsforecast.Processed.ordinal() + 1))
                        .add(Pair.of(PHRASES.FORECAST_DATE,
                                java.sql.Timestamp.valueOf(LocalDateTime.of(2017, 6, 6, 9, 19, 57))))
                        .add(Pair.of(PHRASES.STATUS_POST_MODERATE, PhrasesStatusmoderate.No.ordinal() + 1))
                        .add(Pair.of(PHRASES.MW_ID, null))
                        .add(Pair.of(PHRASES.CID, 18803772L))
                        .add(Pair.of(PHRASES.IS_BS_RARELY_LOADED, 0))
                        .build());
        dictRepository.addData(DictDataCategory.CAMPAIGN_PATH, 18803772L,
                new ObjectPath.CampaignPath(new ClientId(100500), new CampaignId(18803772L)));

        actionProcessor.handleRows(actionProcessor.makeRowsFromEvents(ImmutableList.of(event)), true);
        List<ActionLogRecord> records = actionProcessor.makeBufferCopy();
        Assertions.assertThat(records).isEmpty();
    }
}
