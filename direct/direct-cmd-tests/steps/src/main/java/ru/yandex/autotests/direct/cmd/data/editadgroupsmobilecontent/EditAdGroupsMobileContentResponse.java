package ru.yandex.autotests.direct.cmd.data.editadgroupsmobilecontent;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.Campaign;

public class EditAdGroupsMobileContentResponse {

    @SerializedName("campaign")
    private Campaign campaign;

    @SerializedName("has_errors")
    private String hasErrors;

    public Campaign getCampaign() {
        return campaign;
    }

    public String getHasErrors() {
        return hasErrors;
    }
}
