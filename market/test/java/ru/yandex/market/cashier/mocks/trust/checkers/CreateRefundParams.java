package ru.yandex.market.cashier.mocks.trust.checkers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hamcrest.Matcher;
import ru.yandex.market.cashier.trust.TrustBasketId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import static com.google.common.collect.Streams.stream;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class CreateRefundParams {
    private TrustBasketId basketKey;
    private String userIp;
    private String reason;
    private Collection<RefundLine> lines = new ArrayList<>();


    public static CreateRefundParams refund(TrustBasketId basketKey) {
        CreateRefundParams config = new CreateRefundParams();
        config.basketKey = basketKey;
        return config;
    }

    public String getUserIp() {
        return userIp;
    }

    public CreateRefundParams withReason(String reason) {
        this.reason = reason;
        return this;
    }

    public TrustBasketId getBasketKey() {
        return basketKey;
    }

    public CreateRefundParams withUserIp(String userIp) {
        this.userIp = userIp;
        return this;
    }

    public CreateRefundParams withRefundLine(String trustOrderId, int quantityDelta, BigDecimal deltaAmount) {
        lines.add(new RefundLine(trustOrderId, quantityDelta, deltaAmount));
        return this;
    }

    public Matcher<JsonElement> toJsonMatcher() {
        return new AbstractMatcher(this.toString()) {
            @Override
            protected boolean matchObjectFields(JsonObject object) {
                JsonElement linesElement = object.get("orders");
                if ((linesElement == null || !linesElement.isJsonArray() || linesElement.getAsJsonArray().size() == 0)) {
                    if (!lines.isEmpty()) {
                        return false;
                    }
                } else {
                    JsonArray jsonOrders = linesElement.getAsJsonArray();
                    assertThat(jsonOrders.size(), equalTo(lines.size()));
                    if (!stream(jsonOrders.iterator()).allMatch(
                            jsonOrder -> anyOf(lines.stream().map(CreateRefundParams.RefundLine::toJsonMatcher).collect(Collectors.toList())).matches(jsonOrder)
                    )) {
                        return false;
                    }
                }

                return matchPropertiesSize(object, reason, basketKey.getPurchaseToken(), lines)
                        && matchValue(object, "reason_desc", reason)
                        && matchValue(object, "purchase_token", basketKey.getPurchaseToken());
            }
        };
    }

    @Override
    public String toString() {
        return "CreateRefundParams{" +
                "basketKey=" + basketKey +
                ", userIp='" + userIp + '\'' +
                ", reason='" + reason + '\'' +
                ", lines=" + lines +
                '}';
    }

    private static class RefundLine {
        private String trustOrderId;
        private int quantityDelta;
        private BigDecimal deltaAmount;

        public RefundLine(String trustOrderId, int quantityDelta, BigDecimal deltaAmount) {
            this.trustOrderId = trustOrderId;
            this.quantityDelta = quantityDelta;
            this.deltaAmount = deltaAmount;
        }

        public Matcher<JsonElement> toJsonMatcher() {
            return new AbstractMatcher(this.toString()) {
                @Override
                protected boolean matchObjectFields(JsonObject object) {
                    return matchPropertiesSize(object, trustOrderId, quantityDelta, deltaAmount)
                            && matchValue(object, "order_id", trustOrderId)
                            && matchValue(object, "delta_qty", quantityDelta)
                            && matchValue(object, "delta_amount", deltaAmount);
                }
            };
        }

        @Override
        public String toString() {
            //TODO  json
            return "RefundLine{" +
                    "trustOrderId='" + trustOrderId + '\'' +
                    ", quantityDelta=" + quantityDelta +
                    ", deltaAmount=" + deltaAmount +
                    '}';
        }
    }

}


