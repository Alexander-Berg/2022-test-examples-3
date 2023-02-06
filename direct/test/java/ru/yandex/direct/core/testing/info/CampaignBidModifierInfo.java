package ru.yandex.direct.core.testing.info;

import java.util.List;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public class CampaignBidModifierInfo {
    private CampaignInfo campaignInfo = new CampaignInfo();
    private List<BidModifier> bidModifiers;

    public CampaignInfo getCampaignInfo() {
        return campaignInfo;
    }

    public CampaignBidModifierInfo withCampaignInfo(CampaignInfo campaignInfo) {
        this.campaignInfo = campaignInfo;
        return this;
    }

    public List<BidModifier> getBidModifiers() {
        return bidModifiers;
    }

    public BidModifier getBidModifier() {
        if (isEmpty(bidModifiers)) {
            return null;
        }
        return bidModifiers.get(0);
    }

    public CampaignBidModifierInfo withBidModifiers(List<BidModifier> bidModifiers) {
        this.bidModifiers = bidModifiers;
        return this;
    }

    public Long getCampaignId() {
        return campaignInfo.getCampaignId();
    }

    public Long getUid() {
        return campaignInfo.getUid();
    }

    public ClientId getClientId() {
        return campaignInfo.getClientId();
    }

    public Integer getShard() {
        return campaignInfo.getShard();
    }
}
