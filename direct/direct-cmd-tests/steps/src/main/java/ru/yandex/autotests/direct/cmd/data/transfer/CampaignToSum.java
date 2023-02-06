package ru.yandex.autotests.direct.cmd.data.transfer;

import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class CampaignToSum {
    private String objectId;

    @SerializeKey("to__")
    private String campaignToSum;

    public String getObjectId() {
        return objectId;
    }

    public CampaignToSum withObjectId(String objectId) {
        this.objectId = objectId;
        return this;
    }

    public String getCampaignToSum() {
        return campaignToSum;
    }

    public CampaignToSum withCampaignToSum(String campaignToSum) {
        this.campaignToSum = campaignToSum;
        return this;
    }
}
