package ru.yandex.market.rg.asyncreport.united.netting.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.exception.EmptyReportException;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.feed.supplier.report.UnitedReportsInformationService;
import ru.yandex.market.core.order.BankOrderInfoDao;
import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.core.partner.PartnerCommonInfoService;
import ru.yandex.market.core.partner.PartnerNameHelper;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.contract.PartnerContractService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.supplier.SupplierExposedActService;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.order.BankOrderInfoYtDao;
import ru.yandex.market.rg.asyncreport.united.netting.UnitedNettingParams;
import ru.yandex.market.rg.config.FunctionalTest;
import ru.yandex.market.yql_test.annotation.YqlTest;

import static ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils.assertEquals;

/**
 * Тест на {@link UnitedNettingXlsReportGenerator}
 */
public class UnitedNettingXlsReportGeneratorTest extends FunctionalTest {

    private final boolean allRewrite = false;

    @Autowired
    private BankOrderInfoDao bankOrderInfoDao;
    @Autowired
    private BankOrderInfoYtDao bankOrderInfoYtDao;
    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;
    @Autowired
    private SupplierExposedActService supplierExposedActService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private PartnerCommonInfoService partnerCommonInfoService;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private BusinessService businessService;
    @Autowired
    private PartnerContractService supplierContactService;
    @Autowired
    private PartnerPlacementProgramService partnerPlacementProgramService;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private PartnerNameHelper partnerNameHelper;
    @Autowired
    private SupplierService supplierService;

    private UnitedNettingXlsReportGenerator unitedNettingXlsReportGenerator;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        DefaultPrettyPrinter.Indenter indenter =
                new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);

        UnitedReportsInformationService unitedReportsInformationService =
                new UnitedReportsInformationService(
                        supplierExposedActService,
                        supplierContactService,
                        businessService,
                        partnerPlacementProgramService,
                        partnerNameHelper,
                        supplierService,
                        campaignService,
                        partnerCommonInfoService,
                        environmentService);

        UnitedNettingReportService unitedNettingReportService = new UnitedNettingReportService(
                bankOrderInfoDao,
                bankOrderInfoYtDao,
                partnerTypeAwareService,
                supplierExposedActService,
                orderService,
                partnerCommonInfoService,
                campaignService,
                unitedReportsInformationService,
                environmentService
        );
        unitedNettingXlsReportGenerator = new UnitedNettingXlsReportGenerator(unitedNettingReportService, environmentService);
    }

    @Test
    @DbUnitDataSet(before = "UnitedNettingReportGeneratorTest.before.csv")
    public void testTransactionsXls() throws IOException, InvalidFormatException {
        // MBI-72952 - этот тест попал под хф удержания, нужно будет исправить результирующий файл как только будет
        // понятно со стороны бизнеса, как отображать еще не посчитанный взаимозачёт.
        String name = "UnitedNettingReportGeneratorTestTransactions2.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("UnitedTransactionsXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            10001,
                            LocalDate.parse("2019-01-01")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            LocalDate.parse("2019-12-31")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            null,
                            null
                    ),
                    output
            );
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);

        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @Test
    @DbUnitDataSet(before = "UnitedNettingReportGeneratorTest.before.csv")
    public void testNettingXls() throws IOException, InvalidFormatException {
        // MBI-72952 - этот тест попал под хф удержания, нужно будет исправить результирующий файл как только будет
        // понятно со стороны бизнеса, как отображать еще не посчитанный взаимозачёт.
        String name = "UnitedNettingReportGeneratorTestNetting.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("UnitedNettingXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            10001,
                            null,
                            null,
                            485001L,
                            LocalDate.parse("2019-03-25")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                    ),
                    output
            );
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);

        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @Test
    @DbUnitDataSet(before = "UnitedNettingReportGeneratorTest.887584.before.csv")
    public void testNettingWithSortingXls() throws IOException, InvalidFormatException {
        String name = "UnitedNettingReportGeneratorTestNettingSorted.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("UnitedNettingXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            887584,
                            null,
                            null,
                            525068L,
                            LocalDate.parse("2021-08-18")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                    ),
                    output
            );
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);

        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @Test
    @DbUnitDataSet(before = "UnitedNettingReportGeneratorTest.887584.before.csv")
    public void testNettingWithSorting2Xls() throws IOException, InvalidFormatException {
        String name = "UnitedNettingReportGeneratorTestNettingSorted2.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("UnitedNettingXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            887584,
                            null,
                            null,
                            914373L,
                            LocalDate.parse("2021-08-27")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                    ),
                    output
            );
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);

        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @DisplayName("Генерация отчета по взаимозачету по дате с новым шаблоном")
    @DbUnitDataSet(before = {
            "UnitedNettingReportGeneratorTest.before.csv", "UnitedNettingReportGeneratorTest.newTemplate.before.csv"})
    @Test
    void testGenerateWorkbookForTransactionsNewTemplate() throws IOException, InvalidFormatException {
        // MBI-72952 - этот тест попал под хф удержания, нужно будет исправить результирующий файл как только будет
        // понятно со стороны бизнеса, как отображать еще не посчитанный взаимозачёт.
        String name = "UnitedNettingReportGeneratorTestTransactions.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("WorkbookForNettingTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream out = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(new UnitedNettingParams(
                    10001,
                    LocalDate.parse("2019-01-01")
                            .atStartOfDay()
                            .atZone(ZoneId.systemDefault())
                            .toInstant(),
                    LocalDate.parse("2019-12-31")
                            .atStartOfDay()
                            .atZone(ZoneId.systemDefault())
                            .toInstant(),
                    null,
                    null
            ), out);
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);

        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @DisplayName("Генерация отчета по взаимозачету по дате с новым шаблоном с долгом")
    @DbUnitDataSet(before = {
            "UnitedNettingReportGeneratorTest.before.csv", "UnitedNettingReportGeneratorTest.newTemplate.before.csv"})
    @Test
    void testGenerateWorkbookForTransactionsDebtNewTemplate() throws IOException, InvalidFormatException {
        // MBI-72952 - этот тест попал под хф удержания, нужно будет исправить результирующий файл как только будет
        // понятно со стороны бизнеса, как отображать еще не посчитанный взаимозачёт.
        String name = "UnitedNettingReportGeneratorTestDebt.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path expectedPath = Path.of(getClass()
                .getResource("UnitedNettingReportGeneratorTestDebt.xlsx")
                .getFile());

        Path tempFilePath = Files.createTempFile("WorkbookForNettingTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream out = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            10001,
                            LocalDate.parse("2019-01-01")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            LocalDate.parse("2019-12-31")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            null,
                            null
                    ), out);
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);

        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @DisplayName("Генерация отчета по взаимозачетам для ДСБС с новым шаблоном")
    @Test
    @DbUnitDataSet(before = {
            "UnitedNettingReportGeneratorTest.before.csv",
            "UnitedNettingReportGeneratorTest.dsbs.before.csv",
            "UnitedNettingReportGeneratorTest.newTemplate.before.csv"})
    void testGenerateNettingReportForDsbsNewTemplate() throws IOException, InvalidFormatException {
        // MBI-72952 - этот тест попал под хф удержания, нужно будет исправить результирующий файл как только будет
        // понятно со стороны бизнеса, как отображать еще не посчитанный взаимозачёт.
        String name = "UnitedNettingReportGeneratorTestDbs.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("DSBSNettingReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream out = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            10001,
                            LocalDate.parse("2019-01-01")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            LocalDate.parse("2019-12-31")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            null,
                            null
                    ), out);
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);

        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @DisplayName("В ДБС отчёт должен выгружаться только один чек по доставке")
    @Test
    @DbUnitDataSet(before = {
            "UnitedNettingReportGeneratorTest.before.csv", "UnitedNettingReportGeneratorTest.delivery.before.csv"})
    void dbsNettingReportWithDelivery() throws IOException, InvalidFormatException {
        String name = "UnitedNettingReportGeneratorTestDbsNetting.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("DSBSNettingReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream out = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            10001,
                            LocalDate.parse("2021-06-29")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            LocalDate.parse("2021-07-01")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            null,
                            null
                    ), out);
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);

        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @Test
    @DisplayName("Генерация пустого отчета неттинга")
    @DbUnitDataSet(before = {
            "UnitedNettingReportGeneratorTest.before.csv", "UnitedNettingReportGeneratorTest.dsbs.before.csv"})
    void testEmptyNettingReport() {
        Assertions.assertThatExceptionOfType(EmptyReportException.class).isThrownBy(() -> {
            Path tempFilePath = Files.createTempFile("testEmptyNettingReport", ".xlsx");
            File reportFile = new File(tempFilePath.toString());
            try (OutputStream out = new FileOutputStream(reportFile)) {
                unitedNettingXlsReportGenerator.generate(
                        new UnitedNettingParams(
                                10001,
                                null,
                                null,
                                459613L,
                                LocalDate.parse("2012-07-09")
                                        .atStartOfDay()
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()
                        ),
                        out);
            }
        });
    }

    @Test
    @DisplayName("Генерация пустого отчета по транзакциям")
    void testEmptyTransactionsReport() {
        Assertions.assertThatExceptionOfType(EmptyReportException.class).isThrownBy(() -> {
            Path tempFilePath = Files.createTempFile("testEmptyTransactionsReport", ".xlsx");
            File reportFile = new File(tempFilePath.toString());
            try (OutputStream out = new FileOutputStream(reportFile)) {
                unitedNettingXlsReportGenerator.generate(new UnitedNettingParams(
                        10001,
                        LocalDate.parse("2012-01-01")
                                .atStartOfDay()
                                .atZone(ZoneId.systemDefault())
                                .toInstant(),
                        LocalDate.parse("2012-12-31")
                                .atStartOfDay()
                                .atZone(ZoneId.systemDefault())
                                .toInstant(),
                        null,
                        null
                ), out);
            }
        });
    }

    @Test
    @DisplayName("Тест для отчета с платежами, которые идут через MBI: by date")
    @DbUnitDataSet(before = "UnitedNettingReportGeneratorTest.paymentOrder.before.csv")
    public void testTransactionsXlsPaymentOrder() throws IOException, InvalidFormatException {
        String name = "UnitedNettingReportGeneratorTestTransactionsPO.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("UnitedTransactionsXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            10001,
                            LocalDate.parse("2021-09-01")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            LocalDate.parse("2021-09-16")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            null,
                            null
                    ),
                    output
            );
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @Test
    @DisplayName("Тест для отчета с платежами, которые идут через MBI: by bank_order_id")
    @DbUnitDataSet(before = "UnitedNettingReportGeneratorTest.paymentOrder.before.csv")
    public void testNettingXlsPaymentOrder() throws IOException, InvalidFormatException {
        String name = "UnitedNettingReportGeneratorTestNettingPO.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("UnitedNettingXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            10001,
                            null,
                            null,
                            86266L,
                            LocalDate.parse("2021-09-14")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                    ),
                    output
            );
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @Test
    @DisplayName("Тест для отчета с платежами, которые идут через Биллинг, с двумя поставщиками для бизнеса: by " +
            "bank_order_id")
    @DbUnitDataSet(before = "UnitedNettingReportGeneratorTest.testNettingXlsPaymentOrderForBusiness.before.csv")
    public void testNettingXlsPaymentOrderForBusiness() throws IOException, InvalidFormatException {
        String name = "UnitedNettingReportGeneratorTest.testNettingXlsPaymentOrderForBusiness.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("UnitedNettingXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            10001,
                            null,
                            null,
                            86266L,
                            LocalDate.parse("2021-09-14")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                    ),
                    output
            );
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @DisplayName("В отчёт должна прорастать субсидия на доставку")
    @Test
    @DbUnitDataSet(before = {
            "UnitedNettingReportGeneratorTest.before.csv",
            "UnitedNettingReportGeneratorTest.delivery_subsidy.before.csv"})
    void nettingReportWithDeliverySubsidy() throws IOException, InvalidFormatException {
        String name = "UnitedNettingReportGeneratorTestDeliverySubsidyNetting.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("DeliverySubsidyNettingReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream out = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            1160713,
                            LocalDate.parse("2021-10-01")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            LocalDate.parse("2021-10-15")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            null,
                            null
                    ), out);
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @Test
    @DisplayName("Тест для отчета с платежами, которые идут через MBI: by date + корректировки 1")
    @DbUnitDataSet(before = "UnitedNettingReportGeneratorTest.paymentOrderCorr.before.csv")
    public void testTransactionsXlsPaymentOrderWithCorrection() throws IOException, InvalidFormatException {
        String name = "UnitedNettingReportGeneratorTestTransactionsPOCorr.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("UnitedTransactionsXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            10001,
                            LocalDate.parse("2021-08-01")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            LocalDate.parse("2021-09-30")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            null,
                            null
                    ),
                    output
            );
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @Test
    @DisplayName("Тест для отчета с платежами, которые идут через MBI: by date + корректировки 2")
    @DbUnitDataSet(before = "UnitedNettingReportGeneratorTest.paymentOrderCorr2.before.csv")
    public void testTransactionsXlsPaymentOrderWithCorrection2() throws IOException, InvalidFormatException {
        String name = "UnitedNettingReportGeneratorTestTransactionsPOCorr2.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("UnitedTransactionsXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            10001,
                            LocalDate.parse("2021-07-01")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            LocalDate.parse("2021-09-30")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            null,
                            null
                    ),
                    output
            );
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @Test
    @DisplayName("Тест для отчета с платежами, которые идут через MBI: by date + корректировки 3")
    @DbUnitDataSet(before = "UnitedNettingReportGeneratorTest.paymentOrderCorr3.before.csv")
    public void testTransactionsXlsPaymentOrderWithCorrection3() throws IOException, InvalidFormatException {
        String name = "UnitedNettingReportGeneratorTestTransactionsPOCorr3.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("UnitedTransactionsXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            10001,
                            LocalDate.parse("2021-08-01")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            LocalDate.parse("2021-09-30")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            null,
                            null
                    ),
                    output
            );
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @Test
    @DisplayName("Тест для отчета с платежами с типом oebs_correction")
    @DbUnitDataSet(before = "UnitedNettingReportGeneratorTest.oebsCorr.before.csv")
    public void testTransactionsXlsOebsCorrection() throws IOException, InvalidFormatException {
        String name = "UnitedNettingReportGeneratorTestTransactionsOebsCorr.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("UnitedTransactionsXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            10001,
                            LocalDate.parse("2022-01-25")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            LocalDate.parse("2022-01-27")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            null,
                            null
                    ),
                    output
            );
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @Test
    @DisplayName("Тест для отчета с платежами с типом oebs_correction")
    @DbUnitDataSet(before = "UnitedNettingReportGeneratorTest.oebsCorr.before.csv")
    public void testTransactionsXlsOebsCorrectionSingle() throws IOException, InvalidFormatException {
        String name = "UnitedNettingReportGeneratorTestTransactionsOebsCorrSingle.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(
                getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("UnitedTransactionsXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            10001,
                            null,
                            null,
                            195321L,
                            LocalDate.parse("2022-01-26")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                    ),
                    output
            );
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @Test
    @DisplayName("Тест для отчета с платежами на YT, которые идут через MBI: by date")
    @DbUnitDataSet(before = "UnitedNettingReportGeneratorTest.YT.paymentOrder.before.csv")
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mbi/reports/agg_payment_report"
            },
            csv = "UnitedNettingXlsReportGeneratorTest.paymentOrder.yql.csv",
            yqlMock = "UnitedNettingXlsReportGeneratorTest.paymentOrder.yql.mock"
    )
    public void testTransactionsXlsPaymentOrderYT() throws IOException, InvalidFormatException {
        String name = "UnitedNettingReportGeneratorTestYT.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("UnitedTransactionsXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            10001,
                            LocalDate.parse("2022-01-01")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            LocalDate.parse("2023-01-01")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant(),
                            null,
                            null
                    ),
                    output
            );
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @Test
    @DisplayName("Тест для отчета по п.п. на YT, которые идут через Биллинг, с двумя поставщиками для бизнеса: by " +
            "bank_order_id")
    @DbUnitDataSet(before = "UnitedNettingReportGeneratorTest.YT.testNettingXlsPaymentOrderForBusiness.before.csv")
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mbi/reports/agg_payment_report"
            },
            csv = "UnitedNettingXlsReportGeneratorTest.testNettingXlsPaymentOrderForBusiness.yql.csv",
            yqlMock = "UnitedNettingXlsReportGeneratorTest.testNettingXlsPaymentOrderForBusiness.yql.mock"
    )
    public void testNettingXlsPaymentOrderForBusinessYT() throws IOException, InvalidFormatException {
        String name = "UnitedNettingReportGeneratorTest.testNettingXlsPaymentOrderForBusinessYT.xlsx";
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(name));

        Path tempFilePath = Files.createTempFile("UnitedNettingXlsReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedNettingXlsReportGenerator.generate(
                    new UnitedNettingParams(
                            10001,
                            null,
                            null,
                            86266L,
                            LocalDate.parse("2022-04-22")
                                    .atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                    ),
                    output
            );
        }

        // Поменяйте флаг allRewrite для перезаписи всех тестов или false -> true для перезаписи этого конкретного теста
        if (allRewrite || false) {
            rewriteExpectedTemplate(reportFile, name);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        assertEquals(
                expected,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    private void rewriteExpectedTemplate(File generatedFile, String name) {
        try {
            Files.copy(
                    generatedFile.toPath(),
                    Path.of("pathToFiles/" + name),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Generated file cannot copied");
        }
    }
}
