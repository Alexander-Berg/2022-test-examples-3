package ru.yandex.autotests.direct.httpclient.data.campaigns.editcamp;

import ru.yandex.autotests.direct.httpclient.data.campaigns.campaignInfo.CampaignInfoCmd;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 01.04.15
 */
public class EditCampResponse {

    @JsonPath(responsePath = "campaign")
    private CampaignInfoCmd campaign;

    @JsonPath(responsePath = "sms_phone")
    private String smsPhone;

    @JsonPath(responsePath = "easy_direct")
    private String easyDirect;

    public CampaignInfoCmd getCampaign() {
        return campaign;
    }

    public void setCampaign(CampaignInfoCmd campaign) {
        this.campaign = campaign;
    }

    public String getSmsPhone() {
        return smsPhone;
    }

    public void setSmsPhone(String smsPhone) {
        this.smsPhone = smsPhone;
    }

    public String getEasyDirect() {
        return easyDirect;
    }

    public void setEasyDirect(String easyDirect) {
        this.easyDirect = easyDirect;
    }
}
