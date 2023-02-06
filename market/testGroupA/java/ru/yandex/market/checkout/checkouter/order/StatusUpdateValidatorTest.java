package ru.yandex.market.checkout.checkouter.order;

import java.util.EnumSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.backbone.validation.order.status.StatusUpdateValidator;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class StatusUpdateValidatorTest extends AbstractWebTestBase {

    private static final EnumSet<ClientRole> SHOP_ROLES = EnumSet.of(ClientRole.SHOP, ClientRole.SHOP_USER);

    private static final long SHOP_ID = 42L;
    @Autowired
    private StatusUpdateValidator statusUpdateValidator;

    private static Order prepareOrderWithPayment(OrderStatus orderStatus, PaymentStatus paymentStatus) {
        Order order = mock(Order.class);
        Payment payment = mock(Payment.class);
        when(order.getPayment()).thenReturn(payment);
        when(payment.getStatus()).thenReturn(paymentStatus);
        when(payment.isSuccessful()).thenReturn(true);
        when(order.getStatus()).thenReturn(orderStatus);
        return order;
    }

    // UNPAID
    @Test
    public void successFromUnpaidToProcessing() {
        Order order = prepareOrderWithPayment(OrderStatus.UNPAID, PaymentStatus.HOLD);
        statusUpdateValidator.validateStatusUpdate(order, OrderStatus.PROCESSING, OrderSubstatus.STARTED,
                ClientInfo.SYSTEM);
    }

    @Test
    public void wrongPaymentStatusFromUnpaidToProcessing() {
        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            Order order = prepareOrderWithPayment(OrderStatus.UNPAID, PaymentStatus.CANCELLED);
            statusUpdateValidator.validateStatusUpdate(order, OrderStatus.PROCESSING, null, ClientInfo.SYSTEM);
        });
    }

    @Test
    public void successFromUnpaidToPending() {
        Order order = prepareOrderWithPayment(OrderStatus.UNPAID, PaymentStatus.HOLD);
        statusUpdateValidator.validateStatusUpdate(order, OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION,
                ClientInfo.SYSTEM);
    }

    @Test
    public void wrongPaymentStatusFromUnpaidToPending() {
        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            Order order = prepareOrderWithPayment(OrderStatus.UNPAID, PaymentStatus.CANCELLED);
            statusUpdateValidator.validateStatusUpdate(order, OrderStatus.PENDING, null, ClientInfo.SYSTEM);
        });
    }

    @Test
    void successFromAntifraudToAwaitConfirmation() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        order.setStatus(OrderStatus.PENDING);
        order.setSubstatus(OrderSubstatus.ANTIFRAUD);

        ClientInfo clientInfo = new ClientInfo(ClientRole.CALL_CENTER_OPERATOR, 123L);

        statusUpdateValidator.validateStatusUpdate(
                order, OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION, clientInfo);
    }

    @Test
    void successFromPickupToUserFraudTest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        order.setStatus(OrderStatus.PICKUP);
        order.setSubstatus(null);

        ClientInfo clientInfo = new ClientInfo(ClientRole.ANTIFRAUD_ROBOT, 123L);

        statusUpdateValidator.validateCancellationRequest(
                order, new CancellationRequest(OrderSubstatus.USER_FRAUD, "sd"), clientInfo);

    }

    private void assertFailStatusUpdateForShop(Order order, OrderStatus status, OrderSubstatus substatus) {
        for (ClientRole shopRole : SHOP_ROLES) {
            try {
                statusUpdateValidator.validateStatusUpdate(order, status, substatus, new ClientInfo(shopRole, SHOP_ID));
                Assertions.fail();
            } catch (OrderStatusNotAllowedException ignored) {
            }
        }
    }
}
