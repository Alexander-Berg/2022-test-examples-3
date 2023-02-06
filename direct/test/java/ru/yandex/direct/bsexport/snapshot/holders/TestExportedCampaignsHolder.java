package ru.yandex.direct.bsexport.snapshot.holders;

import ru.yandex.direct.bsexport.snapshot.model.ExportedCampaign;

public class TestExportedCampaignsHolder extends ExportedCampaignsHolder {
    public TestExportedCampaignsHolder() {
        //noinspection ConstantConditions
        super(null, null);
    }

    @Override
    protected void checkInitialized() {
    }

    public void put(ExportedCampaign campaign) {
        Long campaignId = campaign.getId();
        put(campaignId, campaign);
    }

    public void removeExternal(Long campaignId) {
        remove(campaignId);
    }
}
