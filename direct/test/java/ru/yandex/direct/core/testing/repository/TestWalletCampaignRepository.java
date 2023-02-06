package ru.yandex.direct.core.testing.repository;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.campaign.model.AggregatingSumStatus;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.WALLET_CAMPAIGNS;

public class TestWalletCampaignRepository {

    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestWalletCampaignRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    public void addDefaultWalletWithMigrationStatus(int shard, Long campaignId, AggregatingSumStatus status) {
        dslContextProvider.ppc(shard)
                .insertInto(WALLET_CAMPAIGNS, WALLET_CAMPAIGNS.WALLET_CID, WALLET_CAMPAIGNS.IS_SUM_AGGREGATED)
                .values(campaignId, AggregatingSumStatus.toSource(status))
                .execute();
    }

    public void addWalletWithTotalSum(int shard, Long campaignId, BigDecimal totalSum) {
        dslContextProvider.ppc(shard)
                .insertInto(WALLET_CAMPAIGNS, WALLET_CAMPAIGNS.WALLET_CID, WALLET_CAMPAIGNS.TOTAL_SUM)
                .values(campaignId, totalSum)
                .execute();
    }

    public Long addDefaultWalletWithMigrationStatus(int shard, AggregatingSumStatus status) {
        Long walletId = getNextWalletCampaignId(shard);

        addDefaultWalletWithMigrationStatus(shard, walletId, status);

        return walletId;
    }

    public void addDefaultWallet(int shard, Long campaignId) {
        dslContextProvider.ppc(shard)
                .insertInto(WALLET_CAMPAIGNS, WALLET_CAMPAIGNS.WALLET_CID)
                .values(campaignId)
                .execute();
    }

    private Long getNextWalletCampaignId(int shard) {
        return UtilRepository.getNextId(dslContextProvider.ppc(shard), WALLET_CAMPAIGNS, WALLET_CAMPAIGNS.WALLET_CID);
    }

    public BigDecimal getTotalChipsCosts(int shard, long walletCid) {
        return dslContextProvider.ppc(shard)
                .select(WALLET_CAMPAIGNS.TOTAL_CHIPS_COST)
                .from(WALLET_CAMPAIGNS)
                .where(WALLET_CAMPAIGNS.WALLET_CID.eq(walletCid))
                .fetchOne(WALLET_CAMPAIGNS.TOTAL_CHIPS_COST);
    }
}
