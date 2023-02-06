package ru.yandex.market.billing.report.fulfillment.supply;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.report.fulfillment.supply.exception.YtExportException;
import ru.yandex.market.billing.report.fulfillment.supply.exception.YtReportDataExportException;
import ru.yandex.market.billing.report.fulfillment.supply.model.ModifiedBillingDate;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class StocksBySupplyReportExportExecutorTest extends FunctionalTest {

    @Mock
    private StocksBySupplyReportExportService exportServiceMock;
    @Autowired
    private EnvironmentService environmentService;

    private StocksBySupplyReportExportExecutor exportExecutor;

    @BeforeEach
    void setup() {
        exportExecutor = new StocksBySupplyReportExportExecutor(environmentService, exportServiceMock);
    }

    @Test
    @DbUnitDataSet(
            before = "db/StocksBySupplyReportExportExecutorTest.export.before.csv",
            after = "db/StocksBySupplyReportExportExecutorTest.export.after.csv"
    )
    void testExport() throws YtExportException {
        List<ModifiedBillingDate> dateToExport = List.of(
                new ModifiedBillingDate(
                        LocalDate.of(2021, 10, 1),
                        LocalDateTime.of(2021, 10, 2, 8, 20)
                ),
                new ModifiedBillingDate(
                        LocalDate.of(2021, 10, 2),
                        LocalDateTime.of(2021, 10, 2, 10, 20, 30)
                )
        );

        Mockito.when(exportServiceMock
                .findModifiedBillingDates(
                        eq(LocalDateTime.of(2021, 10, 2, 8, 15, 30))
                )
        ).thenReturn(dateToExport);

        exportExecutor.doJob();

        ArgumentCaptor<LocalDate> dateArgumentCaptor = ArgumentCaptor.forClass(LocalDate.class);

        Mockito.verify(exportServiceMock, times(2))
                .exportBilledReport(dateArgumentCaptor.capture());

        List<LocalDate> expected =
                dateToExport.stream()
                        .map(ModifiedBillingDate::getBillingDate)
                        .collect(Collectors.toList());

        assertThat(dateArgumentCaptor.getAllValues())
                .containsExactlyElementsOf(expected);
    }

    @Test
    @DbUnitDataSet(
            before = "db/StocksBySupplyReportExportExecutorTest.export.before.csv",
            after = "db/StocksBySupplyReportExportExecutorTest.export.before.csv"
    )
    void testNoDatesToExport() {
        Mockito.when(exportServiceMock.findModifiedBillingDates(any()))
                .thenReturn(List.of());

        exportExecutor.doJob();
        Mockito.verify(exportServiceMock)
                .findModifiedBillingDates(any());
        Mockito.verifyNoMoreInteractions(exportServiceMock);
    }

    @Test
    void testNoLastExportDate() {
        assertThatCode(exportExecutor::doJob)
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Does not have last export date in EnvironmentService");
    }

    @Test
    @DbUnitDataSet(
            before = "db/StocksBySupplyReportExportExecutorTest.export.before.csv",
            after = "db/StocksBySupplyReportExportExecutorTest.export.before.csv"
    )
    void testSomeClusterRaiseException() {
        Mockito.doThrow(YtExportException.class)
                .when(exportServiceMock)
                .exportBilledReport(any());

        Mockito.when(exportServiceMock
                .findModifiedBillingDates(any())
        ).thenReturn(List.of(
                new ModifiedBillingDate(
                        LocalDate.of(2021, 10, 1),
                        LocalDateTime.of(2021, 10, 2, 8, 20)
                )
        ));

        //  Экспорт сделали, но не сдвинули дату
        assertThatCode(() -> exportExecutor.doJob())
                .doesNotThrowAnyException();
    }

    @Test
    @DbUnitDataSet(
            before = "db/StocksBySupplyReportExportExecutorTest.export.before.csv",
            after = "db/StocksBySupplyReportExportExecutorTest.export.before.csv"
    )
    void testAllClusterRaiseException() {
        Mockito.doThrow(YtReportDataExportException.class)
                .when(exportServiceMock)
                .exportBilledReport(any());

        Mockito.when(exportServiceMock
                .findModifiedBillingDates(any())
        ).thenReturn(List.of(
                new ModifiedBillingDate(
                        LocalDate.of(2021, 10, 1),
                        LocalDateTime.of(2021, 10, 2, 8, 20)
                )
        ));

        //  Экспорт не произошел, выкидываем ошибку
        assertThatCode(() -> exportExecutor.doJob())
                .isExactlyInstanceOf(YtReportDataExportException.class);
    }
}
