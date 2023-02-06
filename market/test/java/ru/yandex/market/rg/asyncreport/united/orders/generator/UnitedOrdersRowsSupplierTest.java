package ru.yandex.market.rg.asyncreport.united.orders.generator;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.delivery.DeliveryInfoService;
import ru.yandex.market.core.express.ExpressDeliveryBillingDao;
import ru.yandex.market.core.feed.supplier.report.UnitedReportsInformationService;
import ru.yandex.market.core.fulfillment.report.generator.OrdersReportCommissionsService;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.order.CpaOrderStatusHistoryDao;
import ru.yandex.market.core.order.OrderItemTransactionService;
import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.core.order.OrdersInfoService;
import ru.yandex.market.core.order.ParcelService;
import ru.yandex.market.core.order.ReceiptItemDao;
import ru.yandex.market.core.order.resupply.ResupplyOrderDao;
import ru.yandex.market.core.partner.PartnerCommonInfoService;
import ru.yandex.market.core.partner.PartnerNameHelper;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.contract.PartnerContractDao;
import ru.yandex.market.core.partner.contract.PartnerContractService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.supplier.SupplierExposedActService;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.rg.asyncreport.united.orders.UnitedOrdersParams;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест на {@link UnitedOrdersRowsSupplier}
 */
@DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.common.csv")
public class UnitedOrdersRowsSupplierTest extends FunctionalTest {
    private static final Instant DATE_2015_11_26 = DateUtil.asDate(LocalDate.of(2015, 11, 26)).toInstant();
    private static final Instant DATE_2022_11_28 = DateUtil.asDate(LocalDate.of(2022, 11, 28)).toInstant();
    private static final int BUSINESS_ID = 10614662;
    private static final long ORDER_ID = 1898862L;

    private static final UnitedOrdersParams DATE_PARAMS =
            new UnitedOrdersParams(BUSINESS_ID, DATE_2015_11_26, DATE_2022_11_28);
    private static final UnitedOrdersParams ORDER_PARAMS =
            new UnitedOrdersParams(BUSINESS_ID, ORDER_ID);


    @Autowired
    OrderItemTransactionService orderItemTransactionService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ReceiptItemDao receiptItemDao;
    @Autowired
    private DeliveryInfoService deliveryInfoService;
    @Autowired
    private PartnerContractDao supplierContractDao;
    @Autowired
    private RegionService regionService;
    @Autowired
    private CpaOrderStatusHistoryDao cpaOrderStatusHistoryDao;
    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;
    @Autowired
    private ResupplyOrderDao resupplyOrderDao;
    @Autowired
    private OrdersInfoService ordersInfoService;
    @Autowired
    private SupplierExposedActService supplierExposedActService;
    @Autowired
    private ParcelService parcelService;
    @Autowired
    private BusinessService businessService;
    @Autowired
    private PartnerContractService supplierContactService;
    @Autowired
    private PartnerPlacementProgramService partnerPlacementProgramService;
    @Autowired
    private ExpressDeliveryBillingDao expressDeliveryBillingDao;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private PartnerNameHelper partnerNameHelper;
    @Autowired
    private SupplierService supplierService;
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private PartnerCommonInfoService partnerCommonInfoService;

    private UnitedOrdersRowsSupplier unitedOrdersRowsSupplier;
    private ObjectMapper objectMapper;
    private DefaultPrettyPrinter printer;

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
                        supplierContactService,
                        businessService,
                        partnerPlacementProgramService,
                        partnerNameHelper,
                        supplierService,
                        campaignService,
                        partnerCommonInfoService,
                        environmentService);
        unitedOrdersRowsSupplier = new UnitedOrdersRowsSupplier(
                orderService,
                orderItemTransactionService,
                receiptItemDao,
                deliveryInfoService,
                supplierContractDao,
                regionService,
                cpaOrderStatusHistoryDao,
                partnerTypeAwareService,
                resupplyOrderDao,
                ordersInfoService,
                mockCommissionService(),
                supplierExposedActService,
                parcelService,
                unitedReportsInformationService,
                expressDeliveryBillingDao,
                environmentService
        );
    }

    @Test
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.dropshipTest.csv")
    void dropshipTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.dropshipTest.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.dropshipTest.services.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.dropshipWithResupplyTest.csv")
    void dropshipWithResupplyTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.dropshipWithResupplyTest.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.dropshipWithResupplyTest.services.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.allSuppliers.csv")
    void allSuppliersTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.allSuppliers.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.allSuppliers.services.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.basicTest.csv")
    void basicTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.basicTest.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.basicTest.services.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.crossdockWithResupplyTest.csv")
    void crossdockWithResuppliesTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.crossdockWithResupplyTest.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.crossdockWithResupplyTest.services.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.clonedSuppliers.csv")
    void clonedSuppliersTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.clonedSuppliers.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.clonedSuppliers.services.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.changedStatusTimeMatters.csv")
    void changedStatusTimeMattersTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.changedStatusTimeMatters.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.changedStatusTimeMatters.services.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.ordersWithoutPayments.csv")
    void explicitNoPaymentsCheckTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.ordersWithoutPayments.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.ordersWithoutPayments.services.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.allFields.csv")
    void allFieldsTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.allFields.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.allFields.services.expected.json");
    }

    @Test
    @DisplayName("Проверка кредитов. Добавлена проверка новой схемы выплат.")
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.credits.csv")
    void getReportDataCreditTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.credits.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.credits.services.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.allFieldsWithSpasibo.csv")
    void allFieldsWithSpasiboTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.allFieldsWithSpasibo.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.allFieldsWithSpasibo.services.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.sameOrderDuplicatedOfferName.csv")
    void sameShopSkuItemsTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.sameOrderDuplicatedOfferName.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.sameOrderDuplicatedOfferName.services.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.newFlowWithCashback.csv")
    @DisplayName("Плюсы по новой схеме правильно отображаются.")
    void test_newFlowWithCashback() throws IOException {
        UnitedOrdersParams customParams = new UnitedOrdersParams(BUSINESS_ID, DateUtil.asDate(LocalDate.of(2021, 9,
                1)).toInstant(),
                DateUtil.asDate(LocalDate.of(2021, 9, 30)).toInstant());
        assertReportData(customParams,
                "UnitedOrdersRowsSupplierTest.newFlowWithCashback.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.newFlowWithCashback.services.expected.json");
    }

    @Test
    @DisplayName("Проверка переуступки")
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.cession.csv")
    void getReportDataCessionTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.cession.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.cession.services.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.basicTest.csv")
    void singleOrderBasicTest() throws IOException {
        assertReportData(ORDER_PARAMS,
                "UnitedOrdersRowsSupplierTest.single.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.single.services.expected.json");
    }

    @Test
    @DisplayName("Проверка рассрочки")
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.installment.csv")
    void getReportDataInstallmentTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.installment.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.installment.services.expected.json");
    }

    @Test
    @DisplayName("Проверка УВ")
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.payout.csv")
    void getReportDataPayoutTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.payout.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.payout.services.expected.json");
    }

    @Test
    @DisplayName("Проверка УВ + корректировки")
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.payoutCorrection.csv")
    void getReportDataPayoutCorrectionTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.payoutCorrection.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.payoutCorrection.services.expected.json");
    }

    @Test
    @DisplayName("Проверка до УВ + корректировки")
    @DbUnitDataSet(before = "UnitedOrdersRowsSupplierTest.correction.csv")
    void getReportDataCorrectionTest() throws IOException {
        assertReportData(DATE_PARAMS,
                "UnitedOrdersRowsSupplierTest.correction.orders.expected.json",
                "UnitedOrdersRowsSupplierTest.correction.services.expected.json");
    }

    // TODO переделать на честный вызов generate()
    private void assertReportData(UnitedOrdersParams params,
                                  String pathToOrderData,
                                  String pathToServiceData) throws IOException {

        var orderData = unitedOrdersRowsSupplier.getOrderItems(params)
                .collect(Collectors.toList());
        var servicesData = unitedOrdersRowsSupplier.getOrderServiceAndMarginRow(params)
                .collect(Collectors.toList());
        String actualOrders = objectMapper.writeValueAsString(orderData);
        String actualServices = objectMapper.writeValueAsString(servicesData);

        JSONAssert.assertEquals(
                JsonTestUtil.fromJsonTemplate(getClass(), pathToOrderData).toString(),
                actualOrders,
                JSONCompareMode.NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
                JsonTestUtil.fromJsonTemplate(getClass(), pathToServiceData).toString(),
                actualServices,
                JSONCompareMode.NON_EXTENSIBLE
        );
    }

    private OrdersReportCommissionsService mockCommissionService() {
        OrdersReportCommissionsService commissionsService = mock(OrdersReportCommissionsService.class);

        Map<Long, BigDecimal> commissions = new HashMap<>();

        commissions.put(1898847L, BigDecimal.valueOf(100));
        commissions.put(1898849L, BigDecimal.valueOf(100));
        commissions.put(1898860L, BigDecimal.valueOf(100));
        commissions.put(1898861L, BigDecimal.valueOf(100));
        commissions.put(1898862L, BigDecimal.valueOf(100));
        commissions.put(5679434L, BigDecimal.valueOf(100));

        when(commissionsService.getFee(any())).thenReturn(commissions);
        when(commissionsService.getFfProcessing(any())).thenReturn(commissions);
        when(commissionsService.getInstallmentCommissions(any())).thenReturn(commissions);
        when(commissionsService.getDeliveryCommission(any())).thenReturn(commissions);
        when(commissionsService.getAgencyCommission(any())).thenReturn(commissions);
        when(commissionsService.getFfStorageBilling(any())).thenReturn(commissions);
        when(commissionsService.getFfSurplusSupply(any())).thenReturn(commissions);
        when(commissionsService.getSortingCommission(any())).thenReturn(commissions);
        when(commissionsService.getReturnStorage(any())).thenReturn(commissions);
        when(commissionsService.getDeliveryToCustomerReturn(any())).thenReturn(commissions);
        when(commissionsService.getWithdrawCommissions(any())).thenReturn(commissions);
        when(commissionsService.getLoyaltyParticipationCommission(any())).thenReturn(commissions);
        when(commissionsService.getCpaAuctionPromotionCommissions(any())).thenReturn(commissions);

        return commissionsService;
    }
}
