package ru.yandex.market.checkout.util.balance.checkers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hamcrest.Matcher;

import ru.yandex.market.checkout.checkouter.pay.TrustBasketKey;

public class UpdateBasketParams {

    private TrustBasketKey basketKey;
    private String userIp;
    private Long uid;
    private final Collection<UpdateLineParams> linesToUpdate = new ArrayList<>();

    public static UpdateBasketParams updateBasket(TrustBasketKey basketKey) {
        UpdateBasketParams params = new UpdateBasketParams();
        params.basketKey = basketKey;
        return params;
    }

    public TrustBasketKey getBasketKey() {
        return basketKey;
    }

    public String getUserIp() {
        return userIp;
    }

    public Long getUid() {
        return uid;
    }

    public Collection<UpdateLineParams> getLinesToUpdate() {
        return linesToUpdate;
    }

    public UpdateBasketParams withUserIp(String userIp) {
        this.userIp = userIp;
        return this;
    }

    public UpdateBasketParams withUid(Long uid) {
        this.uid = uid;
        return this;
    }

    public UpdateBasketParams withLineToUpdate(String orderId, int quantity, BigDecimal totalLineAmount) {
        linesToUpdate.add(UpdateLineParams.updateLine(orderId, quantity, totalLineAmount));
        return this;
    }

    public UpdateBasketParams withLineToRemove(String orderId) {
        linesToUpdate.add(UpdateLineParams.removeLine(orderId));
        return this;
    }

    enum UpdateAction {
        CANCEL,
        UPDATE
    }

    static class UpdateLineParams {

        private String orderId;
        private UpdateAction action;

        private Integer quantity;
        private BigDecimal totalLineAmount;

        static UpdateLineParams removeLine(String orderId) {
            UpdateLineParams updateLineParams = new UpdateLineParams();
            updateLineParams.orderId = orderId;
            updateLineParams.action = UpdateAction.CANCEL;
            return updateLineParams;
        }

        static UpdateLineParams updateLine(String orderId, int quantity, BigDecimal totalLineAmount) {
            UpdateLineParams updateLineParams = new UpdateLineParams();
            updateLineParams.orderId = orderId;
            updateLineParams.action = UpdateAction.UPDATE;
            updateLineParams.quantity = quantity;
            updateLineParams.totalLineAmount = totalLineAmount;
            return updateLineParams;
        }

        public String getOrderId() {
            return orderId;
        }

        public UpdateAction getAction() {
            return action;
        }

        public Matcher<JsonElement> toResizeJsonMatcher() {
            return new AbstractMatcher(this.toString()) {
                @Override
                protected boolean matchObjectFields(JsonObject object) {
                    return matchPropertiesSize(object, quantity, totalLineAmount)
                            && matchValue(object, "amount", totalLineAmount)
                            && matchValue(object, "qty", quantity);
                }
            };

        }
    }

}
