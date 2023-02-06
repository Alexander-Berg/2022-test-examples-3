package ru.yandex.autotests.direct.cmd.data.editdynamicadgroups;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.Campaign;

public class EditDynamicAdGroupsResponse {

    @SerializedName("campaign")
    private Campaign campaign;

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }
}
