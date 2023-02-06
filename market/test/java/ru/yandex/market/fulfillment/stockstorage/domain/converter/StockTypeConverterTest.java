package ru.yandex.market.fulfillment.stockstorage.domain.converter;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class StockTypeConverterTest {

    @Test
    public void successfulConvert() {
        assertEquals(ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.DEFECT,
                StockTypeConverter.convert(StockType.DEFECT));
        assertEquals(ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.EXPIRED,
                StockTypeConverter.convert(StockType.EXPIRED));
        assertEquals(ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.FIT,
                StockTypeConverter.convert(StockType.FIT));
        assertEquals(ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.QUARANTINE,
                StockTypeConverter.convert(StockType.QUARANTINE));
    }

    @Test
    public void successfulConvertFF() {
        assertEquals(StockType.DEFECT,
                StockTypeConverter.convert(
                        ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.DEFECT));
        assertEquals(StockType.EXPIRED,
                StockTypeConverter.convert(
                        ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.EXPIRED));
        assertEquals(StockType.FIT,
                StockTypeConverter.convert(
                        ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.FIT));
        assertEquals(StockType.QUARANTINE,
                StockTypeConverter.convert(
                        ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.QUARANTINE));
        assertNull(StockTypeConverter.convert(
                ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.ACCEPTANCE));
        assertNull(StockTypeConverter.convert(
                ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.AVAILABLE));
        assertNull(StockTypeConverter.convert(
                ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.UNKNOWN));
        assertNull(StockTypeConverter.convert(
                ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.CANCELLATION));
    }
}
