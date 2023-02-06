package ru.yandex.direct.bsexport.snapshot.holders;

import ru.yandex.direct.bsexport.snapshot.model.QueuedCampaign;

public class TestQueuedCampaignsHolder extends QueuedCampaignsHolder {
    public TestQueuedCampaignsHolder() {
        //noinspection ConstantConditions
        super(null, null, null, null);
    }

    @Override
    protected void checkInitialized() {
    }

    public void put(QueuedCampaign campaign) {
        Long campaignId = campaign.getId();
        put(campaignId, campaign);
    }

    public void removeExternal(Long campaignId) {
        remove(campaignId);
    }
}
