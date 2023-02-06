package ru.yandex.market.fulfillment.stockstorage.service.validator;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SimpleStock;
import ru.yandex.market.fulfillment.stockstorage.domain.exception.DataValidationException;

public class FreezeRequestItemsValidatorTest {

    private final FreezeRequestItemsValidator validator = new FreezeRequestItemsValidator();

    @Test
    public void validRequest() {
        SimpleStock s1 = createSimpleStock("sku1", 5);
        SimpleStock s2 = createSimpleStock("sku2", 10);

        validator.validateRequiredStocksState(Arrays.asList(s1, s2));
    }

    @Test
    public void itemsDuplication() {
        SimpleStock s1 = createSimpleStock("sku1", 5);
        SimpleStock s2 = createSimpleStock("sku2", 10);

        Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateRequiredStocksState(Arrays.asList(s1, s1, s2)),
                "Duplicate items found for id" + s1.toString());
    }

    @Test
    public void quantityIsNull() {
        SimpleStock s1 = createSimpleStock("sku1", null);
        SimpleStock s2 = createSimpleStock("sku2", 10);

        Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateRequiredStocksState(Arrays.asList(s1, s2)),
                "Required stock.quantity not found" + s1.toString());
    }

    @Test
    public void quantityIsNegative() {
        SimpleStock s1 = createSimpleStock("sku1", -1);
        SimpleStock s2 = createSimpleStock("sku2", 10);

        Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateRequiredStocksState(Arrays.asList(s1, s2)),
                "Required stock.quantity negative" + s1.toString());
    }

    private SimpleStock createSimpleStock(String sku, Integer quantity) {
        return new SimpleStock(sku, 1L, sku, quantity, 1);
    }
}
