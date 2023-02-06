package ru.yandex.direct.useractionlog.writer.generator;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.binlog.reader.EnrichedEvent;
import ru.yandex.direct.binlog.reader.EnrichedRow;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusactive;
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

import static ru.yandex.direct.dbschema.ppc.Ppc.PPC;

@ParametersAreNonnullByDefault
@SuppressWarnings("unchecked")
public class CollectionsDiffStrategyTest {
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2017, 11, 21, 0, 0);
    private static final ObjectPath.CampaignPath CAMPAIGN_PATH =
            new ObjectPath.CampaignPath(new ClientId(100500L), new CampaignId(123L));
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();
    private MemoryDictRepository dictRepository;
    private CollectionsDiffStrategy collectionsDiffStrategy = CollectionsDiffStrategy.builder()
            .withIdField(PPC.CAMPAIGNS.CID.getName())
            .withCommaDelimitedListCategory(DictDataCategory.CAMPAIGN_DONT_SHOW,
                    PPC.CAMPAIGNS.DONT_SHOW.getName())
            .withJsonListCategory(DictDataCategory.CAMPAIGN_DISABLED_SSP,
                    PPC.CAMPAIGNS.DISABLED_SSP.getName())
            .build();
    private RowProcessingStrategy rowProcessingStrategy;
    private RecordSource recordSource;

    @Before
    public void setUp() {
        dictRepository = new MemoryDictRepository();
        dictRepository.addData(DictDataCategory.CAMPAIGN_DONT_SHOW, 123L, "google.ru,yandex.ru");
        dictRepository.addData(DictDataCategory.CAMPAIGN_DISABLED_SSP, 123L, "[\"Google\", \"Yandex\"]");
        dictRepository.requestLog.clear();

        recordSource = RecordSource.makeDaemonRecordSource();
        rowProcessingStrategy = new ModularRowProcessingStrategy(recordSource,
                new DummyPathStrategy(),
                collectionsDiffStrategy);
    }

    private List<ActionLogRecord> processEvent(EnrichedEvent events) {
        BatchRowDictProcessing.Result dictResult = BatchRowDictProcessing.handleEvents(dictRepository,
                rowProcessingStrategy,
                events.rowsStream().collect(Collectors.toList()),
                new ErrorWrapper(false));
        Assertions.assertThat(dictResult.unprocessed).isEmpty();
        return dictResult.processed.stream()
                .flatMap(pair -> rowProcessingStrategy.processEvent(pair.getLeft(), pair.getRight()).stream())
                .collect(Collectors.toList());
    }

    @Test
    public void testHandleUpdateWithoutFields() {
        long eventId = 1L;
        EnrichedEvent updateEvent = BinlogFixtureGenerator.createUpdateEvent(
                DATE_TIME,
                eventId,
                PPC.CAMPAIGNS,
                ImmutableList.of(
                        Pair.of(PPC.CAMPAIGNS.CID, 123L),
                        Pair.of(PPC.CAMPAIGNS.STATUS_ACTIVE, CampaignsStatusactive.No.getLiteral())),
                ImmutableList.of(
                        Pair.of(PPC.CAMPAIGNS.CID, 123L),
                        Pair.of(PPC.CAMPAIGNS.STATUS_ACTIVE, CampaignsStatusactive.Yes.getLiteral())));
        List<ActionLogRecord> records = processEvent(updateEvent);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            ActionLogRecord record = records.get(0);
            softly.assertThat(record.getOldFields().toMap())
                    .describedAs("Old fields")
                    .containsOnly(
                            Pair.of(PPC.CAMPAIGNS.CID.getName(), "123"),
                            Pair.of(PPC.CAMPAIGNS.STATUS_ACTIVE.getName(), CampaignsStatusactive.No.getLiteral()),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
            softly.assertThat(record.getNewFields().toMap())
                    .describedAs("New fields")
                    .containsOnly(
                            Pair.of(PPC.CAMPAIGNS.CID.getName(), "123"),
                            Pair.of(PPC.CAMPAIGNS.STATUS_ACTIVE.getName(), CampaignsStatusactive.Yes.getLiteral()),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
        }
        checkDictWasNotRequested();
    }

    private void checkDictWasNotRequested() {
        softly.assertThat(dictRepository.requestLog)
                .describedAs("No need for requesting text field because it was not changed")
                .hasSize(0);
    }

    @Test
    public void testHandleUpdateWithFields() {
        long eventId = 1L;
        EnrichedEvent updateEvent = BinlogFixtureGenerator.createUpdateEvent(
                DATE_TIME,
                eventId,
                PPC.CAMPAIGNS,
                ImmutableList.of(
                        Pair.of(PPC.CAMPAIGNS.CID, 123L)),
                ImmutableList.of(
                        Pair.of(PPC.CAMPAIGNS.CID, 123L),
                        Pair.of(PPC.CAMPAIGNS.DONT_SHOW, "google.ru,mail.ru"),
                        Pair.of(PPC.CAMPAIGNS.DISABLED_SSP, "[\"Google\", \"Mail\"]")));
        List<ActionLogRecord> records = processEvent(updateEvent);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            ActionLogRecord record = records.get(0);
            softly.assertThat(record.getOldFields().toMap())
                    .describedAs("Old fields")
                    .containsOnly(
                            Pair.of(PPC.CAMPAIGNS.CID.getName(), "123"),
                            Pair.of(PPC.CAMPAIGNS.DONT_SHOW.getName(), "google.ru,yandex.ru"),
                            Pair.of(PPC.CAMPAIGNS.DISABLED_SSP.getName(), "[\"Google\", \"Yandex\"]"),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
            softly.assertThat(record.getNewFields().toMap())
                    .describedAs("New fields")
                    .containsOnly(
                            Pair.of(PPC.CAMPAIGNS.CID.getName(), "123"),
                            Pair.of(PPC.CAMPAIGNS.DONT_SHOW.getName() + "__removed", "yandex.ru"),
                            Pair.of(PPC.CAMPAIGNS.DONT_SHOW.getName() + "__added", "mail.ru"),
                            Pair.of(PPC.CAMPAIGNS.DISABLED_SSP.getName() + "__removed", "[\"Yandex\"]"),
                            Pair.of(PPC.CAMPAIGNS.DISABLED_SSP.getName() + "__added", "[\"Mail\"]"),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
        }

        softly.assertThat(dictRepository.repositoryMap)
                .containsAllEntriesOf(ImmutableMap.of(
                        new DictRequest(DictDataCategory.CAMPAIGN_DISABLED_SSP, 123L), "[\"Google\", \"Mail\"]",
                        new DictRequest(DictDataCategory.CAMPAIGN_DONT_SHOW, 123L), "google.ru,mail.ru"));
    }

    @Test
    public void testHandleUpdateNoChanges() {
        long eventId = 1L;
        EnrichedEvent updateEvent = BinlogFixtureGenerator.createUpdateEvent(
                DATE_TIME,
                eventId,
                PPC.CAMPAIGNS,
                ImmutableList.of(
                        Pair.of(PPC.CAMPAIGNS.CID, 123L),
                        Pair.of(PPC.CAMPAIGNS.DONT_SHOW, "google.ru,yandex.ru")),
                // no DISABLED_SSP here
                ImmutableList.of(
                        Pair.of(PPC.CAMPAIGNS.CID, 123L),
                        Pair.of(PPC.CAMPAIGNS.DONT_SHOW, "google.ru,yandex.ru"),
                        Pair.of(PPC.CAMPAIGNS.DISABLED_SSP, "[\"Google\", \"Yandex\"]")));
        List<ActionLogRecord> records = processEvent(updateEvent);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            ActionLogRecord record = records.get(0);
            softly.assertThat(record.getOldFields().toMap())
                    .describedAs("Old fields")
                    .containsOnly(
                            Pair.of(PPC.CAMPAIGNS.CID.getName(), "123"),
                            Pair.of(PPC.CAMPAIGNS.DONT_SHOW.getName(), "google.ru,yandex.ru"),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
            softly.assertThat(record.getNewFields().toMap())
                    .describedAs("New fields")
                    .containsOnly(
                            Pair.of(PPC.CAMPAIGNS.CID.getName(), "123"),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
        }
    }

    /**
     * При изменении записи ничего не будет изменено, если и в {@literal before},
     * и в {@literal after} присутствует текстовое поле (т.е. у текстового поля тип varchar).
     */
    @Test
    public void testHandleUpdateWithFieldsInBeforeAndAfter() {
        long eventId = 1L;
        EnrichedEvent updateEvent = BinlogFixtureGenerator.createUpdateEvent(
                DATE_TIME,
                eventId,
                PPC.CAMPAIGNS,
                ImmutableList.of(
                        Pair.of(PPC.CAMPAIGNS.CID, 123L),
                        Pair.of(PPC.CAMPAIGNS.DONT_SHOW, "google.ru,yandex.ru"),
                        Pair.of(PPC.CAMPAIGNS.DISABLED_SSP, "[\"Google\", \"Yandex\"]")),
                ImmutableList.of(
                        Pair.of(PPC.CAMPAIGNS.CID, 123L),
                        Pair.of(PPC.CAMPAIGNS.DONT_SHOW, "bing.com,yandex.ru"),
                        Pair.of(PPC.CAMPAIGNS.DISABLED_SSP, "[\"Bing\", \"Yandex\"]")));
        List<ActionLogRecord> records = processEvent(updateEvent);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            ActionLogRecord record = records.get(0);
            softly.assertThat(record.getOldFields().toMap())
                    .describedAs("Old fields")
                    .containsOnly(
                            Pair.of(PPC.CAMPAIGNS.CID.getName(), "123"),
                            Pair.of(PPC.CAMPAIGNS.DONT_SHOW.getName(), "google.ru,yandex.ru"),
                            Pair.of(PPC.CAMPAIGNS.DISABLED_SSP.getName(), "[\"Google\", \"Yandex\"]"),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
            softly.assertThat(record.getNewFields().toMap())
                    .describedAs("New fields")
                    .containsOnly(
                            Pair.of(PPC.CAMPAIGNS.CID.getName(), "123"),
                            Pair.of(PPC.CAMPAIGNS.DONT_SHOW.getName() + "__removed", "google.ru"),
                            Pair.of(PPC.CAMPAIGNS.DONT_SHOW.getName() + "__added", "bing.com"),
                            Pair.of(PPC.CAMPAIGNS.DISABLED_SSP.getName() + "__removed", "[\"Google\"]"),
                            Pair.of(PPC.CAMPAIGNS.DISABLED_SSP.getName() + "__added", "[\"Bing\"]"),
                            Pair.of(CampaignRowModel.getVersionField(), new CampaignRowModel().getVersion()));
        }
        checkDictWasNotRequested();

        softly.assertThat(dictRepository.repositoryMap)
                .containsAllEntriesOf(ImmutableMap.of(
                        new DictRequest(DictDataCategory.CAMPAIGN_DISABLED_SSP, 123L), "[\"Bing\", \"Yandex\"]",
                        new DictRequest(DictDataCategory.CAMPAIGN_DONT_SHOW, 123L), "bing.com,yandex.ru"));
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
