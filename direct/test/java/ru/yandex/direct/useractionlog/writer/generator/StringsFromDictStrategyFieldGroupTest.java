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
public class StringsFromDictStrategyFieldGroupTest {
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2017, 11, 21, 0, 0);
    private static final ObjectPath.CampaignPath CAMPAIGN_PATH =
            new ObjectPath.CampaignPath(new ClientId(100500L), new CampaignId(123L));
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();
    private MemoryDictRepository dictRepository;
    private final StringsFromDictStrategy stringsFromDictStrategy = StringsFromDictStrategy.builder()
            .withIdField(TestTable.TABLE.cid.getName())
            .with(DictDataCategory.CAMPAIGN_TIME_TARGET, TestTable.TABLE.timeTarget.getName())
            .withFieldGroup(TestTable.TABLE.timeTarget.getName(), TestTable.TABLE.timeZoneId.getName())
            .build();
    private RowProcessingStrategy rowProcessingStrategy;

    @Before
    public void setUp() {
        dictRepository = new MemoryDictRepository();
        dictRepository.addData(DictDataCategory.CAMPAIGN_TIME_TARGET, 123L, "ABCDEF");

        RecordSource recordSource = RecordSource.makeDaemonRecordSource();
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
     * При изменение записи из группы, другое текстовое поле из этой же группы будет добавлено в {@literal before},
     * не смотря на то, что оно отсутствует в {@literal after}, т.е. не изменилось.
     * Таким образом если хотя бы одно поле из группы изменилось - все другие строковые элементы этой группы окажуться
     * в {@literal before}.
     */
    @Test
    public void testHandleUpdateTimeZone_timeTargetingAdded() {
        long eventId = 1L;
        EnrichedEvent updateEvent = BinlogFixtureGenerator.createUpdateEvent(
                DATE_TIME,
                eventId,
                TestTable.TABLE,
                ImmutableList.of(
                        Pair.of(TestTable.TABLE.cid, 123L),
                        Pair.of(TestTable.TABLE.timeZoneId, 3L)),
                ImmutableList.of(
                        Pair.of(TestTable.TABLE.cid, 123L),
                        Pair.of(TestTable.TABLE.timeZoneId, 4L)));
        List<ActionLogRecord> records = processEvents(updateEvent);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            ActionLogRecord record = records.get(0);
            softly.assertThat(record.getOldFields().toMap())
                    .describedAs("Old fields")
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "123"),
                            Pair.of(TestTable.TABLE.timeZoneId.getName(),"3"),
                            Pair.of(TestTable.TABLE.timeTarget.getName(), "ABCDEF"),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
            softly.assertThat(record.getNewFields().toMap())
                    .describedAs("New fields")
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "123"),
                            Pair.of(TestTable.TABLE.timeZoneId.getName(), "4"),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
        }
        checkTextFieldWasRequested();
    }

    /**
     * Если никакая запись из группы не изменилось, то в before ничего не добавится и из словаря ничего запрашивать не
     * нужно.
     */
    @Test
    public void testHandleUpdateTimeZoneToTheSameValue_timeTargetingNotAdded() {
        long eventId = 1L;
        EnrichedEvent updateEvent = BinlogFixtureGenerator.createUpdateEvent(
                DATE_TIME,
                eventId,
                TestTable.TABLE,
                ImmutableList.of(
                        Pair.of(TestTable.TABLE.cid, 123L),
                        Pair.of(TestTable.TABLE.timeZoneId, 3L)),
                ImmutableList.of(
                        Pair.of(TestTable.TABLE.cid, 123L),
                        Pair.of(TestTable.TABLE.timeZoneId, 3L)));
        List<ActionLogRecord> records = processEvents(updateEvent);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            ActionLogRecord record = records.get(0);
            softly.assertThat(record.getOldFields().toMap())
                    .describedAs("Old fields")
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "123"),
                            Pair.of(TestTable.TABLE.timeZoneId.getName(), "3"),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
            softly.assertThat(record.getNewFields().toMap())
                    .describedAs("New fields")
                    .containsOnly(
                            Pair.of(TestTable.TABLE.cid.getName(), "123"),
                            Pair.of(TestTable.TABLE.timeZoneId.getName(), "3"),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
        }
        checkTextFieldWasNotRequested();
    }

    private void checkTextFieldWasRequested() {
        softly.assertThat(dictRepository.requestLog)
                .containsExactly(new DictRequest(DictDataCategory.CAMPAIGN_TIME_TARGET, 123L));
    }

    private void checkTextFieldWasNotRequested() {
        softly.assertThat(dictRepository.requestLog)
                .describedAs("No need for requesting text field because it was not changed")
                .hasSize(0);
    }

    public static class TestTable extends TableImpl {
        public static final TestTable TABLE = new TestTable();
        public final TableField<CampaignsRecord, Long> cid =
                createField("cid", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");
        public final TableField<CampaignsRecord, String> timeTarget =
                createField("timeTarget", org.jooq.impl.SQLDataType.CLOB.nullable(true), this, "");
        public final TableField<CampaignsRecord, Long> timeZoneId =
                createField("timezone_id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

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
