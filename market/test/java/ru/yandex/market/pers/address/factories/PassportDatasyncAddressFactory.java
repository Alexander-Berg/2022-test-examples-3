package ru.yandex.market.pers.address.factories;

import ru.yandex.market.pers.address.services.model.PassportDataSyncAddress;

import static ru.yandex.market.pers.address.factories.AddressDtoFactory.TOLSTOGO_STREET;

public class PassportDatasyncAddressFactory {
    public static PassportDataSyncAddress tolstogoStreet() {
        return new PassportDataSyncAddress(
            "work",
            "улица Льва Толстого, 16",
            "Россия, Москва, " + TOLSTOGO_STREET + ", 16",
            "37.588149",
            "55.733847"
        );
    }
}
