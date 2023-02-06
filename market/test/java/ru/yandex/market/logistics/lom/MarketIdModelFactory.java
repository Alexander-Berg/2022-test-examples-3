package ru.yandex.market.logistics.lom;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;

@ParametersAreNonnullByDefault
public final class MarketIdModelFactory {
    private MarketIdModelFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static MarketAccount marketAccount(long marketId) {
        return MarketAccount.newBuilder()
            .setLegalInfo(
                LegalInfo.newBuilder()
                    .setLegalName("Рога и копыта")
                    .setType("OOO")
                    .setLegalAddress("Блюхера 15")
                    .setRegistrationNumber("555777")
                    .setInn("1231231234")
            )
            .setMarketId(marketId)
            .build();
    }
}
