package ru.yandex.market.logistics.iris.domain.converter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Expiration;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemExpiration;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Stock;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistics.iris.converter.ItemExpirationConverter;
import ru.yandex.market.logistics.iris.core.index.complex.FlatStock;
import ru.yandex.market.logistics.iris.core.index.complex.StockLifetime;
import ru.yandex.market.logistics.iris.core.index.complex.StockType;
import ru.yandex.market.logistics.iris.jobs.consumers.reference.dto.FlatItemExpiration;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime.fromLocalDateTime;

public class ItemExpirationConverterTest {

    private static final DateTime MANUFACTURED_DATE = fromLocalDateTime(LocalDateTime.of(1970, 1, 1, 0, 0));
    private static final DateTime SECOND_MANUFACTURED_DATE = fromLocalDateTime(LocalDateTime.of(1970, 1, 1, 20, 0));

    private static final DateTime DEFAULT_UPDATED_VALUE = fromLocalDateTime(LocalDateTime.of(1970, 1, 1, 0, 0));

    @Test
    public void conversion() {
        ItemExpiration itemExpiration = new ItemExpiration(
            new UnitId("", 1L, "sku"),
            Collections.singletonList(new Expiration(MANUFACTURED_DATE, Collections.singletonList(new Stock(ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.FIT, 10, DEFAULT_UPDATED_VALUE))))
        );

        ZonedDateTime updatedDateTime = ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.UTC);
        ItemExpirationConverter converter = new ItemExpirationConverter(new FixedUtcTimestampProvider(updatedDateTime));

        FlatItemExpiration result = converter.toFlatItemExpiration(itemExpiration);
        assertSoftly(assertions -> {
            assertions.assertThat(result.getPartnerId()).isEqualTo("1");
            assertions.assertThat(result.getPartnerSku()).isEqualTo("sku");
            assertions.assertThat(result.getUpdatedDateTime()).isEqualTo(updatedDateTime);
            assertions.assertThat(result.getStockLifetime()).isEqualTo(StockLifetime.of(ImmutableMap.of(
                "1970-01-01",
                Collections.singleton(new FlatStock(StockType.FIT, 10))
            )));
        });
    }

    @Test
    public void conversionWhenHasItemExpirationsForSameDateButDifferentTime() {
        Stock firstExpirationFirstStock = new Stock(ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.FIT, 10, DEFAULT_UPDATED_VALUE);
        Stock firstExpirationSecondStock = new Stock(ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.QUARANTINE, 20, DEFAULT_UPDATED_VALUE);
        Stock secondExpirationFirstStock = new Stock(ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.FIT, 15, DEFAULT_UPDATED_VALUE);
        Stock secondExpirationSecondStock = new Stock(ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.QUARANTINE, 33, DEFAULT_UPDATED_VALUE);
        Expiration firstExpiration = new Expiration(MANUFACTURED_DATE, Arrays.asList(firstExpirationFirstStock, firstExpirationSecondStock));
        Expiration secondExpiration = new Expiration(SECOND_MANUFACTURED_DATE, Arrays.asList(secondExpirationFirstStock, secondExpirationSecondStock));
        ItemExpiration itemExpiration = new ItemExpiration(
                new UnitId("", 1L, "sku"),
                Arrays.asList(firstExpiration, secondExpiration)
        );

        ZonedDateTime updatedDateTime = ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.UTC);
        ItemExpirationConverter converter = new ItemExpirationConverter(new FixedUtcTimestampProvider(updatedDateTime));

        FlatItemExpiration result = converter.toFlatItemExpiration(itemExpiration);
        assertSoftly(assertions -> {
            assertions.assertThat(result.getPartnerId()).isEqualTo("1");
            assertions.assertThat(result.getPartnerSku()).isEqualTo("sku");
            assertions.assertThat(result.getUpdatedDateTime()).isEqualTo(updatedDateTime);
            assertions.assertThat(result.getStockLifetime()).isEqualTo(StockLifetime.of(ImmutableMap.of(
                    "1970-01-01",
                    ImmutableSet.of(new FlatStock(StockType.FIT, 25), new FlatStock(StockType.QUARANTINE, 53))
            )));
        });
    }
}
