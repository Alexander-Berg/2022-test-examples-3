package ru.yandex.market.rg.asyncreport.closure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feed.supplier.report.UnitedReportsInformationService;
import ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils;
import ru.yandex.market.core.order.BankOrderInfoDao;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.contract.PartnerContractService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.order.BankOrderInfoYtDao;
import ru.yandex.market.rg.asyncreport.closure.outcome.generator.PeriodClosureOutcomeXlsReportGenerator;
import ru.yandex.market.rg.closure.PeriodClosureYtDao;
import ru.yandex.market.rg.closure.model.PeriodClosureOutcomeOebsRow;
import ru.yandex.market.rg.config.FunctionalTest;
import ru.yandex.market.yql_test.annotation.YqlTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Тестирование отчёта по закрытию периода по договору на продвижение {@link PeriodClosureOutcomeXlsReportGenerator}
 */
@DbUnitDataSet(before = {
        "PeriodClosureXlsReportGeneratorTest.before.csv",
        "PeriodClosureOutcomeXlsReportGeneratorTest.before.csv"
})
public class PeriodClosureOutcomeXlsReportGeneratorTest extends FunctionalTest {

    private static final long PARTNER_ID = 101;
    private static final long CONTRACT_ID = 100001;
    private static final LocalDate FEB_BEGIN = LocalDate.of(2022, Month.FEBRUARY, 1);
    private static final Clock MARCH_CLOCK = Clock.fixed(
            LocalDate.of(2022, Month.MARCH, 3).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC
    );

    @Autowired
    private BankOrderInfoDao bankOrderInfoDao;

    @Autowired
    private BankOrderInfoYtDao bankOrderInfoYtDao;

    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;

    @Autowired
    private PartnerContractService partnerContractService;

    @Autowired
    private UnitedReportsInformationService unitedReportsInformationService;

    @Autowired
    private EnvironmentService environmentService;

    private PeriodClosureOutcomeXlsReportGenerator generator;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mbi/reports/agg_payment_report"
            },
            csv = "PeriodClosureOutcomeXlsReportGeneratorTest.yt.csv",
            yqlMock = "PeriodClosureOutcomeXlsReportGeneratorTest.generateNoDiffs.yt.mock"
    )
    void generateNoDiffs() throws IOException, InvalidFormatException {
        prepareGenerator(BigDecimal.valueOf(100), BigDecimal.valueOf(100.01), null);
        PeriodClosureParams params = new PeriodClosureParams(PARTNER_ID, FEB_BEGIN);

        Path tempFilePath = Files.createTempFile("PeriodClosureOutcomeXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            generator.generate(params, output);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expected = new XSSFWorkbook(
                getClass().getResourceAsStream("PeriodClosureOutcomeXlsReportGeneratorTest.nodiffs.expected.xlsx"));

        ExcelTestUtils.assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(before = "PeriodClosureXlsReportGeneratorTest.dbs.before.csv")
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mbi/reports/agg_payment_report"
            },
            csv = "PeriodClosureOutcomeXlsReportGeneratorTest.yt.csv",
            yqlMock = "PeriodClosureOutcomeXlsReportGeneratorTest.generateWithDiffs.yt.mock"
    )
    void generateWithDiffs() throws IOException, InvalidFormatException {
        prepareGenerator(BigDecimal.valueOf(85.5), BigDecimal.valueOf(215.15), null);
        PeriodClosureParams params = new PeriodClosureParams(PARTNER_ID, FEB_BEGIN);

        Path tempFilePath = Files.createTempFile("PeriodClosureOutcomeXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            generator.generate(params, output);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expected = new XSSFWorkbook(
                getClass().getResourceAsStream("PeriodClosureOutcomeXlsReportGeneratorTest.diffs.expected.xlsx"));

        ExcelTestUtils.assertEquals(expected, actual);
    }

    void prepareGenerator(BigDecimal subsidy, BigDecimal plus, BigDecimal delivery) {
        List<PeriodClosureOutcomeOebsRow> rows = new ArrayList<>();
        if (subsidy != null) {
            rows.add(new PeriodClosureOutcomeOebsRow()
                    .setContractId(CONTRACT_ID)
                    .setContractName("12910/12")
                    .setId(123L)
                    .setPeriodStartDate(FEB_BEGIN)
                    .setPeriodEndDate(FEB_BEGIN.plusMonths(1).minusDays(1))
                    .setRewardAmount(subsidy)
                    .setRewardWithNds(subsidy)
                    .setDistributionSetId(15129L));
        }
        if (plus != null) {
            rows.add(new PeriodClosureOutcomeOebsRow()
                    .setContractId(CONTRACT_ID)
                    .setContractName("12910/12")
                    .setId(124L)
                    .setPeriodStartDate(FEB_BEGIN)
                    .setPeriodEndDate(FEB_BEGIN.plusMonths(1).minusDays(1))
                    .setRewardAmount(plus)
                    .setRewardWithNds(plus)
                    .setDistributionSetId(21231L));
        }
        if (delivery != null) {
            rows.add(new PeriodClosureOutcomeOebsRow()
                    .setContractId(CONTRACT_ID)
                    .setContractName("12910/12")
                    .setId(124L)
                    .setPeriodStartDate(FEB_BEGIN)
                    .setPeriodEndDate(FEB_BEGIN.plusMonths(1).minusDays(1))
                    .setRewardAmount(delivery)
                    .setRewardWithNds(delivery)
                    .setDistributionSetId(23546L));
        }

        PeriodClosureYtDao dao = Mockito.mock(PeriodClosureYtDao.class);
        Mockito.when(dao.readPeriodFromOebs(any(), any(), eq(PeriodClosureOutcomeOebsRow.class))).thenReturn(rows);
        Mockito.when(dao.isPeriodExists(any(), any())).thenReturn(true);

        PeriodClosureReportService periodClosureService =
                new PeriodClosureReportService(
                        bankOrderInfoDao,
                        bankOrderInfoYtDao,
                        partnerTypeAwareService,
                        partnerContractService,
                        unitedReportsInformationService,
                        dao,
                        environmentService,
                        MARCH_CLOCK);

        generator = new PeriodClosureOutcomeXlsReportGenerator(periodClosureService, environmentService);
    }
}
