package ru.yandex.market.rg.asyncreport.united.services.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.billing.BillingStatusService;
import ru.yandex.market.core.billing.cpa_auction.CpaAuctionBillingDao;
import ru.yandex.market.core.billing.dao.AgencyCommissionDao;
import ru.yandex.market.core.billing.dao.DbInstallmentBilledAmountDao;
import ru.yandex.market.core.billing.dao.OrderPromotionDao;
import ru.yandex.market.core.billing.dao.OrdersBillingDao;
import ru.yandex.market.core.billing.dao.SupplyShortageBilledAmountDao;
import ru.yandex.market.core.billing.fulfillment.disposal.DisposalBillingDao;
import ru.yandex.market.core.billing.fulfillment.disposal.correction.DisposalBillingCorrectionDao;
import ru.yandex.market.core.billing.fulfillment.returns_orders.report.StorageReturnsOrdersReportDao;
import ru.yandex.market.core.billing.fulfillment.surplus.SurplusSupplyBillingDao;
import ru.yandex.market.core.billing.fulfillment.xdoc.XdocSupplyBillingDao;
import ru.yandex.market.core.billing.operation.CampaignOperationDaoImpl;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.express.ExpressDeliveryBillingDao;
import ru.yandex.market.core.feed.supplier.report.UnitedReportsInformationService;
import ru.yandex.market.core.fulfillment.billing.storage.dao.StorageBillingBilledAmountDao;
import ru.yandex.market.core.fulfillment.correction.SupplyBillingCorrectionDao;
import ru.yandex.market.core.fulfillment.correction.SupplyBillingCorrectionService;
import ru.yandex.market.core.fulfillment.report.OrderReportDao;
import ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.offer.mapping.MboMappingService;
import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.core.order.shipment.FirstMileWarehouseInfoService;
import ru.yandex.market.core.partner.PartnerCommonInfoService;
import ru.yandex.market.core.partner.PartnerNameHelper;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.contract.PartnerContractService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.sorting.model.report.SortingBillingReportDao;
import ru.yandex.market.core.supplier.SupplierExposedActService;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.rg.asyncreport.united.services.UnitedMarketplaceServicesParams;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplacePlacementRow;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link UnitedMarketplaceXlsGenerator}.
 */
class UnitedMarketplaceServicesXlsReportGeneratorTest extends FunctionalTest {

    @Autowired
    private BillingStatusService billingStatusService;

    @Autowired
    private OrdersBillingDao ordersBillingDao;

    @Autowired
    private SupplyShortageBilledAmountDao supplyShortageBilledAmountDao;

    @Autowired
    private OrderReportDao orderReportDao;

    @Autowired
    private AgencyCommissionDao agencyCommissionDao;

    @Autowired
    private StorageBillingBilledAmountDao storageBillingBilledAmountDao;

    @Autowired
    private MboMappingService mboMappingService;

    @Autowired
    private SupplyBillingCorrectionService supplyBillingCorrectionService;

    @Autowired
    private XdocSupplyBillingDao xdocSupplyBillingDao;

    @Autowired
    private OrderPromotionDao orderPromotionDao;

    @Autowired
    private CampaignOperationDaoImpl campaignOperationDao;

    @Autowired
    private SurplusSupplyBillingDao surplusSupplyBillingDao;

    @Autowired
    private SupplyBillingCorrectionDao supplyBillingCorrectionDao;

    @Autowired
    private PartnerContractService supplierContractService;

    @Autowired
    private SupplierExposedActService supplierExposedActService;

    @Autowired
    private BusinessService businessService;

    @Autowired
    private PartnerCommonInfoService partnerCommonInfoService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private PartnerPlacementProgramService partnerPlacementProgramService;

    @Autowired
    private MboMappingsService patientMboMappingsService;

    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RegionService regionService;

    @Autowired
    private DbInstallmentBilledAmountDao dbInstallmentBilledAmountDao;

    @Autowired
    private FirstMileWarehouseInfoService firstMileWarehouseInfoService;

    @Autowired
    private CpaAuctionBillingDao cpaAuctionBillingDao;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private PartnerNameHelper partnerNameHelper;

    @Autowired
    private SupplierService supplierService;

    private ObjectMapper objectMapper;
    private DefaultPrettyPrinter printer;

    private UnitedMarketplaceXlsGenerator generator;

    @BeforeEach
    void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        DefaultPrettyPrinter.Indenter indenter =
                new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
        printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        UnitedReportsInformationService unitedReportsInformationService =
                new UnitedReportsInformationService(
                        supplierExposedActService,
                        supplierContractService,
                        businessService,
                        partnerPlacementProgramService,
                        partnerNameHelper,
                        supplierService,
                        campaignService,
                        partnerCommonInfoService,
                        environmentService);

        SortingBillingReportDao sortingBillingReportDao = new SortingBillingReportDao(namedParameterJdbcTemplate);
        DisposalBillingCorrectionDao disposalBillingCorrectionDao =
                new DisposalBillingCorrectionDao(namedParameterJdbcTemplate);
        DisposalBillingDao disposalBillingDao = new DisposalBillingDao(namedParameterJdbcTemplate);
        ExpressDeliveryBillingDao expressDeliveryBillingDao = new ExpressDeliveryBillingDao(namedParameterJdbcTemplate);
        StorageReturnsOrdersReportDao storageReturnsOrdersReportDao =
                new StorageReturnsOrdersReportDao(namedParameterJdbcTemplate);

        UnitedMarketplaceRowsSupplier rowsSupplier =
                new UnitedMarketplaceRowsSupplier(
                        unitedReportsInformationService,
                        billingStatusService,
                        orderService,
                        ordersBillingDao,
                        supplyShortageBilledAmountDao,
                        orderReportDao,
                        agencyCommissionDao,
                        storageBillingBilledAmountDao,
                        mboMappingService,
                        sortingBillingReportDao,
                        supplyBillingCorrectionService,
                        xdocSupplyBillingDao,
                        campaignService,
                        regionService,
                        disposalBillingDao,
                        disposalBillingCorrectionDao,
                        orderPromotionDao,
                        campaignOperationDao,
                        surplusSupplyBillingDao,
                        supplyBillingCorrectionDao,
                        storageReturnsOrdersReportDao,
                        expressDeliveryBillingDao,
                        partnerTypeAwareService,
                        dbInstallmentBilledAmountDao,
                        firstMileWarehouseInfoService,
                        cpaAuctionBillingDao,
                        environmentService);

        generator = new UnitedMarketplaceXlsGenerator(rowsSupplier, environmentService);

        when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(createSearchMappingsResponse());
    }

    @Test
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.before.csv")
    void checkSummary() throws IOException, InvalidFormatException {

        Path tempFilePath = Files.createTempFile("UnitedMarketplaceGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            generator.generate(createBusinessParams(1000, "2018-01-12", "2019-01-13"), output);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expectedXls =
                new XSSFWorkbook(getClass().getResourceAsStream("UnitedMarketplaceGeneratorTest.expected.xlsx"));

        Sheet summarySheet = actual.getSheetAt(0);
        Assertions.assertEquals("Сводка", summarySheet.getSheetName());
        Assertions.assertEquals("ID бизнес-аккаунта: 1000", summarySheet.getRow(2).getCell(0).getStringCellValue());
        Assertions.assertEquals("Модели работы: FBY, FBY+", summarySheet.getRow(3).getCell(0).getStringCellValue());

        Assertions.assertEquals("Итого:", summarySheet.getRow(17).getCell(0).getStringCellValue());

        ExcelTestUtils.assertEquals(expectedXls, actual, new HashSet<>(Set.of(0)));
    }

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(
                        "excelBusinessTest.json",
                        "UnitedMarketplaceServicesGeneratorTest.expected.xlsx")
        );
    }

    @MethodSource("args")
    @ParameterizedTest
    void testXls(String ordersPathToData,
                 String expectedFilePath) throws IOException, InvalidFormatException {
        List<UnitedMarketplacePlacementRow> expected = objectMapper.readValue(
                StringTestUtil.getString(getClass(), ordersPathToData),
                new TypeReference<List<UnitedMarketplacePlacementRow>>() {
                });

        UnitedMarketplaceRowsSupplier unitedMarketplaceRowsSupplier =
                mock(UnitedMarketplaceRowsSupplier.class);
        when(unitedMarketplaceRowsSupplier.getPlacementRows(any(UnitedMarketplaceServicesParams.class), any()))
                .thenReturn(expected);

        UnitedMarketplaceXlsGenerator marketplaceServicesXlsGenerator =
                new UnitedMarketplaceXlsGenerator(unitedMarketplaceRowsSupplier, environmentService);

        Path tempFilePath = Files.createTempFile("ReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            marketplaceServicesXlsGenerator.generate(
                    new UnitedMarketplaceServicesParams(1000L, Instant.now(), Instant.now()),
                    output
            );
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expectedXls = new XSSFWorkbook(getClass().getResourceAsStream(expectedFilePath));

        //Отсутствие сводки это норм, т.к. данные замоканы
        ExcelTestUtils.assertEquals(
                expectedXls,
                actual,
                new HashSet<>(Set.of(0))//игнорим первый столбец, там в сводке проставляется текущая дата отчёта
        );
    }

    private MboMappings.SearchMappingsResponse createSearchMappingsResponse() {
        return MboMappings.SearchMappingsResponse.newBuilder()
                .addOffers(createSupplierOffer("Новая вещь", "sku_1", 100L, "товар"))
                .addOffers(createSupplierOffer("Светильник", "sku_2", 150L, "Светильник_1"))
                .addOffers(createSupplierOffer("Лопата", "new_sku_1", 250L, "Лопата"))
                .addOffers(createSupplierOffer("Таз", "new_sku_2", 350L, "Таз"))
                .setTotalCount(2)
                .build();
    }

    private SupplierOffer.Offer createSupplierOffer(String title,
                                                    String shopSku,
                                                    long marketSku,
                                                    String skuName) {
        return SupplierOffer.Offer.newBuilder()
                .setTitle(title)
                .setSupplierId(1)
                .setShopSkuId(shopSku)
                .setApprovedMapping(
                        SupplierOffer.Mapping.newBuilder()
                                .setSkuId(marketSku)
                                .setSkuName(skuName)
                                .setCategoryId(90403)
                                .build()
                )
                .build();
    }

    private UnitedMarketplaceServicesParams createBusinessParams(final long businessId,
                                                                 final String from,
                                                                 final String to
    ) {
        try {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return new UnitedMarketplaceServicesParams(
                    businessId,
                    DateTimes.toInstant(LocalDate.parse(from, formatter)),
                    DateTimes.toInstant(LocalDate.parse(to, formatter))
            );
        } catch (final Exception ex) {
            throw new RuntimeException("Could not create report params", ex);
        }
    }
}
