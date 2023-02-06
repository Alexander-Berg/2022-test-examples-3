package ru.yandex.autotests.direct.httpclient.data.campaigns;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 09.06.15
 */
public class DelCampRequestBean extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "cid")
    private String campaignId;

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }
}
