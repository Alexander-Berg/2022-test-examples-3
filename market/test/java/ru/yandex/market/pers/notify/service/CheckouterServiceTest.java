package ru.yandex.market.pers.notify.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.CheckouterReturnClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.Receipts;
import ru.yandex.market.checkout.checkouter.request.BasicOrderRequest;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.PagedOrderEventsRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.request.ReturnPdfRequest;
import ru.yandex.market.checkout.checkouter.returns.ReturnNotFoundException;
import ru.yandex.market.checkout.common.rest.EntityNotFoundException;
import ru.yandex.market.pers.notify.external.checkouter.CheckouterService;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CheckouterServiceTest extends MarketMailerMockedDbTest {
    private static final Long ORDER_ID = 12345L;
    private static final Long CLIENT_ID = 111L;

    @Autowired
    private CheckouterClient checkouterClient;

    @Autowired
    private CheckouterOrderHistoryEventsApi checkouterOrderHistoryEventsApi;

    @Autowired
    private CheckouterReturnClient checkouterReturnClient;

    private CheckouterService checkouterService;

    @BeforeEach
    public void startUp() {
        this.checkouterService =
                new CheckouterService(checkouterClient, checkouterOrderHistoryEventsApi, checkouterReturnClient);
    }

    @Test
    public void testIfOrderArchivedServiceTryGetHimFromArchive() {
        Answer<Order> answer = invocation -> {
            OrderRequest orderRequest = invocation.getArgumentAt(1, OrderRequest.class);
            if (orderRequest.isArchived()) {
                Order order = new Order();
                order.setId(ORDER_ID);
                return order;
            } else {
                throw new OrderNotFoundException(ORDER_ID);
            }
        };

        when(checkouterClient.getOrder(any(RequestClientInfo.class), any(OrderRequest.class))).thenAnswer(answer);

        Order actualOrder = checkouterService.getOrder(ORDER_ID, ClientRole.USER, CLIENT_ID, false);

        assertEquals(ORDER_ID, actualOrder.getId());
        verify(checkouterClient, times(2))
                .getOrder(any(RequestClientInfo.class), any(OrderRequest.class));
    }

    @Test
    public void testIfReceiptsArchivedServiceTryGetHimFromArchive() {
        long receiptId = 1111L;

        Answer<Receipts> answer = invocation -> {
            BasicOrderRequest request = invocation.getArgumentAt(1, BasicOrderRequest.class);
            if (request.isArchived()) {
                Receipt receipt = new Receipt();
                receipt.setId(receiptId);
                return new Receipts(Collections.singleton(receipt));
            } else {
                throw new EntityNotFoundException("code", "msg");
            }
        };

        when(checkouterClient.getOrderReceipts(any(RequestClientInfo.class), any(BasicOrderRequest.class)))
                .thenAnswer(answer);

        Receipts receipts = checkouterService.getOrderReceipts(ORDER_ID, ClientRole.USER, CLIENT_ID, null);

        assertEquals(receiptId, receipts.getContent().iterator().next().getId());
        verify(checkouterClient, times(2))
                .getOrderReceipts(any(RequestClientInfo.class), any(BasicOrderRequest.class));
    }

    @Test
    public void testIfOrderEventsArchivedServiceTryGetHimFromArchive() {
        Long eventId = 11111L;

        Answer<PagedEvents> answer = invocation -> {
            PagedOrderEventsRequest request = invocation.getArgumentAt(1, PagedOrderEventsRequest.class);
            if (request.isArchived()) {
                OrderHistoryEvent event = new OrderHistoryEvent();
                event.setId(eventId);
                return new PagedEvents(Collections.singleton(event), null);
            } else {
                throw new EntityNotFoundException("code", "msg");
            }
        };

        when(checkouterOrderHistoryEventsApi.getOrderHistoryEvents(any(RequestClientInfo.class), any(PagedOrderEventsRequest.class)))
                .thenAnswer(answer);

        PagedEvents pagedEvents =
                checkouterService.getOrderHistoryEvents(ORDER_ID, ClientRole.USER, CLIENT_ID, 1, 1);

        assertEquals(eventId, pagedEvents.getItems().iterator().next().getId());
        verify(checkouterOrderHistoryEventsApi, times(2))
                .getOrderHistoryEvents(any(RequestClientInfo.class), any(PagedOrderEventsRequest.class));
    }

    @Test
    public void testIfWarrantyPdfArchivedServiceTryGetHimFromArchive() throws IOException {
        Answer<InputStream> answer = invocation -> {
            BasicOrderRequest request = invocation.getArgumentAt(1, BasicOrderRequest.class);
            if (request.isArchived()) {
                return new ByteArrayInputStream(new byte[0]);
            } else {
                throw new OrderNotFoundException("code", "msg");
            }
        };

        when(checkouterClient.getWarrantyPdf(any(RequestClientInfo.class), any(BasicOrderRequest.class)))
                .thenAnswer(answer);

        InputStream warrantyPdf = checkouterService.getWarrantyPdf(ORDER_ID, ClientRole.USER, CLIENT_ID);

        assertNotNull(warrantyPdf);
        verify(checkouterClient, times(2))
                .getWarrantyPdf(any(RequestClientInfo.class), any(BasicOrderRequest.class));
    }

    @Test
    public void testIfApplicationPdfArchivedServiceTryGetHimFromArchive() throws IOException {
        int returnId = 123;

        Answer<InputStream> answer = invocation -> {
            ReturnPdfRequest request = invocation.getArgumentAt(1, ReturnPdfRequest.class);
            if (request.isArchived()) {
                return new ByteArrayInputStream(new byte[0]);
            } else {
                throw new ReturnNotFoundException(returnId);
            }
        };

        when(checkouterReturnClient.getReturnApplicationPdf(any(RequestClientInfo.class), any(ReturnPdfRequest.class)))
                .thenAnswer(answer);

        InputStream appPdf = checkouterService.getReturnApplicationPdf(ORDER_ID, returnId, ClientRole.USER, CLIENT_ID);

        assertNotNull(appPdf);
        verify(checkouterReturnClient, times(2))
                .getReturnApplicationPdf(any(RequestClientInfo.class), any(ReturnPdfRequest.class));
    }
}
