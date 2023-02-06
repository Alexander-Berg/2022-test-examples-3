package ru.yandex.market.fulfillment.wrap.marschroute.model.response.tracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

import java.util.Map;

class MarschrouteOrderTrackingEmptyResponseParsingTest extends ParsingTest<TrackingResponse> {

    MarschrouteOrderTrackingEmptyResponseParsingTest() {
        super(new ObjectMapper(), TrackingResponse.class, "order_tracking/empty_response.json");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return ImmutableMap.of(
                "success", false,
                "code", 2002,
                "comment", "Не найден заказ с таким order_id");
    }
}
