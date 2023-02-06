package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.campaign.model.Dialog;

import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

public class DialogInfo {
    private CampaignInfo campaignInfo = new CampaignInfo();
    private Dialog dialog;

    public CampaignInfo getCampaignInfo() {
        return campaignInfo;
    }

    public DialogInfo withCampaignInfo(CampaignInfo campaignInfo) {
        this.campaignInfo = campaignInfo;
        return this;
    }

    public Dialog getDialog() {
        return dialog;
    }

    public DialogInfo withDialog(Dialog dialog) {
        this.dialog = dialog;
        return this;
    }

    public Long getCampaignId() {
        return ifNotNull(campaignInfo, CampaignInfo::getCampaignId);
    }

    public Integer getShard() {
        return getCampaignInfo().getShard();
    }
}
