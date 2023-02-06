package ru.yandex.autotests.direct.httpclient.data.campaigns;

import ru.yandex.autotests.direct.httpclient.data.campaigns.campaignInfo.CampaignInfoCmd;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 09.06.15
 */
public class ShowCampsResponseBean {

    @JsonPath(responsePath = "campaigns_by_type/text/all")
    private List<CampaignInfoCmd> campaigns;

    public List<CampaignInfoCmd> getCampaigns() {
        return campaigns;
    }

    public void setCampaigns(List<CampaignInfoCmd> campaigns) {
        this.campaigns = campaigns;
    }
}
