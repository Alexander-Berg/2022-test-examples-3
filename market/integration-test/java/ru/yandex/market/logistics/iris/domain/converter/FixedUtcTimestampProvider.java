package ru.yandex.market.logistics.iris.domain.converter;

import java.time.ZonedDateTime;

import ru.yandex.market.logistics.iris.util.UtcTimestampProvider;

public class FixedUtcTimestampProvider implements UtcTimestampProvider {
    private final ZonedDateTime utcTimestamp;

    public FixedUtcTimestampProvider(ZonedDateTime utcTimestamp) {
        this.utcTimestamp = utcTimestamp;
    }

    @Override
    public ZonedDateTime getCurrentUtcTimestamp() {
        return utcTimestamp;
    }
}
