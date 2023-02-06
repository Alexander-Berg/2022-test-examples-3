package ru.yandex.autotests.direct.cmd.data.commons.adjustment;

import com.google.gson.annotations.SerializedName;

public class DemographyCondition {

    @SerializedName("age")
    private String age;
    @SerializedName("gender")
    private String gender;
    @SerializedName("multiplier_pct")
    private String multiplierPct;

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMultiplierPct() {
        return multiplierPct;
    }

    public void setMultiplierPct(String multiplierPct) {
        this.multiplierPct = multiplierPct;
    }

    public DemographyCondition withAge(String age){
        this.age = age;
        return this;
    }

    public DemographyCondition withGender(String gender){
        this.gender = gender;
        return this;
    }

    public DemographyCondition withMultiplierPct(String multiplierPct){
        this.multiplierPct = multiplierPct;
        return this;
    }
}
