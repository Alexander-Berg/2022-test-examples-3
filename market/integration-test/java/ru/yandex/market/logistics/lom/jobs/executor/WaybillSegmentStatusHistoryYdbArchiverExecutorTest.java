package ru.yandex.market.logistics.lom.jobs.executor;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.yandex.ydb.core.StatusCode;
import com.yandex.ydb.core.UnexpectedResultException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.converter.ydb.WaybillSegmentStatusHistoryYdbConverter;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.entity.ydb.WaybillSegmentStatusHistoryYdb;
import ru.yandex.market.logistics.lom.repository.ydb.description.WaybillSegmentStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.utils.HashUtils;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.market.ydb.integration.YdbTemplate;
import ru.yandex.market.ydb.integration.query.QFrom;
import ru.yandex.market.ydb.integration.query.QSelect;
import ru.yandex.market.ydb.integration.query.YdbQuery;
import ru.yandex.market.ydb.integration.query.YdbSelect;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@DisplayName("Тесты джобы архивирования истории статусов сегментов в YDB")
class WaybillSegmentStatusHistoryYdbArchiverExecutorTest extends AbstractContextualYdbTest {

    private static final String YDB_EXCEPTION_PREFIX = "UPDATE FAILED SUCCESSFULLY!";
    private static final String YDB_EXCEPTION_SUFFIX = ", code: ABORTED";

    @Autowired
    private WaybillSegmentStatusHistoryYdbArchiverExecutor waybillSegmentStatusHistoryYdbArchiverExecutor;

    @Autowired
    private WaybillSegmentStatusHistoryTableDescription waybillSegmentStatusHistoryTable;

    @Autowired
    private WaybillSegmentStatusHistoryYdbConverter waybillSegmentStatusHistoryYdbConverter;

    @Test
    @DisplayName("Успешное архивирование батча")
    @DatabaseSetup("/jobs/executor/waybillSegmentStatusHistoryYdbArchiverExecutor/before/records-full.xml")
    @DatabaseSetup("/jobs/executor/waybillSegmentStatusHistoryYdbArchiverExecutor/before/has-records-to-archive.xml")
    @ExpectedDatabase(
        value = "/jobs/executor/waybillSegmentStatusHistoryYdbArchiverExecutor/after/records-archived.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processBatchSuccess() {
        waybillSegmentStatusHistoryYdbArchiverExecutor.doJob(null);

        softly.assertThat(getYdbRecords()).isEqualTo(makeYdbModels(2L, 3L, 4L));
    }

    @Test
    @DisplayName("Архивирование батча при отсутствии записи в internal_variables")
    @DatabaseSetup("/jobs/executor/waybillSegmentStatusHistoryYdbArchiverExecutor/before/records-full.xml")
    @ExpectedDatabase(
        value = "/jobs/executor/waybillSegmentStatusHistoryYdbArchiverExecutor/after/internal-variables-created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processBatchNoInternalVariable() {
        waybillSegmentStatusHistoryYdbArchiverExecutor.doJob(null);

        softly.assertThat(getYdbRecords()).isEqualTo(makeYdbModels(1L, 2L, 3L));
    }

    @Test
    @DisplayName("Пустой батч (счетчик в internal_variables указывает выше максимального id)")
    @DatabaseSetup("/jobs/executor/waybillSegmentStatusHistoryYdbArchiverExecutor/before/records-full.xml")
    @DatabaseSetup("/jobs/executor/waybillSegmentStatusHistoryYdbArchiverExecutor/before/no-records-to-archive.xml")
    @ExpectedDatabase(
        value = "/jobs/executor/waybillSegmentStatusHistoryYdbArchiverExecutor/before/no-records-to-archive.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void emptyBatch() {
        waybillSegmentStatusHistoryYdbArchiverExecutor.doJob(null);

        softly.assertThat(getYdbRecords()).isEmpty();
    }

    @Test
    @DisplayName("Запись в YDB не прошла")
    @DatabaseSetup("/jobs/executor/waybillSegmentStatusHistoryYdbArchiverExecutor/before/records-full.xml")
    @DatabaseSetup("/jobs/executor/waybillSegmentStatusHistoryYdbArchiverExecutor/before/has-records-to-archive.xml")
    @ExpectedDatabase(
        value = "/jobs/executor/waybillSegmentStatusHistoryYdbArchiverExecutor/before/has-records-to-archive.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateFailed() {
        doThrow(new UnexpectedResultException(YDB_EXCEPTION_PREFIX, StatusCode.ABORTED))
            .when(ydbTemplate)
            .update(any(YdbQuery.QueryBuilder.class), any());

        softly
            .assertThatThrownBy(() -> waybillSegmentStatusHistoryYdbArchiverExecutor.doJob(null))
            .isInstanceOf(UnexpectedResultException.class)
            .hasMessage(YDB_EXCEPTION_PREFIX + YDB_EXCEPTION_SUFFIX);

        softly.assertThat(getYdbRecords()).isEmpty();
    }

    @Test
    @DisplayName("Архивация записей с null во всех nullable полях")
    @DatabaseSetup(
        "/jobs/executor/waybillSegmentStatusHistoryYdbArchiverExecutor/before/records-all-nullable-null.xml"
    )
    @DatabaseSetup("/jobs/executor/waybillSegmentStatusHistoryYdbArchiverExecutor/before/has-records-to-archive.xml")
    @ExpectedDatabase(
        value = "/jobs/executor/waybillSegmentStatusHistoryYdbArchiverExecutor/after/records-archived.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void archiveAllNullableNull() {
        waybillSegmentStatusHistoryYdbArchiverExecutor.doJob(null);

        Set<WaybillSegmentStatusHistoryYdb> ydbModels = makeYdbModels(2L, 3L, 4L);

        ydbModels.forEach(model -> {
            model.setTrackerCheckpointId(null);
            model.setTrackerCheckpointIdHash(null);
            model.setStatus(null);
            model.setTrackerStatus(null);
        });

        softly.assertThat(getYdbRecords()).isEqualTo(ydbModels);
    }

    private Set<WaybillSegmentStatusHistoryYdb> getYdbRecords() {
        return new HashSet<>(
            ydbTemplate.selectList(
                YdbSelect
                    .select(
                        QSelect
                            .of(waybillSegmentStatusHistoryTable.fields())
                            .from(QFrom.table(waybillSegmentStatusHistoryTable))
                            .select()
                    )
                    .toQuery(),
                YdbTemplate.DEFAULT_READ,
                waybillSegmentStatusHistoryYdbConverter::ydbQueryResultToYdbModels
            )
        );
    }

    private Set<WaybillSegmentStatusHistoryYdb> makeYdbModels(long... recordIds) {

        if (recordIds.length == 0) {
            return Set.of();
        }

        Instant date = Instant.parse("2020-09-18T07:37:35.00Z");
        long dateHash = HashUtils.hashInstant(date);

        Instant created = Instant.parse("2020-09-18T07:37:38.00Z");
        long createdHash = HashUtils.hashInstant(created);

        HashSet<WaybillSegmentStatusHistoryYdb> ydbModels = new HashSet<>(recordIds.length);

        for (long id : recordIds) {
            long idHash = HashUtils.hashLong(id);

            ydbModels.add(WaybillSegmentStatusHistoryYdb.builder()
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
        }

        return ydbModels;
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(waybillSegmentStatusHistoryTable);
    }
}
