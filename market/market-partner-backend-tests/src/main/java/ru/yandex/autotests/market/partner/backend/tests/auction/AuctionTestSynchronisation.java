package ru.yandex.autotests.market.partner.backend.tests.auction;

import ru.yandex.qatools.hazelcast.LockRule;

/**
 * @author vbudnev
 */
public class AuctionTestSynchronisation {
    public static LockRule getCampaignBasedLockRule(AuctionPiConfig config) {
        return new LockRule("${mbi.at.environment}-" + String.valueOf(config.getCampaignId()));
    }
}
