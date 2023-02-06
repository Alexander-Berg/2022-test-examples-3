package ru.yandex.market.cashier.mocks.trust.checkers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hamcrest.Matcher;

import java.util.HashMap;
import java.util.Map;

public class MarkupBasketParams {

    private Long uid;
    private Long tokenId;
    private Matcher<String> purchaseToken;
    private Map<String, MarkupBasketLineParams> linesMarkup = new HashMap<>();


    public static MarkupBasketParams markup(Matcher<String> purchaseToken,Long tokenId, Long uid) {
        MarkupBasketParams result = new MarkupBasketParams();
        result.uid = uid;
        result.tokenId = tokenId;
        result.purchaseToken = purchaseToken;
        return result;
    }

    public Long getUid() {
        return uid;
    }

    public Matcher<String> getPurchaseToken() {
        return purchaseToken;
    }

    public Long getTokenId() {
        return tokenId;
    }

    public MarkupBasketParams withLine(String trustOrderId, MarkupBasketLineParams lineMarkup) {
        linesMarkup.put(trustOrderId, lineMarkup);
        return this;
    }

    @Override
    public String toString() {
        return "MarkupBasketParams{" +
                "uid=" + uid +
                ", linesMarkup=" + linesMarkup +
                '}';
    }

    public Matcher<JsonElement> toJsonMatcher() {
        return new AbstractMatcher(this.toString()) {
            @Override
            protected boolean matchObjectFields(JsonObject object) {
                JsonElement markup = object.get("paymethod_markup");
                if (markup == null || !markup.isJsonObject()) {
                    return false;
                }

                JsonObject markupObject = markup.getAsJsonObject();
                if (linesMarkup.size() != markupObject.size()) {
                    return false;
                }

                for (Map.Entry<String, MarkupBasketLineParams> entry : linesMarkup.entrySet()) {
                    JsonElement lineMarkup = markupObject.get(entry.getKey());
                    if (lineMarkup == null) {
                        return false;
                    }
                    if (!entry.getValue().toJsonMatcher().matches(lineMarkup)) {
                        return false;
                    }
                }

                return true;
            }
        };
    }
}
