package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.dbutil.model.ClientId;

public class VcardInfo {

    private CampaignInfo campaignInfo = new CampaignInfo();
    private Vcard vcard;

    public CampaignInfo getCampaignInfo() {
        return campaignInfo;
    }

    public VcardInfo withCampaignInfo(CampaignInfo campaignInfo) {
        this.campaignInfo = campaignInfo;
        return this;
    }

    public ClientInfo getClientInfo() {
        return getCampaignInfo().getClientInfo();
    }

    public VcardInfo withClientInfo(ClientInfo clientInfo) {
        getCampaignInfo().withClientInfo(clientInfo);
        return this;
    }

    public Vcard getVcard() {
        return vcard;
    }

    public VcardInfo withVcard(Vcard vcard) {
        this.vcard = vcard;
        return this;
    }

    public Long getVcardId() {
        return vcard.getId();
    }

    public Long getCampaignId() {
        return getCampaignInfo().getCampaignId();
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
}
