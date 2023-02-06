package ru.yandex.market.rg.asyncreport.orders.listing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.exception.EmptyReportException;
import ru.yandex.market.core.fulfillment.report.excel.jxls.JxlsOrdersReportModel;
import ru.yandex.market.logistics4shops.client.api.InternalOrderApi;
import ru.yandex.market.logistics4shops.client.model.LogisticOrderBox;
import ru.yandex.market.logistics4shops.client.model.LogisticOrderInfo;
import ru.yandex.market.logistics4shops.client.model.LogisticOrderSearchResponse;
import ru.yandex.market.logistics4shops.client.model.OrderType;
import ru.yandex.market.orderservice.client.model.CurrencyValue;
import ru.yandex.market.orderservice.client.model.OrderStatusGroup;
import ru.yandex.market.orderservice.client.model.PartnerListingOrderPiDto;
import ru.yandex.market.orderservice.client.model.PartnerOrderDeliveryPiDto;
import ru.yandex.market.orderservice.client.model.PartnerOrderItemPiDto;
import ru.yandex.market.rg.client.orderservice.RgOrderServiceClient;
import ru.yandex.market.rg.client.orderservice.filter.OrderListingFilter;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;

@DbUnitDataSet(before = {"Tanker.csv", "OrdersListingReportGeneratorTest.basicTest.csv"})
class OrdersListingReportGeneratorTest extends FunctionalTest {
    private static final ZoneOffset DEFAULT_OFFSET = ZoneOffset.ofHours(3);

    private static final long SUPPLIER_ID = 10254500L;
    private static final long ORDER_ID = 1898847L;
    private static final long ORDER_ID_2 = 1898848L;
    private static final long DELIVERY_SERVICE_ID = 47407L;
    private static final OffsetDateTime CREATION_DATE = OffsetDateTime.of(LocalDateTime.of(
            2020, 11, 25, 23, 30, 15), DEFAULT_OFFSET);
    private static final OffsetDateTime CHANGED_STATUS_TIME = OffsetDateTime.of(LocalDateTime.of(
            2020, 12, 25, 15, 30, 20), DEFAULT_OFFSET);
    private static final LocalDate SHIPMENT_DATE = LocalDate.of(2021, 1, 2);
    private static final OffsetDateTime CREATION_DATE_2 = OffsetDateTime.of(LocalDateTime.of(
            2020, 11, 28, 0, 0, 40), DEFAULT_OFFSET);
    private static final OffsetDateTime CHANGED_STATUS_TIME_2 = OffsetDateTime.of(LocalDateTime.of(
            2020, 12, 28, 5, 18, 15), DEFAULT_OFFSET);
    private static final LocalDate SHIPMENT_DATE_2 = LocalDate.of(2021, 1, 6);

    @Autowired
    private RgOrderServiceClient orderServiceClient;
    @Autowired
    private InternalOrderApi internalOrderApi;
    @Autowired
    private OrderServiceListingReportGenerator reportGenerator;

    @Test
    @DisplayName("Успешная батчевая генерация отчета с фильтрами")
    void testGenerateReportWithFilterStream() throws IOException {
        mockForOrders();
        var expectedData = prepareModelForFilteredOrdersReport(false);
        var reportParams = getReportParams(null, null);
        checkIfFilledForOrdersFilterBatch(expectedData, reportParams);
        ArgumentCaptor<OrderListingFilter> captor = ArgumentCaptor.forClass(OrderListingFilter.class);
        Mockito.verify(orderServiceClient).streamOrders(anyLong(), captor.capture(), any(), anyInt());
        OrderListingFilter searchRequest = captor.getValue();
        assertThat(searchRequest.getStatusGroup()).isEqualTo(OrderStatusGroup.PROCESSING);
    }

    @Test
    @DisplayName("Успешная батчевая генерация отчета в пользовательском часовом поясе")
    void testGenerateReportWithZoneOffset() throws IOException {
        mockForOrders();
        var expectedData = prepareModelForFilteredOrdersReport(true);
        var reportParams = getReportParams(null,
                (int) Duration.of(7, ChronoUnit.HOURS).toMinutes());

        checkIfFilledForOrdersFilterBatch(expectedData, reportParams);
        ArgumentCaptor<OrderListingFilter> captor = ArgumentCaptor.forClass(OrderListingFilter.class);
        Mockito.verify(orderServiceClient).streamOrders(anyLong(), captor.capture(), any(), anyInt());
        OrderListingFilter searchRequest = captor.getValue();
        assertThat(searchRequest.getStatusGroup()).isEqualTo(OrderStatusGroup.PROCESSING);
    }

    @Test
    @DisplayName("Пустой отчет не должен загружаться в MDS")
    public void testEmptyReport() {
        mockOrdersNotFound();
        assertThatExceptionOfType(EmptyReportException.class).isThrownBy(() -> {
            reportGenerator.generateStreamReportWithOrdersFilter(
                    getReportParams(List.of(ORDER_ID), null), null);
        });
        Mockito.verify(orderServiceClient, times(1))
                .streamOrders(anyLong(), any(OrderListingFilter.class), any(), anyInt());
    }

    private void checkIfFilledForOrdersFilterBatch(List<JxlsOrdersReportModel> items, OrdersListingParams params)
            throws IOException {
        File reportFile = TempFileUtils.createTempFile("tmpReport", ".xlsx");
        try (OutputStream output = new FileOutputStream(reportFile)) {
            reportGenerator.generateStreamReportWithOrdersFilter(params, output);
        }
        assertReportFile(items, reportFile);
    }

    private OrdersListingParams getReportParams(@Nullable List<Long> orderIds, @Nullable Integer zoneOffset) {
        return new OrdersListingParams(1, List.of(SUPPLIER_ID), zoneOffset,
                LocalDateTime.of(
                        2019, 11, 27, 13, 30, 15).toInstant(DEFAULT_OFFSET),
                LocalDateTime.of(
                        2021, 11, 27, 13, 30, 15).toInstant(DEFAULT_OFFSET),
                null, null, OrderStatusGroup.PROCESSING,
                orderIds, false);
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

    private List<JxlsOrdersReportModel> prepareModelForFilteredOrdersReport(boolean timeZoneShift) {
        List<JxlsOrdersReportModel> models = new ArrayList<>(createOrder47Items(timeZoneShift));
        models.add(createOrder48Items(timeZoneShift));
        return models;
    }

    private List<JxlsOrdersReportModel> createOrder47Items(boolean timeZoneShift) {
        JxlsOrdersReportModel item1 = JxlsOrdersReportModel.builder()
                .withOrderId(ORDER_ID)
                .withOrderNum("order_num_1_1")
                .withCreationDate(timeZoneShift ? "26.11.2020" : "25.11.2020")
                .withShopSku("sku1")
                .withOfferName("offer1")
                .withInitialCount(1)
                .withStatus("Обрабатывается")
                .withChangedStatusTime("25.12.2020")
                .withPaymentType(PaymentType.POSTPAID)
                .withDeliverServiceName("Склад дропшипа")
                .withShipmentDate("02.01.2021")
                .withRegionToName("Московская область")
                .withItemId(1)
                .withBoxesCount(2)
                .withBillingPrice(BigDecimal.valueOf(1270.0))
                .build();
        JxlsOrdersReportModel item2 = JxlsOrdersReportModel.builder()
                .withOrderId(ORDER_ID)
                .withOrderNum("order_num_1_1")
                .withCreationDate(timeZoneShift ? "26.11.2020" : "25.11.2020")
                .withShopSku("sku2")
                .withOfferName("offer2")
                .withInitialCount(1)
                .withStatus("Обрабатывается")
                .withChangedStatusTime("25.12.2020")
                .withPaymentType(PaymentType.POSTPAID)
                .withDeliverServiceName("Склад дропшипа")
                .withShipmentDate("02.01.2021")
                .withRegionToName("Московская область")
                .withItemId(2)
                .withBoxesCount(2)
                .withBillingPrice(BigDecimal.valueOf(550.0))
                .build();
        return List.of(item1, item2);
    }

    private JxlsOrdersReportModel createOrder48Items(boolean timeZoneShift) {
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
                .withBoxesCount(1)
                .withBillingPrice(BigDecimal.valueOf(1250.0))
                .build();
    }

    private void mockForOrders() {
        Mockito.when(orderServiceClient.streamOrders(anyLong(), any(OrderListingFilter.class), any(), anyInt()))
                .thenReturn(Stream.of(prepareOrder(), prepareOrder2()));
        Mockito.when(internalOrderApi.internalSearchOrders(any())).thenReturn(new LogisticOrderSearchResponse()
                .orders(List.of(
                        new LogisticOrderInfo()
                                .type(OrderType.FBS)
                                .id(String.valueOf(ORDER_ID))
                                .boxes(List.of(new LogisticOrderBox(), new LogisticOrderBox())),
                        new LogisticOrderInfo()
                                .type(OrderType.FBS)
                                .id(String.valueOf(ORDER_ID_2))
                                .boxes(List.of(new LogisticOrderBox()))
                )));
    }

    private void mockOrdersNotFound() {
        Mockito.when(orderServiceClient.streamOrders(anyLong(), any(OrderListingFilter.class), any(), anyInt()))
                .thenReturn(Stream.empty());
    }

    private PartnerListingOrderPiDto prepareOrder() {
        return new PartnerListingOrderPiDto()
                .orderId(ORDER_ID)
                .partnerId(SUPPLIER_ID)
                .merchantOrderId("order_num_1_1")
                .createdAt(CREATION_DATE)
                .status(ru.yandex.market.orderservice.client.model.OrderStatus.PROCESSING)
                .updatedAt(CHANGED_STATUS_TIME)
                .paymentType(ru.yandex.market.orderservice.client.model.PaymentType.POSTPAID)
                .shipmentDate(SHIPMENT_DATE)
                .lines(List.of(
                        new PartnerOrderItemPiDto()
                                .price(new CurrencyValue()
                                        .currency("RUB")
                                        .value(BigDecimal.valueOf(1270)))
                                .offerName("offer1")
                                .shopSku("sku1")
                                .count(1L)
                                .feedId(10L)
                                .itemId(1L),
                        new PartnerOrderItemPiDto()
                                .price(new CurrencyValue()
                                        .currency("RUB")
                                        .value(BigDecimal.valueOf(550)))
                                .offerName("offer2")
                                .shopSku("sku2")
                                .count(1L)
                                .feedId(20L)
                                .itemId(2L)
                ))
                .deliveryInfo(new PartnerOrderDeliveryPiDto()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .deliveryRegionId(4L));
    }

    private PartnerListingOrderPiDto prepareOrder2() {
        return new PartnerListingOrderPiDto()
                .orderId(ORDER_ID_2)
                .partnerId(SUPPLIER_ID)
                .merchantOrderId("order_num_1_2")
                .createdAt(CREATION_DATE_2)
                .status(ru.yandex.market.orderservice.client.model.OrderStatus.PROCESSING)
                .updatedAt(CHANGED_STATUS_TIME_2)
                .paymentType(ru.yandex.market.orderservice.client.model.PaymentType.POSTPAID)
                .shipmentDate(SHIPMENT_DATE_2)
                .lines(List.of(
                        new PartnerOrderItemPiDto()
                                .price(new CurrencyValue()
                                        .currency("RUB")
                                        .value(BigDecimal.valueOf(1250)))
                                .offerName("offer3")
                                .shopSku("sku3")
                                .count(5L)
                                .feedId(10L)
                                .itemId(3L)
                ))
                .deliveryInfo(new PartnerOrderDeliveryPiDto()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .deliveryRegionId(2L));
    }
}
