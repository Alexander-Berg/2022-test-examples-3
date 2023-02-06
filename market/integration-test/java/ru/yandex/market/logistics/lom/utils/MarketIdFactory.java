package ru.yandex.market.logistics.lom.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;

public final class MarketIdFactory {
    private MarketIdFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static MarketAccount marketAccount() {
        return marketAccount(null);
    }

    @Nonnull
    public static MarketAccount marketAccount(@Nullable Long marketId) {
        LegalInfo legalInfo = legalInfoBuilder().build();
        return marketAccount(marketId, legalInfo);
    }

    @Nonnull
    public static MarketAccount marketAccount(Long marketId, LegalInfo legalInfo) {
        MarketAccount.Builder builder = MarketAccount.newBuilder()
            .setLegalInfo(legalInfo);
        if (marketId != null) {
            builder.setMarketId(marketId);
        }
        return builder.build();
    }

    public static LegalInfo.Builder legalInfoBuilder() {
        return LegalInfo.newBuilder()
            .setLegalName("Рога и копыта")
            .setType("OOO")
            .setLegalAddress("Блюхера 15")
            .setRegistrationNumber("555777");
    }

    public static LegalInfo.Builder anotherLegalInfoBuilder() {
        return LegalInfo.newBuilder()
            .setLegalName("Ромашка")
            .setType("ZAO")
            .setLegalAddress("Синяя 10")
            .setRegistrationNumber("9990000223")
            .setInn("4757371");
    }
}
