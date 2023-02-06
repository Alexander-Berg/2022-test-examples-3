package ru.yandex.market.loyalty.admin.controller;

import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PagedPayments;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.PagedPaymentsRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.loyalty.admin.service.StartrekService;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.test.TestFor;

import static org.mockito.ArgumentMatchers.any;

@TestFor(SupportDetailsController.class)
public class SupportDetailsControllerTest extends MarketLoyaltyAdminMockedDbTest {

    @Autowired
    private SupportDetailsController supportDetailsController;
    @Autowired
    private CheckouterClient checkouterClient;
    @MockBean
    private StartrekService startrekService;

    @Before
    public void setUp() throws Exception {
        Order mockedOrder = new Order();
        mockedOrder.setCreationDate(new Date());

        Mockito.when(checkouterClient.getOrder(any(RequestClientInfo.class), any(OrderRequest.class)))
                .thenReturn(mockedOrder);
        Mockito.when(checkouterClient.payments().getPayments(any(RequestClientInfo.class),
                        any(PagedPaymentsRequest.class)))
                .thenReturn(new PagedPayments(null, List.of()));
    }

    @Test
    public void shouldNotFailOnEmptyResult() {
        supportDetailsController.sendRawDataToTicket("TEST-1", 101L, 1L);
    }
}
