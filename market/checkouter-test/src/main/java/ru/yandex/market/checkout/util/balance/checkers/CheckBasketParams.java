package ru.yandex.market.checkout.util.balance.checkers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.util.Lists;

import ru.yandex.market.checkout.checkouter.balance.trust.model.BalancePaymentStatus;
import ru.yandex.market.checkout.checkouter.balance.trust.model.TrustTerminal;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.TrustBasketKey;

import static com.google.common.collect.Lists.newArrayList;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CHECK_BASKET_STATUS_CODE;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CHECK_BASKET_STATUS_DESC;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.generateRandomTrustId;

public class CheckBasketParams {

    //check basket response parameters
    private TrustBasketKey basketKey;
    private Collection<BasketLineState> lines;
    private String bankCard;
    private Collection<BasketRefund> refunds;
    private BalancePaymentStatus basketStatus;
    private String payMethod = "card";
    private String payMethodId = "card-x";
    private String statusCode;
    private String statusDesc;
    private String reversalId;
    private String holdTimestamp;
    private TrustTerminal terminal;
    private String unholdTimestamp;
    private BigDecimal cashbackAmount;
    private boolean emptyPaymentUrl;
    private String cardType;
    private String approvalCode;
    private String userAccount;
    private String rrn;
    private boolean emptyTerminal;

    public static CheckBasketParams buildWithBasketKeyConfig(TrustBasketKey basketKey) {
        CheckBasketParams config = new CheckBasketParams();
        config.basketKey = basketKey;
        return config;
    }

    public static CheckBasketParams buildReversalWithRefundConfig(String refundId) {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = BalancePaymentStatus.refunded;
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
        config.basketStatus = BalancePaymentStatus.authorized;
        return config;
    }

    public static CheckBasketParams buildCheckBasketConfig(BalancePaymentStatus basketStatus, boolean yandexMoney) {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = basketStatus;
        if (yandexMoney) {
            config.payMethod = "yandex_money";
        }
        return config;
    }

    public static CheckBasketParams buildDividedItems(Order order) {
        CheckBasketParams checkBasketParams = new CheckBasketParams();
        checkBasketParams.basketStatus = BalancePaymentStatus.cleared;
        checkBasketParams.basketKey = null;
        checkBasketParams.lines = order.getItems().stream()
                .flatMap(it -> IntStream.rangeClosed(1, it.getCount())
                        .mapToObj(i -> String.format("%s-item-%s-%s", order.getId(), it.getId(), i))
                        .map(orderId -> new BasketLineState(orderId, 1, it.getBuyerPrice())))
                .collect(Collectors.toUnmodifiableList());
        return checkBasketParams;
    }

    public static CheckBasketParams buildPostAuth() {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = BalancePaymentStatus.cleared;
        return config;
    }

    public static CheckBasketParams buildNotStarted() {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = BalancePaymentStatus.not_started;
        return config;
    }

    public static CheckBasketParams buildStarted() {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = BalancePaymentStatus.started;
        return config;
    }

    public static CheckBasketParams buildWaitingBankDecision() {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = BalancePaymentStatus.wait_for_processing;
        return config;
    }

    public static CheckBasketParams buildTinkoffCessionClear() {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = BalancePaymentStatus.cleared;
        config.payMethod = "credit::cession";
        return config;
    }

    public static CheckBasketParams buildFailCheckBasket() {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = BalancePaymentStatus.not_authorized;
        config.statusCode = CHECK_BASKET_STATUS_CODE;
        config.statusDesc = CHECK_BASKET_STATUS_DESC;
        return config;
    }

    public static CheckBasketParams buildYaCardClear() {
        CheckBasketParams config = CheckBasketParams.buildPostAuth();
        config.setRrn("207392808269");
        config.setApprovalCode("231973");
        config.setUserAccount("555400****7128");
        config.basketStatus = BalancePaymentStatus.cleared;
        return config;
    }

    public static CheckBasketParams buildHoldCheckBasket() {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = BalancePaymentStatus.authorized;
        return config;
    }

    public static CheckBasketParams buildWaitingBankDecisionCheckBasket() {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = BalancePaymentStatus.wait_for_processing;
        return config;
    }

    public static CheckBasketParams buildHoldWithoutTsCheckBasket() {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = BalancePaymentStatus.authorized;
        config.holdTimestamp = "";
        return config;
    }

    public static CheckBasketParams buildCheckBasketWithConfirmedRefund(String refundId, BigDecimal amount) {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = BalancePaymentStatus.refunded;
        config.refunds = Lists.newArrayList(BasketRefund.confirmed(refundId, amount));
        return config;
    }

    public static CheckBasketParams buildCheckBasketWithCancelledRefund(String refundId, BigDecimal amount) {
        CheckBasketParams config = new CheckBasketParams();
        config.basketStatus = BalancePaymentStatus.cleared;
        config.refunds = Lists.newArrayList(BasketRefund.cancelled(refundId, amount));
        return config;
    }

    public static CheckBasketParams buildUnholdWithTimestamps(String refundId) {
        CheckBasketParams config = buildReversalWithRefundConfig(refundId);
        config.basketStatus = BalancePaymentStatus.canceled;
        config.unholdTimestamp = "1483001851000";
        return config;
    }

    public static CheckBasketParams buildOffsetAdvanceCheckBasket(String balanceOrderId) {
        CheckBasketParams config = new CheckBasketParams();
        config.setLines(new ArrayList<BasketLineState>() {{
            BasketLineState basketLineState = new BasketLineState(balanceOrderId, 1, BigDecimal.ONE);
            basketLineState.setDeliveryReceiptId("6962504a795be234970dac0f");
            add(basketLineState);
        }});
        return config;
    }

    public static CheckBasketParams buildCashbackEmitCheckBasket(String balanceOrderId, TrustBasketKey basketKey) {
        CheckBasketParams config = new CheckBasketParams();
        config.basketKey = basketKey;
        config.setLines(new ArrayList<BasketLineState>() {{
            BasketLineState basketLineState = new BasketLineState(balanceOrderId, 1, BigDecimal.ONE);
            add(basketLineState);
        }});
        return config;
    }

    public static CheckBasketParams buildOffsetAdvanceCheckBasket(
            Map<String, String> balanceOrderIdToDeliveryReceiptId) {
        CheckBasketParams config = new CheckBasketParams();
        config.setLines(
                balanceOrderIdToDeliveryReceiptId.entrySet().stream()
                        .map(e -> {
                            BasketLineState basketLineState = new BasketLineState(e.getKey(), 1,
                                    BigDecimal.ONE);
                            basketLineState.setDeliveryReceiptId(e.getValue());
                            return basketLineState;
                        })
                        .collect(Collectors.toList())
        );
        return config;
    }

    public BalancePaymentStatus getBasketStatus() {
        return basketStatus;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getStatusDesc() {
        return statusDesc;
    }

    public String getTrustPaymentId() {
        return basketKey == null ? null : basketKey.getBasketId();
    }

    public String getPurchaseToken() {
        return basketKey == null ? null : basketKey.getPurchaseToken();
    }

    public String getReversalId() {
        return reversalId;
    }

    public String getPayMethod() {
        return payMethod;
    }

    public void setPayMethod(String payMethod) {
        this.payMethod = payMethod;
    }

    public String getPayMethodId() {
        return payMethodId;
    }

    public void setPayMethodId(String payMethodId) {
        this.payMethodId = payMethodId;
    }

    public String getBankCard() {
        return bankCard;
    }

    public void setBankCard(String bankCard) {
        this.bankCard = bankCard;
    }

    public String getHoldTimestamp() {
        return holdTimestamp;
    }

    public void setHoldTimestamp(String holdTimestamp) {
        this.holdTimestamp = holdTimestamp;
    }

    public Collection<BasketLineState> getLines() {
        return lines;
    }

    public void setLines(Collection<BasketLineState> lines) {
        this.lines = lines;
    }

    public Collection<BasketRefund> getRefunds() {
        return refunds;
    }

    public void setRefunds(Collection<BasketRefund> refunds) {
        this.refunds = refunds;
    }

    public TrustTerminal getTerminal() {
        return terminal;
    }

    public void setTerminal(Long terminalId) {
        terminal = new TrustTerminal(terminalId);
    }

    public String getUnholdTimestamp() {
        return unholdTimestamp;
    }

    public BigDecimal getCashbackAmount() {
        return cashbackAmount;
    }

    public void setCashbackAmount(BigDecimal cashbackAmount) {
        this.cashbackAmount = cashbackAmount;
    }

    public boolean isEmptyPaymentUrl() {
        return emptyPaymentUrl;
    }

    public void setEmptyPaymentUrl(boolean emptyPaymentUrl) {
        this.emptyPaymentUrl = emptyPaymentUrl;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getApprovalCode() {
        return approvalCode;
    }

    public void setApprovalCode(String approvalCode) {
        this.approvalCode = approvalCode;
    }

    public String getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    public String getRrn() {
        return rrn;
    }

    public void setRrn(String rrn) {
        this.rrn = rrn;
    }

    public boolean isEmptyTerminal() {
        return emptyTerminal;
    }

    public void setEmptyTerminal(boolean emptyTerminal) {
        this.emptyTerminal = emptyTerminal;
    }

    public static class BasketLineState {

        private final String orderId;
        private final int quantity;
        private final BigDecimal amount;
        private String deliveryReceiptId;

        public BasketLineState(String orderId, int quantity, BigDecimal amount) {
            this.orderId = orderId;
            this.quantity = quantity;
            this.amount = amount;
        }

        public String getOrderId() {
            return orderId;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getDeliveryReceiptId() {
            return deliveryReceiptId;
        }

        public void setDeliveryReceiptId(String deliveryReceiptId) {
            this.deliveryReceiptId = deliveryReceiptId;
        }
    }

    public static class BasketRefund {

        private String refundId;
        private BigDecimal amount;
        private boolean cancelled;
        private boolean confirmed;

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
    }
}
