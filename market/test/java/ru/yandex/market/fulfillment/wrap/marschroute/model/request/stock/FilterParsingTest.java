package ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class FilterParsingTest extends MarschrouteJsonParsingTest<Filter> {

    FilterParsingTest() {
        super(Filter.class, "filter.json");
    }
}
