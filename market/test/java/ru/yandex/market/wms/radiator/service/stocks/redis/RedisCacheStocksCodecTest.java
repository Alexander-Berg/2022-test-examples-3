package ru.yandex.market.wms.radiator.service.stocks.redis;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.Stock;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.wms.radiator.entity.UnitIdAndStocks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class RedisCacheStocksCodecTest {

    @Test
    void when_encodePageInternal_then_decodePageInternal() {
        var original = List.of(
                new UnitIdAndStocks(
                        new UnitId(null, 100L, "article"),
                        List.of(new Stock(StockType.DEFECT, 1, null))
                )
        );
        var encoded = RedisCacheStocksCodec.encodePageInternal(original);
        var decoded = RedisCacheStocksCodec.decodePageInternal(encoded);

        assertThat(
                decoded,
                is(equalTo(original))
        );
    }
}
