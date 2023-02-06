package ru.yandex.market.partner.mvc.controller.delivery;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.StatusDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.filter.BalancePaymentReportFilter;
import ru.yandex.market.logistics.lom.model.report.dto.BalancePaymentDto;
import ru.yandex.market.logistics.lom.model.report.dto.BalancePaymentOrderDto;
import ru.yandex.market.logistics.lom.model.report.dto.BalancePaymentReportDto;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.model.NamedEntity;
import ru.yandex.market.logistics.nesu.client.model.ShopWithSendersDto;
import ru.yandex.market.logistics.nesu.client.model.filter.ShopWithSendersFilter;
import ru.yandex.market.partner.delivery.report.OrderPaymentsReportGenerator;
import ru.yandex.market.partner.mvc.controller.BaseReportControllerTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;
import ru.yandex.market.personal_market.PersonalMarketService;
import ru.yandex.market.personal_market.PersonalRetrieveResponse;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "OrderPaymentsDaasReportControllerTest.before.csv")
class OrderPaymentsDaasReportControllerTest extends BaseReportControllerTest {
    private static final String TIMESPAN_LABEL = "Период:";

    private static final long CONTRACT_1 = 10774L;
    private static final long CONTRACT_2 = 10775L;

    private static final long SHOP_1 = 774L;
    private static final long SHOP_2 = 775L;

    private static final long MARKET_ID = 10001L;

    private static final long SENDER_1 = 1L;
    private static final long SENDER_2 = 2L;
    private static final long SENDER_3 = 3L;

    private static final ShopWithSendersDto SHOP_WITH_SENDERS_1 = ShopWithSendersDto.builder()
            .id(SHOP_1)
            .balanceContractId(CONTRACT_1)
            .marketId(MARKET_ID)
            .name("Настолки Инкорпорейтид")
            .senders(List.of(
                    NamedEntity.builder()
                            .id(SENDER_1)
                            .name("Крокодил")
                            .build()
            ))
            .build();

    private static final ShopWithSendersDto SHOP_WITH_SENDERS_2 = ShopWithSendersDto.builder()
            .id(SHOP_2)
            .balanceContractId(CONTRACT_2)
            .marketId(MARKET_ID)
            .name("Parcel Store")
            .senders(List.of(
                    NamedEntity.builder()
                            .id(SENDER_2)
                            .name("Black Parcel")
                            .build(),
                    NamedEntity.builder()
                            .id(SENDER_3)
                            .name("Strong Parcel")
                            .build()
            ))
            .build();

    private static final BalancePaymentReportDto LOM_ORDER_1 = BalancePaymentReportDto.builder()
            .order(BalancePaymentOrderDto.builder()
                    .id(1L)
                    .barcode("order-bar-1")
                    .externalId("order-ext-1")
                    .recipientLastName("test-last-name-1")
                    .recipientFirstName("test-first-name-1")
                    .status(StatusDto.builder()
                            .segmentStatus(SegmentStatus.OUT)
                            .partnerType(PartnerType.DELIVERY)
                            .build()
                    )
                    .senderId(SENDER_1)
                    .partner(ru.yandex.market.logistics.lom.model.dto.NamedEntity.builder()
                            .id(69L)
                            .name("Вот такая СД")
                            .build())
                    .orderCost(new BigDecimal("56.25"))
                    .totalCost(new BigDecimal("56.25"))
                    .cashServicePercent(new BigDecimal("1.7"))
                    .deliveredDate(ZonedDateTime.of(2019, 1, 3, 0, 0, 0, 0, DateTimes.MOSCOW_TIME_ZONE))
                    .returnedDate(ZonedDateTime.of(2019, 1, 4, 0, 0, 0, 0, DateTimes.MOSCOW_TIME_ZONE))
                    .build())
            .payments(List.of(BalancePaymentDto.builder()
                    .orderId(123461L)
                    .build()))
            .build();

    private static final BalancePaymentReportDto LOM_ORDER_2 = BalancePaymentReportDto.builder()
            .order(BalancePaymentOrderDto.builder()
                    .id(2L)
                    .barcode("order-bar-2")
                    .externalId("order-ext-2")
                    .recipientLastName("test-last-name-2")
                    .recipientFirstName("test-first-name-2")
                    .recipientMiddleName("test-middle-name-2")
                    .status(StatusDto.builder()
                            .segmentStatus(SegmentStatus.OUT)
                            .partnerType(PartnerType.DELIVERY)
                            .build()
                    )
                    .senderId(SENDER_1)
                    .partner(ru.yandex.market.logistics.lom.model.dto.NamedEntity.builder()
                            .id(69L)
                            .name("Вот такая СД")
                            .build())
                    .orderCost(new BigDecimal("300"))
                    .totalCost(new BigDecimal("300"))
                    .cashServicePercent(new BigDecimal("1.7"))
                    .deliveredDate(ZonedDateTime.of(2019, 1, 3, 0, 0, 0, 0, DateTimes.MOSCOW_TIME_ZONE))
                    .returnedDate(ZonedDateTime.of(2019, 1, 3, 0, 0, 0, 0, DateTimes.MOSCOW_TIME_ZONE))
                    .build())
            .payments(List.of(BalancePaymentDto.builder()
                    .orderId(123462L)
                    .build()))
            .build();

    private static final BalancePaymentReportDto LOM_ORDER_3 = BalancePaymentReportDto.builder()
            .order(BalancePaymentOrderDto.builder()
                    .id(3L)
                    .barcode("order-bar-3")
                    .externalId("order-ext-3")
                    .status(StatusDto.builder()
                            .segmentStatus(SegmentStatus.OUT)
                            .partnerType(PartnerType.DELIVERY)
                            .build()
                    )
                    .senderId(SENDER_2)
                    .partner(ru.yandex.market.logistics.lom.model.dto.NamedEntity.builder()
                            .id(69L)
                            .name("LGBT-4000")
                            .build())
                    .orderCost(new BigDecimal("500"))
                    .totalCost(new BigDecimal("500"))
                    .cashServicePercent(new BigDecimal("2.2"))
                    .deliveredDate(ZonedDateTime.of(2019, 1, 3, 0, 0, 0, 0, DateTimes.MOSCOW_TIME_ZONE))
                    .build())
            .payments(List.of(BalancePaymentDto.builder()
                    .orderId(123463L)
                    .build()))
            .build();

    private static final BalancePaymentReportDto LOM_ORDER_4 = BalancePaymentReportDto.builder()
            .order(BalancePaymentOrderDto.builder()
                    .id(4L)
                    .barcode("order-bar-4")
                    .externalId("order-ext-4")
                    .status(StatusDto.builder()
                            .segmentStatus(SegmentStatus.OUT)
                            .partnerType(PartnerType.DELIVERY)
                            .build()
                    )
                    .senderId(SENDER_3)
                    .partner(ru.yandex.market.logistics.lom.model.dto.NamedEntity.builder()
                            .id(69L)
                            .name("Парселовоз")
                            .build())
                    .orderCost(new BigDecimal("100.50"))
                    .totalCost(new BigDecimal("100.50"))
                    .cashServicePercent(new BigDecimal("2.2"))
                    .deliveredDate(ZonedDateTime.of(2019, 1, 3, 0, 0, 0, 0, DateTimes.MOSCOW_TIME_ZONE))
                    .build())
            .payments(List.of(BalancePaymentDto.builder()
                    .orderId(123464L)
                    .build()))
            .build();

    private static final BalancePaymentReportDto LOM_ORDER_5 = BalancePaymentReportDto.builder()
            .order(BalancePaymentOrderDto.builder()
                    .id(5L)
                    .barcode("order-bar-5")
                    .externalId("order-ext-5")
                    .status(StatusDto.builder()
                            .segmentStatus(SegmentStatus.OUT)
                            .partnerType(PartnerType.DELIVERY)
                            .build()
                    )
                    .senderId(SENDER_3)
                    .partner(ru.yandex.market.logistics.lom.model.dto.NamedEntity.builder()
                            .id(69L)
                            .name("Парселовоз")
                            .build())
                    .orderCost(new BigDecimal("200"))
                    .totalCost(new BigDecimal("200"))
                    .cashServicePercent(new BigDecimal("2.2"))
                    .deliveredDate(ZonedDateTime.of(2019, 1, 3, 0, 0, 0, 0, DateTimes.MOSCOW_TIME_ZONE))
                    .build())
            .payments(List.of(BalancePaymentDto.builder()
                    .orderId(123490L)
                    .build()))
            .build();

    @Autowired
    private LomClient lomClient;

    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private PersonalMarketService personalMarketService;

    /**
     * Тестирование генерации отчета по платежам.
     */
    @Test
    void generateReportTest() {
        when(personalMarketService.retrieve(any())).thenReturn(
                CompletableFuture.completedFuture(new PersonalRetrieveResponse(List.of()))
        );

        when(nesuClient.searchShopWithSenders(
                ShopWithSendersFilter.builder()
                        .shopIds(Set.of(SHOP_1))
                        .build()
        )).thenReturn(List.of(SHOP_WITH_SENDERS_1));

        when(nesuClient.searchShopWithSenders(
                ShopWithSendersFilter.builder()
                        .marketIds(Set.of(MARKET_ID))
                        .build()
        )).thenReturn(List.of(
                SHOP_WITH_SENDERS_1,
                SHOP_WITH_SENDERS_2
        ));

        when(lomClient.getBalancePaymentReport(
                BalancePaymentReportFilter.builder()
                        .platformClientId(3L)
                        .dateFrom(ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, DateTimes.MOSCOW_TIME_ZONE))
                        .dateTo(ZonedDateTime.of(2019, 1, 31, 23, 59, 59, 999999999, DateTimes.MOSCOW_TIME_ZONE))
                        .senderIds(Set.of(SENDER_1, SENDER_2, SENDER_3))
                        .balanceOrderIds(Set.of(123461L, 123462L, 123463L, 123464L))
                        .build()
        )).thenReturn(List.of(LOM_ORDER_1, LOM_ORDER_2, LOM_ORDER_3, LOM_ORDER_4, LOM_ORDER_5));

        Sheet sheet = queryForSheet(
                String.format(
                        "/daas/reports/order-payments?dateFrom=%s&dateTo=%s&datasource_id=774",
                        "2019-01-01",
                        "2019-01-31"
                )
        );

        commonAssert(sheet, 21, 2);

        shopHeaderAssert(sheet, 5, "Parcel Store / Black Parcel");
        rowAssert(sheet, 6, "Платеж", "order-bar-3", "order-ext-3", "", "LGBT-4000", "Доставлен", "03.01.2019", "",
                500D, 500D, 500D, "100500", "09.01.2019", "trust_payment_id_4", "14.02.2019", 2.2, 11D);
        totalAssert(sheet, 7, 500D, 500D, 500D, 11D, false);
        emptyRowAssert(sheet, 8);

        shopHeaderAssert(sheet, 9, "Parcel Store / Strong Parcel");
        rowAssert(sheet, 10, "Платеж", "order-bar-4", "order-ext-4", "", "Парселовоз", "Доставлен", "03.01.2019", "",
                100.5, 100.5, 50D, "100501", "15.01.2019", "trust_payment_id_5", "14.01.2019", 2.2, 1.1);
        rowAssert(sheet, 11, "Платеж", "order-bar-4", "order-ext-4", "", "Парселовоз", "Доставлен", "03.01.2019", "",
                100.5, 100.5, 50.5, "100502", "16.01.2019", "trust_payment_id_6", "16.01.2019", 2.2, 1.11);
        rowAssert(sheet, 12, "Платеж", "order-bar-5", "order-ext-5", "", "Парселовоз", "Доставлен", "03.01.2019", "",
                200D, 200D, 0.0, "", "", "", "", 2.2, 0.0);
        totalAssert(sheet, 13, 401D, 401D, 100.5D, 2.21D, false);
        emptyRowAssert(sheet, 14);

        shopHeaderAssert(sheet, 15, "Настолки Инкорпорейтид / Крокодил");
        rowAssert(sheet, 16, "Возврат", "order-bar-1", "order-ext-1", "test-last-name-1 test-first-name-1", "Вот " +
                        "такая СД", "Доставлен", "03.01.2019", "04.01.2019",
                56.25, 56.25, -56.25, "100499", "31.12.2018", "trust_payment_id_2", "14.01.2019", 1.7, -0.95);
        rowAssert(sheet, 17, "Платеж", "order-bar-2", "order-ext-2", "test-last-name-2 test-first-name-2 " +
                        "test-middle-name-2", "Вот такая СД", "Доставлен", "03.01.2019", "",
                300D, 300D, 300D, "100499", "31.12.2018", "trust_payment_id_3", "14.01.2019", 1.7, 5.1);
        totalAssert(sheet, 18, 356.25D, 356.25D, 243.75D, 4.15D, false);
        emptyRowAssert(sheet, 19);

        totalAssert(sheet, 20, 1257.25D, 1257.25D, 844.25D, 17.36D, true);
    }

    @Test
    void generateReportInvalidRangeTest() {
        badRequestAssert(
                "/daas/reports/order-payments?dateFrom=2019-01-01&dateTo=2019-02-02&datasource_id=774",
                "json/daas-report-invalid-dates-range.json"
        );
    }

    @Test
    void generateReportToDateBeforeFromDateTest() {
        badRequestAssert(
                "/daas/reports/order-payments?dateFrom=2019-01-10&dateTo=2019-01-02&datasource_id=774",
                "json/daas-report-to-is-before-from.json"
        );
    }

    private void badRequestAssert(String url, String jsonPath) {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + url)
        );
        assertThat(exception.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(), jsonPath);

    }

    private void totalAssert(Sheet sheet, int rowNum, Double orderCost, Double paidByCustomer,
                             Double paidToShop, Double totalCashServiceSum, boolean mainTotal) {
        Row row = sheet.getRow(rowNum);
        assertThat(
                row.getCell(0).getStringCellValue(),
                is(mainTotal ? OrderPaymentsReportGenerator.TOTAL_ALL : OrderPaymentsReportGenerator.TOTAL)
        );
        assertThat(row.getCell(OrderPaymentsReportGenerator.ORDER_COST_COL_NUM).getNumericCellValue(), is(orderCost));
        assertThat(row.getCell(OrderPaymentsReportGenerator.PAID_BY_CUSTOMER_COL_NUM).getNumericCellValue(),
                is(paidByCustomer));
        assertThat(row.getCell(OrderPaymentsReportGenerator.PAID_TO_SHOP_COL_NUM).getNumericCellValue(),
                is(paidToShop));
        assertThat(row.getCell(OrderPaymentsReportGenerator.CASH_SERVICE_SUM_COL_NUM).getNumericCellValue(),
                is(totalCashServiceSum));
    }

    private void emptyRowAssert(Sheet sheet, int rowNum) {
        Row row = sheet.getRow(rowNum);
        assertNull(row);
    }

    private void rowAssert(
            Sheet sheet, int rowNum, String type, String orderId, String orderExtId, String recipientFio,
            String deliveryService, String status, String deliveryDate, String returnDate,
            Double orderCost, Double paidByCustomer, Double paidToShop, String bankOrderId,
            String bankOrderDate, String trustPaymentId, String registryDate,
            Double cashCommissionPercent, Double cashCommissionSum
    ) {
        Row row = sheet.getRow(rowNum);
        int col = 0;

        assertThat(row.getCell(col++).getStringCellValue(), is(type));
        assertThat(row.getCell(col++).getStringCellValue(), is(orderId));
        assertThat(row.getCell(col++).getStringCellValue(), is(orderExtId));
        assertThat(row.getCell(col++).getStringCellValue(), is(recipientFio));
        assertThat(row.getCell(col++).getStringCellValue(), is(deliveryService));
        assertThat(row.getCell(col++).getStringCellValue(), is(status));
        assertThat(row.getCell(col++).getStringCellValue(), is(deliveryDate));
        assertThat(row.getCell(col++).getStringCellValue(), is(returnDate));
        assertThat(row.getCell(col++).getNumericCellValue(), is(orderCost));
        assertThat(row.getCell(col++).getNumericCellValue(), is(paidByCustomer));
        assertThat(row.getCell(col++).getNumericCellValue(), is(paidToShop));
        assertThat(row.getCell(col++).getStringCellValue(), is(bankOrderId));
        assertThat(row.getCell(col++).getStringCellValue(), is(bankOrderDate));
        assertThat(row.getCell(col++).getStringCellValue(), is(trustPaymentId));
        assertThat(row.getCell(col++).getStringCellValue(), is(registryDate));
        assertThat(row.getCell(col++).getNumericCellValue(), is(cashCommissionPercent));
        assertThat(row.getCell(col++).getNumericCellValue(), is(cashCommissionSum));
    }

    private void shopHeaderAssert(Sheet sheet, int rowNum, String text) {
        Row row = sheet.getRow(rowNum);
        assertThat(row.getCell(0).getStringCellValue(), is(text));
    }

    private void commonAssert(Sheet sheet, int expectedRowCount, int periodRowNum) {
        // проверяем количество строк
        assertThat(sheet.getLastRowNum() + 1, is(expectedRowCount));
        // проверяем период в шапке отчета
        assertThat(sheet.getRow(periodRowNum).getCell(0).getStringCellValue(), is(TIMESPAN_LABEL));
        assertThat(sheet.getRow(periodRowNum).getCell(1).getStringCellValue(), is("01.01.2019 - 31.01.2019"));
    }
}
