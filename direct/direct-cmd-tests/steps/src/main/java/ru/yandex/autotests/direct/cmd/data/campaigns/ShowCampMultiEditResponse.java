package ru.yandex.autotests.direct.cmd.data.campaigns;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.Campaign;
import ru.yandex.autotests.direct.cmd.data.interest.InterestCategory;

import java.util.List;

public class ShowCampMultiEditResponse {

    @SerializedName("campaign")
    private Campaign campaign;

    @SerializedName("has_errors")
    private String hasErrors;

    @SerializedName("interest_categories")
    private List<InterestCategory> interestCategories;

    @SerializedName("is_featureTurboLandingEnabled")
    private Integer isFeatureTurboLandingEnabled;

    public List<InterestCategory> getInterestCategories() {
        return interestCategories;
    }

    public ShowCampMultiEditResponse withInterestCategories(List<InterestCategory> interestCategories) {
        this.interestCategories = interestCategories;
        return this;
    }


    public Campaign getCampaign() {
        return campaign;
    }

    public String getHasErrors() {
        return hasErrors;
    }

    public Integer getIsFeatureTurboLandingEnabled() {
        return isFeatureTurboLandingEnabled;
    }
}
