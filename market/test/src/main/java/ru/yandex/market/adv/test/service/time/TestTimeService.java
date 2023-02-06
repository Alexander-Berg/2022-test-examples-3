package ru.yandex.market.adv.test.service.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.adv.service.time.TimeService;

/**
 * Класс для получения тестового заранее заданного времени.
 * Date: 25.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
public class TestTimeService implements TimeService {

    private final LocalDateTime localDateTime = LocalDateTime.of(2021, 10, 21, 13, 42, 53);

    @Nonnull
    @Override
    public OffsetDateTime get(ZoneId zoneId) {
        return OffsetDateTime.of(localDateTime, zoneId.getRules().getOffset(localDateTime));
    }

    @Nonnull
    @Override
    public Instant get() {
        return localDateTime.atZone(ZoneOffset.systemDefault())
                .toInstant();
    }
}
