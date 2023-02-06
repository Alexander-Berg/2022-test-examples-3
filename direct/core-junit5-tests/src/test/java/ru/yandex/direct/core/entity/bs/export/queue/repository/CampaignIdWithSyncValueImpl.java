package ru.yandex.direct.core.entity.bs.export.queue.repository;

import ru.yandex.direct.core.entity.bs.export.model.CampaignIdWithSyncValue;

public class CampaignIdWithSyncValueImpl implements CampaignIdWithSyncValue {
    private Long campaignId;
    private Long syncValue;

    public CampaignIdWithSyncValueImpl(long campaignId, long syncValue) {
        this.campaignId = campaignId;
        this.syncValue = syncValue;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    @Override
    public Long getSynchronizeValue() {
        return syncValue;
    }
}
