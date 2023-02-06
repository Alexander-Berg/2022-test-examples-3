package ru.yandex.autotests.direct.cmd.data.commons.adjustment;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.httpclient.data.campaigns.adjustment.DemographyAgeEnum;
import ru.yandex.autotests.direct.httpclient.data.campaigns.adjustment.DemographyGenderEnum;

import java.util.ArrayList;
import java.util.List;

public class DemographyMultiplier {

    @SerializedName("is_enabled")
    private Integer enabled;

    private List<DemographyCondition> conditions;

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public List<DemographyCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<DemographyCondition> conditions) {
        this.conditions = conditions;
    }

    public DemographyMultiplier withConditions(List<DemographyCondition> conditions){
        this.conditions = conditions;
        return this;
    }

    public DemographyMultiplier withEnabled(Integer enabled){
        this.enabled = enabled;
        return this;
    }

    public static DemographyMultiplier getDefaultDemographyMultiplier(String multiplierPct) {
        List<DemographyCondition> demographyConditionList = new ArrayList<>();
        demographyConditionList.add(new DemographyCondition().
                withGender(DemographyGenderEnum.FEMALE.getKey()).
                withAge(DemographyAgeEnum.BETWEEN_0_AND_17.getKey()).
                withMultiplierPct(multiplierPct));
        return new DemographyMultiplier().
                withEnabled(1).
                withConditions(demographyConditionList);
    }
}
