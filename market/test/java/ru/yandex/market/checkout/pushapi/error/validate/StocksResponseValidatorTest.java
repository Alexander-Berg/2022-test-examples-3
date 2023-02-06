package ru.yandex.market.checkout.pushapi.error.validate;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.pushapi.client.entity.StocksResponse;
import ru.yandex.market.checkout.pushapi.client.entity.stock.Stock;
import ru.yandex.market.checkout.pushapi.client.entity.stock.StockItem;
import ru.yandex.market.checkout.pushapi.client.entity.stock.StockType;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StocksResponseValidatorTest {
    StocksResponseValidator validator = new StocksResponseValidator();

    @Test
    public void testOk() {
        StocksResponse response = okResponse();
        validator.validate(response);
    }

    @Test
    public void testDuplicatesInTypes() {
        StocksResponse response = responseWithDuplicates();
        assertThatThrownBy(() -> validator.validate(response))
                .isInstanceOf(ValidationException.class);
    }

    private StocksResponse okResponse() {
        StocksResponse response = new StocksResponse();

        Stock stock1 = new Stock();
        stock1.setWarehouseId(111L);
        stock1.setSku("1");

        StockItem item11 = new StockItem();
        item11.setType(StockType.AVAILABLE);
        item11.setCount(10);

        StockItem item12 = new StockItem();
        item12.setType(StockType.FIT);
        item12.setCount(15);

        Stock stock2 = new Stock();
        stock2.setWarehouseId(111L);
        stock2.setSku("2");

        StockItem item21 = new StockItem();
        item21.setType(StockType.DEFECT);
        item21.setCount(10);

        StockItem item22 = new StockItem();
        item22.setType(StockType.QUARANTINE);
        item22.setCount(15);

        stock2.setItems(List.of(item21, item22));

        response.setSkus(List.of(stock1, stock2));

        return response;
    }

    private StocksResponse responseWithDuplicates() {
        StocksResponse response = new StocksResponse();

        Stock stock1 = new Stock();
        stock1.setWarehouseId(111L);
        stock1.setSku("1");

        StockItem item11 = new StockItem();
        item11.setType(StockType.FIT);
        item11.setCount(10);

        StockItem item12 = new StockItem();
        item12.setType(StockType.AVAILABLE);
        item12.setCount(15);

        stock1.setItems(List.of(item11, item12));

        Stock stock2 = new Stock();
        stock2.setWarehouseId(111L);
        stock2.setSku("2");

        StockItem item21 = new StockItem();
        item21.setType(StockType.AVAILABLE);
        item21.setCount(10);

        StockItem item22 = new StockItem();
        item22.setType(StockType.AVAILABLE);
        item22.setCount(15);

        stock2.setItems(List.of(item21, item22));

        response.setSkus(List.of(stock1, stock2));

        return response;
    }
}
