package ru.yandex.market.rg.asyncreport.closure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
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
import ru.yandex.market.rg.asyncreport.closure.income.generator.PeriodClosureIncomeXlsReportGenerator;
import ru.yandex.market.rg.closure.PeriodClosureYtDao;
import ru.yandex.market.rg.closure.model.PeriodClosureIncomeOebsRow;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Тестирование отчёта по закрытию периода по договору на продвижение {@link PeriodClosureIncomeXlsReportGenerator}
 */
public class PeriodClosureIncomeXlsReportGeneratorTest extends FunctionalTest {

    private static final long PARTNER_ID = 101;
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

    private PeriodClosureIncomeXlsReportGenerator generator;

    @BeforeEach
    void setUp() {
        PeriodClosureYtDao dao = Mockito.mock(PeriodClosureYtDao.class);
        Mockito.when(dao.readPeriodFromOebs(any(), any(), eq(PeriodClosureIncomeOebsRow.class))).thenReturn(List.of());
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

        generator = new PeriodClosureIncomeXlsReportGenerator(periodClosureService, environmentService);
    }

    @Test
    @DbUnitDataSet(before = "PeriodClosureXlsReportGeneratorTest.before.csv")
    void generate() throws IOException, InvalidFormatException {
        PeriodClosureParams params = new PeriodClosureParams(PARTNER_ID, FEB_BEGIN);

        Path tempFilePath = Files.createTempFile("PeriodClosureIncomeXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            generator.generate(params, output);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expected = new XSSFWorkbook(
                getClass().getResourceAsStream("PeriodClosureIncomeXlsReportGeneratorTest.expected.xlsx"));

        ExcelTestUtils.assertEquals(expected, actual);
    }
}
