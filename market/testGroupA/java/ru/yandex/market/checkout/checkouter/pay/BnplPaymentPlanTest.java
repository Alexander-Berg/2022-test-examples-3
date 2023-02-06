package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.BnplInfo;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.viewmodel.CreatePaymentResponse;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.prepaidBlueOrderParameters;


/**
 * @author : poluektov
 * date: 2021-06-30.
 */
public class BnplPaymentPlanTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;
    @Autowired
    private BnplPaymentService bnplPaymentService;
    @Autowired
    private CheckouterClient checkouterClient;

    @BeforeEach
    public void mockBnpl() {
        checkouterProperties.setEnableBnpl(true);
        bnplMockConfigurer.mockWholeBnpl();
    }

    @Test
    public void testGetBnplPaymentPlan() {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        Order order = orderCreateHelper.createOrder(parameters);
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);

        Payment payment = paymentService.getPayment(paymentResponse.getId(), ClientInfo.SYSTEM);
        assertNotNull(payment.getBasketKey().getBasketId());

        BnplOrerPlanResponse response = bnplPaymentService.getOrderPlan(order.getId(), ClientInfo.SYSTEM, List.of());
        assertNotNull(response.getBnplPlanDetails());
        assertEquals(new BigDecimal("509.8000"), response.getBnplPlanDetails().getDeposit());
        assertEquals("paid", response.getBnplPlanDetails().getPayments().get(0).getPaymentStatus());
    }

    @Test
    public void testGetBnplPaymentPlanViaClient() {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        Order order = orderCreateHelper.createOrder(parameters);
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);

        Payment payment = paymentService.getPayment(paymentResponse.getId(), ClientInfo.SYSTEM);
        assertNotNull(payment.getBasketKey().getBasketId());

        BnplOrerPlanResponse response = checkouterClient.payments()
                .getBnplOrderPlan(order.getId(), new RequestClientInfo(ClientRole.SYSTEM, 0L));
        assertNotNull(response.getBnplPlanDetails());
        assertEquals(new BigDecimal("509.8"), response.getBnplPlanDetails().getDeposit());
        assertEquals("paid", response.getBnplPlanDetails().getPayments().get(0).getPaymentStatus());
    }
}
