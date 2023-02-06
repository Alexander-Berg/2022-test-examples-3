package ru.yandex.market.fulfillment.wrap.marschroute.model.response.order;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

class MarschrouteCancelOrderSuccessResponseTest
    extends MarschrouteJsonParsingTest<MarschrouteCancelOrderResponse> {

    MarschrouteCancelOrderSuccessResponseTest() {
        super(MarschrouteCancelOrderResponse.class, "order/cancel/success_response.json");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId", "EXT40914255");
        map.put("status", 35);
        map.put("date", LocalDateTime.of(2017, 9, 11, 14, 40));
        map.put("code", 0);
        map.put("comment", "Отказ");
        map.put("success", true);

        return map;
    }
}
