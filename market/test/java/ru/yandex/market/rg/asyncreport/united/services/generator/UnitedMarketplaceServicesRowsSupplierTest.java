package ru.yandex.market.rg.asyncreport.united.services.generator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
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
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.rg.asyncreport.services.MarketplaceServicesParams;
import ru.yandex.market.rg.asyncreport.united.services.UnitedMarketplaceServicesParams;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplaceAgencyCommissionRow;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplaceDeliveryRow;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplaceExpressDeliveryRow;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplaceInstallmentRow;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplaceLoyaltyRow;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplacePaidStorageRow;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplacePlacementRow;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplacePromotionRow;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplaceResupplyRow;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplaceRow;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplaceShipmentTransitWarehouseRow;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplaceSortCenterRow;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplaceSurplusOnWarehouseRow;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplaceUtilizationRow;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplaceWarehouseRow;
import ru.yandex.market.rg.asyncreport.united.services.rows.UnitedMarketplaceWithdrawalRow;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Тест на получение данных для отчета услуги
 */
@DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.common.before.csv")
class UnitedMarketplaceServicesRowsSupplierTest extends FunctionalTest {

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
    private LMSClient lmsClient;

    @Autowired
    private CpaAuctionBillingDao cpaAuctionBillingDao;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private PartnerNameHelper partnerNameHelper;

    @Autowired
    private SupplierService supplierService;

    private UnitedMarketplaceRowsSupplier rowsSupplier;
    private ObjectMapper objectMapper;
    private DefaultPrettyPrinter printer;
    private Consumer<List<UnitedMarketplaceSummaryCounter>> defaultCounter;

    @BeforeEach
    void setUp() {
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
        rowsSupplier = new UnitedMarketplaceRowsSupplier(
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

        when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(createSearchMappingsResponse());

        defaultCounter = (List<UnitedMarketplaceSummaryCounter> counters) -> {
        };
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

    private void mockLmsClient() {
        when(lmsClient.getLogisticsPoints(any())).thenReturn(List.of(
                LogisticsPointResponse.newBuilder()
                        .active(true)
                        .partnerId(145L)
                        .address(Address.newBuilder()
                                .locationId(98603)
                                .build())
                        .build()
        ));
    }

    @Test
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.before.csv")
    void testWarehouseRows() throws JsonProcessingException {
        List<UnitedMarketplaceWarehouseRow> partnerResult =
                rowsSupplier.getWarehouseRows(createPartnerParams(1, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplaceWarehouseRow> businessResult =
                rowsSupplier.getWarehouseRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testWarehouseRows.partner.expected.json");

        assertJsonEquals(businessResult, "testWarehouseRows.business.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.before.csv")
    void testPlacementRows() throws JsonProcessingException {
        List<UnitedMarketplaceSummaryCounter> counters = new ArrayList<>();
        List<UnitedMarketplacePlacementRow> partnerResult =
                rowsSupplier.getPlacementRows(
                        createPartnerParams(1, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplacePlacementRow> businessResult =
                rowsSupplier.getPlacementRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), counters::addAll);

        assertJsonEquals(partnerResult, "testPlacementRows.partner.expected.json");

        assertJsonEquals(businessResult, "testPlacementRows.business.expected.json");

        assertThat(counters).hasSize(2);
    }

    @Test
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.testShortage.before.csv")
    void testShortage() throws JsonProcessingException {
        List<UnitedMarketplacePlacementRow> partnerResult =
                rowsSupplier.getPlacementRows(
                        createPartnerParams(1, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplacePlacementRow> businessResult =
                rowsSupplier.getPlacementRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testShortage.partner.expected.json");

        assertJsonEquals(businessResult, "testShortage.business.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.before.csv")
    void testFilterByServiceTypeFfWithdraw() throws JsonProcessingException {
        List<UnitedMarketplaceWithdrawalRow> partnerResult =
                rowsSupplier.getWithdrawalRows(createPartnerParams(1, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplaceWithdrawalRow> businessResult =
                rowsSupplier.getWithdrawalRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testFilterByServiceTypeFfWithdraw.partner.expected.json");

        assertJsonEquals(businessResult, "testFilterByServiceTypeFfWithdraw.business.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.before.csv")
    void testFilterByServiceTypeAgencyCommission() throws JsonProcessingException {
        List<UnitedMarketplaceAgencyCommissionRow> partnerResult =
                rowsSupplier.getAgencyRows(createPartnerParams(1, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplaceAgencyCommissionRow> businessResult =
                rowsSupplier.getAgencyRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testFilterByServiceTypeAgencyCommission.partner.expected.json");

        assertJsonEquals(businessResult, "testFilterByServiceTypeAgencyCommission.business.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.before.csv")
    @DisplayName("Страница платное хранение для поставщика")
    void testStorageBilling() throws JsonProcessingException {
        List<UnitedMarketplacePaidStorageRow> partnerResult =
                rowsSupplier.getPaidStorageRows(createPartnerParams(1, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplacePaidStorageRow> businessResult =
                rowsSupplier.getPaidStorageRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testStorageBilling.partner.expected.json");

        assertJsonEquals(businessResult, "testStorageBilling.business.expected.json");
    }

    @DisplayName("Начисление за поставку через транзитный склад для поставщика")
    @Test
    @DbUnitDataSet(before = {
            "UnitedMarketplaceServicesGeneratorTest.before.csv",
            "UnitedMarketplaceServicesGeneratorTest.xdoc.before.csv"})
    void testXdocSupplyBilling() throws JsonProcessingException {
        List<UnitedMarketplaceShipmentTransitWarehouseRow> partnerResult =
                rowsSupplier.getTransitWarehouseRows(
                        createPartnerParams(1, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplaceShipmentTransitWarehouseRow> businessResult =
                rowsSupplier.getTransitWarehouseRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testXdocSupplyBilling.partner.expected.json");

        assertJsonEquals(businessResult, "testXdocSupplyBilling.business.expected.json");
    }

    @DisplayName("Начисление за излишки в поставках для поставщика")
    @Test
    @DbUnitDataSet(before = {"UnitedMarketplaceServicesGeneratorTest.before.csv",
            "UnitedMarketplaceServicesGeneratorTest.surplus.before.csv"})
    void testSurplusSupplyBilling() throws JsonProcessingException {
        List<UnitedMarketplaceSurplusOnWarehouseRow> partnerResult =
                rowsSupplier.getSurplusOnWarehouseRows(
                        createPartnerParams(1, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplaceSurplusOnWarehouseRow> businessResult =
                rowsSupplier.getSurplusOnWarehouseRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);


        assertJsonEquals(partnerResult, "testSurplusSupplyBilling.partner.expected.json");

        assertJsonEquals(businessResult, "testSurplusSupplyBilling.business.expected.json");
    }

    @DisplayName("Услуга сортировки заказов на Синем Маркете")
    @Test
    @DbUnitDataSet(before = {"UnitedMarketplaceServicesGeneratorTest.before.csv",
            "UnitedMarketplaceServicesGeneratorTest.sorting.before.csv"})
    void testSortingBilling() throws JsonProcessingException {
        List<UnitedMarketplaceSortCenterRow> partnerResult =
                rowsSupplier.getSortCenterRows(
                        createPartnerParams(1, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplaceSortCenterRow> businessResult =
                rowsSupplier.getSortCenterRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testSortingBilling.partner.expected.json");

        assertJsonEquals(businessResult, "testSortingBilling.business.expected.json");
    }

    @Test
    @DisplayName("Услуга хранения возвратов на Синем Маркете")
    @DbUnitDataSet(before = {"UnitedMarketplaceServicesGeneratorTest.before.csv",
            "UnitedMarketplaceServicesGeneratorTest.storage.returned.orders.before.csv"})
    void testStorageReturnedOrders() throws JsonProcessingException {
        List<UnitedMarketplaceResupplyRow> partnerResult =
                rowsSupplier.getResupplyRows(
                        createPartnerParams(1, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplaceResupplyRow> businessResult =
                rowsSupplier.getResupplyRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testStorageReturnedOrders.partner.expected.json");

        assertJsonEquals(businessResult, "testStorageReturnedOrders.business.expected.json");
    }

    // TODO Сделать основным после тестирования
    @Test
    @DisplayName("Услуга хранения возвратов на Синем Маркете новая версия")
    @DbUnitDataSet(before = {
            "UnitedMarketplaceServicesGeneratorTest.before.csv",
            "UnitedMarketplaceServicesGeneratorTest.storage.returned.orders.new.before.csv"})
    void testStorageReturnedOrdersNew() throws JsonProcessingException {
        List<UnitedMarketplaceResupplyRow> partnerResult =
                rowsSupplier.getResupplyRows(
                        createPartnerParams(1, "2016-01-12", "2022-03-05"));

        List<UnitedMarketplaceResupplyRow> businessResult =
                rowsSupplier.getResupplyRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testStorageReturnedOrders.new.partner.expected.json");

        assertJsonEquals(businessResult, "testStorageReturnedOrders.new.business.expected.json");
    }

    @Test
    @DisplayName("Услуга добровольной утилизации")
    @DbUnitDataSet(before = "UnitedMarketplcaeServicesGeneratorTest.disposal.billing.before.csv")
    void testDisposalBillingTransactions() throws JsonProcessingException {
        List<UnitedMarketplaceUtilizationRow> partnerResult =
                rowsSupplier.getUtilizationRows(
                        createPartnerParams(22, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplaceUtilizationRow> businessResult =
                rowsSupplier.getUtilizationRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testDisposalBillingTransactions.partner.expected.json");

        assertJsonEquals(businessResult, "testDisposalBillingTransactions.business.expected.json");
    }

    @Test
    @DisplayName("Обилливание экспресс заказов")
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.express.billing.before.csv")
    void testExpressOrderBillingSheet() throws JsonProcessingException {
        List<UnitedMarketplaceExpressDeliveryRow> partnerResult =
                rowsSupplier.getExpressDeliveryRows(
                        createPartnerParams(22, "2016-01-12", "2021-12-31"));

        List<UnitedMarketplaceExpressDeliveryRow> businessResult =
                rowsSupplier.getExpressDeliveryRows(
                        createBusinessParams(1000, "2016-01-12", "2021-12-31"), defaultCounter);

        assertJsonEquals(partnerResult, "testExpressOrderBillingSheet.partner.expected.json");

        assertJsonEquals(businessResult, "testExpressOrderBillingSheet.business.expected.json");
    }

    @Test
    @DisplayName("Обилливание экспресс заказов, включая, дату с которой обилливание стало поайтемным.")
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.expressOrderBillingSheetExpressItemsBilling" +
            ".before.csv")
    void testExpressOrderBillingSheetExpressItemsBilling() throws JsonProcessingException {
        List<UnitedMarketplaceExpressDeliveryRow> partnerResult =
                rowsSupplier.getExpressDeliveryRows(
                        createPartnerParams(22, "2016-01-12", "2022-01-31"));

        List<UnitedMarketplaceExpressDeliveryRow> businessResult =
                rowsSupplier.getExpressDeliveryRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-31"), defaultCounter);

        assertJsonEquals(partnerResult, "testExpressOrderBillingSheetExpressItemsBilling.partner.expected.json");

        assertJsonEquals(businessResult, "testExpressOrderBillingSheetExpressItemsBilling.business.expected.json");
    }

    @Test
    @DisplayName("Услуга хранения возвратов на Синем Маркете с корректировками")
    @DbUnitDataSet(before = {
            "UnitedMarketplaceServicesGeneratorTest.before.csv",
            "UnitedMarketplaceServicesGeneratorTest.storage.returned.orders.correction.before.csv"
    })
    void testStorageReturnedOrdersWithCorrection() throws JsonProcessingException {
        List<UnitedMarketplaceResupplyRow> partnerResult =
                rowsSupplier.getResupplyRows(
                        createPartnerParams(1, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplaceResupplyRow> businessResult =
                rowsSupplier.getResupplyRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testStorageReturnedOrdersWithCorrection.partner.expected.json");

        assertJsonEquals(businessResult, "testStorageReturnedOrdersWithCorrection.business.expected.json");
    }

    @Test
    @DisplayName("Участие в программе лояльности")
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.testLoyaltyParticipationFee.before.csv")
    void testLoyaltyParticipationFee() throws JsonProcessingException {
        List<UnitedMarketplaceLoyaltyRow> partnerResult =
                rowsSupplier.getLoyaltyRows(
                        createPartnerParams(1, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplaceLoyaltyRow> businessResult =
                rowsSupplier.getLoyaltyRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testLoyaltyParticipationFee.partner.expected.json");

        assertJsonEquals(businessResult, "testLoyaltyParticipationFee.business.expected.json");
    }

    @Test
    @DisplayName("Участие в программе лояльности округление")
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.testLoyaltyParticipationFeeRounding.before.csv")
    void testLoyaltyParticipationFeeRoundings() throws JsonProcessingException {
        List<UnitedMarketplaceLoyaltyRow> partnerResult =
                rowsSupplier.getLoyaltyRows(
                        createPartnerParams(1, "2016-01-12", "2022-01-13"));

        assertJsonEquals(partnerResult, "testLoyaltyParticipationFeeRounding.partner.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedMarketplaceServicesWithNewColumnsGeneratorTest.before.csv")
    void testAgencyCommissionWithNewColumnsAdded() throws JsonProcessingException {
        List<UnitedMarketplaceAgencyCommissionRow> partnerResult =
                rowsSupplier.getAgencyRows(createPartnerParams(1, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplaceAgencyCommissionRow> businessResult =
                rowsSupplier.getAgencyRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testAgencyCommissionWithNewColumnsAdded.partner.expected.json");

        assertJsonEquals(businessResult, "testAgencyCommissionWithNewColumnsAdded.business.expected.json");
    }

    @DisplayName("Поставщик с промо тарифами для полного месяца")
    @Test
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.promoSupplier.before.csv")
    void testPromoSupplierFullMonth() throws JsonProcessingException {
        List<UnitedMarketplacePlacementRow> partnerResult =
                rowsSupplier.getPlacementRows(
                        createPartnerParams(999, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplacePlacementRow> businessResult =
                rowsSupplier.getPlacementRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testPromoSupplierFullMonth.partner.expected.json");

        assertJsonEquals(businessResult, "testPromoSupplierFullMonth.business.expected.json");
    }

    @DisplayName("Поставщик с промо тарифами для неполного месяца")
    @Test
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.promoSupplier.before.csv")
    void testPromoSupplierNotFullMonth() throws JsonProcessingException {
        List<UnitedMarketplacePlacementRow> partnerResult =
                rowsSupplier.getPlacementRows(
                        createPartnerParams(999, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplacePlacementRow> businessResult =
                rowsSupplier.getPlacementRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testPromoSupplierNotFullMonth.partner.expected.json");

        assertJsonEquals(businessResult, "testPromoSupplierNotFullMonth.business.expected.json");
    }

    @DisplayName("Поставщик с промо тарифами")
    @Test
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.promoSupplier.before.csv")
    void testPromoSupplier() throws JsonProcessingException {
        mockLmsClient();
        List<UnitedMarketplaceDeliveryRow> partnerResult =
                rowsSupplier.getDeliveryRows(
                        createPartnerParams(999, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplaceDeliveryRow> businessResult =
                rowsSupplier.getDeliveryRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testPromoSupplier.partner.expected.json");

        assertJsonEquals(businessResult, "testPromoSupplier.business.expected.json");
    }

    @DisplayName("Поставщик с промо тарифами только в декабре")
    @Test
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.promoSupplierForDecember.before.csv")
    void testPromoSupplierForDecember() throws JsonProcessingException {
        mockLmsClient();
        List<UnitedMarketplaceDeliveryRow> partnerResult =
                rowsSupplier.getDeliveryRows(
                        createPartnerParams(999, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplaceDeliveryRow> businessResult =
                rowsSupplier.getDeliveryRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testPromoSupplierForDecember.partner.expected.json");

        assertJsonEquals(businessResult, "testPromoSupplierForDecember.business.expected.json");
    }

    @DisplayName("Поставщик с промо тарифами: услуга cancelled_order_fee")
    @Test
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.promoSupplierCancelledOrderFee.before.csv")
    void testPromoSupplierCancelledOrderFee() throws JsonProcessingException {
        mockLmsClient();
        List<UnitedMarketplaceDeliveryRow> partnerResult =
                rowsSupplier.getDeliveryRows(
                        createPartnerParams(999, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplaceDeliveryRow> businessResult =
                rowsSupplier.getDeliveryRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testPromoSupplierCancelledOrderFee.partner.expected.json");

        assertJsonEquals(businessResult, "testPromoSupplierCancelledOrderFee.business.expected.json");
    }

    @DisplayName("Поставщик с промо тарифами: услуга delivery_to_customer")
    @Test
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGeneratorTest.promoSupplierDeliveryToCustomer.before.csv")
    void testPromoSupplierDeliveryToCustomer() throws JsonProcessingException {
        mockLmsClient();
        List<UnitedMarketplaceDeliveryRow> partnerResult =
                rowsSupplier.getDeliveryRows(
                        createPartnerParams(999, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplaceDeliveryRow> businessResult =
                rowsSupplier.getDeliveryRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testPromoSupplierDeliveryToCustomer.partner.expected.json");

        assertJsonEquals(businessResult, "testPromoSupplierDeliveryToCustomer.business.expected.json");
    }

    @Test
    @DisplayName("Отчет единый аукцион для поставщиков маркетплейса")
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGenerator.orderPromotion.before.csv")
    void testOrderPromotionSupplierReportSheet() throws JsonProcessingException {
        List<UnitedMarketplacePromotionRow> partnerResult =
                rowsSupplier.getPromoRows(
                        createPartnerParams(2, "2016-01-12", "2022-01-13"));

        List<UnitedMarketplacePromotionRow> businessResult =
                rowsSupplier.getPromoRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(partnerResult, "testOrderPromotionSupplierReportSheet.partner.expected.json");

        assertJsonEquals(businessResult, "testOrderPromotionSupplierReportSheet.business.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedMarketplaceServicesGenerator.installment.before.csv")
    void testInstallment() throws JsonProcessingException {
        List<UnitedMarketplaceInstallmentRow> businessResult =
                rowsSupplier.getInstallmentRows(
                        createBusinessParams(1000, "2016-01-12", "2022-01-13"), defaultCounter);

        assertJsonEquals(businessResult, "installment.business.expected.json");
    }


    private void assertJsonEquals(List<? extends UnitedMarketplaceRow> rows, String fileWithExpected) throws JsonProcessingException {
        var actual = objectMapper.writeValueAsString(rows);
        var expected = StringTestUtil.getString(getClass(), fileWithExpected);
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
    }

    private static MarketplaceServicesParams createPartnerParams(long partnerId, String from, String to) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return new MarketplaceServicesParams(
                    partnerId,
                    DateTimes.toInstant(LocalDate.parse(from, formatter)),
                    DateTimes.toInstant(LocalDate.parse(to, formatter))
            );
        } catch (Exception ex) {
            throw new RuntimeException("Could not create report params", ex);
        }
    }

    private static UnitedMarketplaceServicesParams createBusinessParams(long businessId, String from, String to) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return new UnitedMarketplaceServicesParams(
                    businessId,
                    DateTimes.toInstant(LocalDate.parse(from, formatter)),
                    DateTimes.toInstant(LocalDate.parse(to, formatter))
            );
        } catch (Exception ex) {
            throw new RuntimeException("Could not create report params", ex);
        }
    }
}
