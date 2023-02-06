package ru.yandex.market.loyalty.core.service.perks;


import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

public class AllPerksImplementedTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private StatusFeatureProcessors perkProcessorsHandler;

    @Test
    public void everyPerkShouldHaveProcessor() {
        for (PerkType perk : PerkType.values()) {
            perkProcessorsHandler.findFeatureProcessor(Perk.of(perk));
        }
    }
}
