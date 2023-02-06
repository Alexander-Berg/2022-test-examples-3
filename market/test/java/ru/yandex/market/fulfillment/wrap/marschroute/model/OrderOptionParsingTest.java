package ru.yandex.market.fulfillment.wrap.marschroute.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

import java.util.Map;

class OrderOptionParsingTest extends ParsingTest<MarschrouteOrderOption> {
    OrderOptionParsingTest() {
        super(new ObjectMapper(), MarschrouteOrderOption.class, "order_option.json");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return ImmutableMap.<String, Object>builder()
                .put("liftToFloor", 1)
                .put("canTry", 1)
                .put("manualConfirm", 1)
                .put("expirationPriority", "60")
                .put("cancelAbsentItems", 1)
                .put("invoiceNumber", 1)
                .put("deliveryAsap", 1)
                .build();
    }
}
