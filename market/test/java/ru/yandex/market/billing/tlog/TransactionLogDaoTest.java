package ru.yandex.market.billing.tlog;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.tlog.model.CommonTransactionLogItem;
import ru.yandex.market.billing.tlog.yt.TransactionLogDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

/**
 * Тесты для {@link TransactionLogDao}.
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "TransactionLogDaoTest.before.csv")
class TransactionLogDaoTest extends FunctionalTest {

    @Autowired
    private TransactionLogDao transactionLogDao;

    @Test
    void getTransactionToExport() {
        List<Long> transactionIds = transactionLogDao.getTransactionLogItems(2, 3)
                .stream().map(CommonTransactionLogItem::getTransactionId)
                .collect(Collectors.toList());

        assertIterableEquals(List.of(3L, 4L, 5L), transactionIds);
    }

    @Test
    @DbUnitDataSet(after = "TransactionLogDaoTest.updateExportDate.after.csv")
    void updateExportDate() {
        transactionLogDao.updateExportDate(List.of(2L, 3L, 5L), LocalDate.of(2020, 2, 18));
    }
}
