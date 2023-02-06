package ru.yandex.market.checkout.checkouter.balance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentOperations;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.TrustPaymentOperations;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.BalanceMockHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author : vladislav-gol
 * date: 07.12.2021.
 * Проверяет работу https://st.yandex-team.ru/MARKETCHECKOUT-24681
 */
public class ReplacePaymentPsContractTest extends AbstractWebTestBase {

    @Autowired
    private BalanceMockHelper balanceMockHelper;
    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentOperations trustPaymentOperations;

    private Order order;
    private Payment payment;

    @BeforeEach
    public void before() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);

        order = orderCreateHelper.createOrder(parameters);
        payment = payHelper.payForOrder(order);
    }

    @Test
    public void testCreditOrderPaymentWithoutCession() {
        balanceMockHelper.mockWholeBalance();
        trustMockConfigurer.mockCheckBasket(null);
        trustMockConfigurer.mockStatusBasket(null, null);
        trustPaymentOperations.updatePaymentStateFromPaymentSystem(payment, ClientInfo.SYSTEM);
        Payment updatedPayment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);

        assertThat(updatedPayment.getProperties().getCession(), nullValue());
        assertThat(updatedPayment.getPsContractExternalId(), equalTo(TrustPaymentOperations.DIRECT_CREDIT_EXTERNAL_ID));
        assertThat(updatedPayment.getProperties().getTerminalContractId(),
                equalTo(TrustPaymentOperations.DIRECT_CREDIT_EXTERNAL_ID));
        assertThat(updatedPayment.getProperties().getTerminalId(), notNullValue());
    }

    @Test
    public void testCreditOrderPaymentWithCession() {
        balanceMockHelper.mockWholeBalance();
        payHelper.notifyTinkoffCessionClear(payment);
        trustPaymentOperations.updatePaymentStateFromPaymentSystem(payment, ClientInfo.SYSTEM);
        Payment updatedPayment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);

        assertThat(updatedPayment.getProperties().getCession(), equalTo(true));
        assertThat(updatedPayment.getPsContractExternalId(),
                equalTo(TrustPaymentOperations.CESSION_CREDIT_EXTERNAL_ID));
        assertThat(updatedPayment.getProperties().getTerminalContractId(),
                equalTo(TrustPaymentOperations.CESSION_CREDIT_EXTERNAL_ID));
        assertThat(updatedPayment.getProperties().getTerminalId(), notNullValue());
    }
}
