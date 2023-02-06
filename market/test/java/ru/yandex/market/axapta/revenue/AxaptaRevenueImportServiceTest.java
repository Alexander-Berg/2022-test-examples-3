package ru.yandex.market.axapta.revenue;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.axapta.revenue.dao.AxaptaRevenueDao;
import ru.yandex.market.core.axapta.revenue.pojo.AxaptaRevenueProduct;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class AxaptaRevenueImportServiceTest extends FunctionalTest {

    private static final YearMonth MAY_2020 = YearMonth.of(2020, 5);

    @Autowired
    private AxaptaRevenueDao axaptaRevenueDao;

    @Test
    @DbUnitDataSet(
            before = "AxaptaRevenueImportServiceTest.testImport.before.csv",
            after = "AxaptaRevenueImportServiceTest.testImport.after.csv"
    )
    void testImport() {
        AxaptaRevenueYtImportDao axaptaRevenueYtImportDao = mock(AxaptaRevenueYtImportDao.class);
        doReturn(
                List.of(
                        AxaptaRevenueProduct.builder()
                                .setId("id1")
                                .setYearMonth(MAY_2020)
                                .setClientId(101)
                                .setServiceId(701)
                                .setProductName("product1")
                                .setProductSum(-5001)
                                .setStartDatetime(LocalDateTime.of(2020, 1, 1, 15, 30, 0))
                                .setEndDatetime(LocalDateTime.of(2020, 1, 31, 15, 30, 0))
                                .setType("type1")
                                .setExportedToTlog(false)
                                .build(),
                        AxaptaRevenueProduct.builder()
                                .setId("id1")
                                .setYearMonth(MAY_2020)
                                .setClientId(101)
                                .setServiceId(701)
                                .setProductName("product2")
                                .setProductSum(5002)
                                .setStartDatetime(LocalDateTime.of(2020, 1, 1, 15, 30, 0))
                                .setEndDatetime(LocalDateTime.of(2020, 1, 31, 15, 30, 0))
                                .setType("type1")
                                .setExportedToTlog(false)
                                .build(),
                        AxaptaRevenueProduct.builder()
                                .setId("id2")
                                .setYearMonth(MAY_2020)
                                .setClientId(102)
                                .setServiceId(702)
                                .setProductName("product2")
                                .setProductSum(5003)
                                .setStartDatetime(LocalDateTime.of(2020, 3, 1, 15, 30, 0))
                                .setEndDatetime(LocalDateTime.of(2020, 3, 31, 15, 30, 0))
                                .setType("type2")
                                .setExportedToTlog(false)
                                .build()
                )
        ).when(axaptaRevenueYtImportDao).getAxaptaRevenueProducts(any(YearMonth.class));
        AxaptaRevenueImportService axaptaRevenueImportService = new AxaptaRevenueImportService(
                axaptaRevenueDao,
                axaptaRevenueYtImportDao
        );

        axaptaRevenueImportService.doImport(MAY_2020);
    }

    @Test
    void testImportFailedWhenMultipleMonths() {
        AxaptaRevenueYtImportDao axaptaRevenueYtImportDao = mock(AxaptaRevenueYtImportDao.class);
        doReturn(
                List.of(
                        AxaptaRevenueProduct.builder()
                                .setId("id1")
                                .setYearMonth(MAY_2020)
                                .setClientId(101)
                                .setServiceId(701)
                                .setProductName("product1")
                                .setProductSum(-5001)
                                .setStartDatetime(LocalDateTime.of(2020, 1, 1, 15, 30, 0))
                                .setEndDatetime(LocalDateTime.of(2020, 1, 31, 15, 30, 0))
                                .setType("type1")
                                .setExportedToTlog(false)
                                .build(),
                        AxaptaRevenueProduct.builder()
                                .setId("id1")
                                .setYearMonth(YearMonth.of(2020, 3))
                                .setClientId(101)
                                .setServiceId(701)
                                .setProductName("product2")
                                .setProductSum(5002)
                                .setStartDatetime(LocalDateTime.of(2020, 1, 1, 15, 30, 0))
                                .setEndDatetime(LocalDateTime.of(2020, 1, 31, 15, 30, 0))
                                .setType("type1")
                                .setExportedToTlog(false)
                                .build(),
                        AxaptaRevenueProduct.builder()
                                .setId("id5")
                                .setYearMonth(YearMonth.of(2020, 4))
                                .setClientId(101)
                                .setServiceId(701)
                                .setProductName("product2")
                                .setProductSum(5002)
                                .setStartDatetime(LocalDateTime.of(2020, 1, 1, 15, 30, 0))
                                .setEndDatetime(LocalDateTime.of(2020, 1, 31, 15, 30, 0))
                                .setType("type1")
                                .setExportedToTlog(false)
                                .build(),
                        AxaptaRevenueProduct.builder()
                                .setId("id2")
                                .setYearMonth(MAY_2020)
                                .setClientId(102)
                                .setServiceId(702)
                                .setProductName("product2")
                                .setProductSum(5003)
                                .setStartDatetime(LocalDateTime.of(2020, 3, 1, 15, 30, 0))
                                .setEndDatetime(LocalDateTime.of(2020, 3, 31, 15, 30, 0))
                                .setType("type2")
                                .setExportedToTlog(false)
                                .build()
                )
        ).when(axaptaRevenueYtImportDao).getAxaptaRevenueProducts(any(YearMonth.class));
        AxaptaRevenueImportService axaptaRevenueImportService = new AxaptaRevenueImportService(
                axaptaRevenueDao,
                axaptaRevenueYtImportDao
        );

        final IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> axaptaRevenueImportService.doImport(MAY_2020)
        );

        Assertions.assertEquals("Incorrect year_month for acts id: id1, id5", exception.getMessage());
    }
}
