package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.dbutil.model.ClientId;

public class TypedCampaignInfo {
    private ClientInfo clientInfo;
    private UserInfo operatorInfo;
    private CommonCampaign campaign;

    public TypedCampaignInfo(UserInfo operatorInfo, ClientInfo clientInfo, CommonCampaign campaign) {
        this.operatorInfo = operatorInfo;
        this.clientInfo = clientInfo;
        this.campaign = campaign;
    }

    public Long getId() {
        return campaign.getId();
    }

    public ClientId getClientId() {
        return clientInfo.getClientId();
    }

    public Long getUid() {
        return clientInfo.getUid();
    }

    public Long getOperatorUid() {
        return operatorInfo.getUid();
    }

    public Integer getShard() {
        return clientInfo.getShard();
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public UserInfo getOperatorInfo() {
        return operatorInfo;
    }

    public void setOperatorInfo(UserInfo operatorInfo) {
        this.operatorInfo = operatorInfo;
    }

    public CommonCampaign getCampaign() {
        return campaign;
    }

    public void setCampaign(CommonCampaign campaign) {
        this.campaign = campaign;
    }

    public CampaignInfo toCampaignInfo() {
        return CampaignInfoConverter.toCampaignInfo(clientInfo, campaign);
    }

}
