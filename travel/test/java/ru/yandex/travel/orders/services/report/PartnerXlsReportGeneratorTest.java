package ru.yandex.travel.orders.services.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

import ru.yandex.travel.orders.services.report.model.PartnerOrdersReport;
import ru.yandex.travel.orders.services.report.model.PartnerPaymentOrdersReport;
import ru.yandex.travel.orders.services.report.model.PartnerPayoutsReport;

public class PartnerXlsReportGeneratorTest {
    @Test
    public void testPayoutsReportFromTemplate() throws IOException {
        PartnerXlsReportGenerator partnerXlsReportGenerator = new PartnerXlsReportGenerator();

        PartnerPayoutsReport.TransactionRow pInfo = createTxRow("Оплата");
        PartnerPayoutsReport.TransactionRow pInfo2 = createTxRow("Возврат");
        PartnerPayoutsReport.TransactionRow pInfo3 = createTxRow("Оплата");
        PartnerPayoutsReport.TransactionRow pInfo4 = createTxRow("Возврат");
        PartnerPayoutsReport.TransactionRow pInfo5 = createTxRow("Оплата");


        PartnerPayoutsReport data = new PartnerPayoutsReport();
        data.setTransactions(List.of(pInfo, pInfo2, pInfo3, pInfo4, pInfo5));
        data.setReportName("Реестр бронирований по отчету агента от 30 сентября 2020 для ООО \"Тестовые Рога Без Копыт\"");
        data.setContractData("Договор №123455 от 31.02.2019");
        data.setReportPeriodFrom(LocalDate.now().minusMonths(1));
        data.setReportPeriodTo(LocalDate.now());

        data.setBalanceAtStartOfMonth(-1000.0);
        data.setBalanceAtEndOfMonth(3000.0);
        data.setTransferAmount(4000.0);
        data.setTransferredAmount(3000.0);

        File tempFile = File.createTempFile("payoutReport", ".xlsx");
        System.out.println(tempFile.getAbsolutePath());

        FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        partnerXlsReportGenerator.generatePayoutReport("Реестр транзакций за март 2020", data, fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    @Test
    public void testEmptyPayoutsReportFromTemplate() throws IOException {
        PartnerXlsReportGenerator partnerXlsReportGenerator = new PartnerXlsReportGenerator();

        PartnerPayoutsReport data = new PartnerPayoutsReport();
        data.setTransactions(List.of());
        data.setReportName("Реестр бронирований по отчету агента от 30 сентября 2020 для ООО \"Тестовые Рога Без Копыт\"");
        data.setContractData("Договор №123455 от 31.02.2019");
        data.setReportPeriodFrom(LocalDate.now().minusMonths(1));
        data.setReportPeriodTo(LocalDate.now());

        data.setBalanceAtStartOfMonth(-1000.0);
        data.setBalanceAtEndOfMonth(3000.0);
        data.setTransferAmount(4000.0);
        data.setTransferredAmount(3000.0);

        File tempFile = File.createTempFile("payoutReport", ".xlsx");
        System.out.println(tempFile.getAbsolutePath());

        FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        partnerXlsReportGenerator.generatePayoutReport("Реестр транзакций за март 2020", data, fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    @Test
    public void testOrdersReportFromTemplate() throws IOException {
        PartnerXlsReportGenerator partnerXlsReportGenerator = new PartnerXlsReportGenerator();

        PartnerOrdersReport.TransactionRow pInfo = createOrderTxRow();
        PartnerOrdersReport.TransactionRow pInfo2 = createOrderTxRow();
        PartnerOrdersReport.TransactionRow pInfo3 = createOrderTxRow();
        PartnerOrdersReport.TransactionRow pInfo4 = createOrderTxRow();

        PartnerOrdersReport data = new PartnerOrdersReport();
        data.setTransactions(List.of(pInfo, pInfo2, pInfo3, pInfo4));
        data.setReportName("Реестр завершенных заказов за март 2020");
        data.setReportPeriodFrom(LocalDate.now().minusMonths(1));
        data.setReportPeriodTo(LocalDate.now());

        File tempFile = File.createTempFile("payoutOrders", ".xlsx");
        System.out.println(tempFile.getAbsolutePath());

        FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        partnerXlsReportGenerator.generateOrdersReport("Реестр заказов за март 2020", data, fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    @Test
    public void testEmptyOrdersReportFromTemplate() throws IOException {
        PartnerXlsReportGenerator partnerXlsReportGenerator = new PartnerXlsReportGenerator();

        PartnerOrdersReport data = new PartnerOrdersReport();
        data.setTransactions(List.of());
        data.setReportName("Реестр завершенных заказов за март 2020");
        data.setReportPeriodFrom(LocalDate.now().minusMonths(1));
        data.setReportPeriodTo(LocalDate.now());

        File tempFile = File.createTempFile("payoutOrders", ".xlsx");
        System.out.println(tempFile.getAbsolutePath());

        FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        partnerXlsReportGenerator.generateOrdersReport("Реестр заказов за март 2020", data, fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    @Test
    public void testPaymentOrdersReportFromTemplate() throws IOException {
        PartnerXlsReportGenerator partnerXlsReportGenerator = new PartnerXlsReportGenerator();

        PartnerPaymentOrdersReport.TransactionRow pInfo = createPaymentOrderTxRow("Оплата");
        PartnerPaymentOrdersReport.TransactionRow pInfo2 = createPaymentOrderTxRow("Возврат");
        PartnerPaymentOrdersReport.TransactionRow pInfo3 = createPaymentOrderTxRow("Оплата");
        PartnerPaymentOrdersReport.TransactionRow pInfo4 = createPaymentOrderTxRow("Возврат");
        PartnerPaymentOrdersReport.TransactionRow pInfo5 = createPaymentOrderTxRow("Оплата");


        PartnerPaymentOrdersReport data = new PartnerPaymentOrdersReport();
        data.setTransactions(List.of(pInfo, pInfo2, pInfo3, pInfo4, pInfo5));
        data.setReportName("Реестр платежного поручения №22567 от 30 сентября 2020 для ООО \"Тестовые Рога Без Копыт\"");
        data.setContractData("Договор №123455 от 31.02.2019");
        data.setBankOrderDescription("П/п №22567 от 30.09.2020 Р/сч 4070281003825012301 в ПАО СБЕРБАНК (044525225), \"ООО \"Тестовые Рога Без Копыт\"");


        File tempFile = File.createTempFile("paymentOrdersReport", ".xlsx");
        System.out.println(tempFile.getAbsolutePath());

        FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        partnerXlsReportGenerator.generatePaymentOrdersReport("Реестр Пп №22567 от 30-06-2020", data, fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    @Test
    public void testEmptyPaymentOrdersReportFromTemplate() throws IOException {
        PartnerXlsReportGenerator partnerXlsReportGenerator = new PartnerXlsReportGenerator();

        PartnerPaymentOrdersReport data = new PartnerPaymentOrdersReport();
        data.setReportName("Реестр платежного поручения №22567 от 30 сентября 2020 для ООО \"Тестовые Рога Без Копыт\"");
        data.setContractData("Договор №123455 от 31.02.2019");
        data.setBankOrderDescription("П/п №22567 от 30.09.2020 Р/сч 40702810038250123010 в ПАО СБЕРБАНК (044525225), \"ООО \"Тестовые Рога Без Копыт\"");

        File tempFile = File.createTempFile("paymentOrdersReport", ".xlsx");
        System.out.println(tempFile.getAbsolutePath());

        FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        partnerXlsReportGenerator.generatePaymentOrdersReport("Реестр Пп №22567 от 30-06-2020", data, fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    private PartnerOrdersReport.TransactionRow createOrderTxRow() {
        PartnerOrdersReport.TransactionRow pInfo = new PartnerOrdersReport.TransactionRow();
        pInfo.setHotelName("Hotel 1");
        pInfo.setPrettyId("YA-2023-3033-2123");
        pInfo.setPartnerId("2020023123-320302");
        pInfo.setGuestName("Guest Name");
        pInfo.setBookedAt(LocalDate.now().minusDays(2));
        pInfo.setCheckIn(LocalDate.of(2020, 8, 10));
        pInfo.setCheckOut(LocalDate.of(2020, 8, 12));
        pInfo.setType("Возврат");
        pInfo.setTotalAmount(1000.0);
        pInfo.setPartnerAmount(870.0);
        pInfo.setFeeAmount(130);
        return pInfo;
    }

    private PartnerPayoutsReport.TransactionRow createTxRow(String txType) {
        PartnerPayoutsReport.TransactionRow pInfo = new PartnerPayoutsReport.TransactionRow();
        pInfo.setHotelName("Hotel 1");
        pInfo.setPrettyId("YA-2023-3033-2123");
        pInfo.setPartnerId("2020023123-320302");
        pInfo.setGuestName("Guest Name");
        pInfo.setBookedAt(LocalDate.now().minusDays(2));
        pInfo.setCheckIn(LocalDate.of(2020, 8, 10));
        pInfo.setCheckOut(LocalDate.of(2020, 8, 12));
        pInfo.setTxType(txType);
        pInfo.setTxDate(LocalDate.now().minusDays(ThreadLocalRandom.current().nextInt(2, 5)));
        if ("Возврат".equals(txType)) {
            pInfo.setTotalRefundAmount(1000.0);
        } else {
            pInfo.setTotalAmount(1000.0);
        }
        pInfo.setPaymentDate(LocalDate.now());
        pInfo.setPaymentOrderNumber("123-456");
        pInfo.setPartnerAmount(870.0);
        pInfo.setFeeAmount(130);
        return pInfo;
    }

    private PartnerPaymentOrdersReport.TransactionRow createPaymentOrderTxRow(String txType) {
        PartnerPaymentOrdersReport.TransactionRow pInfo = new PartnerPaymentOrdersReport.TransactionRow();
        pInfo.setHotelName("Hotel 1");
        pInfo.setPrettyId("YA-2023-3033-2123");
        pInfo.setPartnerId("2020023123-320302");
        pInfo.setGuestName("Guest Name");
        pInfo.setBookedAt(LocalDate.now().minusDays(ThreadLocalRandom.current().nextInt(2, 5)));
        pInfo.setCheckIn(LocalDate.of(2020, 8, 10));
        pInfo.setCheckOut(LocalDate.of(2020, 8, 12));
        pInfo.setTxType(txType);
        pInfo.setTotalAmount(1000.0);
        pInfo.setPartnerAmount(870.0);
        return pInfo;
    }
}
