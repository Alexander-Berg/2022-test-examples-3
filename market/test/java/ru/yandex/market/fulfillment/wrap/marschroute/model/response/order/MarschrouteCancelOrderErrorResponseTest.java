package ru.yandex.market.fulfillment.wrap.marschroute.model.response.order;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

import java.util.HashMap;
import java.util.Map;

class MarschrouteCancelOrderErrorResponseTest
    extends MarschrouteJsonParsingTest<MarschrouteCancelOrderResponse> {

    MarschrouteCancelOrderErrorResponseTest() {
        super(MarschrouteCancelOrderResponse.class, "order/cancel/error_response.json");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("code", 206);
        map.put("comment", "Заказ не найден");
        map.put("success", false);

        return map;
    }
}
