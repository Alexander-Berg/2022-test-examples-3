package ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class PaginationParsingTest extends MarschrouteJsonParsingTest<Pagination> {

    PaginationParsingTest() {
        super(Pagination.class, "pagination.json");
    }
}
