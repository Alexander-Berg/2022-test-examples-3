package ru.yandex.market.billing.report.fulfillment.supply;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.report.fulfillment.supply.exception.YtExportException;
import ru.yandex.market.billing.report.fulfillment.supply.model.ModifiedBillingDate;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.billing.report.fulfillment.supply.matchers.StocksBySupplyReportDtoMatchers.hasBillingTimestamp;
import static ru.yandex.market.billing.report.fulfillment.supply.matchers.StocksBySupplyReportDtoMatchers.hasShopSku;
import static ru.yandex.market.billing.report.fulfillment.supply.matchers.StocksBySupplyReportDtoMatchers.hasSupplierId;
import static ru.yandex.market.billing.report.fulfillment.supply.matchers.StocksBySupplyReportDtoMatchers.hasSupplyId;
import static ru.yandex.market.billing.report.fulfillment.supply.matchers.StocksBySupplyReportDtoMatchers.hasWeight;

@ExtendWith(MockitoExtension.class)
class StocksBySupplyReportExportServiceTest extends FunctionalTest {

    @Autowired
    private StocksBySupplyReportDao stocksBySupplyReportDao;

    @Mock
    private StocksBySupplyReportYTExportService ytExportServiceMock;

    private StocksBySupplyReportExportService stocksBySupplyReportExportService;

    @BeforeEach
    void setup() {
        stocksBySupplyReportExportService = new StocksBySupplyReportExportService(
                stocksBySupplyReportDao,
                ytExportServiceMock
        );
    }

    @Test
    @DbUnitDataSet(before = "db/StocksBySupplyReportExportServiceTest.findModifiedBillingDates.csv")
    void testFindModifiedBillingDates() {
        List<ModifiedBillingDate> modifiedBillingDates = stocksBySupplyReportExportService.findModifiedBillingDates(
                LocalDateTime.of(2021, 9, 5, 10, 0)
        );

        assertThat(modifiedBillingDates)
                .containsExactly(
                        new ModifiedBillingDate(
                                LocalDate.of(2021, 9, 1),
                                LocalDateTime.of(2021, 9, 7, 10, 0)
                        ),
                        new ModifiedBillingDate(
                                LocalDate.of(2021, 9, 2),
                                LocalDateTime.of(2021, 9, 7, 10, 20)
                        )
                );
    }

    @Test
    @DbUnitDataSet(before = "db/StocksBySupplyReportExportServiceTest.findModifiedBillingDates.csv")
    void testFindEmptyModifiedBillingDates() {
        List<ModifiedBillingDate> modifiedBillingDates = stocksBySupplyReportExportService.findModifiedBillingDates(
                LocalDateTime.of(2021, 9, 7, 10, 20)
        );
        assertThat(modifiedBillingDates).isEmpty();
    }

    @Test
    @DbUnitDataSet(before = "db/StocksBySupplyReportExportServiceTest.exportReportEntities.csv")
    void testExportReportEntities() throws YtExportException {
        LocalDate billedDate = LocalDate.of(2021, 9, 5);
        stocksBySupplyReportExportService.exportBilledReport(billedDate);

        Mockito.verify(ytExportServiceMock)
                .export(argThat(arg -> {
                            MatcherAssert.assertThat(
                                    arg,
                                    contains(
                                            Matchers.allOf(
                                                    hasSupplierId(10313962L),
                                                    hasSupplyId(364865L),
                                                    hasShopSku("check.100256331726"),
                                                    hasBillingTimestamp(LocalDate.of(2021, 9, 5)),
                                                    hasWeight(new BigDecimal("0.100"))
                                            ),
                                            Matchers.allOf(
                                                    hasSupplierId(10313962L),
                                                    hasSupplyId(364865L),
                                                    hasShopSku("check.100439187061"),
                                                    hasBillingTimestamp(LocalDate.of(2021, 9, 5)),
                                                    hasWeight(new BigDecimal("0.600"))
                                            )
                                    ));
                            return true;
                        }),
                        eq(billedDate)
                );
    }
}
