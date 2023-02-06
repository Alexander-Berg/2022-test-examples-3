package ru.yandex.direct.bsexport.snapshot;

import org.junit.jupiter.api.BeforeEach;

import ru.yandex.direct.bsexport.snapshot.holders.TestBillingAggregatesHolder;
import ru.yandex.direct.bsexport.snapshot.holders.TestCampaignsHolder;
import ru.yandex.direct.bsexport.snapshot.holders.TestClientsHolder;
import ru.yandex.direct.bsexport.snapshot.holders.TestExportedCampaignsHolder;
import ru.yandex.direct.bsexport.snapshot.holders.TestInternalAdsProductHolder;
import ru.yandex.direct.bsexport.snapshot.holders.TestQueuedCampaignsHolder;
import ru.yandex.direct.bsexport.snapshot.holders.TestUsersHolder;
import ru.yandex.direct.bsexport.snapshot.model.ExportedCampaign;
import ru.yandex.direct.bsexport.snapshot.model.ExportedClient;
import ru.yandex.direct.bsexport.snapshot.model.ExportedUser;
import ru.yandex.direct.bsexport.snapshot.model.QueuedCampaign;
import ru.yandex.direct.core.entity.campaign.model.BillingAggregateCampaign;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsProduct;
import ru.yandex.direct.core.entity.product.model.ProductType;

import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;

public class BsExportSnapshotTestBase {

    private final TestQueuedCampaignsHolder queuedCampaignsHolder = new TestQueuedCampaignsHolder();
    private final TestCampaignsHolder campaignsHolder = new TestCampaignsHolder();
    private final TestExportedCampaignsHolder exportedCampaignsHolder = new TestExportedCampaignsHolder();
    private final TestClientsHolder clientsHolder = new TestClientsHolder();
    private final TestUsersHolder usersHolder = new TestUsersHolder();
    private final TestInternalAdsProductHolder internalAdsProductsHolder = new TestInternalAdsProductHolder();
    private final TestBillingAggregatesHolder billingAggregatesHolder = new TestBillingAggregatesHolder();

    protected BsExportSnapshot snapshot;

    @BeforeEach
    void prepareMocksAndSnapshot() {
        snapshot = new BsExportSnapshot.Builder()
                .withQueuedCampaignsHolder(queuedCampaignsHolder)
                .withCampaignsHolder(campaignsHolder)
                .withExportedCampaignsHolder(exportedCampaignsHolder)
                .withClientsHolder(clientsHolder)
                .withUsersHolder(usersHolder)
                .withInternalAdsProductsHolder(internalAdsProductsHolder)
                .withBillingAggregatesHolder(billingAggregatesHolder)
                .buildForTests();
    }

    protected void putQueuedCampaignToSnapshot(QueuedCampaign campaign) {
        queuedCampaignsHolder.put(campaign);
    }

    protected void removeQueuedCampaignFromSnapshot(Long campaignId) {
        queuedCampaignsHolder.removeExternal(campaignId);
    }

    protected void putCampaignToSnapshot(CommonCampaign campaign) {
        campaignsHolder.put(campaign);
    }

    protected void removeCampaignFromSnapshot(Long campaignId) {
        campaignsHolder.removeExternal(campaignId);
    }

    protected void putExportedCampaignToSnapshot(ExportedCampaign campaign) {
        exportedCampaignsHolder.put(campaign);
    }

    protected void removeExportedCampaignFromSnapshot(Long campaignId) {
        exportedCampaignsHolder.removeExternal(campaignId);
    }

    protected void putClientToSnapshot(ExportedClient client) {
        clientsHolder.put(client);
    }

    protected void putInternalAdsProductToSnapshot(InternalAdsProduct internalAdsProduct) {
        internalAdsProductsHolder.put(internalAdsProduct);
    }

    protected void putUserToSnapshot(ExportedUser user) {
        usersHolder.put(user);
    }

    protected void putBillingAggregateToSnapshot(ProductType productType,
                                                 BillingAggregateCampaign billingAggregateCampaign) {
        billingAggregatesHolder.put(productType, billingAggregateCampaign);
    }

    protected CommonCampaign createCampaign() {
        long campaignId = nextPositiveLong();
        return new TextCampaign()
                .withId(campaignId);
    }

    protected long getCampaignId() {
        return nextPositiveLong();
    }
}
