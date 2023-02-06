package ru.yandex.market.checkout.checkouter.pay;

/**
 * @author mkasumov
 */
public class ExpectedRefund extends Refund {

    private RefundableItems refundedItems;
    private boolean isPartialRefund;

    public RefundableItems getRefundedItems() {
        return refundedItems;
    }

    public void setRefundedItems(RefundableItems refundedItems) {
        this.refundedItems = refundedItems;
    }

    public boolean isPartialRefund() {
        return isPartialRefund;
    }

    public void setPartialRefund(boolean partialRefund) {
        isPartialRefund = partialRefund;
    }
}
