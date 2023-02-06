package ru.yandex.market.delivery.transport_manager.factory;

import java.util.Optional;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;

import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;

@UtilityClass
public class MarketIdFactory {
    @Nonnull
    public Optional<MarketAccount> optionalMarketAccount(Long marketId, String inn) {
        LegalInfo legalInfo = LegalInfo.newBuilder()
            .setInn(inn)
            .setType("type")
            .setLegalAddress("address")
            .setLegalName("name")
            .setRegistrationNumber("regNumber")
            .build();

        return Optional.of(
            MarketAccount.newBuilder()
                .setMarketId(marketId)
                .setLegalInfo(legalInfo)
                .build()
        );
    }
}
