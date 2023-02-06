package ru.yandex.market.tpl.billing.dao;

import java.time.LocalDate;
import java.time.Month;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.yt.YtExportTransactionDto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Тесты для {@link ExportTransactionDao}
 */
public class ExportTransactionDaoTest extends AbstractFunctionalTest {

    @Autowired
    private ExportTransactionDao exportTransactionDao;

    @Test
    @DbUnitDataSet(before = "/database/dao/exportTransactionDao/before/testGetData.csv")
    void testGetData() {
        List<YtExportTransactionDto> transactions = exportTransactionDao.getYtData(LocalDate.of(2022, Month.APRIL, 19));
        transactions.sort(Comparator.comparingLong(YtExportTransactionDto::getBillingTransactionId));

        assertThat(transactions, hasSize(2));
        assertThat(transactions.get(0).getClientId(), is(123L));
        assertThat(transactions.get(1).getClientId(), is(456L));
    }

    @Test
    @DbUnitDataSet(before = "/database/dao/exportTransactionDao/before/testGetDataWithFilter.csv")
    void testGetDataWithFilter() {
        List<YtExportTransactionDto> transactions = exportTransactionDao.getYtData(LocalDate.of(2022, Month.APRIL, 19));
        assertThat(transactions, hasSize(1));
        assertThat(transactions.get(0).getClientId(), is(123L));
    }
}
