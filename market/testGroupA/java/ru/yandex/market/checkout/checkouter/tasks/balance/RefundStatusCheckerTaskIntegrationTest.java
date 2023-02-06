package ru.yandex.market.checkout.checkouter.tasks.balance;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.storage.OrderEntityGroup;
import ru.yandex.market.checkout.storage.Storage;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class RefundStatusCheckerTaskIntegrationTest extends AbstractPaymentTestBase {

    @Autowired
    private RefundService refundService;
    @Autowired
    private Storage storage;

    @Test
    public void shouldUpdateRefundStatusIfNotSuccess() throws Exception {
        checkouterProperties.setEnableServicesPrepay(true);
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        orderUpdateService.updateOrderStatus(order.get().getId(), OrderStatus.DELIVERY);
        long refundId = refundTestHelper.makeFullRefund();

        Refund refund = refundService.getRefund(refundId);
        assertThat(refund.getStatus(), is(RefundStatus.SUCCESS));

        storage.updateEntityGroup(new OrderEntityGroup(order().getId()), () -> {
            masterJdbcTemplate.update(
                    "UPDATE REFUND set UPDATED_AT = UPDATED_AT - INTERVAL '2' HOUR where ID = ?", refundId
            );
            return null;
        });

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockCheckBasket(
                CheckBasketParams.buildCheckBasketWithCancelledRefund(refund.getTrustRefundId(), refund.getAmount()));
        trustMockConfigurer.mockStatusBasket(
                CheckBasketParams.buildCheckBasketWithCancelledRefund(refund.getTrustRefundId(), refund.getAmount()),
                null);

        tmsTaskHelper.runSyncRefundWithBillingPartitionTaskV2();
        Refund refund2 = refundService.getRefund(refundId);
        Assertions.assertEquals(RefundStatus.FAILED, refund2.getFinalBalanceStatus());
    }
}
