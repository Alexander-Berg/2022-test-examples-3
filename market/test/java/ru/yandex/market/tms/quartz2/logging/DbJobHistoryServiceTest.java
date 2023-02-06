package ru.yandex.market.tms.quartz2.logging;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.config.FunctionalTest;
import ru.yandex.market.tms.quartz2.model.JobLogEntry;

/**
 * @author r-posokhin
 */
public class DbJobHistoryServiceTest extends FunctionalTest {
    private static final int TEST_LAST_N = 10;
    private static final String TEST_JOB_NAME = "test";
    private static final String TEST_JOB_NAME_2 = "test-2";

    @Autowired
    private JobHistoryService dbJobHistoryService;

    /**
     * Метод DbJobHistoryService#getJobLogEntries не возвращает запись из лога,
     * если значение соответствует одному из шаблонов, переданных в property market.tms-core-quartz2.jobStatusesToSkip.
     * Сопоставление производится через SQL-оператор LIKE
     */
    @Test
    @DbUnitDataSet(before = "DbJobHistoryServiceJobStatusesTest.no_wildcard.csv")
    public void skippingJobStatusesNoWildcard() {
        Set<String> actual = dbJobHistoryService.getJobLogEntries(TEST_JOB_NAME, TEST_LAST_N).stream()
                .map(JobLogEntry::getJobStatus)
                .collect(Collectors.toSet());

        Set<String> expected = Sets.newHashSet("no_wildcard_other");
        Assertions.assertEquals(expected, actual);
    }


    @Test
    @DbUnitDataSet(before = "DbJobHistoryServiceJobStatusesTest.several_log_for_jobname.csv")
    public void lastRunAll() {
        List<String> jobsNames = Lists.newArrayList(TEST_JOB_NAME, TEST_JOB_NAME_2);

        Set<String> actual =
                dbJobHistoryService.getLastRunJobsLogEntries(jobsNames).stream()
                        .map(JobLogEntry::getJobStatus)
                        .collect(Collectors.toSet());

        Set<String> expected = Sets.newHashSet("ok", "ok2");
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(before = "DbJobHistoryServiceJobStatusesTest.job_log_entries_for_offset.csv")
    public void jobLogEntriesWithOffsetWithMinMaxDurationFilter() {

        Set<String> actual =
                dbJobHistoryService.getJobLogEntriesWithOffset(TEST_JOB_NAME_2, 300L, 600L, null, 0, 5).stream()
                        .map(JobLogEntry::getJobStatus)
                        .collect(Collectors.toSet());

        Set<String> expected = Sets.newHashSet("ok3", "ok4", "ok5");
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(before = "DbJobHistoryServiceJobStatusesTest.job_log_entries_for_offset.csv")
    public void jobLogEntriesWithOffsetWithoutFilters() {

        Set<String> actual =
                dbJobHistoryService.getJobLogEntriesWithOffset(TEST_JOB_NAME_2, null, null, null, 0, 5).stream()
                        .map(JobLogEntry::getJobStatus)
                        .collect(Collectors.toSet());

        Set<String> expected = Sets.newHashSet("ok2", "ok3", "ok4", "ok5", "ok6");
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(before = "DbJobHistoryServiceJobStatusesTest.job_log_entries_for_offset.csv")
    public void jobLogEntriesWithOffsetWithStatusFilterOnly() {

        Set<String> actual =
                dbJobHistoryService.getJobLogEntriesWithOffset(TEST_JOB_NAME_2, null, null, "ok4", 0, 5).stream()
                        .map(JobLogEntry::getJobStatus)
                        .collect(Collectors.toSet());

        Set<String> expected = Sets.newHashSet("ok4");
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(before = "DbJobHistoryServiceJobStatusesTest.job_log_entries_for_offset.csv")
    public void jobLogEntriesWithOffsetCheckOffsetAndLimit() {

        Set<String> actual =
                dbJobHistoryService.getJobLogEntriesWithOffset(TEST_JOB_NAME_2, 300L, 1000L, null, 2, 2).stream()
                        .map(JobLogEntry::getJobStatus)
                        .collect(Collectors.toSet());

        Set<String> expected = Sets.newHashSet("ok3", "ok4");
        Assertions.assertEquals(expected, actual);
    }


    /**
     * Согласно спецификации ANSI SQL, в шаблоне поддерживается  wildcard '_', заменяющий любой символ
     * Пример: шаблону 's_ngle' соответствует 'single', но не соответствуют 'sngle', 'siingle'.
     */
    @Test
    @DbUnitDataSet(before = "DbJobHistoryServiceJobStatusesTest.single_wildcard.csv")
    public void skippingJobStatusesSingleWildcard() {
        Set<String> actual = dbJobHistoryService.getJobLogEntries(TEST_JOB_NAME, TEST_LAST_N).stream()
                .map(JobLogEntry::getJobStatus)
                .collect(Collectors.toSet());

        Set<String> expected = Sets.newHashSet("sngle", "siingle");
        Assertions.assertEquals(expected, actual);
    }

    /**
     * Согласно спецификации ANSI SQL, в шаблоне поддерживается  wildcard '_', заменяющий любой символ
     * Пример: шаблону 'mu%ple' соответствуют значения 'multiple', 'multple' и др.
     */
    @Test
    @DbUnitDataSet(before = "DbJobHistoryServiceJobStatusesTest.sequence_wildcard.csv")
    public void skippingJobStatusesSequenceWildcard() {
        Set<String> actual = dbJobHistoryService.getJobLogEntries(TEST_JOB_NAME, TEST_LAST_N).stream()
                .map(JobLogEntry::getJobStatus)
                .collect(Collectors.toSet());

        Set<String> expected = Sets.newHashSet("multiple_other");
        Assertions.assertEquals(expected, actual);
    }


    /**
     * У каждого из шаблонов игнорируются лидирующие и замыкающие пробелы
     */
    @Test
    @DbUnitDataSet(before = "DbJobHistoryServiceJobStatusesTest.space_trimmed.csv")
    public void skippingJobStatusesSpaceTrimmed() {
        Set<String> actual = dbJobHistoryService.getJobLogEntries(TEST_JOB_NAME, TEST_LAST_N).stream()
                .map(JobLogEntry::getJobStatus)
                .collect(Collectors.toSet());

        Set<String> expected = Sets.newHashSet("check_space_other");
        Assertions.assertEquals(expected, actual);
    }
}
