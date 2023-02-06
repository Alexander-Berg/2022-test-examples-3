package ru.yandex.market.core.fulfillment.report.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
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
import ru.yandex.market.core.order.resupply.ResupplyOrderDao;
import ru.yandex.market.core.partner.PartnerCommonInfoService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.contract.PartnerContractDao;
import ru.yandex.market.core.shop.BeruVirtualShop;
import ru.yandex.market.core.supplier.SupplierExposedActService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasBillingPrice;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasChangedStatusTime;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasCountInDelivery;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasCreationDate;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasDeliveryServiceName;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasInitialCount;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasOfferName;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasOrderId;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasPaymentType;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasRegionToName;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasShopSku;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasSpasiboPerItem;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasStatus;
import static ru.yandex.market.core.matchers.fulfillment.report.OrderReportDataMatcher.hasSubsidy;

@DbUnitDataSet(before = "FulfillmentReportGeneratorTest.common.csv")
public class DSBSShopOrdersReportGeneratorTest extends FunctionalTest {

    private static final Date DATE_2017_11_26 = DateUtil.asDate(LocalDate.of(2017, 11, 26));
    private static final Date DATE_2020_11_28 = DateUtil.asDate(LocalDate.of(2020, 11, 28));
    private static final String PAYMENT = "payment";
    private static final String SUBSIDY = "subsidy";
    private static final String SPASIBO = "spasibo";
    private static final String REFUND = "refund";
    private static final String SUBSIDY_REFUND = "subsidyRefund";
    private static final String SPASIBO_REFUND = "spasiboRefund";
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
    private DSBSShopOrdersReportGenerator dsbsReportGenerator;
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
        dsbsReportGenerator = new DSBSShopOrdersReportGenerator(
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
                mock(OrdersReportCommissionsService.class),
                supplierExposedActService,
                campaignService,
                partnerCommonInfoService,
                parcelService,
                expressDeliveryBillingDao
        );
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
     * Перегенерирует файл для сравнения. (сравниваются только кол-ва строк).
     */
    @Test
    @DbUnitDataSet(before = "dropshipBySeller.offer.csv")
    void basicTestRegenerate() throws IOException, InvalidFormatException {
        Path tempFilePath = Files.createTempFile("DSBSShopReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            dsbsReportGenerator.xlsOfferReport(
                    OrdersReportParams.createOrdersReportParamsForDatesRange(
                            BeruVirtualShop.ID,
                            DATE_2017_11_26.getTime(),
                            DATE_2020_11_28.getTime(),
                            false
                    ),
                    output
            );
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expected = new XSSFWorkbook(getClass()
                .getResourceAsStream("DSBSShopReportGeneratorTest.expected.xlsx"));

        ExcelTestUtils.assertEquals(
                expected,
                actual,
                new HashSet<>()
        );
    }

    @Test
    @DbUnitDataSet(before = "dropshipBySellerDelivery.offer.csv")
    void dbsDelivery() throws IOException, InvalidFormatException {
        Path tempFilePath = Files.createTempFile("DSBSShopReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            dsbsReportGenerator.xlsOfferReport(
                    OrdersReportParams.createOrdersReportParamsForDatesRange(
                            1174299L,
                            DateUtil.asDate(LocalDate.of(2021, 6, 29)).getTime(),
                            DateUtil.asDate(LocalDate.of(2021, 6, 29)).getTime(),
                            false
                    ),
                    output
            );
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expected = new XSSFWorkbook(getClass()
                .getResourceAsStream("DSBSShopReportGeneratorTestDelivery.expected.xlsx"));

        ExcelTestUtils.assertEquals(
                expected,
                actual,
                new HashSet<>()
        );
    }

    private OrdersReportRawData getReportData(long orderId, long shopId, boolean useTestingVersion) {
        Collection<MbiOrderItem> orderItems =
                dsbsReportGenerator.fetchOrderItems(
                        OrdersReportParams.createOrdersReportParamsForOrderId(shopId, orderId, useTestingVersion),
                        false
                );

        Collection<MbiOrder> orders = dsbsReportGenerator.fetchOrders(shopId, orderItems);

        return dsbsReportGenerator.builderFrom(shopId, orders, orderItems).build();
    }
}
