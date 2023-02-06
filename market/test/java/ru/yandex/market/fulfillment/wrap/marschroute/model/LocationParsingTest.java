package ru.yandex.market.fulfillment.wrap.marschroute.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

import java.util.Map;

class LocationParsingTest extends ParsingTest<MarschrouteLocation> {
    LocationParsingTest() {
        super(new ObjectMapper(), MarschrouteLocation.class, "location.json");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return ImmutableMap.<String, Object>builder()
                .put("cityId", "5200000100000")
                .put("index", "123456")
                .put("street", "street")
                .put("building1", "building_1")
                .put("building2", "building_2")
                .put("room", "room")
                .put("floor", "floor")
                .put("entrance", "entrance")
                .put("entranceCode", "entrance_code")
                .build();
    }
}
