package ru.yandex.market.checkout.util.balance.checkers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hamcrest.Matcher;

import ru.yandex.market.checkout.checkouter.balance.trust.model.BasketMarkup;
import ru.yandex.market.checkout.checkouter.pay.TrustBasketKey;

import static com.google.common.collect.Streams.stream;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class CreateRefundParams {

    private TrustBasketKey basketKey;
    private String userIp;
    private String reason;
    private final Collection<RefundLine> lines = new ArrayList<>();
    private BasketMarkup markup = null;

    public static CreateRefundParams refund(TrustBasketKey basketKey) {
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

    public CreateRefundParams withUserIp(String userIp) {
        this.userIp = userIp;
        return this;
    }

    public CreateRefundParams withRefundLine(String balanceOrderId, BigDecimal quantityDelta, BigDecimal deltaAmount) {
        lines.add(new RefundLine(balanceOrderId, quantityDelta, deltaAmount));
        return this;
    }

    public CreateRefundParams withMarkup(BasketMarkup markup) {
        this.markup = markup;
        return this;
    }

    public Matcher<JsonElement> toJsonMatcher() {
        return new AbstractMatcher(this.toString()) {
            @Override
            protected boolean matchObjectFields(JsonObject object) {
                JsonElement linesElement = object.get("orders");
                if ((linesElement == null || !linesElement.isJsonArray()
                        || linesElement.getAsJsonArray().size() == 0)) {
                    if (!lines.isEmpty()) {
                        return false;
                    }
                } else {
                    JsonArray jsonOrders = linesElement.getAsJsonArray();
                    assertThat(jsonOrders.size(), equalTo(lines.size()));
                    if (!stream(jsonOrders.iterator()).allMatch(
                            jsonOrder -> anyOf(lines.stream().map(CreateRefundParams.RefundLine::toJsonMatcher)
                                    .collect(Collectors.toList())).matches(jsonOrder)
                    )) {
                        return false;
                    }
                }

                return matchPropertiesSize(object, reason, basketKey.getPurchaseToken(), lines, markup)
                        && matchValue(object, "reason_desc", reason)
                        && matchValue(object, "purchase_token", basketKey.getPurchaseToken())
                        && matchMarkup(object);
            }
        };
    }

    private boolean matchMarkup(JsonObject object) {
        JsonElement markupElement = object.get("paymethod_markup");
        if ((markupElement == null) || (!markupElement.isJsonObject())) {
            return markup == null;
        } else {
            JsonObject markupObject = markupElement.getAsJsonObject();
            if (markup == null) {
                return false;
            }
            for (String serviceOrderId : markup.getBasketLinesMarkup().keySet()) {
                JsonElement markupLine = markupObject.get(serviceOrderId);
                if (markupLine == null) {
                    return false;
                }
            }
        }
        return true;
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

        private final String balanceOrderId;
        private final BigDecimal quantityDelta;
        private final BigDecimal deltaAmount;

        RefundLine(String balanceOrderId, BigDecimal quantityDelta, BigDecimal deltaAmount) {
            this.balanceOrderId = balanceOrderId;
            this.quantityDelta = quantityDelta;
            this.deltaAmount = deltaAmount;
        }

        public Matcher<JsonElement> toJsonMatcher() {
            return new AbstractMatcher(this.toString()) {
                @Override
                protected boolean matchObjectFields(JsonObject object) {
                    return matchPropertiesSize(object, balanceOrderId, quantityDelta, deltaAmount)
                            && matchValue(object, "order_id", balanceOrderId)
                            && matchValue(object, "delta_qty", quantityDelta)
                            && matchValue(object, "delta_amount", deltaAmount);
                }
            };
        }

        @Override
        public String toString() {
            //TODO  json
            return "RefundLine{" +
                    "balanceOrderId='" + balanceOrderId + '\'' +
                    ", quantityDelta=" + quantityDelta +
                    ", deltaAmount=" + deltaAmount +
                    '}';
        }
    }
}
