package ru.yandex.market.logistics.lom.jobs.executor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.RangeLimit;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.operations.Operation;
import ru.yandex.inside.yt.kosher.operations.YtOperations;
import ru.yandex.inside.yt.kosher.operations.specs.MergeSpec;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.configuration.properties.YtProperties;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.entity.ydb.WaybillSegmentStatusHistoryYdb;
import ru.yandex.market.logistics.lom.repository.ydb.WaybillSegmentStatusHistoryYdbRepository;
import ru.yandex.market.logistics.lom.repository.ydb.description.WaybillSegmentStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.utils.HashUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.misc.io.RuntimeIoException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.YtUtils.buildMapNode;
import static ru.yandex.market.logistics.lom.utils.YtUtils.getIterator;

@DisplayName("Тесты джобы архивирования истории статусов сегментов из YDB в YT")
@DatabaseSetup("/jobs/executor/waybillSegmentStatusHistoryYtArchiverExecutor/before/default_ydb_id.xml")
class WaybillSegmentStatusHistoryYtArchiverExecutorTest extends AbstractContextualYdbTest {

    private static final YPath EXPECTED_TABLE_PATH =
        YPath.simple("//home/market/testing/delivery/logistics_lom/waybill_segment_status_history_archive");
    private static final long ROWS_COUNT = 1L;

    private static final String EXPECTED_EXCEPTION_MESSAGE = "YT WRITE FAILED SUCCESSFULLY";

    @Autowired
    private WaybillSegmentStatusHistoryYtArchiverExecutor waybillSegmentStatusHistoryYtArchiverExecutor;

    @Autowired
    private WaybillSegmentStatusHistoryTableDescription waybillSegmentStatusHistoryTable;

    @Autowired
    private WaybillSegmentStatusHistoryYdbRepository waybillSegmentStatusHistoryYdbRepository;

    @Autowired
    private Yt hahnYt;

    @Autowired
    private YtTables ytTables;

    @Autowired
    private YtOperations ytOperations;

    @Autowired
    private Cypress cypress;

    private final Operation ytOperation = mock(Operation.class);

    private List<WaybillSegmentStatusHistoryYdb> ydbTestData;

    @Autowired
    private YtProperties ytProperties;

    @BeforeEach
    void setup() {
        when(hahnYt.tables()).thenReturn(ytTables);
        when(hahnYt.operations()).thenReturn(ytOperations);
        when(ytOperations.mergeAndGetOp(
            MergeSpec.builder()
                .addInputTable(EXPECTED_TABLE_PATH)
                .setOutputTable(EXPECTED_TABLE_PATH)
                .setCombineChunks(true)
                .build()
        ))
            .thenReturn(ytOperation);
        when(hahnYt.cypress()).thenReturn(cypress);
    }

    @AfterEach
    void noMoreInteractionsCheck() {
        verifyNoMoreInteractions(ytTables, ytOperation, hahnYt, cypress);
    }

    @Test
    @DisplayName("Успешная архивация записей (полный батч, все записи из одного окна createdHash)")
    void archiveSuccessFullBatch() {
        when(cypress.get(EXPECTED_TABLE_PATH, List.of("row_count")))
            .thenReturn(YtUtils.rowCountNode(ROWS_COUNT));

        when(ytTables.read(
            EXPECTED_TABLE_PATH.withExact(RangeLimit.row(ROWS_COUNT - 1)),
            YTableEntryTypes.YSON
        ))
            .then(invocation -> getIterator(Set.of(
                buildYTreeMapNode(1, Instant.parse("2020-09-18T03:00:38Z").toEpochMilli())
            )));

        ydbTestData = prepareYdbTestData(1L, 2L, 3L, 4L, 5L);
        waybillSegmentStatusHistoryYdbRepository.saveAll(ydbTestData);

        waybillSegmentStatusHistoryYtArchiverExecutor.doJob(null);

        verifyWriteRows(makeExpectedRows(1, 3));
        verifyMergeChunks();
    }

    @Test
    @DisplayName("Успешная архивация записей (не хватает записей на полный батч в текущем окне)")
    void archiveSuccessPartialBatch() {
        when(cypress.get(EXPECTED_TABLE_PATH, List.of("row_count")))
            .thenReturn(YtUtils.rowCountNode(ROWS_COUNT));

        when(ytTables.read(
            EXPECTED_TABLE_PATH.withExact(RangeLimit.row(ROWS_COUNT - 1)),
            YTableEntryTypes.YSON
        ))
            .then(invocation -> getIterator(Set.of(
                buildYTreeMapNode(3, Instant.parse("2020-09-18T13:00:38Z").toEpochMilli())
            )));
        ydbTestData = prepareYdbTestData(1L, 2L, 3L, 4L, 5L);
        waybillSegmentStatusHistoryYdbRepository.saveAll(ydbTestData);

        waybillSegmentStatusHistoryYtArchiverExecutor.doJob(null);
        verifyWriteRows(makeExpectedRows(3, 3));
        verifyMergeChunks();
    }

    @Test
    @DisplayName("Архивация записей из YDB в YT при отсутствии записи в internal_variables")
    void archiveNoInternalVariable() {
        when(cypress.get(EXPECTED_TABLE_PATH, List.of("row_count")))
            .thenReturn(YtUtils.rowCountNode(0));

        ydbTestData = prepareYdbTestData(1L, 2L, 3L, 4L, 5L);
        waybillSegmentStatusHistoryYdbRepository.saveAll(ydbTestData);

        waybillSegmentStatusHistoryYtArchiverExecutor.doJob(null);
        verifyWriteRowsWhenNoRowsBefore(makeExpectedRows(0, 2));
        verifyMergeChunks();
    }

    @Test
    @DisplayName("Все записи уже заархивированы (пустой батч, без сдвига даты). Не пытаемся вычислить следующий батч")
    void emptyBatchNoDateChange() {
        int currentBatchCount = ytProperties.getWaybillSegmentStatusHistoryArchive().getBatchCount();
        ytProperties.getWaybillSegmentStatusHistoryArchive().setBatchCount(2);
        when(cypress.get(EXPECTED_TABLE_PATH, List.of("row_count")))
            .thenReturn(YtUtils.rowCountNode(ROWS_COUNT));

        when(ytTables.read(
            EXPECTED_TABLE_PATH.withExact(RangeLimit.row(ROWS_COUNT - 1)),
            YTableEntryTypes.YSON
        ))
            .then(invocation -> getIterator(Set.of(
                buildYTreeMapNode(5, Instant.parse("2020-09-18T05:00:38Z").toEpochMilli())
            )));

        ydbTestData = prepareYdbTestData(1L, 2L, 3L, 4L, 5L);
        waybillSegmentStatusHistoryYdbRepository.saveAll(ydbTestData);

        waybillSegmentStatusHistoryYtArchiverExecutor.doJob(null);

        verify(hahnYt).cypress();
        verify(cypress).get(EXPECTED_TABLE_PATH, List.of("row_count"));
        verify(hahnYt).tables();
        verify(ytTables).read(
            EXPECTED_TABLE_PATH.withExact(RangeLimit.row(ROWS_COUNT - 1)),
            YTableEntryTypes.YSON
        );
        verifyMergeChunks();
        ytProperties.getWaybillSegmentStatusHistoryArchive().setBatchCount(currentBatchCount);
    }

    @Test
    @DisplayName("Все записи за текущую дату заархивированы (пустой батч, сдвиг даты на день)")
    void emptyBatchWithDateChange() {
        when(cypress.get(EXPECTED_TABLE_PATH, List.of("row_count")))
            .thenReturn(YtUtils.rowCountNode(ROWS_COUNT));

        when(ytTables.read(
            EXPECTED_TABLE_PATH.withExact(RangeLimit.row(ROWS_COUNT - 1)),
            YTableEntryTypes.YSON
        ))
            .then(invocation -> getIterator(Set.of(
                buildYTreeMapNode(4, Instant.parse("2020-09-18T21:00:38Z").toEpochMilli())
            )));
        ydbTestData = prepareYdbTestData(1L, 2L, 3L, 4L, 5L);
        waybillSegmentStatusHistoryYdbRepository.saveAll(ydbTestData);

        waybillSegmentStatusHistoryYtArchiverExecutor.doJob(null);
        verifyWriteRows(makeExpectedRows(4, 4));
        verifyMergeChunks();
    }

    @Test
    @DisplayName("Все записи за текущую дату заархивированы (пустой батч, сдвиг даты на несколько дней)")
    void emptyBatchWithShiftOnTwoDays() {
        when(cypress.get(EXPECTED_TABLE_PATH, List.of("row_count")))
            .thenReturn(YtUtils.rowCountNode(ROWS_COUNT));

        when(ytTables.read(
            EXPECTED_TABLE_PATH.withExact(RangeLimit.row(ROWS_COUNT - 1)),
            YTableEntryTypes.YSON
        ))
            .then(invocation -> getIterator(Set.of(
                buildYTreeMapNode(4, Instant.parse("2020-09-18T21:00:38Z").toEpochMilli())
            )));
        ydbTestData = prepareYdbTestData(1L, 2L, 3L, 4L, 5L);
        Instant created = Instant.parse("2020-09-20T21:00:38Z");
        ydbTestData.get(4)
            .setCreated(created)
            .setCreatedHash(HashUtils.hashInstant(created));
        waybillSegmentStatusHistoryYdbRepository.saveAll(ydbTestData);

        waybillSegmentStatusHistoryYtArchiverExecutor.doJob(null);
        verifyWriteRows(makeExpectedRows(4, 4));
        verifyMergeChunks();
    }

    @Test
    @DisplayName("Ошибка при записи в YT")
    void archiveWriteFailed() {
        when(cypress.get(EXPECTED_TABLE_PATH, List.of("row_count")))
            .thenReturn(YtUtils.rowCountNode(ROWS_COUNT));

        when(ytTables.read(
            EXPECTED_TABLE_PATH.withExact(RangeLimit.row(ROWS_COUNT - 1)),
            YTableEntryTypes.YSON
        ))
            .then(invocation -> getIterator(Set.of(
                buildYTreeMapNode(1, Instant.parse("2020-09-18T03:00:38Z").toEpochMilli())
            )));
        ydbTestData = prepareYdbTestData(1L, 2L, 3L, 4L, 5L);
        waybillSegmentStatusHistoryYdbRepository.saveAll(ydbTestData);

        List<YTreeMapNode> expectedRows = makeExpectedRows(1, 3);

        doThrow(new RuntimeIoException(EXPECTED_EXCEPTION_MESSAGE))
            .when(ytTables)
            .write(EXPECTED_TABLE_PATH.append(true), YTableEntryTypes.YSON, expectedRows);

        softly
            .assertThatThrownBy(() -> waybillSegmentStatusHistoryYtArchiverExecutor.doJob(null))
            .isInstanceOf(RuntimeIoException.class)
            .hasMessage(EXPECTED_EXCEPTION_MESSAGE);

        verifyWriteRows(expectedRows);
    }

    @Test
    @DisplayName("Архивация записей, с пропуском в значениях ID, большим чем размер батча")
    @DatabaseSetup("/jobs/executor/waybillSegmentStatusHistoryYtArchiverExecutor/before/id-gap.xml")
    void idGapGreaterThanBatchSize() {
        when(cypress.get(EXPECTED_TABLE_PATH, List.of("row_count")))
            .thenReturn(YtUtils.rowCountNode(ROWS_COUNT));

        when(ytTables.read(
            EXPECTED_TABLE_PATH.withExact(RangeLimit.row(ROWS_COUNT - 1)),
            YTableEntryTypes.YSON
        ))
            .then(invocation -> getIterator(Set.of(
                buildYTreeMapNode(1, Instant.parse("2020-09-18T03:00:38Z").toEpochMilli())
            )));
        ydbTestData = prepareYdbTestData(1L, 6L, 7L, 8L, 9L);
        waybillSegmentStatusHistoryYdbRepository.saveAll(ydbTestData);

        waybillSegmentStatusHistoryYtArchiverExecutor.doJob(null);
        verifyWriteRows(makeExpectedRows(1, 3));
        verifyMergeChunks();
    }

    @Test
    @DisplayName("Успешная архивация записей с коллизией по хэшам created (1 запись из другого окна, не архивируется)")
    void archiveSuccessHashCollision() {
        when(cypress.get(EXPECTED_TABLE_PATH, List.of("row_count")))
            .thenReturn(YtUtils.rowCountNode(ROWS_COUNT));

        when(ytTables.read(
            EXPECTED_TABLE_PATH.withExact(RangeLimit.row(ROWS_COUNT - 1)),
            YTableEntryTypes.YSON
        ))
            .then(invocation -> getIterator(Set.of(
                buildYTreeMapNode(2, Instant.parse("2020-09-18T03:00:38Z").toEpochMilli())
            )));
        ydbTestData = prepareYdbTestData(1L, 2L, 3L, 4L, 5L);
        ydbTestData.get(4).setCreatedHash(ydbTestData.get(3).getCreatedHash());
        waybillSegmentStatusHistoryYdbRepository.saveAll(ydbTestData);

        waybillSegmentStatusHistoryYtArchiverExecutor.doJob(null);

        verifyWriteRows(makeExpectedRows(2, 3));
        verifyMergeChunks();
    }

    @Nonnull
    private List<WaybillSegmentStatusHistoryYdb> prepareYdbTestData(long... ids) {
        List<WaybillSegmentStatusHistoryYdb> testData = new ArrayList<>();

        Instant date = Instant.parse("2020-09-18T07:37:35.00Z");
        long dateHash = HashUtils.hashInstant(date);

        Instant created = Instant.parse("2020-09-18T03:00:38.00Z");

        for (long id : ids) {
            long idHash = HashUtils.hashLong(id);
            long createdHash = HashUtils.hashInstant(created);

            testData.add(WaybillSegmentStatusHistoryYdb.builder()
                .orderIdHash(idHash)
                .orderId(id)
                .waybillSegmentId(id)
                .idHash(idHash)
                .id(id)
                .status(SegmentStatus.PENDING)
                .trackerCheckpointDateHash(dateHash)
                .trackerCheckpointDate(date)
                .trackerStatus("GONE")
                .createdHash(createdHash)
                .created(created)
                .trackerCheckpointIdHash(idHash)
                .trackerCheckpointId(id)
                .build()
            );

            created = created.plus(5L, ChronoUnit.HOURS);
        }

        return testData;
    }

    private void verifyWriteRowsWhenNoRowsBefore(List<YTreeMapNode> rows) {
        verify(hahnYt).cypress();
        verify(cypress).get(EXPECTED_TABLE_PATH, List.of("row_count"));
        verify(hahnYt).tables();

        verify(ytTables).write(
            EXPECTED_TABLE_PATH.append(true),
            YTableEntryTypes.YSON,
            rows
        );
    }

    private void verifyWriteRows(List<YTreeMapNode> rows) {
        verify(hahnYt).cypress();
        verify(cypress).get(EXPECTED_TABLE_PATH, List.of("row_count"));

        verify(hahnYt, times(2)).tables();

        verify(ytTables).read(
            EXPECTED_TABLE_PATH.withExact(RangeLimit.row(ROWS_COUNT - 1)),
            YTableEntryTypes.YSON
        );

        verify(ytTables).write(
            EXPECTED_TABLE_PATH.append(true),
            YTableEntryTypes.YSON,
            rows
        );
    }

    private void verifyMergeChunks() {
        verify(hahnYt).operations();

        verify(ytOperations).mergeAndGetOp(
            MergeSpec.builder()
                .addInputTable(EXPECTED_TABLE_PATH)
                .setOutputTable(EXPECTED_TABLE_PATH)
                .setCombineChunks(true)
                .build()
        );
        verify(ytOperation).await();
    }

    @Nonnull
    private List<YTreeMapNode> makeExpectedRows(int firstRowId, int lastRowId) {
        return ydbTestData.subList(firstRowId, lastRowId + 1).stream()
            .map(this::ydbModelToYTreeMapNode)
            .collect(Collectors.toList());
    }

    @Nonnull
    private YTreeMapNode ydbModelToYTreeMapNode(WaybillSegmentStatusHistoryYdb ydbModel) {
        return (YTreeMapNode) new YTreeBuilder()
            .beginMap()
            .key("order_id").value(ydbModel.getOrderId())
            .key("waybill_segment_id").value(ydbModel.getWaybillSegmentId())
            .key("id").value(ydbModel.getId())
            .key("status").value(ydbModel.getStatus().name())
            .key("tracker_checkpoint_date").value(ydbModel.getTrackerCheckpointDate().toEpochMilli())
            .key("tracker_status").value(ydbModel.getTrackerStatus())
            .key("created").value(ydbModel.getCreated().toEpochMilli())
            .key("tracker_checkpoint_id").value(ydbModel.getTrackerCheckpointId())
            .endMap().build();
    }

    @Nonnull
    private YTreeMapNode buildYTreeMapNode(long id, long created) {
        return buildMapNode(Map.of(
            "id", id,
            "created", created
        ));
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(waybillSegmentStatusHistoryTable);
    }
}
