package ru.yandex.market.cashier.mocks.trust.checkers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hamcrest.Matcher;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class MarkupBasketLineParams {
    private Map<String, BigDecimal> markup = new HashMap<>();

    public static MarkupBasketLineParams line() {
        return new MarkupBasketLineParams();
    }

    public MarkupBasketLineParams withPaymethod(String method, BigDecimal amount) {
        markup.put(method, amount);
        return this;
    }


    @Override
    public String toString() {
        return "MarkupBasketLineParams{" +
                "markup=" + markup +
                '}';
    }

    public Matcher<JsonElement> toJsonMatcher() {
        return new AbstractMatcher(this.toString()) {
            @Override
            protected boolean matchObjectFields(JsonObject object) {
                if (markup.size() != object.size()) {
                    return false;
                }

                for (Map.Entry<String, BigDecimal> entry : markup.entrySet()) {
                    if (!matchValue(object, entry.getKey(), entry.getValue())) {
                        return false;
                    }
                }

                return true;
            }
        };

    }
}
