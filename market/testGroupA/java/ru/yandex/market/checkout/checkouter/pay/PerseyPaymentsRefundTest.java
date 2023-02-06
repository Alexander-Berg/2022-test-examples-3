package ru.yandex.market.checkout.checkouter.pay;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.persey.model.RefundOrderDonationRequest;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.perseypayments.PerseyMockConfigurer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;

public class PerseyPaymentsRefundTest extends AbstractWebTestBase {

    @Autowired
    private PerseyMockConfigurer perseyMockConfigurer;
    @Autowired
    private WireMockServer perseyPaymentsMock;
    @Autowired
    private ObjectMapper perseyObjectMapper;
    private Order order;

    @BeforeEach
    public void setup() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        assertThat(order.getStatus(), equalTo(OrderStatus.DELIVERY));
        perseyPaymentsMock.resetRequests();
        perseyMockConfigurer.mockRefund();
    }

    @Test
    public void shouldFullRefundSuccessfully() throws IOException {
        order = orderStatusHelper.proceedOrderToStatus(order, CANCELLED);
        List<ServeEvent> events = perseyPaymentsMock.getServeEvents().getRequests();
        assertThat(events, hasSize(1));
        ServeEvent event = events.get(0);
        LoggedRequest actualRequest = event.getRequest();
        assertThat(actualRequest.getHeader("X-Yandex-UID"), is(String.valueOf(order.getPayment().getUid())));
        RefundOrderDonationRequest request =
                perseyObjectMapper.readValue(actualRequest.getBodyAsString(), RefundOrderDonationRequest.class);
        assertThat(request.getOrderId(), is(String.valueOf(order.getId())));
    }
}
