package ru.yandex.market.b2b.clients.impl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.b2b.clients.PaymentInvoiceGenerationTaskDao;
import ru.yandex.market.b2b.clients.PaymentInvoiceNumberDao;
import ru.yandex.market.b2b.clients.PaymentInvoiceReportType;
import ru.yandex.market.b2b.clients.PaymentInvoiceService;
import ru.yandex.market.b2b.clients.jreport.ReportService;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.jmf.time.Now;
import ru.yandex.mj.generated.server.model.DocumentDto;
import ru.yandex.mj.generated.server.model.DocumentType;
import ru.yandex.mj.generated.server.model.MultiOrderDto;
import ru.yandex.mj.generated.server.model.OrderCustomerDto;
import ru.yandex.mj.generated.server.model.OrderDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PaymentInvoiceServiceImplTest {

    private static final String bucketName = "bucketName";
    private static final String s3endpoint = "s3endpoint";
    private static final String s3publicEndpoint = "s3publicEndpoint";

    @Mock
    private ReportService reportService;
    @Mock
    private MdsS3Client mdsS3Client;
    @Mock
    private PaymentInvoiceNumberDao invoiceNumberGenerator;
    @Mock
    private PaymentInvoiceGenerationTaskDao dao;

    private PaymentInvoiceService paymentInvoiceService;

    @BeforeEach
    public void beforeEach() {
        paymentInvoiceService = new PaymentInvoiceServiceImpl(
                bucketName,
                s3endpoint,
                s3publicEndpoint,
                reportService,
                mdsS3Client,
                invoiceNumberGenerator,
                dao
        );
    }

    @Test
    public void generateForOneOrder_callsGeneratorsForPdfAndQrCode() throws Exception {
        OrderDto order = orderWithCustomer().id(BigDecimal.ONE);
        doReturn("FileContent".getBytes(StandardCharsets.UTF_8)).when(reportService).generate(any(), any());
        doReturn(task(order)).when(dao).create(eq(order), any());

        paymentInvoiceService.generate(order);
        Thread.sleep(50);

        verify(reportService).generate(PaymentInvoiceReportType.INSTANCE, new OrderDtoWithDate(order));
        verify(reportService).generate(PaymentInvoiceReportType.FAST_PAY_QR, new OrderDtoWithDate(order));
    }

    private OrderDto orderWithCustomer() {
        OrderDto order = new OrderDto();
        order.setCustomer(new OrderCustomerDto().id(1L).buyer("Покупатель"));
        return order;
    }

    @Test
    public void generateForOneOrder_returnsPdfInvoiceDocument() throws Exception {
        // Given
        OrderDto order = orderWithCustomer().id(BigDecimal.ONE);
        doReturn("PdfInvoiceContent".getBytes(StandardCharsets.UTF_8))
                .when(reportService).generate(eq(PaymentInvoiceReportType.INSTANCE), any());
        doReturn("QrCodeContent".getBytes(StandardCharsets.UTF_8))
                .when(reportService).generate(eq(PaymentInvoiceReportType.FAST_PAY_QR), any());
        doReturn(task(order)).when(dao).create(eq(order), any());

        // When
        List<DocumentDto> documents = paymentInvoiceService.generate(order);
        Thread.sleep(50);

        // Then
        DocumentDto pdfDocument = documents.get(0);
        assertEquals(order.getId(), pdfDocument.getOrder());

        assertTrue(pdfDocument.getUrl().contains(order.getId().toString()));
        assertTrue(pdfDocument.getUrl().contains(s3publicEndpoint));
        assertTrue(pdfDocument.getUrl().contains(".pdf"));

        assertEquals(PaymentInvoiceServiceImpl.PAYMENT_INVOICE_NUMBER_PREFIX + order.getId(), pdfDocument.getNumber());
        assertEquals(DocumentType.PAYMENT_INVOICE, pdfDocument.getType());
        assertNotNull(pdfDocument.getDate());
    }

    @Test
    public void generateForOneOrder_returnsQrCodeDocumentA() throws Exception {
        // Given
        OrderDto order = orderWithCustomer().id(BigDecimal.ONE);
        doReturn("PdfInvoiceContent".getBytes(StandardCharsets.UTF_8))
                .when(reportService).generate(eq(PaymentInvoiceReportType.INSTANCE), any());
        doReturn("QrCodeContent".getBytes(StandardCharsets.UTF_8))
                .when(reportService).generate(eq(PaymentInvoiceReportType.FAST_PAY_QR), any());
        doReturn(task(order)).when(dao).create(eq(order), any());


        // When
        List<DocumentDto> documents = paymentInvoiceService.generate(order);
        Thread.sleep(50);

        // Then
        DocumentDto pdfDocument = documents.get(1);
        assertEquals(order.getId(), pdfDocument.getOrder());

        assertTrue(pdfDocument.getUrl().contains(order.getId().toString()));
        assertTrue(pdfDocument.getUrl().contains(s3publicEndpoint));
        assertTrue(pdfDocument.getUrl().contains(".png"));
        assertTrue(pdfDocument.getUrl().contains("qr-"));

        assertEquals(PaymentInvoiceServiceImpl.PAYMENT_INVOICE_NUMBER_PREFIX + order.getId(), pdfDocument.getNumber());
        assertEquals(DocumentType.PAYMENT_INVOICE_FAST_PAY_QR, pdfDocument.getType());
        assertNotNull(pdfDocument.getDate());
    }

    @Test
    public void generateForListOrder_ifEmptyOrdersThenEmptyDocuments() {
        List<OrderDto> orders = Collections.emptyList();
        String multiOrderId = "multiOrderId";
        MultiOrderDto multi = multi(orders, multiOrderId);

        assertTrue(paymentInvoiceService.generate(multi).isEmpty());
    }

    private MultiOrderDto multi(List<OrderDto> orders, String multiOrderId) {
        return new MultiOrderDto().multiOrderId(multiOrderId).orders(orders);
    }

    @Test
    public void generateForListOrder_callsInvoiceNumberGeneratorWithCorrectArguments() throws Exception{
        // Given
        OrderDto order1 = orderWithCustomer().id(BigDecimal.valueOf(2));
        OrderDto order2 = orderWithCustomer().id(BigDecimal.valueOf(1));
        OrderDto order3 = orderWithCustomer().id(BigDecimal.valueOf(3));
        List<OrderDto> orders = List.of(order1, order2, order3);
        MultiOrderDto multi = multi(orders, "multiOrderId");
        doReturn("FileContent".getBytes(StandardCharsets.UTF_8)).when(reportService).generate(any(), any());
        doReturn(BigDecimal.valueOf(12)).when(invoiceNumberGenerator).getInvoiceNumber(any(), any());
        doReturn(task(multi)).when(dao).create(eq(multi), any());

        // When
        paymentInvoiceService.generate(multi);
        Thread.sleep(50);

        // Then
        ArgumentCaptor<List<BigDecimal>> orderIdCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> multiOrderIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(invoiceNumberGenerator).getInvoiceNumber(multiOrderIdCaptor.capture(), orderIdCaptor.capture());

        assertEquals(orders.stream().map(OrderDto::getId).collect(Collectors.toList()), orderIdCaptor.getValue());
        assertEquals("multiOrderId", multiOrderIdCaptor.getValue());
    }

    @Test
    public void generateForListOrder_callsGeneratorsForPdfAndQrCode() throws Exception {
        OrderDto order1 = orderWithCustomer().id(new BigDecimal(1));
        OrderDto order2 = orderWithCustomer().id(new BigDecimal(2));
        List<OrderDto> orders = List.of(order1, order2);
        MultiOrderDto multi = multi(orders, "multiOrderId");
        doReturn("FileContent".getBytes(StandardCharsets.UTF_8)).when(reportService).generate(any(), any());
        BigDecimal generatedInvoiceNumber = BigDecimal.valueOf(12);
        doReturn(generatedInvoiceNumber).when(invoiceNumberGenerator).getInvoiceNumber(any(), any());
        doReturn(task(multi)).when(dao).create(eq(multi), any());


        paymentInvoiceService.generate(multi);
        Thread.sleep(50);

        verify(reportService).generate(PaymentInvoiceReportType.INSTANCE_MULTI,
                new OrdersDtoWithDate(orders, generatedInvoiceNumber));
        verify(reportService).generate(PaymentInvoiceReportType.FAST_PAY_QR,
                new OrdersDtoWithDate(orders, generatedInvoiceNumber));
    }

    @Test
    public void generateForListOrder_returnsPdfInvoiceDocumentForEachOrder() throws Exception{
        // Given
        OrderDto order1 = orderWithCustomer().id(BigDecimal.valueOf(2));
        OrderDto order2 = orderWithCustomer().id(BigDecimal.valueOf(1));
        OrderDto order3 = orderWithCustomer().id(BigDecimal.valueOf(3));
        List<OrderDto> orders = List.of(order1, order2, order3);
        MultiOrderDto multi = multi(orders, "multiOrderId");
        doReturn("FileContent".getBytes(StandardCharsets.UTF_8)).when(reportService).generate(any(), any());
        BigDecimal invoiceNumber = BigDecimal.valueOf(12);
        doReturn(invoiceNumber).when(invoiceNumberGenerator).getInvoiceNumber(any(), any());
        doReturn(task(multi)).when(dao).create(eq(multi), any());

        // When
        List<DocumentDto> documents = paymentInvoiceService.generate(multi);
        Thread.sleep(50);

        // Then
        Iterator<OrderDto> iterator = orders.iterator();
        for (int i = 0; i < documents.size(); i = i + 2) {  // Каждый четный документ должен быть PDF-счетом
            OrderDto orderDto = iterator.next();
            DocumentDto pdfDocument = documents.get(i);
            assertEquals(orderDto.getId(), pdfDocument.getOrder());

            assertTrue(pdfDocument.getUrl().contains(invoiceNumber.toString()));
            assertTrue(pdfDocument.getUrl().contains(s3publicEndpoint));
            assertTrue(pdfDocument.getUrl().contains(".pdf"));

            assertEquals(PaymentInvoiceServiceImpl.PAYMENT_INVOICE_NUMBER_PREFIX + invoiceNumber,
                    pdfDocument.getNumber());
            assertEquals(DocumentType.PAYMENT_INVOICE, pdfDocument.getType());
            assertNotNull(pdfDocument.getDate());
        }
    }

    @Test
    public void generateForListOrder_returnsQrCodeDocumentForEachOrder() throws Exception {
        // Given
        OrderDto order1 = orderWithCustomer().id(BigDecimal.valueOf(2));
        OrderDto order2 = orderWithCustomer().id(BigDecimal.valueOf(1));
        OrderDto order3 = orderWithCustomer().id(BigDecimal.valueOf(3));
        List<OrderDto> orders = List.of(order1, order2, order3);
        MultiOrderDto multi = multi(orders, "multiOrderId");
        doReturn("FileContent".getBytes(StandardCharsets.UTF_8)).when(reportService).generate(any(), any());
        BigDecimal invoiceNumber = BigDecimal.valueOf(12);
        doReturn(invoiceNumber).when(invoiceNumberGenerator).getInvoiceNumber(any(), any());
        doReturn(task(multi)).when(dao).create(eq(multi), any());

        // When
        List<DocumentDto> documents = paymentInvoiceService.generate(multi);
        Thread.sleep(50);

        // Then
        Iterator<OrderDto> iterator = orders.iterator();
        for (int i = 1; i < documents.size(); i = i + 2) {  // Каждый нечетный документ должен быть QR-кодом
            OrderDto orderDto = iterator.next();
            DocumentDto pdfDocument = documents.get(i);
            assertEquals(orderDto.getId(), pdfDocument.getOrder());

            assertTrue(pdfDocument.getUrl().contains(invoiceNumber.toString()));
            assertTrue(pdfDocument.getUrl().contains(s3publicEndpoint));
            assertTrue(pdfDocument.getUrl().contains(".png"));
            assertTrue(pdfDocument.getUrl().contains("qr-"));

            assertEquals(PaymentInvoiceServiceImpl.PAYMENT_INVOICE_NUMBER_PREFIX + invoiceNumber,
                    pdfDocument.getNumber());
            assertEquals(DocumentType.PAYMENT_INVOICE_FAST_PAY_QR, pdfDocument.getType());
            assertNotNull(pdfDocument.getDate());
        }
    }

    @Test
    //@Disabled
    public void generateForOneOrderFromMultiOrder_returnsPdfInvoiceDocument() throws Exception {
        // Given
        OrderDto order = orderWithCustomer().id(BigDecimal.ONE).multiOrderId("aaa-bbb-ccc");
        doReturn("PdfInvoiceContent".getBytes(StandardCharsets.UTF_8))
                .when(reportService).generate(eq(PaymentInvoiceReportType.INSTANCE), any());
        doReturn("QrCodeContent".getBytes(StandardCharsets.UTF_8))
                .when(reportService).generate(eq(PaymentInvoiceReportType.FAST_PAY_QR), any());
        doReturn(BigDecimal.valueOf(10000)).when(invoiceNumberGenerator)
                .getInvoiceNumberForManyOrders(eq("aaa-bbb-ccc"), any(), any());
        doReturn(task(order)).when(dao).create(eq(order), any());

        // When
        List<DocumentDto> documents = paymentInvoiceService.generate(order);
        Thread.sleep(50);

        // Then
        DocumentDto pdfDocument = documents.get(0);
        assertEquals(order.getId(), pdfDocument.getOrder());

        assertTrue(pdfDocument.getUrl().contains(order.getId().toString()));
        assertTrue(pdfDocument.getUrl().contains(s3publicEndpoint));
        assertTrue(pdfDocument.getUrl().contains(".pdf"));

        assertEquals(PaymentInvoiceServiceImpl.PAYMENT_INVOICE_NUMBER_PREFIX + order.getId(), pdfDocument.getNumber());
        assertEquals(DocumentType.PAYMENT_INVOICE, pdfDocument.getType());
        assertNotNull(pdfDocument.getDate());
    }

    @Test
    //@Disabled
    public void generateForOneOrderFromMultiOrder_returnsQrCodeDocument() throws Exception{
        // Given
        OrderDto order = orderWithCustomer().id(BigDecimal.ONE).multiOrderId("aaa-bbb-ccc");
        doReturn("PdfInvoiceContent".getBytes(StandardCharsets.UTF_8))
                .when(reportService).generate(eq(PaymentInvoiceReportType.INSTANCE), any());
        doReturn("QrCodeContent".getBytes(StandardCharsets.UTF_8))
                .when(reportService).generate(eq(PaymentInvoiceReportType.FAST_PAY_QR), any());
        doReturn(BigDecimal.valueOf(10000)).when(invoiceNumberGenerator)
                .getInvoiceNumberForManyOrders(eq("aaa-bbb-ccc"), any(), any());
        doReturn(task(order)).when(dao).create(eq(order), any());

        // When
        List<DocumentDto> documents = paymentInvoiceService.generate(order);
        Thread.sleep(50);

        // Then
        DocumentDto pdfDocument = documents.get(1);
        assertEquals(order.getId(), pdfDocument.getOrder());

        assertTrue(pdfDocument.getUrl().contains(order.getId().toString()));
        assertTrue(pdfDocument.getUrl().contains(s3publicEndpoint));
        assertTrue(pdfDocument.getUrl().contains(".png"));
        assertTrue(pdfDocument.getUrl().contains("qr-"));

        assertEquals(PaymentInvoiceServiceImpl.PAYMENT_INVOICE_NUMBER_PREFIX + order.getId(), pdfDocument.getNumber());
        assertEquals(DocumentType.PAYMENT_INVOICE_FAST_PAY_QR, pdfDocument.getType());
        assertNotNull(pdfDocument.getDate());
    }

    @Test
    // @Disabled
    public void generateForOneOrderFromMultiOrder_returnsZipPaymentsDocument() throws Exception {
        // Given
        OrderDto order = orderWithCustomer().id(BigDecimal.ONE).multiOrderId("aaa-bbb-ccc");
        doReturn("PdfInvoiceContent".getBytes(StandardCharsets.UTF_8))
                .when(reportService).generate(eq(PaymentInvoiceReportType.INSTANCE), any());
        doReturn("QrCodeContent".getBytes(StandardCharsets.UTF_8))
                .when(reportService).generate(eq(PaymentInvoiceReportType.FAST_PAY_QR), any());
        doReturn(BigDecimal.valueOf(10000)).when(invoiceNumberGenerator)
                .getInvoiceNumberForManyOrders(eq("aaa-bbb-ccc"), any(), any());
        doReturn(task(order)).when(dao).create(eq(order), any());

        // When
        List<DocumentDto> documents = paymentInvoiceService.generate(order);
        Thread.sleep(50);

        // Then
        DocumentDto pdfDocument = documents.get(2);
        assertEquals(order.getId(), pdfDocument.getOrder());

        assertTrue(pdfDocument.getUrl().contains("10000-aaa-bbb-ccc.zip"));
        assertTrue(pdfDocument.getUrl().contains(s3publicEndpoint));

        assertEquals(PaymentInvoiceServiceImpl.PAYMENT_INVOICE_NUMBER_PREFIX + order.getId(), pdfDocument.getNumber());
        assertEquals(DocumentType.ZIP_PAYMENT_INVOICES, pdfDocument.getType());
        assertNotNull(pdfDocument.getDate());
    }

    @Test
    public void getOrdersFromMultiOrderTest() {
        // Given
        List<BigDecimal> orderIds = List.of(BigDecimal.valueOf(1), BigDecimal.valueOf(2));
        doReturn(orderIds).when(invoiceNumberGenerator).getOrdersIdForMultiOrder(eq("aaa-bbb-ccc"));

        // When
        List<BigDecimal> result = paymentInvoiceService.getOrdersFromMultiOrder("aaa-bbb-ccc");

        // Then
        assertEquals(orderIds, result);
    }

    private static PaymentInvoiceGenerationTaskDao.Task task(OrderDto order) {
        return new PaymentInvoiceGenerationTaskDao.Task(
                UUID.randomUUID().toString(),
                Now.offsetDateTime(),
                order,
                null,
                null
        );
    }
    private static PaymentInvoiceGenerationTaskDao.Task task(MultiOrderDto order) {
        return new PaymentInvoiceGenerationTaskDao.Task(
                UUID.randomUUID().toString(),
                Now.offsetDateTime(),
                null,
                order,
                null
        );
    }
}
