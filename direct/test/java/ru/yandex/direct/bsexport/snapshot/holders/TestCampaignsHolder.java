package ru.yandex.direct.bsexport.snapshot.holders;

import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;

public class TestCampaignsHolder extends CampaignsHolder {
    public TestCampaignsHolder() {
        //noinspection ConstantConditions
        super(null, null);
    }

    @Override
    protected void checkInitialized() {
    }

    public void put(CommonCampaign campaign) {
        Long campaignId = campaign.getId();
        put(campaignId, campaign);
    }

    public void removeExternal(Long campaignId) {
        remove(campaignId);
    }
}
