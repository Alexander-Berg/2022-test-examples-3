package ru.yandex.market.crm.campaign.services.appmetrica;

import java.time.LocalDateTime;

/**
 * @author zloddey
 */
public class FakeTimeProvider extends ServerDateTimeProvider {
    private final LocalDateTime time;

    public FakeTimeProvider(int year, int month, int dayOfMonth, int hour, int minute) {
        this.time = LocalDateTime.of(year, month, dayOfMonth, hour, minute);
    }

    @Override
    public LocalDateTime getDateTime() {
        return time;
    }
}
