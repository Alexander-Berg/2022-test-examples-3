package ru.yandex.market.logistic.api.model.fulfillment;

import java.util.List;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class ItemStocksParsingTest extends ParsingTest<ItemStocks> {
    public ItemStocksParsingTest() {
        super(ItemStocks.class, "fixture/entities/item_stocks.xml");
    }

    @Override
    protected void performAdditionalAssertions(ItemStocks subject) {
        assertions().assertThat(subject.getUnitId())
            .as("Asserting unit id value")
            .isEqualTo(new UnitId("1", 2L, "AAA"));

        List<Stock> stocks = subject.getStocks();
        assertions().assertThat(stocks)
            .as("Asserting stocks have size of 1")
            .hasSize(1);

        assertions().assertThat(subject.getWarehouseId().getYandexId())
            .as("Asserting yandex id value")
            .isEqualTo("YA_ID");

        assertions().assertThat(subject.getWarehouseId().getPartnerId())
            .as("Asserting fulfillment id value")
            .isEqualTo("FF_ID");

        Stock stock = stocks.get(0);
        assertions().assertThat(stock.getType())
            .as("Asserting stock[0] stock type")
            .isEqualTo(StockType.QUARANTINE);

        assertions().assertThat(stock.getCount())
            .as("Asserting stock[0] stock count")
            .isEqualTo(100);
    }
}
