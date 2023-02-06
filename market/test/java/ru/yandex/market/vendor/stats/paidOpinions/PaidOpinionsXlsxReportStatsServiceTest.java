package ru.yandex.market.vendor.stats.paidOpinions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.vendor.stats.paidOpinions.dao.PaidOpinionsStatsContext;
import ru.yandex.vendor.stats.paidOpinions.report.PaidOpinionsXlsxReportStatsService;

import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.vendor.stats.StatsTestUtils.verifyGeneratedReport;

public class PaidOpinionsXlsxReportStatsServiceTest extends AbstractVendorPartnerFunctionalTest {

    private static final String REPORT_NAME = "paid_opinions_report.xlsx";

    private final PaidOpinionsXlsxReportStatsService paidOpinionsXlsxReportStatsService;
    private final Clock clock;

    @Autowired
    public PaidOpinionsXlsxReportStatsServiceTest(
            PaidOpinionsXlsxReportStatsService paidOpinionsXlsxReportStatsService,
            Clock clock) {
        this.paidOpinionsXlsxReportStatsService = paidOpinionsXlsxReportStatsService;
        this.clock = clock;
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/paidOpinions/PaidOpinionsXlsxReportStatsServiceTest/testGenerateReport/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/paidOpinions/PaidOpinionsXlsxReportStatsServiceTest/testGenerateReport/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGenerateReport(@TempDir Path tempDir) throws Exception {

        LocalDate now = LocalDate.of(2021, 4, 25);
        doReturn(TimeUtil.toInstant(now)).when(clock).instant();

        PaidOpinionsStatsContext context = new PaidOpinionsStatsContext(101, 1);
        Path actualReportPath = tempDir.resolve(REPORT_NAME);

        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            paidOpinionsXlsxReportStatsService.generateXlsxReport(os, actualReportPath, context);
            InputStreamResource expectedReport = getInputStreamResource("/expected.xlsx");
            verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/paidOpinions/PaidOpinionsXlsxReportStatsServiceTest/testEmpty/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/paidOpinions/PaidOpinionsXlsxReportStatsServiceTest/testEmpty/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testEmpty(@TempDir Path tempDir) throws Exception {
        LocalDate now = LocalDate.of(2021, 4, 25);
        doReturn(TimeUtil.toInstant(now)).when(clock).instant();
        PaidOpinionsStatsContext context = new PaidOpinionsStatsContext(101, 1);
        Path actualReportPath = tempDir.resolve(REPORT_NAME);

        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            paidOpinionsXlsxReportStatsService.generateXlsxReport(os, actualReportPath, context);
            InputStreamResource expectedReport = getInputStreamResource("/testEmpty/expected.xlsx");
            verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/paidOpinions/PaidOpinionsXlsxReportStatsServiceTest/testGenerateWithProdData/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/paidOpinions/PaidOpinionsXlsxReportStatsServiceTest/testGenerateWithProdData/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testGenerateWithProdData(@TempDir Path tempDir) throws Exception{
        LocalDate now = LocalDate.of(2021, 4, 25);
        doReturn(TimeUtil.toInstant(now)).when(clock).instant();
        PaidOpinionsStatsContext context = new PaidOpinionsStatsContext(4698, 1);
        Path actualReportPath = tempDir.resolve(REPORT_NAME);

        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            paidOpinionsXlsxReportStatsService.generateXlsxReport(os, actualReportPath, context);
            InputStreamResource expectedReport = getInputStreamResource("/testGenerateWithProdData/expected.xlsx");
            verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
