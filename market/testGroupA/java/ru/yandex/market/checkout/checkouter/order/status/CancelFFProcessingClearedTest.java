package ru.yandex.market.checkout.checkouter.order.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;

public class CancelFFProcessingClearedTest extends AbstractWebTestBase {

    @Autowired
    private RefundService refundService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Value("${market.checkouter.payments.clear.hours}")
    private int clearHours;

    @Test
    public void shouldNotAllowToCancelClearedOrderFromProcessing() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        // Виртуальный магазини на постоплате
        parameters.addShopMetaData(parameters.getOrder().getShopId(), ShopSettingsHelper.getPostpayMeta());
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Order order = orderCreateHelper.createOrder(parameters);

        Instant processingInstant = getClock().instant().plus(4, ChronoUnit.HOURS);
        setFixedTime(processingInstant);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        setFixedTime(processingInstant.plus(clearHours, ChronoUnit.HOURS).plusSeconds(1));
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        order = orderService.getOrder(order.getId());
        assertThat(order.getPayment().getStatus(), is(PaymentStatus.CLEARED));

        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);
        Order cancelled = orderStatusHelper.updateOrderStatus(
                order.getId(), ClientInfo.SYSTEM, CANCELLED, OrderSubstatus.USER_CHANGED_MIND
        );

        assertThat(cancelled.getStatus(), is(CANCELLED));

        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);

        Collection<Refund> refunds = refundService.getRefunds(cancelled.getId());
        assertThat(refunds, hasSize(1));
    }
}
