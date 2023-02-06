package ru.yandex.market.api.server.sec.client.internal;

import ru.yandex.market.api.server.sec.client.internal.Tariff;
import ru.yandex.market.api.server.sec.client.internal.Tariffs;

/**
 * Created by tesseract on 07.07.17.
 */
public class TestTariffs {

    public static final Tariff BASE = new Tariff(Tariffs.BASE, false);

    public static final Tariff CUSTOM = new Tariff(Tariffs.CUSTOM, false);

    public static final Tariff VENDOR = new Tariff(1234, true);
}
