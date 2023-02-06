package ru.yandex.market.rg.asyncreport.orders.info;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.exception.EmptyReportException;
import ru.yandex.market.core.delivery.DeliveryInfoService;
import ru.yandex.market.core.fulfillment.report.excel.jxls.JxlsOrdersReportModel;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.tanker.TankerService;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;

@DbUnitDataSet(before = {"Tanker.csv", "OrdersInfoReportGeneratorTest.basicTest.csv"})
class OrdersInfoReportGeneratorTest extends FunctionalTest {

    private static final long SUPPLIER_ID = 10254500L;
    private static final long ORDER_ID = 1898847L;
    private static final long ORDER_ID_2 = 1898848L;
    private static final long DELIVERY_SERVICE_ID = 47407L;
    private static final Date CREATION_DATE = DateUtil.asDate(LocalDateTime.of(
            2020, 11, 27, 13, 30, 15));
    private static final Date CHANGED_STATUS_TIME = DateUtil.asDate(LocalDateTime.of(
            2020, 12, 27, 15, 30, 20));
    private static final LocalDateTime SHIPMENT_DATE =
            LocalDateTime.of(2021, 1, 2, 10, 30, 15);
    private static final Date CREATION_DATE_2 = DateUtil.asDate(LocalDateTime.of(
            2020, 11, 28, 16, 0, 40));
    private static final Date CHANGED_STATUS_TIME_2 = DateUtil.asDate(LocalDateTime.of(
            2020, 12, 28, 5, 18, 15));
    private static final LocalDateTime SHIPMENT_DATE_2 =
            LocalDateTime.of(2021, 1, 6, 10, 30, 15);

    @Autowired
    private DeliveryInfoService deliveryInfoService;
    @Autowired
    private RegionService regionService;
    @Autowired
    private CheckouterAPI checkouterClient;
    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;
    @Autowired
    private TankerService tankerService;
    private OrdersShipmentInfoReportGenerator reportGenerator;

    @BeforeEach
    void setUp() {
        reportGenerator = new OrdersShipmentInfoReportGenerator(deliveryInfoService, regionService, checkouterClient,
                partnerTypeAwareService, tankerService);
    }

    @Test
    @DisplayName("Успешная стримовая генерация отчета по 1 заказу")
    void testBatchGenerateReportForSpecificOrder() throws IOException {
        mockCheckouterForSingleOrder();
        var expectedData = prepareModelForSingleOrderReport();
        checkIfFilledForSpecificOrder(expectedData);
        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        Mockito.verify(checkouterClient).getOrders(any(), captor.capture());
        OrderSearchRequest searchRequest = captor.getValue();
        assertThat(searchRequest.orderIds).isEqualTo(List.of(ORDER_ID));
    }

    @Test
    @DisplayName("Успешная батчевая генерация отчета с фильтрами")
    void testGenerateReportWithFilterStream() throws IOException {
        mockCheckouterForOrders();
        var expectedData = prepareModelForFilteredOrdersReport();
        var reportParams = getReportParams();
        checkIfFilledForOrdersFilterBatch(expectedData, reportParams);
        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        Mockito.verify(checkouterClient).getOrders(any(), captor.capture());
        OrderSearchRequest searchRequest = captor.getValue();
        assertThat(searchRequest.statuses).contains(OrderStatus.PROCESSING);
    }

    @Test
    @DisplayName("Пустой отчет не должен загружаться в MDS")
    public void testEmptyReport() {
        mockCheckouterOrderNotFound();
        assertThatExceptionOfType(EmptyReportException.class).isThrownBy(() -> {
            reportGenerator.generateReportForSpecificOrders(SUPPLIER_ID, List.of(ORDER_ID), null);
        });
        Mockito.verify(checkouterClient, Mockito.times(1))
                .getOrders(any(), any(OrderSearchRequest.class));
    }

    private void checkIfFilledForSpecificOrder(List<JxlsOrdersReportModel> items) throws IOException {
        File reportFile = TempFileUtils.createTempFile("tmpReport", ".xlsx");
        try (OutputStream output = new FileOutputStream(reportFile)) {
            reportGenerator.generateReportForSpecificOrders(SUPPLIER_ID, List.of(ORDER_ID), output);
        }
        assertReportFile(items, reportFile);
    }

    private void checkIfFilledForOrdersFilterBatch(List<JxlsOrdersReportModel> items, OrdersInfoParams params)
            throws IOException {
        File reportFile = TempFileUtils.createTempFile("tmpReport", ".xlsx");
        try (OutputStream output = new FileOutputStream(reportFile)) {
            reportGenerator.generateStreamReportWithOrdersFilter(params, output);
        }
        assertReportFile(items, reportFile);
    }

    private OrdersInfoParams getReportParams() {
        return new OrdersInfoParams(SUPPLIER_ID, null, null, null,
                null, OrderStatus.PROCESSING, null, null, false);
    }

    private void assertReportFile(List<JxlsOrdersReportModel> items, File reportFile) throws IOException {
        Sheet testSheet = WorkbookFactory.create(Files.newInputStream(reportFile.toPath())).getSheetAt(0);
        assertThat(testSheet.getLastRowNum()).isEqualTo(items.size() + 1);

        //первые две строчки -- хедер таблицы
        for (int i = 2; i < items.size() + 2; i++) {
            assertItemRow(items.get(i - 2), testSheet.getRow(i));
        }
    }

    private void assertItemRow(JxlsOrdersReportModel expected, Row row) {
        assertThat(row.getCell(0).getNumericCellValue()).isEqualTo((double) Objects.requireNonNull(expected.getOrderId()));
        assertThat(row.getCell(1).getStringCellValue()).isEqualTo(expected.getOrderNum());
        assertThat(row.getCell(2).getStringCellValue()).isEqualTo(expected.getCreationDate());
        assertThat(row.getCell(3).getStringCellValue()).isEqualTo(expected.getShopSku());
        assertThat(row.getCell(4).getStringCellValue()).isEqualTo(expected.getOfferName());
        assertThat(row.getCell(5).getNumericCellValue()).isEqualTo((double) Objects.requireNonNull(expected.getInitialCount()));
        assertThat(BigDecimal.valueOf(row.getCell(6).getNumericCellValue())).isEqualTo(Objects.requireNonNull(expected.getBillingPrice()));
        assertThat(row.getCell(7).getStringCellValue()).isEqualTo(expected.getStatus());
        assertThat(row.getCell(8).getStringCellValue()).isEqualTo(expected.getChangedStatusTime());
        assertThat(row.getCell(9).getStringCellValue()).isEqualTo(expected.getPaymentType());
        assertThat(row.getCell(10).getStringCellValue()).isEqualTo(expected.getDeliveryServiceName());
        assertThat(row.getCell(11).getStringCellValue()).isEqualTo(expected.getShipmentDate());
        assertThat(row.getCell(12).getNumericCellValue()).isEqualTo((double) Objects.requireNonNull(expected.getBoxesCount()));
        assertThat(row.getCell(13).getStringCellValue()).isEqualTo(expected.getRegionToName());
    }

    private List<JxlsOrdersReportModel> prepareModelForSingleOrderReport() {
        return createOrder47Items();
    }

    private List<JxlsOrdersReportModel> prepareModelForFilteredOrdersReport() {
        List<JxlsOrdersReportModel> models = new ArrayList<>(createOrder47Items());
        models.add(createOrder48Items());
        return models;
    }

    private List<JxlsOrdersReportModel> createOrder47Items() {
        JxlsOrdersReportModel item1 = JxlsOrdersReportModel.builder()
                .withOrderId(ORDER_ID)
                .withOrderNum("order_num_1_1")
                .withCreationDate("27.11.2020")
                .withShopSku("sku1")
                .withOfferName("offer1")
                .withInitialCount(1)
                .withStatus("Обрабатывается")
                .withChangedStatusTime("27.12.2020")
                .withPaymentType(PaymentType.POSTPAID)
                .withDeliverServiceName("Склад дропшипа")
                .withShipmentDate("02.01.2021")
                .withRegionToName("Московская область")
                .withItemId(1)
                .withBillingPrice(BigDecimal.valueOf(1270.0))
                .withBoxesCount(1)
                .build();
        JxlsOrdersReportModel item2 = JxlsOrdersReportModel.builder()
                .withOrderId(ORDER_ID)
                .withOrderNum("order_num_1_1")
                .withCreationDate("27.11.2020")
                .withShopSku("sku2")
                .withOfferName("offer2")
                .withInitialCount(1)
                .withStatus("Обрабатывается")
                .withChangedStatusTime("27.12.2020")
                .withPaymentType(PaymentType.POSTPAID)
                .withDeliverServiceName("Склад дропшипа")
                .withShipmentDate("02.01.2021")
                .withRegionToName("Московская область")
                .withItemId(2)
                .withBillingPrice(BigDecimal.valueOf(550.0))
                .withBoxesCount(1)
                .build();
        return List.of(item1, item2);
    }

    private JxlsOrdersReportModel createOrder48Items() {
        return JxlsOrdersReportModel.builder()
                .withOrderId(ORDER_ID_2)
                .withOrderNum("order_num_1_2")
                .withCreationDate("28.11.2020")
                .withShopSku("sku3")
                .withOfferName("offer3")
                .withInitialCount(5)
                .withStatus("Обрабатывается")
                .withChangedStatusTime("28.12.2020")
                .withPaymentType(PaymentType.POSTPAID)
                .withDeliverServiceName("Склад дропшипа")
                .withShipmentDate("06.01.2021")
                .withRegionToName("Санкт-Петербург и Ленинградская область")
                .withItemId(3)
                .withBillingPrice(BigDecimal.valueOf(1250.0))
                .withBoxesCount(1)
                .build();
    }

    private void mockCheckouterForSingleOrder() {
        var order = prepareOrder();
        Mockito.when(checkouterClient.getOrders(any(RequestClientInfo.class), any(OrderSearchRequest.class)))
                .thenReturn(new PagedOrders(List.of(order), Pager.atPage(1, 1)));
    }

    private void mockCheckouterForOrders() {
        var order1 = prepareOrder();
        var order2 = prepareOrder2();
        List<Order> orders = new ArrayList<>(Arrays.asList(order1, order2));
        PagedOrders pagedOrders = new PagedOrders();
        pagedOrders.setItems(orders);
        Mockito.when(checkouterClient.getOrders(any(RequestClientInfo.class), any(OrderSearchRequest.class)))
                .thenReturn(pagedOrders);
    }

    private void mockCheckouterOrderNotFound() {
        Mockito.when(checkouterClient.getOrders(
                        any(RequestClientInfo.class), any(OrderSearchRequest.class)))
                .thenReturn(new PagedOrders());
    }

    private Order prepareOrder() {
        Order order = new Order();
        Delivery orderDelivery = new Delivery();
        Parcel parcel = new Parcel();
        parcel.setBoxes(List.of(new ParcelBox()));
        parcel.setShipmentDateTimeBySupplier(SHIPMENT_DATE);

        orderDelivery.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        orderDelivery.setParcels(List.of(parcel));
        orderDelivery.setRegionId(4L);

        OrderItem orderItem1 = new OrderItem();
        OrderItem orderItem2 = new OrderItem();

        orderItem1.setId(1L);
        orderItem2.setId(2L);

        orderItem1.setOrderId(ORDER_ID);
        orderItem2.setOrderId(ORDER_ID);

        orderItem1.setShopSku("sku1");
        orderItem2.setShopSku("sku2");

        orderItem1.setOfferName("offer1");
        orderItem2.setOfferName("offer2");

        orderItem1.setCount(1);
        orderItem2.setCount(1);

        orderItem1.setFeedId(10L);
        orderItem2.setFeedId(20L);

        orderItem1.setBuyerPrice(BigDecimal.valueOf(1270));
        orderItem2.setBuyerPrice(BigDecimal.valueOf(550));

        order.setId(ORDER_ID);
        order.setShopId(SUPPLIER_ID);
        order.setShopOrderId("order_num_1_1");
        order.setCreationDate(CREATION_DATE);
        order.setArchived(false);
        order.setFake(false);
        order.setItems(List.of(orderItem1, orderItem2));
        order.setStatus(OrderStatus.PROCESSING);
        order.setStatusUpdateDate(CHANGED_STATUS_TIME);
        order.setPaymentType(PaymentType.POSTPAID);
        order.setDelivery(orderDelivery);
        return order;
    }

    private Order prepareOrder2() {
        Order order = new Order();
        Delivery orderDelivery = new Delivery();
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(SHIPMENT_DATE_2);
        parcel.setBoxes(List.of(new ParcelBox()));

        orderDelivery.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        orderDelivery.setParcels(List.of(parcel));
        orderDelivery.setRegionId(2L);

        OrderItem orderItem1 = new OrderItem();

        orderItem1.setId(3L);
        orderItem1.setOrderId(ORDER_ID_2);
        orderItem1.setShopSku("sku3");
        orderItem1.setOfferName("offer3");
        orderItem1.setCount(5);
        orderItem1.setFeedId(10L);
        orderItem1.setBuyerPrice(BigDecimal.valueOf(1250));

        order.setId(ORDER_ID_2);
        order.setShopId(SUPPLIER_ID);
        order.setShopOrderId("order_num_1_2");
        order.setCreationDate(CREATION_DATE_2);
        order.setArchived(false);
        order.setFake(false);
        order.setItems(List.of(orderItem1));
        order.setStatus(OrderStatus.PROCESSING);
        order.setStatusUpdateDate(CHANGED_STATUS_TIME_2);
        order.setPaymentType(PaymentType.POSTPAID);
        order.setDelivery(orderDelivery);
        return order;
    }
}
