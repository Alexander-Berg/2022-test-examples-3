package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.intapi.entity.balanceclient.container.CampaignDataForNotifyOrder;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters;
import ru.yandex.direct.intapi.entity.balanceclient.service.migration.MigrationSchema;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderService.walletTotalSpentShouldBeUpdated;

public class NotifyOrderServiceWalletTotalSpentShouldBeUpdatedTest {

    private static final BigDecimal DB_CHIPS_COST = BigDecimal.valueOf(5);
    private static final BigDecimal UPDATE_CHIPS_COST = BigDecimal.valueOf(23);

    private CampaignDataForNotifyOrder dbCampaignData;
    private NotifyOrderParameters updateRequest;
    private MigrationSchema.State state;

    @Before
    public void before() {
        dbCampaignData = new CampaignDataForNotifyOrder()
                .withWalletId(1L)
                .withCmsChipsCost(DB_CHIPS_COST);

        updateRequest = new NotifyOrderParameters()
                .withChipsCost(UPDATE_CHIPS_COST);

        state = MigrationSchema.State.NEW;
    }

    @Test
    public void walletTotalSpentShouldBeUpdated_NewSchemaOnWalletNotEquelsCost_True() {
        assertTrue(walletTotalSpentShouldBeUpdated(dbCampaignData, updateRequest, state));
    }

    @Test
    public void walletTotalSpentShouldBeUpdated_OldSchema_False() {
        assertFalse(walletTotalSpentShouldBeUpdated(dbCampaignData, updateRequest, MigrationSchema.State.OLD));
    }

    @Test
    public void walletTotalSpentShouldBeUpdated_EqualsCost_False() {
        assertFalse(walletTotalSpentShouldBeUpdated(dbCampaignData, updateRequest.withChipsCost(DB_CHIPS_COST), state));
    }

    @Test
    public void walletTotalSpentShouldBeUpdated_CampaignNotUnderWallet_False() {
        assertFalse(walletTotalSpentShouldBeUpdated(dbCampaignData.withWalletId(0L), updateRequest, state));
    }
}
