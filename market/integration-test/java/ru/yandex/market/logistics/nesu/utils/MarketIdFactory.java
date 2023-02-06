package ru.yandex.market.logistics.nesu.utils;

import javax.annotation.Nonnull;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;

public final class MarketIdFactory {
    private MarketIdFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static MarketAccount marketAccount() {
        LegalInfo legalInfo = LegalInfo.newBuilder()
            .setLegalName("Рога и копыта")
            .setType("OOO")
            .setLegalAddress("Блюхера 15")
            .setRegistrationNumber("555777")
            .build();

        return MarketAccount.newBuilder()
            .setLegalInfo(legalInfo)
            .build();
    }
}
