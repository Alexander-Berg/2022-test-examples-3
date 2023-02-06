package ru.yandex.market.fulfillment.wrap.marschroute.model.response.delivery.city;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDate;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

import java.util.Arrays;
import java.util.Map;

class DeliveryOptionParsingTest extends ParsingTest<DeliveryOption> {

    DeliveryOptionParsingTest() {
        super(new ObjectMapper(), DeliveryOption.class, "delivery_option/value.json");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return new ImmutableMap.Builder<String, Object>()
                .put("name", "Почтой РФ")
                .put("deliveryCode", "P22010NEW")
                .put("deliveryId", 3)
                .put("placeId", 3)
                .put("transportApiCode", "api_code")
                .put("possibleDates", Arrays.asList(MarschrouteDate.create("05.10.2017"),
                        MarschrouteDate.create("06.10.2017")))
                .put("cost", 168)
                .build();
    }
}
