package ru.yandex.autotests.direct.httpclient.data.campaigns.editcamp;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

public class EditCampParameters extends BasicDirectFormParameters {

    @FormParameter("new_camp")
    private String new_camp;
    @FormParameter("mediaType")
    private String mediaType;
    @FormParameter("cid")
    private String campaignId;

    public String getNew_camp() {
        return new_camp;
    }

    public void setNew_camp(String new_camp) {
        this.new_camp = new_camp;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }


    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }
}
