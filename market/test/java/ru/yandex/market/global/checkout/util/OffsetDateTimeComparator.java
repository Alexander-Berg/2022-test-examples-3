package ru.yandex.market.global.checkout.util;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class OffsetDateTimeComparator implements Comparator<OffsetDateTime> {

    private final Duration delta;

    @Override
    public int compare(OffsetDateTime o1, OffsetDateTime o2) {
        Duration between = Duration.between(o1, o2);
        if (between.abs().compareTo(delta) < 0) {
            return 0;
        }
        return between.isNegative() ? 1 : -1;
    }

}
