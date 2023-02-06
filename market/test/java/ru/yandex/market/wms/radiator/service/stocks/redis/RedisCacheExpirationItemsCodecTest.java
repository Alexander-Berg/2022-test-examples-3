package ru.yandex.market.wms.radiator.service.stocks.redis;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.Stock;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.wms.radiator.entity.ManufacturedDateAndStocks;
import ru.yandex.market.wms.radiator.entity.UnitIdAndExpirations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class RedisCacheExpirationItemsCodecTest {

    @Test
    void when_encodePageInternal_then_decodePageInternal() {
        var original = List.of(
                new UnitIdAndExpirations(
                        new UnitId(null, 100L, "article"),
                        List.of(
                                new ManufacturedDateAndStocks(
                                        new DateTime("2020-01-01T00:00:00+03:00"),
                                        List.of(
                                                new Stock(StockType.DEFECT, 1, null)
                                        )
                                )
                        )
                )
        );
        var encoded = RedisCacheExpirationItemsCodec.encodePageInternal(original);
        var decoded = RedisCacheExpirationItemsCodec.decodePageInternal(encoded);

        assertThat(
                decoded,
                is(equalTo(original))
        );
    }
}
