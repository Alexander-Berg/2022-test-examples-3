package ru.yandex.market.logistics.lom.jobs.executor;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

@DisplayName("Тест логирования статистики архивирования записей waybill_segment_status_history")
class WaybillSegmentStatusHistoryArchivingStatisticsExecutorTest extends AbstractContextualTest {

    private static final String LOG_MESSAGE_TEMPLATE =
        "level=INFO\t" +
        "format=plain\t" +
        "code=COUNT_WAYBILL_SEGMENT_STATUS_HISTORY_NOT_ARCHIVED\t" +
        "payload=%d waybill_segment_status_history records not archived to %s\t" +
        "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
        "tags=WAYBILL_SEGMENT_STATUS_HISTORY_ARCHIVING_STATS\t" +
        "extra_keys=amount,type\t" +
        "extra_values=%d,%s\n";

    @Autowired
    private WaybillSegmentStatusHistoryArchivingStatisticsExecutor executor;

    @Test
    @DisplayName("Ни одной записи не заархивировано никуда, записи в waybill_segment_status_history есть")
    @DatabaseSetup("/jobs/executor/waybillSegmentStatusHistoryArchivingStatisticsExecutor/records.xml")
    void allNotArchived() {
        executor.doJob(null);

        String backLog = backLogCaptor.getResults().toString();

        softly.assertThat(backLog).contains(expectedLogMessage(5L, "YDB"));
        softly.assertThat(backLog).contains(expectedLogMessage(0L, "YT"));
    }

    @Test
    @DisplayName("Нет записей ни в waybill_segment_status_history, ни в internal_variable")
    void noRecords() {
        executor.doJob(null);

        String backLog = backLogCaptor.getResults().toString();

        softly.assertThat(backLog).contains(expectedLogMessage(0L, "YDB"));
        softly.assertThat(backLog).contains(expectedLogMessage(0L, "YT"));
    }

    @Test
    @DisplayName("Часть записей заархивирована в YDB и в YT")
    @DatabaseSetup("/jobs/executor/waybillSegmentStatusHistoryArchivingStatisticsExecutor/records.xml")
    @DatabaseSetup("/jobs/executor/waybillSegmentStatusHistoryArchivingStatisticsExecutor/partially-archived.xml")
    void partiallyArchived() {
        executor.doJob(null);

        String backLog = backLogCaptor.getResults().toString();

        softly.assertThat(backLog).contains(expectedLogMessage(1L, "YDB"));
        softly.assertThat(backLog).contains(expectedLogMessage(2L, "YT"));
    }

    @Test
    @DisplayName("Все записи заархивированы в YDB и в YT")
    @DatabaseSetup("/jobs/executor/waybillSegmentStatusHistoryArchivingStatisticsExecutor/records.xml")
    @DatabaseSetup("/jobs/executor/waybillSegmentStatusHistoryArchivingStatisticsExecutor/all-archived.xml")
    void allArchived() {
        executor.doJob(null);

        String backLog = backLogCaptor.getResults().toString();

        softly.assertThat(backLog).contains(expectedLogMessage(0L, "YDB"));
        softly.assertThat(backLog).contains(expectedLogMessage(0L, "YT"));
    }

    @Nonnull
    private String expectedLogMessage(long amount, String type) {
        return String.format(LOG_MESSAGE_TEMPLATE, amount, type, amount, type);
    }
}
