package ru.yandex.market.logistic.api.model.fulfillment;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class StockParsingTest extends ParsingTest<Stock> {

    public StockParsingTest() {
        super(Stock.class, "fixture/entities/stock.xml");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return ImmutableMap.of(
            "type", StockType.QUARANTINE,
            "count", 100
        );
    }
}
