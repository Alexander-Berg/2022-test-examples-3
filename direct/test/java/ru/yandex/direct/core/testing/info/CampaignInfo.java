package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.model.ClientId;

import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

public class CampaignInfo {

    public CampaignInfo() {
        this(new ClientInfo());
    }

    public CampaignInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public CampaignInfo(ClientInfo clientInfo, Campaign campaign) {
        this.clientInfo = clientInfo;
        this.campaign = campaign;
    }

    private ClientInfo clientInfo;
    private Campaign campaign;

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public CampaignInfo withClientInfo(ClientInfo clientInfo) {
        setClientInfo(clientInfo);
        return this;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public CampaignInfo withCampaign(Campaign campaign) {
        setCampaign(campaign);
        return this;
    }

    public Long getCampaignId() {
        return ifNotNull(campaign, Campaign::getId);
    }

    public Long getOrderId() {
        return ifNotNull(campaign, Campaign::getOrderId);
    }

    public Long getUid() {
        return getClientInfo().getUid();
    }

    public ClientId getClientId() {
        return getClientInfo().getClientId();
    }

    public Integer getShard() {
        return getClientInfo().getShard();
    }
}
