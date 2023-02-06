package ru.yandex.market.fulfillment.stockstorage.domain.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.domain.exception.StockStorageBadRequestException;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.SyncJobName;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SyncTypeConverterTest {

    private SyncTypeConverter converter = new SyncTypeConverter();

    @Test
    public void successfulConvert() {
        assertEquals(SyncJobName.FULL_SYNC, converter.convert("FullSync"));
        assertEquals(SyncJobName.KOROBYTE_SYNC, converter.convert("KorobyteSync"));
    }

    @Test
    public void failedConvert() {
        Assertions.assertThrows(StockStorageBadRequestException.class, () -> {
            converter.convert("FULL_SYNC");
        });
    }

}
