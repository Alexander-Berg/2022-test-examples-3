package ru.yandex.market.checkout.checkouter.delivery.dropship;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DropshipSubsidyTest extends AbstractWebTestBase {

    private static final String PROMO_CODE = "PROMO-CODE";

    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private PaymentService paymentService;

    @DisplayName("Субсидии в кроссдок заказе создаются поайтемно")
    @Test
    public void testGenerateSubsidyReceipt() {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        parameters.setupPromo(PROMO_CODE);
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);

        List<Payment> subsidies = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal.SUBSIDY);

        assertThat(subsidies, hasSize(1));
        Payment subsidy = subsidies.stream().findAny().orElseThrow(() -> new RuntimeException("Not found!"));
        assertNotNull(subsidy.getBasketKey().getBasketId());

        Order finalOrder = orderService.getOrder(order.getId());
        assertNull(finalOrder.getSubsidyBalanceOrderId());
        assertThat(finalOrder.getItems(), everyItem(hasProperty("ffSubsidyBalanceOrderId", not(nullValue()))));
    }
}
