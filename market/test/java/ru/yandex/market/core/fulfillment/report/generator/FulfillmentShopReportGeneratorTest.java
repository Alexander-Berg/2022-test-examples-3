package ru.yandex.market.core.fulfillment.report.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.delivery.DeliveryInfoService;
import ru.yandex.market.core.express.ExpressDeliveryBillingDao;
import ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils;
import ru.yandex.market.core.fulfillment.report.excel.jxls.JxlsOrdersReportModel;
import ru.yandex.market.core.fulfillment.report.generator.data.OrdersReportRawData;
import ru.yandex.market.core.fulfillment.report.orders.OrdersReportParams;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.order.CpaOrderStatusHistoryDao;
import ru.yandex.market.core.order.OrderItemTransactionService;
import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.core.order.OrdersInfoService;
import ru.yandex.market.core.order.ParcelService;
import ru.yandex.market.core.order.ReceiptItemDao;
import ru.yandex.market.core.order.model.MbiOrder;
import ru.yandex.market.core.order.model.MbiOrderItem;
import ru.yandex.market.core.order.model.MbiOrderStatus;
import ru.yandex.market.core.order.model.OrderInfoStatus;
import ru.yandex.market.core.order.resupply.ResupplyOrderDao;
import ru.yandex.market.core.partner.PartnerCommonInfoService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.contract.PartnerContractDao;
import ru.yandex.market.core.supplier.SupplierExposedActService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasBillingPrice;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasCashbackPerItem;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasChangedStatusTime;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasCis;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasCountInDelivery;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasCreationDate;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasDeliveryServiceName;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasInitialCount;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasOfferName;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasOrderId;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasOrderNum;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasPaymentType;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasRegionToName;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasShopSku;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasSpasiboPerItem;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasStatus;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasSubsidy;
import static ru.yandex.market.core.matchers.fulfillment.report.matchers.JxlsOrderReportTransactionModelMatcher.hasAmount;
import static ru.yandex.market.core.matchers.fulfillment.report.matchers.JxlsOrderReportTransactionModelMatcher.hasBankOrderId;
import static ru.yandex.market.core.matchers.fulfillment.report.matchers.JxlsOrderReportTransactionModelMatcher.hasBankOrderTime;
import static ru.yandex.market.core.matchers.fulfillment.report.matchers.JxlsOrderReportTransactionModelMatcher.hasDateHandlingTime;
import static ru.yandex.market.core.matchers.fulfillment.report.matchers.JxlsOrderReportTransactionModelMatcher.hasPaymentIdentity;

@DbUnitDataSet(before = "FulfillmentReportGeneratorTest.common.csv")
class FulfillmentShopReportGeneratorTest extends FunctionalTest {
    private static final Instant DATE_FROM = Instant.parse("2017-11-26T12:00:00Z");
    private static final Instant DATE_TO = Instant.parse("2017-11-28T12:00:00Z");
    private static final Instant DATE_2020_11_05 = Instant.parse("2020-11-05T12:00:00Z");
    private static final Instant DATE_2020_11_20 = Instant.parse("2020-11-20T12:00:00Z");
    private static final Date DATE_2017_11_26 = DateUtil.asDate(LocalDate.of(2017, 11, 26));
    private static final Date DATE_2017_11_28 = DateUtil.asDate(LocalDate.of(2017, 11, 28));
    private static final String PAYMENT = "payment";
    private static final String SUBSIDY = "subsidy";
    private static final String SPASIBO = "spasibo";
    private static final String CASHBACK = "cashback";
    private static final String REFUND = "refund";
    private static final String SUBSIDY_REFUND = "subsidyRefund";
    private static final String SPASIBO_REFUND = "spasiboRefund";
    private static final String CASHBACK_REFUND = "cashbackRefund";
    private static final String COMPENSATION = "compensation";
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
    private EnvironmentService environmentService;
    private FulfillmentShopOrdersReportGenerator fulfillmentReportGenerator;
    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;
    @Autowired
    private ResupplyOrderDao resupplyOrderDao;
    @Autowired
    private OrdersInfoService ordersInfoService;
    @Autowired
    private SupplierExposedActService supplierExposedActService;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private PartnerCommonInfoService partnerCommonInfoService;
    @Autowired
    private ParcelService parcelService;
    @Autowired
    private ExpressDeliveryBillingDao expressDeliveryBillingDao;

    @BeforeEach
    void init() {
        fulfillmentReportGenerator = new FulfillmentShopOrdersReportGenerator(
                orderService,
                orderItemTransactionService,
                receiptItemDao,
                deliveryInfoService,
                supplierContractDao,
                regionService,
                cpaOrderStatusHistoryDao,
                environmentService,
                partnerTypeAwareService,
                resupplyOrderDao,
                ordersInfoService,
                mockCommissionService(),
                supplierExposedActService,
                campaignService,
                partnerCommonInfoService,
                parcelService,
                expressDeliveryBillingDao
        );
    }

    @Test
    @Disabled
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.basicTest.csv")
    void basicTest() throws Exception {
        Path tempFilePath = Files.createTempFile("ffReport", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            fulfillmentReportGenerator.xlsOfferReport(
                    OrdersReportParams.createOrdersReportParamsForDatesRange(
                            10245732L,
                            DATE_2017_11_26.getTime(),
                            DATE_2017_11_28.getTime(),
                            false
                    ),
                    output
            );
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expected = new XSSFWorkbook(getClass()
                .getResourceAsStream("ffReport.expected.xlsx"));

        ExcelTestUtils.assertEquals(
                expected,
                actual,
                new HashSet<>()
        );
    }

    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.NoCashbackNoSpasibo.csv")
    void noCashbackNoSpasiboTest() throws Exception {
        Path tempFilePath = Files.createTempFile("ffReport.NoCashbackNoSpasibo", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            fulfillmentReportGenerator.xlsOfferReport(
                    OrdersReportParams.createOrdersReportParamsForDatesRange(
                            591397L,
                            DATE_2020_11_05,
                            DATE_2020_11_20,
                            false
                    ),
                    output
            );
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expected = new XSSFWorkbook(getClass()
                .getResourceAsStream("ffReport.expected.NoCashbackNoSpasibo.xlsx"));

        ExcelTestUtils.assertEquals(
                expected,
                actual,
                new HashSet<>()
        );
    }

    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.hasSubsidyNoCashbackNoSpasiboTest.csv")
    void hasSubsidyNoCashbackNoSpasiboTest() throws Exception {
        Path tempFilePath = Files.createTempFile("ffReport.hasSubsidyNoCashbackNoSpasibo", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            fulfillmentReportGenerator.xlsOfferReport(
                    OrdersReportParams.createOrdersReportParamsForDatesRange(
                            472311L,
                            Instant.parse("2021-04-01T12:00:00Z"),
                            Instant.parse("2021-05-01T12:00:00Z"),
                            false
                    ),
                    output
            );
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expected = new XSSFWorkbook(getClass()
                .getResourceAsStream("ffReport.expected.hasSubsidyNoCashbackNoSpasiboTest.xlsx"));

        ExcelTestUtils.assertEquals(expected, actual, new HashSet<>());
    }

    @Test
    @Disabled
    @DbUnitDataSet(before = {"FulfillmentReportGeneratorTest.basicTest.csv", "FulfillmentReportGeneratorTest.env.csv"})
    void basicWithSummaryTest() throws Exception {
        Path tempFilePath = Files.createTempFile("ffReport.summary", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            fulfillmentReportGenerator.xlsOfferReport(
                    OrdersReportParams.createOrdersReportParamsForDatesRange(
                            10245732L,
                            DATE_FROM,
                            DATE_TO,
                            true),
                    output
            );
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expected = new XSSFWorkbook(getClass()
                .getResourceAsStream("ffReport.expected.summary.xlsx"));

        ExcelTestUtils.assertEquals(
                expected,
                actual,
                new HashSet<>()
        );
    }

    @Test
    @DbUnitDataSet(before = {"FulfillmentReportGeneratorTest.basicTest.csv", "FulfillmentReportGeneratorTest.env.csv"})
    void basicWithWithFormulasSummaryTest() throws Exception {
        Path tempFilePath = Files.createTempFile("ffReport.summary", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            fulfillmentReportGenerator.xlsOfferReport(
                    OrdersReportParams.createOrdersReportParamsForDatesRange(
                            10245732L,
                            DATE_FROM,
                            DATE_TO,
                            true),
                    output
            );
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expected = new XSSFWorkbook(getClass()
                    .getResourceAsStream("ffReport.expected.summaryWithFormulas.xlsx"));

        ExcelTestUtils.assertEquals(
                expected,
                actual,
                new HashSet<>()
        );
    }

    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.cashRefundTest.csv")
    void cashRefundTest() {
        OrdersReportRawData reportData = getReportData(14214294, 478261, false);
        Iterable<JxlsOrdersReportModel> actual = reportData.convertToModel();
        assertThat(actual, contains(
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(
                                allOf(
                                        hasOrderId(14214294L),
                                        hasOrderNum("14214294"),
                                        hasOfferName("Жесткий диск Western Digital WD Blue 1 TB (WD10EZEX)"),
                                        hasShopSku("WD10EZEX"),
                                        hasCountInDelivery(5),
                                        hasInitialCount(5),
                                        hasStatus(MbiOrderStatus.DELIVERED.getName()),
                                        hasBillingPrice(new BigDecimal("2587.00")),
                                        hasSubsidy(new BigDecimal("0.00")),
                                        hasSpasiboPerItem(null),
                                        hasCreationDate("13.02.2020"),
                                        hasChangedStatusTime("18.02.2020"),
                                        hasPaymentType("оплата при получении"),
                                        hasDeliveryServiceName("Маршрут (Котельники)"),
                                        hasRegionToName(null),
                                        hasCis(null)
                                )
                        )
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("12935.00")),
                                        hasBankOrderId("986392"),
                                        hasBankOrderTime("19.02.2020"),
                                        hasPaymentIdentity("5e4bdaaa792ab17c0931f895"),
                                        hasDateHandlingTime("18.02.2020")
                                ),
                                PAYMENT
                        )
                        .add(
                                JxlsOrdersReportModel::getRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("2587.00")),
                                        hasBankOrderId("182237"),
                                        hasBankOrderTime("12.08.2020"),
                                        hasPaymentIdentity("5f32b4cfdff13b560ff095ad"),
                                        hasDateHandlingTime("11.08.2020")
                                ),
                                REFUND
                        )
                        .add(
                                JxlsOrdersReportModel::getCompensationTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("400.00")),
                                        hasBankOrderId("182237"),
                                        hasBankOrderTime("12.08.2020"),
                                        hasPaymentIdentity("5f32b4d1792ab16a675c4fe0"),
                                        hasDateHandlingTime("11.08.2020")
                                ),
                                COMPENSATION
                        ).build()
        ));
    }

    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.cashbackRefundTest.csv")
    void cashbackRefundTest() {
        OrdersReportRawData reportData = getReportData(29981775, 649748, false);
        Iterable<JxlsOrdersReportModel> actual = reportData.convertToModel();
        assertThat(actual, contains(
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(
                                allOf(
                                        hasOrderId(29981775L),
                                        hasOrderNum("29981775"),
                                        hasOfferName("Vis-a-Vis Трусы"),
                                        hasShopSku("4605676715386"),
                                        hasCountInDelivery(1),
                                        hasInitialCount(1),
                                        hasStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY.getName()),
                                        hasBillingPrice(new BigDecimal("221.0000")),
                                        hasSubsidy(new BigDecimal("9.0000")),
                                        hasSpasiboPerItem(null),
                                        hasCashbackPerItem(new BigDecimal("9.00")),
                                        hasCreationDate("24.11.2020"),
                                        hasChangedStatusTime("29.11.2020"),
                                        hasPaymentType("предоплата"),
                                        hasDeliveryServiceName(null),
                                        hasRegionToName(null),
                                        hasCis(null)
                                )
                        )
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("203.00")),
                                        hasBankOrderId("774662"),
                                        hasBankOrderTime("26.11.2020"),
                                        hasPaymentIdentity("5fbd6bce32da83802713dee9"),
                                        hasDateHandlingTime("26.11.2020")
                                ),
                                PAYMENT
                        )
                        .add(
                                JxlsOrdersReportModel::getSubsidyTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("9.00")),
                                        hasBankOrderId("808043"),
                                        hasBankOrderTime("30.11.2020"),
                                        hasPaymentIdentity("5fc19d709066f44ff8d490d1"),
                                        hasDateHandlingTime("28.11.2020")
                                ),
                                SUBSIDY
                        )
                        .add(
                                JxlsOrdersReportModel::getCashbackTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("9.00")),
                                        hasBankOrderId("773437"),
                                        hasBankOrderTime("26.11.2020"),
                                        hasPaymentIdentity("5fbd6bceb9f8ed40bb837d17"),
                                        hasDateHandlingTime("26.11.2020")
                                ),
                                CASHBACK
                        )
                        .add(
                                JxlsOrdersReportModel::getRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("203.00")),
                                        hasBankOrderId("808145"),
                                        hasBankOrderTime("30.11.2020"),
                                        hasPaymentIdentity("5fc2ff01dbdc31c78a0f406c"),
                                        hasDateHandlingTime("29.11.2020")
                                ),
                                REFUND
                        )
                        .add(
                                JxlsOrdersReportModel::getSubsidyRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("9.00")),
                                        hasBankOrderId("808043"),
                                        hasBankOrderTime("30.11.2020"),
                                        hasPaymentIdentity("5fc2ff0004e943af298a22e3"),
                                        hasDateHandlingTime("29.11.2020")
                                ),
                                SUBSIDY_REFUND
                        )
                        .add(
                                JxlsOrdersReportModel::getCashbackRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("9.00")),
                                        hasBankOrderId("808043"),
                                        hasBankOrderTime("30.11.2020"),
                                        hasPaymentIdentity("5fc2ff01b9f8ed12b787d8b3"),
                                        hasDateHandlingTime("29.11.2020")
                                ),
                                CASHBACK_REFUND
                        ).build()
        ));
    }

    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.dropshipTest.csv")
    void dropshipTest() {
        OrdersReportRawData reportData = getReportData(DATE_2017_11_26, DATE_2017_11_28, 10245732L, false);
        List<JxlsOrdersReportModel> jxlsOrdersReportModels = iterableToCollection(reportData.convertToModel());
        assertThat(jxlsOrdersReportModels, contains(
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(
                                allOf(
                                        hasOrderId(1898847L),
                                        hasOrderNum(null),
                                        hasOfferName("Attento"),
                                        hasShopSku("cbb2282b-eea7-11e6-810b-00155d000405"),
                                        hasCountInDelivery(1),
                                        hasInitialCount(1),
                                        hasStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY.getName()),
                                        hasBillingPrice(new BigDecimal("4171.00")),
                                        hasSubsidy(new BigDecimal("371.00")),
                                        hasSpasiboPerItem(null),
                                        hasCreationDate("27.11.2017"),
                                        hasChangedStatusTime("27.12.2017"),
                                        hasPaymentType("оплата при получении"),
                                        hasDeliveryServiceName("Маршрут dropship"),
                                        hasRegionToName("Московская область")
                                )
                        )
                        .add(
                                JxlsOrdersReportModel::getSubsidyTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(2)),
                                        hasBankOrderId("77186"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c1971fa250d50910bf543"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                SUBSIDY
                        )
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(2)),
                                        hasBankOrderId("77187"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c152b795be240ef48a220"),
                                        hasDateHandlingTime("03.01.2018")
                                ),
                                PAYMENT
                        )
                        .add(
                                JxlsOrdersReportModel::getRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(3)),
                                        hasBankOrderId("1234"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c152b795be240ef48a200"),
                                        hasDateHandlingTime("04.01.2018")
                                ),
                                REFUND
                        )
                        .add(
                                JxlsOrdersReportModel::getSubsidyRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(6)),
                                        hasBankOrderId(null),
                                        hasBankOrderTime(null),
                                        hasPaymentIdentity("5a1c152b795be240ef48a002"),
                                        hasDateHandlingTime("28.11.2017")
                                ),
                                SUBSIDY_REFUND
                        )
                        .add(
                                JxlsOrdersReportModel::getCompensationTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(250)),
                                        hasBankOrderId("555777"),
                                        hasBankOrderTime("26.11.2017"),
                                        hasPaymentIdentity("compensation_payment_id"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                COMPENSATION
                        ).build()
                ,
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(1898847L),
                                hasOrderNum(null),
                                hasOfferName("Attento"),
                                hasShopSku("cbb2282b-eea7-11e6-810b-00155d000405"),
                                hasCountInDelivery(null),
                                hasInitialCount(null),
                                hasStatus(null),
                                hasBillingPrice(null),
                                hasSubsidy(null),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime(null),
                                hasPaymentType(null),
                                hasDeliveryServiceName(null),
                                hasRegionToName(null)
                        ))
                        .add(JxlsOrdersReportModel::getPaymentsTransaction, nullValue(), PAYMENT)
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), CASHBACK)
                        .add(
                                JxlsOrdersReportModel::getRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(4)),
                                        hasBankOrderId(null),
                                        hasBankOrderTime(null),
                                        hasPaymentIdentity("5a1c152b795be240ef48a000"),
                                        hasDateHandlingTime(null)
                                ),
                                REFUND
                        )
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(), SUBSIDY_REFUND)
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), CASHBACK_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build(),
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(1898847L),
                                hasOrderNum(null),
                                hasOfferName("Attento"),
                                hasShopSku("cbb2282b-eea7-11e6-810b-00155d000405"),
                                hasCountInDelivery(null),
                                hasStatus(null),
                                hasBillingPrice(null),
                                hasSubsidy(null),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime(null),
                                hasPaymentType(null),
                                hasDeliveryServiceName(null),
                                hasRegionToName(null)
                        ))
                        .add(JxlsOrdersReportModel::getPaymentsTransaction, nullValue(), PAYMENT)
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), CASHBACK)
                        .add(JxlsOrdersReportModel::getRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(5)),
                                        hasBankOrderId(null),
                                        hasBankOrderTime(null),
                                        hasPaymentIdentity("5a1c152b795be240ef48a001"),
                                        hasDateHandlingTime(null)
                                ),
                                REFUND
                        )
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(), SUBSIDY_REFUND)
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), CASHBACK_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build(),
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(1898847L),
                                hasOrderNum(null),
                                hasOfferName("offer"),
                                hasShopSku("cbb2282b-eea7-11e6-810b-00155d000405"),
                                hasCountInDelivery(1),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY.getName()),
                                hasBillingPrice(new BigDecimal("1.00")),
                                hasSubsidy(new BigDecimal("0.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime("27.12.2017"),
                                hasPaymentType("оплата при получении"),
                                hasDeliveryServiceName(null),
                                hasRegionToName("Московская область")
                        ))
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("20.02")),
                                        hasBankOrderId("77187"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c152b795be240ef48a220"),
                                        hasDateHandlingTime("03.01.2018")
                                ),
                                PAYMENT
                        )
                        .add(
                                JxlsOrdersReportModel::getSubsidyTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("20.02")),
                                        hasBankOrderId("77186"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c1971fa250d50910bf543"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                SUBSIDY
                        )
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getRefundTransaction, nullValue(), REFUND)
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(),
                                SUBSIDY_REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build(),
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(1898849L),
                                hasOrderNum(null),
                                hasOfferName("Attento"),
                                hasShopSku("cbb2282b-eea7-11e6-810b-00155d000405"),
                                hasCountInDelivery(1),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY.getName()),
                                hasBillingPrice(new BigDecimal("4171.00")),
                                hasSubsidy(new BigDecimal("371.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("25.11.2017"),
                                hasChangedStatusTime("27.12.2017"),
                                hasPaymentType("предоплата"),
                                hasDeliveryServiceName("Маршрут dropship"),
                                hasRegionToName("Санкт-Петербург и Ленинградская область")
                        ))
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(6)),
                                        hasBankOrderId("77187"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c152b795be240ef48a003"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                PAYMENT
                        )
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getRefundTransaction, nullValue(), REFUND)
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction,
                                nullValue(), SUBSIDY_REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(),
                                COMPENSATION
                        )
                        .build()
        ));
    }

    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.dropshipWithResupplyTest.csv")
    void dropshipWithResupplyTest() {
        OrdersReportRawData reportData = getReportData(DATE_2017_11_26, DATE_2017_11_28, 10245732L, false);
        List<JxlsOrdersReportModel> jxlsOrdersReportModels = iterableToCollection(reportData.convertToModel());
        assertThat(jxlsOrdersReportModels, contains(
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(
                                allOf(
                                        hasOrderId(1898847L),
                                        hasOrderNum(null),
                                        hasOfferName("Attento"),
                                        hasShopSku("cbb2282b-eea7-11e6-810b-00155d000405"),
                                        hasCountInDelivery(1),
                                        hasInitialCount(1),
                                        hasStatus(OrderInfoStatus.UNREDEEMED.getName()),
                                        hasBillingPrice(new BigDecimal("4171.00")),
                                        hasSubsidy(new BigDecimal("371.00")),
                                        hasSpasiboPerItem(null),
                                        hasCreationDate("27.11.2017"),
                                        hasChangedStatusTime("30.12.2017"),
                                        hasPaymentType("оплата при получении"),
                                        hasDeliveryServiceName("Маршрут dropship"),
                                        hasRegionToName("Московская область")
                                )
                        )
                        .add(
                                JxlsOrdersReportModel::getSubsidyTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(2)),
                                        hasBankOrderId("77186"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c1971fa250d50910bf543"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                SUBSIDY
                        )
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(2)),
                                        hasBankOrderId("77187"),
                                        hasBankOrderTime("26.03.2019"),
                                        hasPaymentIdentity("5a1c152b795be240ef48a220"),
                                        hasDateHandlingTime("03.01.2018")
                                ),
                                PAYMENT
                        )
                        .add(
                                JxlsOrdersReportModel::getRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(3)),
                                        hasBankOrderId("1234"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c152b795be240ef48a200"),
                                        hasDateHandlingTime("04.01.2018")
                                ),
                                REFUND
                        )
                        .add(
                                JxlsOrdersReportModel::getSubsidyRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(6)),
                                        hasBankOrderId(null),
                                        hasBankOrderTime(null),
                                        hasPaymentIdentity("5a1c152b795be240ef48a002"),
                                        hasDateHandlingTime("28.11.2017")
                                ),
                                SUBSIDY_REFUND
                        )
                        .add(
                                JxlsOrdersReportModel::getCompensationTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(250)),
                                        hasBankOrderId("555777"),
                                        hasBankOrderTime("26.11.2017"),
                                        hasPaymentIdentity("compensation_payment_id"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                COMPENSATION
                        ).build()
                ,
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(1898847L),
                                hasOrderNum(null),
                                hasOfferName("Attento"),
                                hasShopSku("cbb2282b-eea7-11e6-810b-00155d000405"),
                                hasCountInDelivery(null),
                                hasInitialCount(null),
                                hasStatus(null),
                                hasBillingPrice(null),
                                hasSubsidy(null),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime(null),
                                hasPaymentType(null),
                                hasDeliveryServiceName(null),
                                hasRegionToName(null)
                        ))
                        .add(JxlsOrdersReportModel::getPaymentsTransaction, nullValue(), PAYMENT)
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), CASHBACK)
                        .add(
                                JxlsOrdersReportModel::getRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(4)),
                                        hasBankOrderId(null),
                                        hasBankOrderTime(null),
                                        hasPaymentIdentity("5a1c152b795be240ef48a000"),
                                        hasDateHandlingTime(null)
                                ),
                                REFUND
                        )
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(), SUBSIDY_REFUND)
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), CASHBACK_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build(),
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(1898847L),
                                hasOrderNum(null),
                                hasOfferName("Attento"),
                                hasShopSku("cbb2282b-eea7-11e6-810b-00155d000405"),
                                hasCountInDelivery(null),
                                hasStatus(null),
                                hasBillingPrice(null),
                                hasSubsidy(null),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime(null),
                                hasPaymentType(null),
                                hasDeliveryServiceName(null),
                                hasRegionToName(null)
                        ))
                        .add(JxlsOrdersReportModel::getPaymentsTransaction, nullValue(), PAYMENT)
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), CASHBACK)
                        .add(JxlsOrdersReportModel::getRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(5)),
                                        hasBankOrderId(null),
                                        hasBankOrderTime(null),
                                        hasPaymentIdentity("5a1c152b795be240ef48a001"),
                                        hasDateHandlingTime(null)
                                ),
                                REFUND
                        )
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(), SUBSIDY_REFUND)
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), CASHBACK_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build(),
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(1898849L),
                                hasOrderNum(null),
                                hasOfferName("Attento"),
                                hasShopSku("cbb2282b-eea7-11e6-810b-00155d000405"),
                                hasCountInDelivery(1),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY.getName()),
                                hasBillingPrice(new BigDecimal("4171.00")),
                                hasSubsidy(new BigDecimal("371.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("25.11.2017"),
                                hasChangedStatusTime("27.12.2017"),
                                hasPaymentType("предоплата"),
                                hasDeliveryServiceName("Маршрут dropship"),
                                hasRegionToName("Санкт-Петербург и Ленинградская область")
                        ))
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(6)),
                                        hasBankOrderId("77187"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c152b795be240ef48a003"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                PAYMENT
                        )
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getRefundTransaction, nullValue(), REFUND)
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction,
                                nullValue(), SUBSIDY_REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(),
                                COMPENSATION
                        )
                        .build(),
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(1898851L),
                                hasOrderNum(null),
                                hasOfferName("Attento"),
                                hasShopSku("cbb2282b-eea7-11e6-810b-00155d000405"),
                                hasCountInDelivery(0),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.CANCELLED_IN_PROCESSING.getName()),
                                hasBillingPrice(new BigDecimal("4171.00")),
                                hasSubsidy(new BigDecimal("371.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime("27.12.2017"),
                                hasPaymentType("предоплата"),
                                hasDeliveryServiceName("Маршрут dropship"),
                                hasRegionToName("Санкт-Петербург и Ленинградская область")
                        ))
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(6)),
                                        hasBankOrderId("77188"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c152b795be240ef48a004"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                PAYMENT
                        )
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getRefundTransaction, nullValue(), REFUND)
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction,
                                nullValue(), SUBSIDY_REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(),
                                COMPENSATION
                        )
                        .build()
        ));
    }

    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.crossdockWithResupplyTest.csv")
    void crossdockWithResuppliesTest() {
        OrdersReportRawData reportData = getReportData(DATE_2017_11_26, DATE_2017_11_28, 10245732L, false);
        List<JxlsOrdersReportModel> jxlsOrdersReportModels = iterableToCollection(reportData.convertToModel());
        assertThat(jxlsOrdersReportModels, contains(
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(
                                allOf(
                                        hasOrderId(1898847L),
                                        hasOrderNum(null),
                                        hasOfferName("Attento"),
                                        hasShopSku("cbb2282b-eea7-11e6-810b-00155d000405"),
                                        hasCountInDelivery(1),
                                        hasInitialCount(1),
                                        hasStatus(OrderInfoStatus.UNREDEEMED.getName()),
                                        hasBillingPrice(new BigDecimal("4171.00")),
                                        hasSubsidy(new BigDecimal("371.00")),
                                        hasSpasiboPerItem(null),
                                        hasCreationDate("27.11.2017"),
                                        hasChangedStatusTime("30.12.2017"),
                                        hasPaymentType("оплата при получении"),
                                        hasDeliveryServiceName("Маршрут FF"),
                                        hasRegionToName("Московская область")
                                )
                        )
                        .add(
                                JxlsOrdersReportModel::getSubsidyTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(2)),
                                        hasBankOrderId("77186"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c1971fa250d50910bf543"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                SUBSIDY
                        )
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(2)),
                                        hasBankOrderId("77187"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c152b795be240ef48a220"),
                                        hasDateHandlingTime("03.01.2018")
                                ),
                                PAYMENT
                        )
                        .add(
                                JxlsOrdersReportModel::getRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(3)),
                                        hasBankOrderId("1234"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c152b795be240ef48a200"),
                                        hasDateHandlingTime("04.01.2018")
                                ),
                                REFUND
                        )
                        .add(
                                JxlsOrdersReportModel::getSubsidyRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(6)),
                                        hasBankOrderId(null),
                                        hasBankOrderTime(null),
                                        hasPaymentIdentity("5a1c152b795be240ef48a002"),
                                        hasDateHandlingTime("28.11.2017")
                                ),
                                SUBSIDY_REFUND
                        )
                        .add(
                                JxlsOrdersReportModel::getCompensationTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(250)),
                                        hasBankOrderId("555777"),
                                        hasBankOrderTime("26.11.2017"),
                                        hasPaymentIdentity("compensation_payment_id"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                COMPENSATION
                        ).build()
                ,
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(1898847L),
                                hasOrderNum(null),
                                hasOfferName("Attento"),
                                hasShopSku("cbb2282b-eea7-11e6-810b-00155d000405"),
                                hasCountInDelivery(null),
                                hasInitialCount(null),
                                hasStatus(null),
                                hasBillingPrice(null),
                                hasSubsidy(null),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime(null),
                                hasPaymentType(null),
                                hasDeliveryServiceName(null),
                                hasRegionToName(null)
                        ))
                        .add(JxlsOrdersReportModel::getPaymentsTransaction, nullValue(), PAYMENT)
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), CASHBACK)
                        .add(
                                JxlsOrdersReportModel::getRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(4)),
                                        hasBankOrderId(null),
                                        hasBankOrderTime(null),
                                        hasPaymentIdentity("5a1c152b795be240ef48a000"),
                                        hasDateHandlingTime(null)
                                ),
                                REFUND
                        )
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(), SUBSIDY_REFUND)
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), CASHBACK_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build(),
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(1898847L),
                                hasOrderNum(null),
                                hasOfferName("Attento"),
                                hasShopSku("cbb2282b-eea7-11e6-810b-00155d000405"),
                                hasCountInDelivery(null),
                                hasStatus(null),
                                hasBillingPrice(null),
                                hasSubsidy(null),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime(null),
                                hasPaymentType(null),
                                hasDeliveryServiceName(null),
                                hasRegionToName(null)
                        ))
                        .add(JxlsOrdersReportModel::getPaymentsTransaction, nullValue(), PAYMENT)
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), CASHBACK)
                        .add(JxlsOrdersReportModel::getRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(5)),
                                        hasBankOrderId(null),
                                        hasBankOrderTime(null),
                                        hasPaymentIdentity("5a1c152b795be240ef48a001"),
                                        hasDateHandlingTime(null)
                                ),
                                REFUND
                        )
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(), SUBSIDY_REFUND)
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), CASHBACK_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build(),
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(1898847L),
                                hasOrderNum(null),
                                hasOfferName("offer"),
                                hasShopSku("cbb2282b-eea7-11e6-810b-00155d000405"),
                                hasCountInDelivery(1),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY.getName()),
                                hasBillingPrice(new BigDecimal("1.00")),
                                hasSubsidy(new BigDecimal("0.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime("27.12.2017"),
                                hasPaymentType("оплата при получении"),
                                hasDeliveryServiceName(null),
                                hasRegionToName("Московская область")
                        ))
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("20.02")),
                                        hasBankOrderId("77187"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c152b795be240ef48a220"),
                                        hasDateHandlingTime("03.01.2018")
                                ),
                                PAYMENT
                        )
                        .add(
                                JxlsOrdersReportModel::getSubsidyTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("20.02")),
                                        hasBankOrderId("77186"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c1971fa250d50910bf543"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                SUBSIDY
                        )
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getRefundTransaction, nullValue(), REFUND)
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(),
                                SUBSIDY_REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build(),
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(1898849L),
                                hasOrderNum(null),
                                hasOfferName("Attento"),
                                hasShopSku("cbb2282b-eea7-11e6-810b-00155d000405"),
                                hasCountInDelivery(3),
                                hasInitialCount(3),
                                hasStatus(OrderInfoStatus.PARTIALLY_RETURNED.getName()),
                                hasBillingPrice(new BigDecimal("4171.00")),
                                hasSubsidy(new BigDecimal("371.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("25.11.2017"),
                                hasChangedStatusTime("10.01.2018"),
                                hasPaymentType("предоплата"),
                                hasDeliveryServiceName("Маршрут FF"),
                                hasRegionToName("Санкт-Петербург и Ленинградская область")
                        ))
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(6)),
                                        hasBankOrderId("77187"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c152b795be240ef48a003"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                PAYMENT
                        )
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getRefundTransaction, nullValue(), REFUND)
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction,
                                nullValue(), SUBSIDY_REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(),
                                COMPENSATION
                        )
                        .build(),
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(1898851L),
                                hasOrderNum(null),
                                hasOfferName("Attento"),
                                hasShopSku("cbb2282b-eea7-11e6-810b-00155d000405"),
                                hasCountInDelivery(0),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.CANCELLED_IN_PROCESSING.getName()),
                                hasBillingPrice(new BigDecimal("4171.00")),
                                hasSubsidy(new BigDecimal("371.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime("27.12.2017"),
                                hasPaymentType("предоплата"),
                                hasDeliveryServiceName("Маршрут FF"),
                                hasRegionToName("Санкт-Петербург и Ленинградская область")
                        ))
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(6)),
                                        hasBankOrderId("77188"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("5a1c152b795be240ef48a004"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                PAYMENT
                        )
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getRefundTransaction, nullValue(), REFUND)
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction,
                                nullValue(), SUBSIDY_REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(),
                                COMPENSATION
                        )
                        .build()
        ));
    }

    /**
     * Для заданного поставщика не должны теряться ПП, для которых datasource_id некорректно отрезолвился на этапе
     * забора данных из баланса(некорректный резолв в связи с коллизиями поставщиков клонов с одним contract_id).
     * Smoke test, так как не покрывает кейсы для всех полей транзакций, а только для прямого платежа.
     */
    @DisplayName("Для клона матчим по item если возможно")
    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.clonedSuppliers.csv")
    void test_clonedSuppliers() {
        OrdersReportRawData reportData = getReportData(DATE_2017_11_26, DATE_2017_11_28, 505L, false);
        assertThat(reportData.convertToModel(), contains(
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(5679434L),
                                hasOfferName("SomeOfferFor505"),
                                hasShopSku("shop_sku_3"),
                                hasCountInDelivery(1),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.DELIVERED.getName()),
                                hasBillingPrice(new BigDecimal("1.00")),
                                hasSubsidy(new BigDecimal("0.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime("27.11.2017"),
                                hasPaymentType("оплата при получении"),
                                hasDeliveryServiceName("Маршрут ФФ"),
                                hasRegionToName(null)
                        ))
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("124.00")),
                                        hasBankOrderId("485003"),
                                        hasBankOrderTime("25.03.2019"),
                                        hasPaymentIdentity("trans_id_with_cloned"),
                                        hasDateHandlingTime("22.03.2019")
                                ),
                                PAYMENT
                        )
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getRefundTransaction, nullValue(), REFUND)
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(),
                                SUBSIDY_REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build(),
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(5679434L),
                                hasOfferName("SomeOfferFor505_2"),
                                hasShopSku("shop_sku_4"),
                                hasCountInDelivery(1),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.DELIVERED.getName()),
                                hasBillingPrice(new BigDecimal("1.00")),
                                hasSubsidy(new BigDecimal("0.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime("27.11.2017"),
                                hasPaymentType("оплата при получении"),
                                hasDeliveryServiceName("Маршрут ФФ"),
                                hasRegionToName(null)
                        ))
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("100.00")),
                                        hasBankOrderId("485004"),
                                        hasBankOrderTime("27.03.2019"),
                                        hasPaymentIdentity("trans_id_with_cloned"),
                                        hasDateHandlingTime("27.03.2019")
                                ),
                                PAYMENT
                        )
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getRefundTransaction, nullValue(), REFUND)
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(),
                                SUBSIDY_REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build()
        ));
    }

    /**
     * Вынесено в тест, так как был кейс с duplicate key в мапе, для ключа
     * trans_id,refund_id,datasource_id,
     * где дубль datasource_id порождается при коллизии контракта для клонов.
     */
    @DisplayName("Для клона не подтягиваем позиции с кривым datasource_id, используем item_id")
    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.clonedSuppliers.csv")
    void test_clonedSuppliers_noForeignTransactions() {
        OrdersReportRawData reportData = getReportData(DATE_2017_11_26, DATE_2017_11_28, 506L, false);
        assertThat(reportData.convertToModel(), contains(
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(5679434L),
                                hasOfferName("SomeOfferFor506"),
                                hasShopSku("shop_sku_2"),
                                hasCountInDelivery(1),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.DELIVERED.getName()),
                                hasBillingPrice(new BigDecimal("4171.00")),
                                hasSubsidy(new BigDecimal("371.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime("27.11.2017"),
                                hasPaymentType("оплата при получении"),
                                hasDeliveryServiceName("Маршрут ФФ"),
                                hasRegionToName(null)
                                )
                        )
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("740.00")),
                                        hasBankOrderId("485002"),
                                        hasBankOrderTime("24.03.2019"),
                                        hasPaymentIdentity("trans_id_with_cloned"),
                                        hasDateHandlingTime("22.03.2019")
                                ),
                                PAYMENT
                        )
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getRefundTransaction, nullValue(), REFUND)
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(),
                                SUBSIDY_REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build()
        ));
    }

    @DisplayName("Фильтрация учитывает время доставки")
    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.changedStatusTimeMatters.csv")
    void test_changedStatusTimeMatters() {
        OrdersReportRawData reportData = getReportData(DATE_2017_11_26, DATE_2017_11_28, 501L, false);
        assertThat(reportData.convertToModel(), contains(
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(2L),
                                hasOfferName("some_order_2_item_2"),
                                hasShopSku("shop_sku_2"),
                                hasCountInDelivery(1),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.DELIVERY.getName()),
                                hasBillingPrice(new BigDecimal("1.00")),
                                hasSubsidy(new BigDecimal("0.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("20.11.2017"),
                                hasChangedStatusTime("26.11.2017"),
                                hasPaymentType("предоплата"),
                                hasDeliveryServiceName(null),
                                hasRegionToName(null)
                                )
                        )
                        .add(JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal(10)),
                                        hasBankOrderId("77187"),
                                        hasBankOrderTime("25.11.2017"),
                                        hasPaymentIdentity("trust_payment_trans_id_2"),
                                        hasDateHandlingTime("25.11.2017")
                                ),
                                PAYMENT
                        )
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getRefundTransaction, nullValue(), REFUND)
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(),
                                SUBSIDY_REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build()
        ));
    }

    @DisplayName("Заказы без платежей также попадают в отчет")
    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.ordersWithoutPayments.csv")
    void test_explicitNoPaymentsCheck() {
        OrdersReportRawData reportData = getReportData(DATE_2017_11_26, DATE_2017_11_28, 501L, false);
        assertThat(reportData.convertToModel(), contains(
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(101L),
                                hasOfferName("some_order_101_item_1"),
                                hasShopSku("shop_sku_1"),
                                hasCountInDelivery(0),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.PROCESSING.getName()),
                                hasBillingPrice(new BigDecimal("4171.00")),
                                hasSubsidy(new BigDecimal("371.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime("20.11.2017"),
                                hasPaymentType("оплата при получении"),
                                hasDeliveryServiceName("Маршрут ФФ"),
                                hasRegionToName(null)
                        ))
                        .add(JxlsOrdersReportModel::getPaymentsTransaction, nullValue(), PAYMENT)
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getRefundTransaction, nullValue(), REFUND)
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(),
                                SUBSIDY_REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build(),
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(102L),
                                hasOfferName("some_order_102_item_2"),
                                hasShopSku("shop_sku_2"),
                                hasCountInDelivery(0),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.PROCESSING.getName()),
                                hasBillingPrice(new BigDecimal("4171.00")),
                                hasSubsidy(new BigDecimal("371.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime("20.11.2017"),
                                hasPaymentType("предоплата"),
                                hasDeliveryServiceName(null),
                                hasRegionToName(null)
                        ))
                        .add(JxlsOrdersReportModel::getPaymentsTransaction, nullValue(), PAYMENT)
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getRefundTransaction, nullValue(), REFUND)
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(),
                                SUBSIDY_REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build(),
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(103L),
                                hasOfferName("some_order_103_item_3"),
                                hasShopSku("shop_sku_3"),
                                hasCountInDelivery(0),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.PROCESSING.getName()),
                                hasBillingPrice(new BigDecimal("4171.00")),
                                hasSubsidy(new BigDecimal("371.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime("20.11.2017"),
                                hasPaymentType("предоплата"),
                                hasDeliveryServiceName(null),
                                hasRegionToName(null)
                        ))
                        .add(JxlsOrdersReportModel::getPaymentsTransaction, nullValue(), PAYMENT)
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getRefundTransaction, nullValue(), REFUND)
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(),
                                SUBSIDY_REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build(),
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(103L),
                                hasOfferName("some_order_103_item_4"),
                                hasShopSku("shop_sku_4"),
                                hasCountInDelivery(0),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.PROCESSING.getName()),
                                hasBillingPrice(new BigDecimal("0.00")),
                                hasSubsidy(new BigDecimal("0.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime("20.11.2017"),
                                hasPaymentType("предоплата"),
                                hasDeliveryServiceName(null),
                                hasRegionToName(null)
                        ))
                        .add(JxlsOrdersReportModel::getPaymentsTransaction, nullValue(), PAYMENT)
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getRefundTransaction, nullValue(), REFUND)
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(),
                                SUBSIDY_REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build()
        ));
    }

    /**
     * Значения полей дат и сумм не имеют целостности - просто, чтобы все было заполнено.
     * Все, кроме спасибо, так как там надо трюкачить)
     */
    @DisplayName("Все поля кроме спасибо на примере одного item'а")
    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.allFields.csv")
    void test_allFields() {
        OrdersReportRawData reportData = getReportData(DATE_2017_11_26, DATE_2017_11_28, 501L, false);
        assertThat(reportData.convertToModel(), contains(
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(5679434L),
                                hasOfferName("SomeOfferFor501"),
                                hasShopSku("shop_sku_1"),
                                hasCountInDelivery(1),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.DELIVERED.getName()),
                                hasBillingPrice(new BigDecimal("4171.00")),
                                hasSubsidy(new BigDecimal("371.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime("20.02.2019"),
                                hasPaymentType("оплата при получении"),
                                hasDeliveryServiceName("Маршрут ФФ"),
                                hasRegionToName("Московская область")
                                )
                        )
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("1598.00")),
                                        hasBankOrderId("485001"),
                                        hasBankOrderTime("25.03.2019"),
                                        hasPaymentIdentity("trans_id_payment"),
                                        hasDateHandlingTime("26.03.2019")
                                ),
                                PAYMENT
                        )
                        .add(
                                JxlsOrdersReportModel::getSubsidyTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("200.00")),
                                        hasBankOrderId("485002"),
                                        hasBankOrderTime("26.03.2019"),
                                        hasPaymentIdentity("trans_id_subsidy"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                SUBSIDY
                        )
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(
                                JxlsOrdersReportModel::getRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("1598.00")),
                                        hasBankOrderId("485003"),
                                        hasBankOrderTime("27.03.2019"),
                                        hasPaymentIdentity("trans_id_refund"),
                                        hasDateHandlingTime("28.03.2019")
                                ),
                                REFUND
                        )
                        .add(
                                JxlsOrdersReportModel::getSubsidyRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("200.00")),
                                        hasBankOrderId("485004"),
                                        hasBankOrderTime("28.03.2019"),
                                        hasPaymentIdentity("trans_id_subsidy_refund"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(
                                JxlsOrdersReportModel::getCompensationTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("300.00")),
                                        hasBankOrderId("485005"),
                                        hasBankOrderTime("30.03.2019"),
                                        hasPaymentIdentity("trans_compensation_id"),
                                        hasDateHandlingTime("01.04.2019")
                                ),
                                COMPENSATION
                        )
                        .build()
        ));
    }

    @DisplayName("Кредитный платеж")
    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.credits.csv")
    void test_getReportData_credit() {
        OrdersReportRawData reportData = getReportData(DATE_2017_11_26, DATE_2017_11_28, 501L, false);

        assertThat(reportData.convertToModel(), contains(
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(11L),
                                hasOfferName("SomeOfferFor501"),
                                hasShopSku("shop_sku_1"),
                                hasCountInDelivery(1),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.DELIVERED.getName()),
                                hasBillingPrice(new BigDecimal("4171.00")),
                                hasSubsidy(new BigDecimal("371.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime("27.11.2017"),
                                hasPaymentType("кредит"),
                                hasDeliveryServiceName("Маршрут ФФ"),
                                hasRegionToName("Московская область")
                        ))
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("500")),
                                        hasBankOrderId("397072"),
                                        hasBankOrderTime("27.11.2017"),
                                        hasPaymentIdentity("trans_credit_payment"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                PAYMENT
                        )
                        .build()
                )
        );
    }

    /**
     * Значения полей дат и сумм не имеют целостности - просто, чтобы все было заполнено.
     */
    @DisplayName("Все поля на примере одного item'а")
    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.allFieldsWithSpasibo.csv")
    void test_allFieldsWithSpasibo() {
        OrdersReportRawData reportData = getReportData(DATE_2017_11_26, DATE_2017_11_28, 501L, false);
        assertThat(reportData.convertToModel(), contains(
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(
                                allOf(
                                        hasOrderId(5679434L),
                                        hasOfferName("SomeOfferFor501"),
                                        hasShopSku("shop_sku_1"),
                                        hasCountInDelivery(2),
                                        hasStatus(MbiOrderStatus.DELIVERED.getName()),
                                        hasBillingPrice(new BigDecimal("4171.00")),
                                        hasSubsidy(new BigDecimal("371.00")),
                                        hasSpasiboPerItem(new BigDecimal("50.00")),
                                        hasCreationDate("27.11.2017"),
                                        hasChangedStatusTime("20.02.2019"),
                                        hasPaymentType("оплата при получении"),
                                        hasDeliveryServiceName("Маршрут ФФ"),
                                        hasRegionToName("Московская область")
                                )
                        )
                        .add(
                                JxlsOrdersReportModel::getPaymentsTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("1598.00")),
                                        hasBankOrderId("485001"),
                                        hasBankOrderTime("25.03.2019"),
                                        hasPaymentIdentity("trans_id_payment_real_card"),
                                        hasDateHandlingTime("26.03.2019")
                                ),
                                PAYMENT
                        )
                        .add(
                                JxlsOrdersReportModel::getSubsidyTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("200.00")),
                                        hasBankOrderId("485002"),
                                        hasBankOrderTime("26.03.2019"),
                                        hasPaymentIdentity("trans_id_subsidy"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                SUBSIDY
                        )
                        .add(
                                JxlsOrdersReportModel::getSpasiboTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("100.00")),
                                        hasBankOrderId("485008"),
                                        hasBankOrderTime("25.03.2019"),
                                        hasPaymentIdentity("trans_id_payment_real_spasibo"),
                                        hasDateHandlingTime("26.03.2019")
                                ),
                                SPASIBO
                        )
                        .add(
                                JxlsOrdersReportModel::getRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("1598.00")),
                                        hasBankOrderId("485003"),
                                        hasBankOrderTime("27.03.2019"),
                                        hasPaymentIdentity("trans_id_refund_real_card"),
                                        hasDateHandlingTime("28.03.2019")
                                ),
                                REFUND
                        )
                        .add(
                                JxlsOrdersReportModel::getSubsidyRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("200.00")),
                                        hasBankOrderId("485004"),
                                        hasBankOrderTime("28.03.2019"),
                                        hasPaymentIdentity("trans_id_subsidy_refund"),
                                        hasDateHandlingTime("27.11.2017")
                                ),
                                SUBSIDY_REFUND
                        )
                        .add(
                                JxlsOrdersReportModel::getSpasiboRefundTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("100")),
                                        hasBankOrderId("485009"),
                                        hasBankOrderTime("27.03.2019"),
                                        hasPaymentIdentity("trans_id_refund_real_spasibo"),
                                        hasDateHandlingTime("28.03.2019")
                                ),
                                SPASIBO_REFUND
                        )
                        .add(
                                JxlsOrdersReportModel::getCompensationTransaction,
                                allOf(
                                        hasAmount(new BigDecimal("300.00")),
                                        hasBankOrderId("485005"),
                                        hasBankOrderTime("30.03.2019"),
                                        hasPaymentIdentity("trans_compensation_id"),
                                        hasDateHandlingTime("01.04.2019")
                                ),
                                COMPENSATION
                        )
                        .build()
        ));
    }

    @DisplayName("В рамках одного заказа item с одинаковым offerName корректно отображаются по строкам")
    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.sameOrderDuplicatedOfferName.csv")
    void test_sameShopSkuItems() {
        OrdersReportRawData reportData = getReportData(DATE_2017_11_26, DATE_2017_11_28, 501L, false);
        assertThat(reportData.convertToModel(), contains(
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(1L),
                                hasOfferName("SameOrderDuplicatedOfferName"),
                                hasShopSku("duplicated_offer_name_ssku_1"),
                                hasCountInDelivery(1),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.DELIVERED.getName()),
                                hasBillingPrice(new BigDecimal("3800.00")),
                                hasSubsidy(new BigDecimal("0.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime("20.02.2019"),
                                hasPaymentType("оплата при получении"),
                                hasDeliveryServiceName("Маршрут ФФ"),
                                hasRegionToName("Московская область")
                                )
                        )
                        .build()
                ,
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(1L),
                                hasOfferName("SameOrderDuplicatedOfferName"),
                                hasShopSku("duplicated_offer_name_ssku_2"),
                                hasCountInDelivery(1),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.DELIVERED.getName()),
                                hasBillingPrice(new BigDecimal("1000.00")),
                                hasSubsidy(new BigDecimal("0.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime("20.02.2019"),
                                hasPaymentType("оплата при получении"),
                                hasDeliveryServiceName("Маршрут ФФ"),
                                hasRegionToName("Московская область")
                        ))
                        .build()
        ));
    }

    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.ordersWithoutPayments.csv")
    void testOrderIdReport() {
        OrdersReportRawData reportData = getReportData(101L, 501L, false);
        assertThat(reportData.convertToModel(), contains(
                MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                        .add(allOf(
                                hasOrderId(101L),
                                hasOfferName("some_order_101_item_1"),
                                hasShopSku("shop_sku_1"),
                                hasCountInDelivery(0),
                                hasInitialCount(1),
                                hasStatus(MbiOrderStatus.PROCESSING.getName()),
                                hasBillingPrice(new BigDecimal("4171.00")),
                                hasSubsidy(new BigDecimal("371.00")),
                                hasSpasiboPerItem(null),
                                hasCreationDate("27.11.2017"),
                                hasChangedStatusTime("20.11.2017"),
                                hasPaymentType("оплата при получении"),
                                hasDeliveryServiceName("Маршрут ФФ"),
                                hasRegionToName(null)
                        ))
                        .add(JxlsOrdersReportModel::getPaymentsTransaction, nullValue(), PAYMENT)
                        .add(JxlsOrdersReportModel::getSubsidyTransaction, nullValue(), SUBSIDY)
                        .add(JxlsOrdersReportModel::getSpasiboTransaction, nullValue(), SPASIBO)
                        .add(JxlsOrdersReportModel::getRefundTransaction, nullValue(), REFUND)
                        .add(JxlsOrdersReportModel::getSubsidyRefundTransaction, nullValue(),
                                SUBSIDY_REFUND
                        )
                        .add(JxlsOrdersReportModel::getSpasiboRefundTransaction, nullValue(), SPASIBO_REFUND)
                        .add(JxlsOrdersReportModel::getCompensationTransaction, nullValue(), COMPENSATION)
                        .build()
        ));
    }

    /**
     * Проверка хедера таблицы
     */
    @Test
    @Disabled
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.basicTest.csv")
    void basicHeaderTest() throws IOException {
        File reportFile = TempFileUtils.createTempFile("tmpReport", ".xlsx");
        try (OutputStream output = new FileOutputStream(reportFile)) {
            fulfillmentReportGenerator.xlsOfferReport(
                    OrdersReportParams.createOrdersReportParamsForDatesRange(
                            10245732L,
                            DATE_2017_11_26.getTime(),
                            DATE_2017_11_28.getTime(),
                            false
                    ),
                    output
            );
        }
        Sheet testSheet = WorkbookFactory.create(Files.newInputStream(reportFile.toPath())).getSheetAt(0);

        String[] expectedHeaderText = getExpectedHeaderText();
        Row rowHeader = testSheet.getRow(0);
        Assertions.assertEquals(expectedHeaderText[0], rowHeader.getCell(0).getStringCellValue());
        for (int i = 16, j = 1; i <= 56; i += 5, j++) {
            Assertions.assertEquals(expectedHeaderText[j], rowHeader.getCell(i).getStringCellValue());
        }
        Row rowHeader2 = testSheet.getRow(1);
        Assertions.assertEquals("Скидка маркетплейса\n(за шт.)", rowHeader2.getCell(8).getStringCellValue());
        Assertions.assertEquals("Оплата бонусами «СберСпасибо»\n(за шт.)", rowHeader2.getCell(9).getStringCellValue());
        Assertions.assertEquals("Оплата баллами Яндекс.Плюса", rowHeader2.getCell(10).getStringCellValue());
    }

    @Test
    @DbUnitDataSet(before = {"FulfillmentReportGeneratorTest.basicTest.csv", "FulfillmentReportGeneratorTest.env.csv"})
    void basicHeaderWithSummaryTest() throws IOException {
        File reportFile = TempFileUtils.createTempFile("tmpReport", ".xlsx");
        try (OutputStream output = new FileOutputStream(reportFile)) {
            fulfillmentReportGenerator.xlsOfferReport(
                    OrdersReportParams.createOrdersReportParamsForDatesRange(
                            10245732L,
                            DATE_2017_11_26.getTime(),
                            DATE_2017_11_28.getTime(),
                            true
                    ),
                    output
            );
        }
        Sheet testSheet = WorkbookFactory.create(Files.newInputStream(reportFile.toPath())).getSheetAt(0);

        String[] summaryHeaderText = getSummaryHeaderTexts();
        Row summaryRowHeaders = testSheet.getRow(8);
        for (int i = 0; i < 14; i++) {
            Assertions.assertEquals(summaryHeaderText[i], summaryRowHeaders.getCell(i).getStringCellValue());
        }

        String[] expectedHeaderText = getExpectedSummaryHeaderText();
        Row rowHeader = testSheet.getRow(13);
        Assertions.assertEquals(expectedHeaderText[0], rowHeader.getCell(0).getStringCellValue());
        for (int i = 17, j = 1; i <= 57; i += 5, j++) {
            Assertions.assertEquals(expectedHeaderText[j], rowHeader.getCell(i).getStringCellValue());
        }
        Row rowHeader2 = testSheet.getRow(14);
        Assertions.assertEquals("Скидка маркетплейса\n(за шт.)", rowHeader2.getCell(8).getStringCellValue());
        Assertions.assertEquals("Оплата бонусами СберСпасибо\n(за шт.)", rowHeader2.getCell(9).getStringCellValue());
        Assertions.assertEquals("Оплата баллами Яндекс.Плюса", rowHeader2.getCell(10).getStringCellValue());
    }

    @Test
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.bnplTest.csv")
    void bnplTest() throws Exception {
        Path tempFilePath = Files.createTempFile("ffReport.bnpl", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            fulfillmentReportGenerator.xlsOfferReport(
                    OrdersReportParams.createOrdersReportParamsForDatesRange(
                            472311L,
                            Instant.parse("2021-04-01T12:00:00Z"),
                            Instant.parse("2021-05-01T12:00:00Z"),
                            false
                    ),
                    output
            );
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream("ffReport.expected.bnplTest.xlsx"));
        ExcelTestUtils.assertEquals(expected, actual, new HashSet<>());
    }

    /**
     * Перегенерирует файл для сравнения.
     */
    @Test
    @Disabled
    @DbUnitDataSet(before = "FulfillmentReportGeneratorTest.basicTest.csv")
    void basicTestRegenerate() throws IOException {
        try (OutputStream output = new FileOutputStream(
                "src/test/resources/FulfillmentShopReportGeneratorTest.basicTest.xlsx")) {
            fulfillmentReportGenerator.xlsOfferReport(
                    OrdersReportParams.createOrdersReportParamsForDatesRange(
                            10245732L,
                            DATE_2017_11_26.getTime(),
                            DATE_2017_11_28.getTime(),
                            false
                    ),
                    output
            );
        }
    }

    private OrdersReportRawData getReportData(Date from,
                                              Date to,
                                              Long supplierId,
                                              boolean useTestingVersion) {
        Collection<MbiOrderItem> orderItems =
                fulfillmentReportGenerator.fetchOrderItems(
                        OrdersReportParams.createOrdersReportParamsForDatesRange(
                                supplierId, from.getTime(), to.getTime(), useTestingVersion),
                        false
                );

        Collection<MbiOrder> orders = fulfillmentReportGenerator.fetchOrders(supplierId, orderItems);

        return fulfillmentReportGenerator.builderFrom(supplierId, orders, orderItems).build();
    }

    private OrdersReportRawData getReportData(long orderId,
                                              long shopId,
                                              boolean useTestingVersion) {
        Collection<MbiOrderItem> orderItems =
                fulfillmentReportGenerator.fetchOrderItems(
                        OrdersReportParams.createOrdersReportParamsForOrderId(shopId, orderId, useTestingVersion),
                        false
                );

        Collection<MbiOrder> orders = fulfillmentReportGenerator.fetchOrders(shopId, orderItems);

        return fulfillmentReportGenerator.builderFrom(shopId, orders, orderItems).build();
    }

    private OrdersReportCommissionsService mockCommissionService() {
        OrdersReportCommissionsService commissionsService = mock(OrdersReportCommissionsService.class);

        Map<Long, BigDecimal> commissions = new HashMap<>();

        commissions.put(1898847L, BigDecimal.valueOf(100));
        commissions.put(1898849L, BigDecimal.valueOf(100));
        commissions.put(1898860L, BigDecimal.valueOf(100));
        commissions.put(1898861L, BigDecimal.valueOf(100));
        commissions.put(1898862L, BigDecimal.valueOf(100));

        when(commissionsService.getFee(any())).thenReturn(commissions);
        when(commissionsService.getFfProcessing(any())).thenReturn(commissions);
        when(commissionsService.getDeliveryCommission(any())).thenReturn(commissions);
        when(commissionsService.getAgencyCommission(any())).thenReturn(commissions);
        when(commissionsService.getFfStorageBilling(any())).thenReturn(commissions);
        when(commissionsService.getFfSurplusSupply(any())).thenReturn(commissions);
        when(commissionsService.getSortingCommission(any())).thenReturn(commissions);
        when(commissionsService.getReturnStorage(any())).thenReturn(commissions);
        when(commissionsService.getDeliveryToCustomerReturn(any())).thenReturn(commissions);
        when(commissionsService.getWithdrawCommissions(any())).thenReturn(commissions);
        when(commissionsService.getLoyaltyParticipationCommission(any())).thenReturn(commissions);

        return commissionsService;
    }

    public static <T> List<T> iterableToCollection(Iterable<T> iterable) {
        List<T> collection = new ArrayList<>();
        iterable.forEach(collection::add);
        return collection;
    }

    private String[] getSummaryHeaderTexts() {
        return new String[]{
                "Заказов оформлено",
                "Заказов отменено до отгрузки",
                "Заказов отгружено",
                "Заказов выкуплено",
                "Заказов не выкуплено",
                "Заказов возвращено",
                "Заказов в доставке",
                "Общая выручка по заказам, руб.",
                "Общая сумма вознаграждения за скидки, руб.",
                "Общая стоимость услуг по заказам, руб.",
                "Комиссия за продажу товаров, руб.",
                "Складская обработка, руб.",
                "Доставка товаров покупателям, руб.",
                "Экспресс-доставка товаров покупателям, руб.",
                "Приём и перевод платежей покупателей, руб.",
                "Обработка заказов в сортировочном центре, руб.",
                "Хранение невыкупов и возвратов, руб.",
                "Возврат невыкупленных товаров, руб.",
                "Участие в программе лояльности, руб.",
        };
    }

    private String[] getExpectedSummaryHeaderText() {
        return new String[]{
                "Информация о заказе",
                "Платёж покупателя",
                "Платёж за скидку маркетплейса",
                "Платёж за скидку по бонусам СберСпасибо",
                "Платёж за скидку по баллам Яндекс.Плюса",
                "Возврат платежа покупателя",
                "Возврат платежа за скидку маркетплейса",
                "Возврат платежа за скидку по бонусам СберСпасибо",
                "Возврат платежа за скидку по баллам Яндекс.Плюса",
                "Выплата расходов покупателю при возврате товара ненадлежащего качества"
        };
    }

    private String[] getExpectedHeaderText() {
        return new String[]{
                "Информация о заказе",
                "Платёж покупателя",
                "Платёж за скидку маркетплейса",
                "Платёж за скидку по бонусам «СберСпасибо»",
                "Платёж за скидку по баллам Яндекс.Плюса",
                "Возврат платежа покупателя",
                "Возврат платежа за скидку маркетплейса",
                "Возврат платежа за скидку по бонусам СберСпасибо",
                "Возврат платежа за скидку по баллам Яндекс.Плюса",
                "Выплата расходов покупателю при возврате товара ненадлежащего качества"
        };
    }
}
