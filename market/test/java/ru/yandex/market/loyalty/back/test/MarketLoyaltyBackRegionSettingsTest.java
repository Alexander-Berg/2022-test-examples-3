package ru.yandex.market.loyalty.back.test;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.test.RegionSettingsLoader;

public abstract class MarketLoyaltyBackRegionSettingsTest extends MarketLoyaltyBackMockedDbTestBase {

    @Autowired
    RegionSettingsLoader regionSettingsLoader;


    @Override
    public void prepareDatabase() {
        super.prepareDatabase();
        regionSettingsLoader.loadRegionThresholdAndCoinEmissionSettings();
    }

}
