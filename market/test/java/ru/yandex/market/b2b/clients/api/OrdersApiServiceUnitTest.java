package ru.yandex.market.b2b.clients.api;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.b2b.clients.DocumentService;
import ru.yandex.market.b2b.clients.Documents;
import ru.yandex.market.b2b.clients.PaymentInvoiceService;
import ru.yandex.mj.generated.server.model.DocumentDto;
import ru.yandex.mj.generated.server.model.DocumentResponseDto;
import ru.yandex.mj.generated.server.model.MultiOrderDto;
import ru.yandex.mj.generated.server.model.OrderDto;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class OrdersApiServiceUnitTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private PaymentInvoiceService invoiceService;

    @InjectMocks
    private OrdersApiService service;

    @Test
    public void generateMultiPaymentInvoice_savesCreatedDocuments() {
        OrderDto orderDto = new OrderDto();
        orderDto.setId(BigDecimal.ONE);
        List<OrderDto> orders = List.of(orderDto);
        MultiOrderDto multi = multi(orders, "multiOrderId");

        DocumentDto documentDto = new DocumentDto();
        documentDto.setUrl("link_to_document");
        doReturn(List.of(documentDto)).when(invoiceService).generate(multi);

        service.generateMultiPaymentInvoice(multi);

        verify(documentService).addDocuments(List.of(documentDto));
    }

    private MultiOrderDto multi(List<OrderDto> orders, String multiOrderId) {
        return new MultiOrderDto().multiOrderId(multiOrderId).orders(orders);
    }

    @Test
    public void generateMultiPaymentInvoice_returnsFirstDocumnet() {
        // Given
        DocumentDto firstDocument = new DocumentDto();
        firstDocument.setUrl("link_to_first_document");

        DocumentDto secondDocument = new DocumentDto();
        secondDocument.setUrl("link_to_second_document");

        OrderDto orderDto = new OrderDto();
        orderDto.setId(BigDecimal.ONE);
        List<OrderDto> orders = List.of(orderDto);
        MultiOrderDto multi = multi(orders, "multiOrderId");
        doReturn(List.of(firstDocument, secondDocument)).when(invoiceService).generate(multi);

        // When
        ResponseEntity<DocumentResponseDto> response = service.generateMultiPaymentInvoice(multi);

        // Then
        Assertions.assertNotNull(response.getBody());
        Documents.assertEquals(firstDocument, response.getBody());
    }
}
