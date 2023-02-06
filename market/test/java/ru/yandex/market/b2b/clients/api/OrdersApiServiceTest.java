package ru.yandex.market.b2b.clients.api;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import ru.yandex.market.b2b.clients.AbstractFunctionalTest;
import ru.yandex.market.b2b.clients.Documents;
import ru.yandex.market.b2b.clients.ForwardableDocumentDao;
import ru.yandex.market.b2b.clients.common.ForwardableDocumentStatus;
import ru.yandex.market.b2b.clients.impl.dto.ForwardableDocumentDto;
import ru.yandex.mj.generated.server.model.DocumentDto;
import ru.yandex.mj.generated.server.model.DocumentResponseDto;
import ru.yandex.mj.generated.server.model.DocumentType;
import ru.yandex.mj.generated.server.model.MultiOrderDto;
import ru.yandex.mj.generated.server.model.OrderCustomerDto;
import ru.yandex.mj.generated.server.model.OrderDeliveryDto;
import ru.yandex.mj.generated.server.model.OrderDto;
import ru.yandex.mj.generated.server.model.OrderItemDto;
import ru.yandex.mj.generated.server.model.Vat;

public class OrdersApiServiceTest extends AbstractFunctionalTest {

    @Autowired
    OrdersApiService api;
    @Autowired
    ForwardableDocumentDao forwardableDocumentDao;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "document", "document_forwardable");
    }

    @Test
    public void addDocument() {
        // Given
        DocumentDto document = Documents.random();
        if (DocumentType.ZIP_PAYMENT_INVOICES.equals(document.getType())) { // getOrderDocuments не отдает архивы
            document.setType(DocumentType.PAYMENT_INVOICE);
        }
        api.addOrderDocuments(List.of(document));

        // When
        ResponseEntity<List<DocumentResponseDto>> result = api.getOrderDocuments(document.getOrder(), null, false);

        // Then
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        List<DocumentResponseDto> body = result.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(1, body.size());
        DocumentResponseDto d = body.get(0);
        Documents.assertEquals(document, d);
    }

    @Test
    public void generateMultiPaymentInvoice_generatesTheSameDocumentsForOrdersInMultiOrder() throws Exception {
        // Given
        String multiOrderId = "multiOrderId";

        BigDecimal order1Id = BigDecimal.valueOf(1);
        OrderItemDto item1 = new OrderItemDto()
                .id(BigDecimal.valueOf(1))
                .name("Товар №1")
                .count(1)
                .price(BigDecimal.valueOf(10))
                .total(BigDecimal.valueOf(10))
                .vat(Vat.VAT_20);
        OrderDto order1 = new OrderDto()
                .id(order1Id)
                .customer(customer())
                .items(List.of(item1))
                .delivery(new OrderDeliveryDto().total(BigDecimal.valueOf(60)).vat(Vat.VAT_20));

        BigDecimal order2Id = BigDecimal.valueOf(2);
        OrderItemDto item2 = new OrderItemDto()
                .id(BigDecimal.valueOf(2))
                .name("Товар №2")
                .count(2)
                .price(BigDecimal.valueOf(20))
                .total(BigDecimal.valueOf(40))
                .vat(Vat.VAT_20);
        OrderDto order2 = new OrderDto()
                .id(order2Id)
                .customer(customer())
                .items(List.of(item2))
                .delivery(new OrderDeliveryDto().total(BigDecimal.valueOf(50)).vat(Vat.VAT_20));
        List<OrderDto> orders = List.of(order1, order2);
        MultiOrderDto multi = multi(orders, multiOrderId);

        // When
        DocumentResponseDto pdfInvoiceDocument = api.generateMultiPaymentInvoice(multi).getBody();

        Thread.sleep(1000L);

        // Then
        List<DocumentResponseDto> order1Documents = api.getOrderDocuments(order1Id, null, false).getBody();
        List<DocumentResponseDto> order2Documents = api.getOrderDocuments(order2Id, null, false).getBody();

        List<DocumentResponseDto> pdfInvoiceOrder1 = pdfInvoice(order1Documents);
        List<DocumentResponseDto> pdfInvoiceOrder2 = pdfInvoice(order2Documents);
        Assertions.assertEquals(1 , pdfInvoiceOrder1.size());
        Assertions.assertEquals(1 , pdfInvoiceOrder2.size());
        Assertions.assertEquals(pdfInvoiceOrder1.get(0).getUrl(), pdfInvoiceOrder2.get(0).getUrl());
        Assertions.assertEquals(pdfInvoiceOrder1.get(0).getDate(), pdfInvoiceOrder2.get(0).getDate());
        Assertions.assertEquals(pdfInvoiceOrder1.get(0).getNumber(), pdfInvoiceOrder2.get(0).getNumber());
        Assertions.assertEquals(DocumentType.PAYMENT_INVOICE, pdfInvoiceOrder1.get(0).getType());
        Assertions.assertEquals(DocumentType.PAYMENT_INVOICE, pdfInvoiceOrder2.get(0).getType());
        Assertions.assertEquals(order1Id, pdfInvoiceOrder1.get(0).getOrder());
        Assertions.assertEquals(order2Id, pdfInvoiceOrder2.get(0).getOrder());

        Assertions.assertEquals(pdfInvoiceDocument.getUrl(), pdfInvoiceOrder1.get(0).getUrl());
        Assertions.assertEquals(pdfInvoiceDocument.getDate().truncatedTo(ChronoUnit.MILLIS).toInstant(),
                pdfInvoiceOrder1.get(0).getDate().truncatedTo(ChronoUnit.MILLIS).toInstant());
        Assertions.assertEquals(pdfInvoiceDocument.getNumber(), pdfInvoiceOrder1.get(0).getNumber());
        Assertions.assertEquals(pdfInvoiceDocument.getType(), DocumentType.PAYMENT_INVOICE);

        List<DocumentResponseDto> qrCodeOrder1 = qrCode(order1Documents);
        List<DocumentResponseDto> qrCodeOrder2 = qrCode(order2Documents);
        Assertions.assertEquals(1 , qrCodeOrder1.size());
        Assertions.assertEquals(1 , qrCodeOrder2.size());
        Assertions.assertEquals(qrCodeOrder1.get(0).getUrl(), qrCodeOrder2.get(0).getUrl());
        Assertions.assertEquals(qrCodeOrder1.get(0).getDate(), qrCodeOrder2.get(0).getDate());
        Assertions.assertEquals(qrCodeOrder1.get(0).getNumber(), qrCodeOrder2.get(0).getNumber());
        Assertions.assertEquals(DocumentType.PAYMENT_INVOICE_FAST_PAY_QR, qrCodeOrder1.get(0).getType());
        Assertions.assertEquals(DocumentType.PAYMENT_INVOICE_FAST_PAY_QR, qrCodeOrder2.get(0).getType());
        Assertions.assertEquals(order1Id, qrCodeOrder1.get(0).getOrder());
        Assertions.assertEquals(order2Id, qrCodeOrder2.get(0).getOrder());
    }

    private MultiOrderDto multi(List<OrderDto> orders, String multiOrderId) {
        return new MultiOrderDto().multiOrderId(multiOrderId).orders(orders);
    }

    private List<DocumentResponseDto> qrCode(List<DocumentResponseDto> documents) {
        return documents.stream()
                .filter(doc -> DocumentType.PAYMENT_INVOICE_FAST_PAY_QR.equals(doc.getType()))
                .collect(Collectors.toList());
    }

    private List<DocumentResponseDto> pdfInvoice(List<DocumentResponseDto> documents) {
        return documents.stream()
                .filter(doc -> DocumentType.PAYMENT_INVOICE.equals(doc.getType()))
                .collect(Collectors.toList());
    }

    private OrderCustomerDto customer() {
        return new OrderCustomerDto()
                .id(2L)
                .buyer("Имя покупателя")
                .phone("Телефон покупателя");
    }

    @Test
    public void generateMultiPaymentInvoice_callsAreIdempotentRegardingInvoiceId() throws Exception {
        // Given
        String multiOrderId = "multiOrderId";

        BigDecimal order1Id = BigDecimal.valueOf(1);
        OrderItemDto item1 = new OrderItemDto()
                .id(BigDecimal.valueOf(1))
                .name("Товар №1")
                .count(1)
                .price(BigDecimal.valueOf(10))
                .total(BigDecimal.valueOf(10))
                .vat(Vat.VAT_20);
        OrderDto order1 = new OrderDto()
                .id(order1Id)
                .customer(customer())
                .items(List.of(item1))
                .delivery(new OrderDeliveryDto().total(BigDecimal.valueOf(60)).vat(Vat.VAT_20));

        BigDecimal order2Id = BigDecimal.valueOf(2);
        OrderItemDto item2 = new OrderItemDto()
                .id(BigDecimal.valueOf(2))
                .name("Товар №2")
                .count(2)
                .price(BigDecimal.valueOf(20))
                .total(BigDecimal.valueOf(40))
                .vat(Vat.VAT_20);
        OrderDto order2 = new OrderDto()
                .id(order2Id)
                .customer(customer())
                .items(List.of(item2))
                .delivery(new OrderDeliveryDto().total(BigDecimal.valueOf(50)).vat(Vat.VAT_20));
        List<OrderDto> orders = List.of(order1, order2);
        MultiOrderDto multi = multi(orders, multiOrderId);

        // When
        DocumentResponseDto pdfInvoiceDocumentFirstTime = api.generateMultiPaymentInvoice(multi).getBody();

        Thread.sleep(1000);
        DocumentResponseDto pdfInvoiceDocumentSecondTime = api.generateMultiPaymentInvoice(multi).getBody();
        Thread.sleep(1000);

        // Then
        Assertions.assertEquals(pdfInvoiceDocumentFirstTime.getNumber(), pdfInvoiceDocumentSecondTime.getNumber());
    }

    @Test
    public void addDocumentForwardable() {
        DocumentDto originDocument = Documents.random(true);
        api.addOrderDocuments(List.of(originDocument));

        List<ForwardableDocumentDto> forwardableDocuments = forwardableDocumentDao.getPendingsForUpdate(100);
        Assertions.assertEquals(1, forwardableDocuments.size());

        ForwardableDocumentDto forwardableDocument = forwardableDocuments.get(0);
        Documents.assertEquals(originDocument, forwardableDocument);
        Assertions.assertEquals(ForwardableDocumentStatus.PENDING, forwardableDocument.getForwardStatus());
    }

    @Test
    public void addDocumentNotForwardable() {
        DocumentDto originDocument = Documents.random(false);
        api.addOrderDocuments(List.of(originDocument));

        List<ForwardableDocumentDto> forwardableDocuments = forwardableDocumentDao.getPendingsForUpdate(100);
        Assertions.assertEquals(0, forwardableDocuments.size());
    }
}
