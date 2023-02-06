package ru.yandex.autotests.direct.cmd.data.transfer;

import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class CampaignFromSum {

    private String objectId;

    @SerializeKey("from__")
    private String campaignFromSum;

    public String getObjectId() {
        return objectId;
    }

    public CampaignFromSum withObjectId(String objectId) {
        this.objectId = objectId;
        return this;
    }

    public String getCampaignFromSum() {
        return campaignFromSum;
    }

    public CampaignFromSum withCampaignFromSum(String campaignFromSum) {
        this.campaignFromSum = campaignFromSum;
        return this;
    }
}
