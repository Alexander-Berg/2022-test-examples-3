package ru.yandex.direct.useractionlog.writer.generator;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.binlog.reader.EnrichedEvent;
import ru.yandex.direct.binlog.reader.EnrichedRow;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusactive;
import ru.yandex.direct.dbschema.ppc.tables.records.CampaignsRecord;
import ru.yandex.direct.useractionlog.CampaignId;
import ru.yandex.direct.useractionlog.ClientId;
import ru.yandex.direct.useractionlog.dict.DictDataCategory;
import ru.yandex.direct.useractionlog.dict.DictRequest;
import ru.yandex.direct.useractionlog.dict.DictResponsesAccessor;
import ru.yandex.direct.useractionlog.dict.MemoryDictRepository;
import ru.yandex.direct.useractionlog.model.CampaignRowModel;
import ru.yandex.direct.useractionlog.schema.ActionLogRecord;
import ru.yandex.direct.useractionlog.schema.ObjectPath;
import ru.yandex.direct.useractionlog.schema.RecordSource;
import ru.yandex.direct.useractionlog.writer.BinlogFixtureGenerator;
import ru.yandex.direct.useractionlog.writer.ErrorWrapper;

@ParametersAreNonnullByDefault
@SuppressWarnings("unchecked")
public class StringsFromDictStrategyTest {
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2017, 11, 21, 0, 0);
    private static final ObjectPath.CampaignPath CAMPAIGN_PATH =
            new ObjectPath.CampaignPath(new ClientId(100500L), new CampaignId(123L));
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();
    private MemoryDictRepository dictRepository;
    private StringsFromDictStrategy stringsFromDictStrategy = StringsFromDictStrategy.builder()
            .withIdField(TestTable.TABLE.cid.getName())
            .with(DictDataCategory.CAMPAIGN_NAME, TestTable.TABLE.name.getName())
            .build();
    private RowProcessingStrategy rowProcessingStrategy;
    private RecordSource recordSource;

    @Before
    public void setUp() {
        dictRepository = new MemoryDictRepository();
        dictRepository.addData(DictDataCategory.CAMPAIGN_NAME, 123L, "Campaign 123");

        recordSource = RecordSource.makeDaemonRecordSource();
        rowProcessingStrategy =
                new ModularRowProcessingStrategy(recordSource, new DummyPathStrategy(), stringsFromDictStrategy);
    }

    private List<ActionLogRecord> processEvents(EnrichedEvent... events) {
        BatchRowDictProcessing.Result dictResult = BatchRowDictProcessing.handleEvents(dictRepository,
                rowProcessingStrategy,
                Stream.of(events).flatMap(EnrichedEvent::rowsStream).collect(Collectors.toList()),
                new ErrorWrapper(false));
        Assertions.assertThat(dictResult.unprocessed).isEmpty();
        return dictResult.processed.stream()
                .flatMap(pair -> rowProcessingStrategy.processEvent(pair.getLeft(), pair.getRight()).stream())
                .collect(Collectors.toList());
    }

    /**
     * При изменении записи текстовое поле не будет добавлено в {@literal before},
     * если текстовое поле отсутствует в {@literal after}, т.е. если оно не изменилось.
     */
    @Test
    public void testHandleUpdateWithoutName() {
        long eventId = 1L;
        EnrichedEvent updateEvent = BinlogFixtureGenerator.createUpdateEvent(
                DATE_TIME,
                eventId,
                TestTable.TABLE,
                ImmutableList.of(
                        Pair.of(TestTable.TABLE.cid, 123L),
                        Pair.of(TestTable.TABLE.statusActive, CampaignsStatusactive.No.getLiteral())),
                ImmutableList.of(
                        Pair.of(TestTable.TABLE.cid, 123L),
                        Pair.of(TestTable.TABLE.statusActive, CampaignsStatusactive.Yes.getLiteral())));
        List<ActionLogRecord> records = processEvents(updateEvent);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            ActionLogRecord record = records.get(0);
            softly.assertThat(record.getOldFields().toMap())
                    .describedAs("Old fields")
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "123"),
                            Pair.of(TestTable.TABLE.statusActive.getName(), CampaignsStatusactive.No.getLiteral()),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
            softly.assertThat(record.getNewFields().toMap())
                    .describedAs("New fields")
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "123"),
                            Pair.of(TestTable.TABLE.statusActive.getName(), CampaignsStatusactive.Yes.getLiteral()),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
        }
        checkTextFieldWasNotRequested();
    }

    private void checkTextFieldWasNotRequested() {
        softly.assertThat(dictRepository.requestLog)
                .describedAs("No need for requesting text field because it was not changed")
                .hasSize(0);
    }

    /**
     * При изменении записи текстовое поле будет добавлено в {@literal before},
     * если текстовое поле присутствует в {@literal after}, т.е. если оно изменилось.
     */
    @Test
    public void testHandleUpdateWithName() {
        long eventId = 1L;
        EnrichedEvent updateEvent = BinlogFixtureGenerator.createUpdateEvent(
                DATE_TIME,
                eventId,
                TestTable.TABLE,
                ImmutableList.of(
                        Pair.of(TestTable.TABLE.cid, 123L),
                        Pair.of(TestTable.TABLE.statusActive, CampaignsStatusactive.No.getLiteral())),
                ImmutableList.of(
                        Pair.of(TestTable.TABLE.cid, 123L),
                        Pair.of(TestTable.TABLE.name, "New name"),
                        Pair.of(TestTable.TABLE.statusActive, CampaignsStatusactive.Yes.getLiteral())));
        List<ActionLogRecord> records = processEvents(updateEvent);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            ActionLogRecord record = records.get(0);
            softly.assertThat(record.getOldFields().toMap())
                    .describedAs("Old fields")
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "123"),
                            Pair.of(TestTable.TABLE.name.getName(), "Campaign 123"),
                            Pair.of(TestTable.TABLE.statusActive.getName(), CampaignsStatusactive.No.getLiteral()),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
            softly.assertThat(record.getNewFields().toMap())
                    .describedAs("New fields")
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "123"),
                            Pair.of(TestTable.TABLE.name.getName(), "New name"),
                            Pair.of(TestTable.TABLE.statusActive.getName(),
                                    CampaignsStatusactive.Yes.getLiteral()),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
        }
        checkTextFieldWasRequested();
    }

    private void checkTextFieldWasRequested() {
        softly.assertThat(dictRepository.requestLog)
                .containsExactly(new DictRequest(DictDataCategory.CAMPAIGN_NAME, 123L));
    }

    /**
     * При изменении записи ничего не будет изменено, если и в {@literal before},
     * и в {@literal after} присутствует текстовое поле (т.е. у текстового поля тип varchar).
     */
    @Test
    public void testHandleUpdateWithNameInBeforeAndAfter() {
        long eventId = 1L;
        EnrichedEvent updateEvent = BinlogFixtureGenerator.createUpdateEvent(
                DATE_TIME,
                eventId,
                TestTable.TABLE,
                ImmutableList.of(
                        Pair.of(TestTable.TABLE.cid, 123L),
                        Pair.of(TestTable.TABLE.name, "Old name"),
                        Pair.of(TestTable.TABLE.statusActive, CampaignsStatusactive.No.getLiteral())),
                ImmutableList.of(
                        Pair.of(TestTable.TABLE.cid, 123L),
                        Pair.of(TestTable.TABLE.name, "New name"),
                        Pair.of(TestTable.TABLE.statusActive, CampaignsStatusactive.Yes.getLiteral())));
        List<ActionLogRecord> records = processEvents(updateEvent);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            ActionLogRecord record = records.get(0);
            softly.assertThat(record.getOldFields().toMap())
                    .describedAs("Old fields")
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "123"),
                            Pair.of(TestTable.TABLE.name.getName(), "Old name"),
                            Pair.of(TestTable.TABLE.statusActive.getName(), CampaignsStatusactive.No.getLiteral()),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
            softly.assertThat(record.getNewFields().toMap())
                    .describedAs("New fields")
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "123"),
                            Pair.of(TestTable.TABLE.name.getName(), "New name"),
                            Pair.of(TestTable.TABLE.statusActive.getName(),
                                    CampaignsStatusactive.Yes.getLiteral()),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
        }
        checkTextFieldWasNotRequested();
    }

    /**
     * При удалении записи будет вставлено текстовое поле, если его не было в кортеже.
     */
    @Test
    public void testHandleDeleteWithoutName() {
        long eventId = 1L;
        EnrichedEvent deleteEvent = BinlogFixtureGenerator.createDeleteEvent(
                DATE_TIME,
                eventId,
                TestTable.TABLE,
                ImmutableList.of(Pair.of(TestTable.TABLE.cid, 123L)));
        List<ActionLogRecord> records = processEvents(deleteEvent);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            ActionLogRecord record = records.get(0);
            softly.assertThat(record.getOldFields().toMap())
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "123"),
                            Pair.of(TestTable.TABLE.name.getName(), "Campaign 123"),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
            softly.assertThat(record.getNewFields().toMap())
                    .containsOnly(Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
        }
        checkTextFieldWasRequested();
    }

    /**
     * При удалении записи не будет вставлено текстовое поле, если оно уже было в кортеже.
     */
    @Test
    public void testHandleDeleteWithName() {
        long eventId = 1L;
        EnrichedEvent deleteEvent = BinlogFixtureGenerator.createDeleteEvent(
                DATE_TIME,
                eventId,
                TestTable.TABLE,
                ImmutableList.of(
                        Pair.of(TestTable.TABLE.cid, 123L),
                        Pair.of(TestTable.TABLE.name, "Some name")));
        List<ActionLogRecord> records = processEvents(deleteEvent);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            ActionLogRecord record = records.get(0);
            softly.assertThat(record.getOldFields().toMap())
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "123"),
                            Pair.of(TestTable.TABLE.name.getName(), "Some name"),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
            softly.assertThat(record.getNewFields().toMap())
                    .containsOnly(Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
        }
        checkTextFieldWasNotRequested();
    }

    /**
     * При создании новой записи ничего не запрашивает в словаре, но зато пишет в него.
     */
    @Test
    public void testHandleInsert() {
        EnrichedEvent insertEvent = BinlogFixtureGenerator.createInsertEvent(
                DATE_TIME,
                1L,
                TestTable.TABLE,
                ImmutableList.of(
                        Pair.of(TestTable.TABLE.cid, 100500L),
                        Pair.of(TestTable.TABLE.name, "Some Campaign Name 100500"),
                        Pair.of(TestTable.TABLE.statusActive, CampaignsStatusactive.Yes.getLiteral())));
        List<ActionLogRecord> records = processEvents(insertEvent);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            ActionLogRecord record = records.get(0);
            softly.assertThat(record.getNewFields().toMap())
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "100500"),
                            Pair.of(TestTable.TABLE.name.getName(), "Some Campaign Name 100500"),
                            Pair.of(TestTable.TABLE.statusActive.getName(), CampaignsStatusactive.Yes.getLiteral()),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
            softly.assertThat(record.getOldFields().toMap())
                    .containsOnly(Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
        }
        checkTextFieldWasNotRequested();

        softly.assertThat(dictRepository.repositoryMap)
                .describedAs("New campaign name was added to dictionary")
                .containsEntry(new DictRequest(DictDataCategory.CAMPAIGN_NAME, 100500L), "Some Campaign Name 100500");
    }

    /**
     * Если встречает NULL, то не падает, а преобразовывает в пустую строку.
     */
    @Test
    public void testHandleInsertNull() {
        EnrichedEvent insertEvent = BinlogFixtureGenerator.createInsertEvent(
                DATE_TIME,
                1L,
                TestTable.TABLE,
                ImmutableList.of(
                        Pair.of(TestTable.TABLE.cid, 100500L),
                        Pair.of(TestTable.TABLE.name, null),
                        Pair.of(TestTable.TABLE.statusActive, CampaignsStatusactive.Yes.getLiteral())));
        List<ActionLogRecord> records = processEvents(insertEvent);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            ActionLogRecord record = records.get(0);
            softly.assertThat(record.getNewFields().toMap())
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "100500"),
                            Pair.of(TestTable.TABLE.name.getName(), ""),
                            Pair.of(TestTable.TABLE.statusActive.getName(), CampaignsStatusactive.Yes.getLiteral()),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
        }
        checkTextFieldWasNotRequested();

        softly.assertThat(dictRepository.repositoryMap)
                .describedAs("Empty string instead of null was written in dictionary")
                .containsEntry(new DictRequest(DictDataCategory.CAMPAIGN_NAME, 100500L), "");
    }

    /**
     * Если встречает NULL, то не падает, а преобразовывает в пустую строку.
     */
    @Test
    public void testHandleUpdateNull() {
        long eventId = 1L;
        EnrichedEvent updateEvent = BinlogFixtureGenerator.createUpdateEvent(
                DATE_TIME,
                eventId,
                TestTable.TABLE,
                ImmutableList.of(
                        Pair.of(TestTable.TABLE.cid, 123L)),
                ImmutableList.of(
                        Pair.of(TestTable.TABLE.cid, 123L),
                        Pair.of(TestTable.TABLE.name, null)));
        List<ActionLogRecord> records = processEvents(updateEvent);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            ActionLogRecord record = records.get(0);
            softly.assertThat(record.getOldFields().toMap())
                    .describedAs("Old fields")
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "123"),
                            Pair.of(TestTable.TABLE.name.getName(), "Campaign 123"),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
            softly.assertThat(record.getNewFields().toMap())
                    .describedAs("New fields")
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "123"),
                            Pair.of(TestTable.TABLE.name.getName(), ""),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
        }
        checkTextFieldWasRequested();
    }

    /**
     * Если встречает NULL, то не падает, а преобразовывает в пустую строку.
     */
    @Test
    public void testHandleDeleteNull() {
        long eventId = 1L;
        EnrichedEvent deleteEvent = BinlogFixtureGenerator.createDeleteEvent(
                DATE_TIME,
                eventId,
                TestTable.TABLE,
                ImmutableList.of(
                        Pair.of(TestTable.TABLE.cid, 123L),
                        Pair.of(TestTable.TABLE.name, null)));
        List<ActionLogRecord> records = processEvents(deleteEvent);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            ActionLogRecord record = records.get(0);
            softly.assertThat(record.getOldFields().toMap())
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "123"),
                            Pair.of(TestTable.TABLE.name.getName(), ""),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
            softly.assertThat(record.getNewFields().toMap())
                    .containsOnly(Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
        }
        checkTextFieldWasNotRequested();
    }

    public static class TestTable extends TableImpl {
        public static final TestTable TABLE = new TestTable();
        public final TableField<CampaignsRecord, Long> cid =
                createField("cid", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");
        public final TableField<CampaignsRecord, String> name =
                createField("name", org.jooq.impl.SQLDataType.CLOB.nullable(true), this, "");
        public final TableField<CampaignsRecord, CampaignsStatusactive> statusActive =
                createField("statusActive",
                        org.jooq.util.mysql.MySQLDataType.VARCHAR
                                .asEnumDataType(ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusactive.class),
                        this, "");

        TestTable() {
            super(DSL.name("campaigns"));
        }
    }

    private static class DummyPathStrategy extends DummyFiller implements ObjectPathStrategy {
        @Override
        public Collection<String> objectPathColumns() {
            return Collections.emptyList();
        }

        @Override
        public ObjectPath extract(EnrichedRow row, DictResponsesAccessor dictResponsesAccessor) {
            return CAMPAIGN_PATH;
        }
    }
}
