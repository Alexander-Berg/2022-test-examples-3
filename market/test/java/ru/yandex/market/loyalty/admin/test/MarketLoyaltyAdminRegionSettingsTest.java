package ru.yandex.market.loyalty.admin.test;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.test.RegionSettingsLoader;

public abstract class MarketLoyaltyAdminRegionSettingsTest extends MarketLoyaltyAdminMockedDbTest {

    @Autowired
    RegionSettingsLoader regionSettingsLoader;


    @Override
    public void prepareDatabase() {
        super.prepareDatabase();
        regionSettingsLoader.loadRegionThresholdAndCoinEmissionSettings();
    }

}
