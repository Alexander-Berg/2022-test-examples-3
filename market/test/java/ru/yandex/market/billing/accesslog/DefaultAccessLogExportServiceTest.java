package ru.yandex.market.billing.accesslog;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.common.repository.GenericRepository;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author zoom
 */
class DefaultAccessLogExportServiceTest extends FunctionalTest {

    @Autowired
    private GenericRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DbUnitDataSet(
            after = "AccessLogExportServiceTest.shouldNotExportAnyLinesWhenAccessLogsAreAbsent.after.csv",
            type = DataSetType.SINGLE_CSV
    )
    void shouldNotExportAnyLinesWhenAccessLogsAreAbsent() {
        List<String> lines = new ArrayList<>();
        AccessLogExportService service =
                new DefaultAccessLogExportService(
                        repository,
                        3,
                        lines::add,
                        () -> LocalDate.of(2017, Month.MAY, 1),
                        Instant::now,
                        transactionTemplate
                );
        service.export();
        assertThat(lines, Matchers.empty());
    }

    @Test
    @DbUnitDataSet(
            before = "AccessLogExportServiceTest.shouldExportAllRecord.before.csv",
            after = "AccessLogExportServiceTest.shouldExportAllRecord.after.csv",
            type = DataSetType.SINGLE_CSV
    )
    void shouldExportAllRecord() {
        List<String> lines = new ArrayList<>();
        AccessLogExportService service =
                new DefaultAccessLogExportService(
                        repository,
                        3,
                        inputLines -> lines.addAll(Arrays.asList(inputLines.split("\n"))),
                        () -> LocalDate.of(2017, Month.MAY, 1),
                        () -> LocalDate.of(2017, Month.MAY, 3).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        transactionTemplate
                );
        service.export();
        Assertions.assertEquals(
                new HashSet<>(lines),
                new HashSet<>(Arrays.asList("2017-05-01\t1\t2\t3\t4", "2017-05-02\t11\t22\t33\t44"))
        );
    }

    @Test
    @DbUnitDataSet(
            before = "AccessLogExportServiceTest.shouldExportRecordWithNullVidAsEmptyString.before.csv",
            after = "AccessLogExportServiceTest.shouldExportRecordWithNullVidAsEmptyString.after.csv",
            type = DataSetType.SINGLE_CSV
    )
    void shouldExportRecordWithNullVidAsEmptyString() {
        List<String> lines = new ArrayList<>();
        AccessLogExportService service =
                new DefaultAccessLogExportService(
                        repository,
                        3,
                        inputLines -> lines.addAll(Arrays.asList(inputLines.split("\n"))),
                        () -> LocalDate.of(2017, Month.MAY, 1),
                        () -> LocalDate.of(2017, Month.MAY, 3).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        transactionTemplate
                );
        service.export();
        Assertions.assertEquals(
                new HashSet<>(Arrays.asList("2017-05-01\t1\t2\t3\t4", "2017-05-02\t11\t22\t-1\t44")),
                new HashSet<>(lines)
        );
    }

}
