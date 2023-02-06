package ru.yandex.autotests.direct.httpclient.data.payment.payForAll;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 16.06.15
 */
public class PayForAllRequestBean extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "cid")
    private String campaignId;

    @JsonPath(requestPath = "sums_with_nds")
    private String sumsWithNds;

    private List<CampaignSumRequestBean> campaignSums;

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getSumsWithNds() {
        return sumsWithNds;
    }

    public void setSumsWithNds(String sumsWithNds) {
        this.sumsWithNds = sumsWithNds;
    }

    public List<CampaignSumRequestBean> getCampaignSums() {
        if(campaignSums == null) {
            campaignSums = new ArrayList<CampaignSumRequestBean>();
        }
        return campaignSums;
    }

    public void setCampaignSums(List<CampaignSumRequestBean> campaignSums) {
        this.campaignSums = campaignSums;
    }



    public void addCampaignSum(CampaignSumRequestBean campaignSum) {
        getCampaignSums().add(campaignSum);
    }
}
