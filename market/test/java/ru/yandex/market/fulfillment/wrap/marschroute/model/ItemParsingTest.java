package ru.yandex.market.fulfillment.wrap.marschroute.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteItem;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

import java.util.Collections;
import java.util.Map;

class ItemParsingTest extends ParsingTest<MarschrouteItem> {
    ItemParsingTest() {
        super(new ObjectMapper(), MarschrouteItem.class, "item.json");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return ImmutableMap.<String, Object>builder()
                .put("itemId", "item_id")
                .put("name", "name")
                .put("price", 100)
                .put("comment", "comment")
                .put("barcode", Collections.singletonList("barcode"))
                .put("quantity", 2)
                .build();
    }
}
