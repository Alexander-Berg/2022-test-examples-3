package ru.yandex.market.loyalty.core.service.support;

import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PagedPayments;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.PagedPaymentsRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;

public class SupportDetailsServiceTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private CheckouterClient checkouterClient;
    @Autowired
    private SupportDetailsService supportDetailsService;

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
        String messages = supportDetailsService.collectRawDataMessages(111, 23);
        assertThat(messages, is(not(emptyOrNullString())));
    }
}
