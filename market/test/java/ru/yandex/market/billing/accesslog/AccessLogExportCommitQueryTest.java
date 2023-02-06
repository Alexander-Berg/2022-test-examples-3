package ru.yandex.market.billing.accesslog;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.common.repository.GenericRepository;

/**
 * @author zoom
 */
class AccessLogExportCommitQueryTest extends FunctionalTest {

    @Autowired
    private GenericRepository repository;

    @Test
    @DbUnitDataSet(
            after = "AccessLogExportCommitQueryTest.shouldInsert.after.csv",
            type = DataSetType.SINGLE_CSV
    )
    void shouldInsert() {
        repository.execute(
                new AccessLogExportCommitQuery(
                        LocalDate.of(2017, Month.MAY, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        LocalDate.of(2017, Month.MAY, 2),
                        1,
                        2,
                        "3",
                        4
                )
        );
    }

}
