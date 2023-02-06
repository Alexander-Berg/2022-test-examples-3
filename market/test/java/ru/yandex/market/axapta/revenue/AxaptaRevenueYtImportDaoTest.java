package ru.yandex.market.axapta.revenue;

import java.time.LocalDateTime;
import java.time.YearMonth;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;

class AxaptaRevenueYtImportDaoTest extends FunctionalTest {

    @Autowired
    private AxaptaRevenueYtImportDao axaptaRevenueYtImportDao;

    @Test
    void testToYearMonth() {
        YearMonth actual = axaptaRevenueYtImportDao.toYearMonth("2020-05");

        Assert.assertEquals(YearMonth.of(2020, 5), actual);
    }

    @Test
    void testToLocalDateTime() {
        LocalDateTime actual = axaptaRevenueYtImportDao.toLocalDateTime("01.06.2020 1:20:30");
        Assert.assertEquals(LocalDateTime.of(2020, 6, 1, 1, 20, 30), actual);

        actual = axaptaRevenueYtImportDao.toLocalDateTime("01.06.2020 15:20:30");
        Assert.assertEquals(LocalDateTime.of(2020, 6, 1, 15, 20, 30), actual);
    }

    @Test
    void testGetTablesPath() {
        String actual = axaptaRevenueYtImportDao.getTablesPath("path", YearMonth.of(2020, 5));

        Assert.assertEquals("path/2020/05", actual);
    }
}
