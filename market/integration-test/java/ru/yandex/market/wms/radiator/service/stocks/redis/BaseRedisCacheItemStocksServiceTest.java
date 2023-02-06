package ru.yandex.market.wms.radiator.service.stocks.redis;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.wms.radiator.test.IntegrationTestBackend;
import ru.yandex.market.wms.radiator.test.IntegrationTestConstants;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public abstract class BaseRedisCacheItemStocksServiceTest extends IntegrationTestBackend {

    protected abstract AbstractRedisCacheItemStocksService<?, ?, ?> service();

    @Test
    void when_encodeKeyByOffset_then_decodeKeyByOffset() {
        long original = 1000L;
        var encoded = service().encodeKeyByOffset("wh1", original);
        var decoded = service().decodeKeyByOffset(encoded);

        assertThat(
                decoded,
                is(equalTo(original))
        );
    }

    @Test
    void when_encodeItemStocksByIdKey_then_decodeItemStocksByIdKey() {
        var original = new UnitId(null, 100L, "article");
        var encoded = service().encodeKeyById("wh1", original);
        var decoded = service().decodeKeyById(encoded);

        assertThat(
                decoded,
                is(equalTo(original))
        );
    }

    @Test
    void byOfKeys() {
        String warehouseId = "whs";
        assertThat(
                service().byOfKeys(1000, 2, warehouseId),
                is(arrayContaining(
                        service().encodeKeyByOffset(warehouseId, 1000),
                        service().encodeKeyByOffset(warehouseId, 1002)
                ))
        );
    }

    @Test
    void when_setUpdated_then_getUpdated() throws InterruptedException {
        OffsetDateTime offsetDateTime1 = OffsetDateTime.parse("2020-07-01T10:00:00Z");
        service().setUpdated(IntegrationTestConstants.WH_1_ID, offsetDateTime1.toLocalDateTime());
        Thread.sleep(1000);

        OffsetDateTime offsetDateTime2 = OffsetDateTime.parse("2020-08-01T10:00:00Z");
        service().setUpdated(IntegrationTestConstants.WH_2_ID, offsetDateTime2.toLocalDateTime());

        assertThat(service().getUpdated(IntegrationTestConstants.WH_1_ID), is(equalTo(offsetDateTime1)));
        assertThat(service().getUpdated(IntegrationTestConstants.WH_2_ID), is(equalTo(offsetDateTime2)));
    }
}
