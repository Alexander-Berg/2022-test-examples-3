package ru.yandex.market.loyalty.core.test;

import org.springframework.beans.factory.annotation.Autowired;

public abstract class MarketLoyaltyRegionSettingsCoreMockedDbTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    RegionSettingsLoader regionSettingsLoader;


    @Override
    public void prepareDatabase() {
        super.prepareDatabase();
        regionSettingsLoader.loadRegionThresholdAndCoinEmissionSettings();
    }

}
