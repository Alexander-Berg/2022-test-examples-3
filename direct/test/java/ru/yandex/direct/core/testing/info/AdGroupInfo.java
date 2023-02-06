package ru.yandex.direct.core.testing.info;

import java.util.Optional;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.dbutil.model.ClientId;

import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

public class AdGroupInfo {

    private CampaignInfo campaignInfo = new CampaignInfo();
    private AdGroup adGroup;

    public CampaignInfo getCampaignInfo() {
        return campaignInfo;
    }

    public AdGroupInfo withCampaignInfo(CampaignInfo campaignInfo) {
        this.campaignInfo = campaignInfo;
        return this;
    }

    public ClientInfo getClientInfo() {
        return getCampaignInfo().getClientInfo();
    }

    public AdGroupInfo withClientInfo(ClientInfo clientInfo) {
        getCampaignInfo().withClientInfo(clientInfo);
        return this;
    }

    public AdGroup getAdGroup() {
        return adGroup;
    }

    public AdGroupInfo withAdGroup(AdGroup adGroup) {
        this.adGroup = adGroup;
        return this;
    }

    public Long getAdGroupId() {
        return Optional.ofNullable(adGroup).map(AdGroup::getId).orElse(null);
    }

    public AdGroupType getAdGroupType() {
        return adGroup.getType();
    }

    public Long getCampaignId() {
        return ifNotNull(campaignInfo, CampaignInfo::getCampaignId);
    }

    public Long getUid() {
        return getCampaignInfo().getUid();
    }

    public ClientId getClientId() {
        return getCampaignInfo().getClientId();
    }

    public Integer getShard() {
        return getCampaignInfo().getShard();
    }

    public boolean isExists() {
        return getAdGroupId() != null;
    }
}
