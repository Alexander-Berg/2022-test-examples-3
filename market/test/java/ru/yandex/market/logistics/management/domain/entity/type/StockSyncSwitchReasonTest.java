package ru.yandex.market.logistics.management.domain.entity.type;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.logistics.management.AbstractTest;

class StockSyncSwitchReasonTest extends AbstractTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"RANDOM_ENUM", "ANOTHER_RANDOM_ENUM"})
    void getByNameTest(String reason) {
        StockSyncSwitchReason result = StockSyncSwitchReason.getByName(reason);
        softly.assertThat(result)
            .as("Should not be null or throw ex if null, empty or unrecognizable passed")
            .isEqualTo(StockSyncSwitchReason.UNKNOWN);
    }

    @ParameterizedTest
    @NullSource
    void nameOrUnknownTest(StockSyncSwitchReason reason) {
        StockSyncSwitchReason processedReason = StockSyncSwitchReason.nameOrUnknown(reason);
        softly.assertThat(processedReason).as("Should not be null or throw ex if null  passed")
            .isEqualTo(StockSyncSwitchReason.UNKNOWN);
    }
}
