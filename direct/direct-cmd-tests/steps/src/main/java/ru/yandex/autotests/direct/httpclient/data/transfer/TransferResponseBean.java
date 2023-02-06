package ru.yandex.autotests.direct.httpclient.data.transfer;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 16.06.15
 */
public class TransferResponseBean {

    @JsonPath(responsePath = "campaigns_from/cid")
    private List<String> campaignsFromIds;

    @JsonPath(responsePath = "campaigns_to/cid")
    private List<String> campaignsToIds;

    public List<String> getCampaignsFromIds() {
        return campaignsFromIds;
    }

    public void setCampaignsFromIds(List<String> campaignsFromIds) {
        this.campaignsFromIds = campaignsFromIds;
    }

    public List<String> getCampaignsToIds() {
        return campaignsToIds;
    }

    public void setCampaignsToIds(List<String> campaignsToIds) {
        this.campaignsToIds = campaignsToIds;
    }
}
