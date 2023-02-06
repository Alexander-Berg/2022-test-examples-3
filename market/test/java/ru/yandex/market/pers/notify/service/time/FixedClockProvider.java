package ru.yandex.market.pers.notify.service.time;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * @author vtarasoff
 * @since 05.10.2021
 */
@Primary
@Service
class FixedClockProvider implements ClockProvider {
    private final Clock clock;

    @Autowired
    public FixedClockProvider(@Value("${test.fixed-clock-provider.time:1970-01-01T00:00:00.00Z}") String time,
                              @Value("${test.fixed-clock-provider.zone-id:UTC}") String zoneId) {
        clock = Clock.fixed(Instant.parse(time), ZoneId.of(zoneId));
    }

    @Override
    public Clock clock() {
        return clock;
    }
}
