package ru.yandex.market.cashier.mocks.trust.checkers;

import org.assertj.core.util.Lists;
import ru.yandex.market.cashier.trust.TrustBasketId;
import ru.yandex.market.cashier.trust.api.TrustPaymentStatus;

import java.math.BigDecimal;
import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;
import static ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer.CHECK_BASKET_STATUS_CODE;
import static ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer.CHECK_BASKET_STATUS_DESC;
import static ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer.generateRandomTrustId;

public class CheckBasketParams {

    //check basket response parameters
    private String purchaseToken;
    private Collection<BasketLineState> lines;
    private String bankCard;
    private Collection<BasketRefund> refunds;
    private TrustPaymentStatus basketStatus;
    private String payMethod = "card";
    private String statusCode;
    private String statusDesc;
    private String reversalId;


    public TrustPaymentStatus getBasketStatus() {
        return basketStatus;
    }


    public String getStatusCode() {
        return statusCode;
    }

    public String getStatusDesc() {
        return statusDesc;
    }


    public String getPurchaseToken() {
        return purchaseToken;
    }

    public String getReversalId() {
        return reversalId;
    }

    public String getPayMethod() {
        return payMethod;
    }

    public String getBankCard() {
        return bankCard;
    }

    public Collection<BasketLineState> getLines() {
        return lines;
    }

    public void setLines(Collection<BasketLineState> lines) {
        this.lines = lines;
    }

    public void setRefunds(Collection<BasketRefund> refunds) {
        this.refunds = refunds;
    }

    public void setBankCard(String bankCard) {
        this.bankCard = bankCard;
    }

    public Collection<BasketRefund> getRefunds() {
        return refunds;
    }

    public static class BasketLineState {
        private String orderId;
        private int quantity;
        private BigDecimal amount;

        public String getOrderId() {
            return orderId;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public BasketLineState(String orderId, int quantity, BigDecimal amount) {
            this.orderId = orderId;
            this.quantity = quantity;
            this.amount = amount;
        }
    }


    public static class BasketRefund {
        private String refundId;
        private BigDecimal amount;
        private boolean cancelled;
        private boolean confirmed;


        public String getRefundId() {
            return refundId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public boolean isConfirmed() {
            return confirmed;
        }

        public static BasketRefund cancelled(String refundId, BigDecimal amount) {
            BasketRefund refund = new BasketRefund();
            refund.refundId = refundId;
            refund.amount = amount;
            refund.cancelled = true;
            return refund;
        }

        public static BasketRefund confirmed(String refundId, BigDecimal amount) {
            BasketRefund refund = new BasketRefund();
            refund.refundId = refundId;
            refund.amount = amount;
            refund.confirmed = true;
            return refund;
        }

    }


    public CheckBasketParams withPurchaseToken(String token) {
        this.purchaseToken = token;
        return this;
    }


    public static CheckBasketParams buildReversalWithRefundConfig(String refundId) {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = TrustPaymentStatus.refunded;
        config.reversalId = "5962504a795be234970dac0f";
        config.refunds = newArrayList(BasketRefund.confirmed(
                refundId != null ? refundId : generateRandomTrustId(),
                new BigDecimal(99999)//doesn't matter
        ));
        return config;
    }

    public static CheckBasketParams buildReversalConfig() {
        CheckBasketParams config = new CheckBasketParams();
        config.reversalId = "5962504a795be234970dac0f";
        config.basketStatus = TrustPaymentStatus.authorized;
        return config;
    }

    public static CheckBasketParams buildReversalFailConfig(String refundId) {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = TrustPaymentStatus.cleared;
        config.reversalId = "5962504a795be234970dac0f";
        config.refunds = newArrayList(BasketRefund.cancelled(
                refundId != null ? refundId : generateRandomTrustId(),
                new BigDecimal(99999)//doesn't matter
        ));

        return config;
    }

    public static CheckBasketParams buildCheckBasketConfig(TrustPaymentStatus basketStatus, boolean yandexMoney) {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = basketStatus;
        if (yandexMoney) {
            config.payMethod = "yandex_money";
        }
        return config;
    }

    public static CheckBasketParams buildPostAuth() {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = TrustPaymentStatus.cleared;
        return config;
    }


    public static CheckBasketParams buildFailCheckBasket() {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = TrustPaymentStatus.not_authorized;
        config.statusCode = CHECK_BASKET_STATUS_CODE;
        config.statusDesc = CHECK_BASKET_STATUS_DESC;
        return config;
    }

    public static CheckBasketParams buildCheckBasketWithConfirmedRefund(String refundId, BigDecimal amount) {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = TrustPaymentStatus.refunded;
        config.refunds = Lists.newArrayList(BasketRefund.confirmed(refundId, amount));
        return config;
    }

    public static CheckBasketParams buildCheckBasketWithCancelledRefund(String refundId, BigDecimal amount) {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = TrustPaymentStatus.cleared;
        config.refunds = Lists.newArrayList(BasketRefund.cancelled(refundId, amount));
        return config;
    }
}
