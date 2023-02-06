package ru.yandex.direct.core.testing.info;

import java.util.Collections;
import java.util.List;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

public class AdGroupBidModifierInfo {

    private AdGroupInfo adGroupInfo = new AdGroupInfo();
    private List<BidModifier> bidModifiers;

    public BidModifier getBidModifier() {
        if (isEmpty(bidModifiers)) {
            return null;
        }
        return bidModifiers.get(0);
    }

    public AdGroupBidModifierInfo withBidModifier(BidModifier bidModifier) {
        this.bidModifiers = Collections.singletonList(bidModifier);
        return this;
    }

    public AdGroupInfo getAdGroupInfo() {
        return adGroupInfo;
    }

    public AdGroupBidModifierInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        this.adGroupInfo = adGroupInfo;
        return this;
    }

    public CampaignInfo getCampaignInfo() {
        return getAdGroupInfo().getCampaignInfo();
    }

    public AdGroupBidModifierInfo withCampaignInfo(CampaignInfo campaignInfo) {
        getAdGroupInfo().withCampaignInfo(campaignInfo);
        return this;
    }

    public ClientInfo getClientInfo() {
        return getCampaignInfo().getClientInfo();
    }

    public AdGroupBidModifierInfo withClientInfo(ClientInfo clientInfo) {
        getAdGroupInfo().withClientInfo(clientInfo);
        return this;
    }

    public Long getBidModifierId() {
        return ifNotNull(getBidModifier(), BidModifier::getId);
    }

    public Long getAdGroupId() {
        return getAdGroupInfo().getAdGroupId();
    }

    public Long getCampaignId() {
        return getAdGroupInfo().getCampaignId();
    }

    public Long getUid() {
        return getAdGroupInfo().getUid();
    }

    public ClientId getClientId() {
        return getAdGroupInfo().getClientId();
    }

    public Integer getShard() {
        return getAdGroupInfo().getShard();
    }

    public List<BidModifier> getBidModifiers() {
        return bidModifiers;
    }

    public AdGroupBidModifierInfo withBidModifiers(List<BidModifier> bidModifiers) {
        this.bidModifiers = bidModifiers;
        return this;
    }
}
